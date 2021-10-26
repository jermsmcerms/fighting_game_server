package network;

import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.Random;

import library.ConnectState;
import library.GameInput;
import library.IPollSink;
import library.Poll;
import library.RingBuffer;
import network.UdpMsg.ConnectStatus;
import network.UdpMsg.MsgType;
import server.Server;
import server.TimeSync;

public class UdpProto implements IPollSink {
    private static final int NUM_SYNC_PACKETS = 5;
	private static final int MAX_SEQ_DISTANCE = 32768;
    private static final long SYNC_RETRY_INTERVAL       = 2000000000L;
    private static final long QUALITY_REPORT_INTERVAL   = 1000000000L; // 1000 ms = 1 billion ns
    private static final int SYNC_FIRST_RETRY_INTERVAL  = 500000000;
	private static final long UDP_SHUTDOWN_TIMER 		= 5000000000L;
	private static final long RUNNING_RETRY_INTERVAL 	= 200000000L;
	private static final long NETWORK_STATS_INTERVAL 	= 1000000000L;
    private static int playerNumber = 1;
    public static int numConnections = 0;
    
    private int next_send_seq;
    private int next_recv_seq;

    private RingBuffer<QueueEntry> send_queue;
    private RingBuffer<UdpProtocolEvent> event_queue;
    private RingBuffer<GameInput> pending_output;
    private LinkedList<GameInput> input_queue;
    private SocketAddress client_addr;
    private Udp udp;
    private final long disconnect_timeout = 3000000000L;
    private final long disconnect_notify_start = 1000000000L;
    private State state;
    private ConnectState current_state;
    private boolean isInitialized;
	private int remote_frame_advantage;
	private int local_frame_advantage;
    private TimeSync timeSync;
	private long round_trip_time;
	private GameInput last_received_input;
	public boolean startRequest;
	
	private UdpMsg.ConnectStatus remote_connect_status;
	public boolean connection_reply_sent;
	private int remote_magic_number;
	private boolean connected;
	private long last_send_time;
	private int magic_number;
	private boolean disconnect_notify_sent;
	private long last_recv_time;
	private long shutdown_timeout;
	private GameInput last_sent_input;
	private UdpMsg.ConnectStatus local_connect_status;
	private GameInput last_acked_input;
	private boolean disconnect_event_sent;
    
	public UdpProto() {
		isInitialized = false;
		timeSync = new TimeSync();
        remote_connect_status = new UdpMsg.ConnectStatus();
        current_state = ConnectState.Connecting;
	}
	
	public UdpProto(UdpMsg msg, Udp udp, Poll poll, SocketAddress client_addr, int magicNumber) {
		this.client_addr = client_addr;
		this.udp = udp;
		poll.registerLoop(this);
		pending_output = new RingBuffer<>(64);
		send_queue = new RingBuffer<>(64);
		event_queue = new RingBuffer<>(64);
		state = new State();
        current_state = ConnectState.Connecting;
        this.magic_number = magicNumber;
		onMsg(msg, 0);
		
		last_received_input = new GameInput(GameInput.NULL_FRAME, 0);
        last_sent_input = new GameInput(GameInput.NULL_FRAME, 0);
        last_acked_input = new GameInput(GameInput.NULL_FRAME, 0);
        
        timeSync = new TimeSync();
	}
    
	public void init() {
		if(udp != null) {
	    	state.sync.round_trips_remaining = NUM_SYNC_PACKETS;
			current_state = ConnectState.Syncing;
			sendSyncRequest();
			isInitialized = true;
		}
	}

	private void sendMsg(UdpMsg msg) {
		last_send_time = System.nanoTime();
		msg.hdr.magicNumber = magic_number;
		msg.hdr.sequenceNumber = next_send_seq++;
		send_queue.push(new QueueEntry(System.nanoTime(), client_addr, msg));
		pumpSendQueue();
	}

	private void pumpSendQueue() {
		while(!send_queue.empty()) {
			QueueEntry entry = send_queue.front();
			udp.sendTo(entry.msg, entry.dest_addr);
			send_queue.pop();
		}
	}

	public boolean getIsInitialized() { return isInitialized; }

	public boolean handlesMsg(SocketAddress from) {
		if(udp == null) return false;
		return from.equals(client_addr);
	}

	public void onMsg(UdpMsg msg, int queue) {
		boolean handled = false;
		System.out.println("msg type: " + UdpMsg.MsgType.values()[msg.hdr.type]);
		switch(msg.hdr.type) {
		case 0:
			onInvalid(msg);
			break;
		case 1:
			handled = onConnectReq(msg);
			break;
		case 2:
			handled = onConnectRep(msg);
			break;
		case 3: 
			handled = onSyncReq(msg);
			break;
		case 4:
			handled = onSyncRep(msg);
			break;
        case 5:
            handled = onStartReq(msg);
            break;
        case 6:
            handled = onStartRep(msg);
            break;
        case 7:
            handled = onInput(msg);
            break;
        case 8:
            handled = onQualityReport(msg);
            break;
        case 9:
            handled = onQualityReply(msg);
		}
		
		int seq = msg.hdr.sequenceNumber;
        if(	msg.hdr.type != UdpMsg.MsgType.ConnectReq.ordinal() &&
            msg.hdr.type != UdpMsg.MsgType.ConnectReply.ordinal() &&
            msg.hdr.type != UdpMsg.MsgType.SyncRequest.ordinal() &&
            msg.hdr.type != UdpMsg.MsgType.SyncReply.ordinal()) {
            if(msg.hdr.magicNumber != remote_magic_number) {
                System.out.println("rejecting message from unknown client");
                return;
            }
        }

        int skipped = seq - next_recv_seq;
        if(skipped > MAX_SEQ_DISTANCE) {
            System.out.println("Dropping out of order packet " +
                "(seq: " + seq + ", last seq: " + next_recv_seq);
            return;
        }

        next_recv_seq = seq;
        if(handled) {
            last_recv_time = System.nanoTime();
            if( disconnect_notify_sent && 
                current_state == ConnectState.Running) {
                event_queue.push(new UdpProtocolEvent(
                    UdpProtocolEvent.Event.NetworkResumed));
                disconnect_notify_sent = false;
            }
        }
	}

	private void onInvalid(UdpMsg msg) {
		
	}

	private boolean onConnectReq(UdpMsg msg) {
		System.out.println("connect request received");
		UdpMsg reply = new UdpMsg(MsgType.ConnectReply);
		reply.payload.connRep.random_reply = msg.payload.connReq.random_request;
		reply.payload.connRep.playerNumber = playerNumber++;
		sendMsg(reply);
		connection_reply_sent = true;
		current_state = ConnectState.Connected;
		return true;
	}
	
	private boolean onConnectRep(UdpMsg msg) {
		return false;
	}

	private boolean onSyncReq(UdpMsg msg) {
		if(remote_magic_number > 0 && msg.hdr.magicNumber != remote_magic_number) {
            System.out.println("ignoring sync request from unknown endpoint");
            return false;
        }		
		
        UdpMsg reply = new UdpMsg(UdpMsg.MsgType.SyncReply);
        reply.payload.syncRep.random_reply = msg.payload.syncReq.random_request;
        sendMsg(reply);
        return true;	
    }
	
	private boolean onSyncRep(UdpMsg msg) {
		if(current_state != ConnectState.Syncing) {
            System.out.println("ignoring sync reply while not synchronizing");
            return msg.hdr.magicNumber == remote_magic_number;
        }
        
        if(msg.payload.syncRep.random_reply != state.sync.random) {
            System.out.println("sync reply not quite right. keep looking");
            return false;
        }
        
        if(!connected) {
            event_queue.push(new UdpProtocolEvent(UdpProtocolEvent.Event.Connected));
            connected = true;
        }
        
        System.out.println("checking sync state (" + state.sync.round_trips_remaining + " round trips remaining)");
        if(--state.sync.round_trips_remaining == 0) {
            System.out.println("Synchronized!");
            event_queue.push(new UdpProtocolEvent(UdpProtocolEvent.Event.Synchronized));
            current_state = ConnectState.Running;
            last_received_input = new GameInput(-1,-1);
            remote_magic_number = msg.hdr.magicNumber;
        } else {
            UdpProtocolEvent event = new UdpProtocolEvent(UdpProtocolEvent.Event.Synchronizing);
            event.syncing.total = NUM_SYNC_PACKETS;
            event.syncing.count = NUM_SYNC_PACKETS - state.sync.round_trips_remaining;
            event_queue.push(event);
            sendSyncRequest();
        }
        return true;	}
	
	private void sendSyncRequest() {
		Random rand = new Random();
        state.sync.random = rand.nextInt() & 0xFFFF;
        UdpMsg msg = new UdpMsg(UdpMsg.MsgType.SyncRequest);
        msg.payload.syncReq.random_request = state.sync.random;
        sendMsg(msg);
	}

	private boolean onStartReq(UdpMsg msg) {
		if(current_state == ConnectState.Syncing) {
			current_state = ConnectState.Running;
		}
		UdpMsg reply = new UdpMsg(MsgType.StartRep);
		if(numConnections == Server.NUM_PLAYERS) {
			reply.payload.startRep.response = 1;
		} else {
			reply.payload.startRep.response  = 0;
		}
		
		sendMsg(reply);
        return true;
    }

    private boolean onStartRep(UdpMsg msg) {
        return true;
    }
    
	private boolean onInput(UdpMsg msg) {
         System.out.println("processing input");
        boolean disconnect_requested = msg.payload.input.disconnect_requested;
        if(disconnect_requested) {
            if(current_state != ConnectState.Disconnected && !disconnect_event_sent) {
                System.out.println("Disconnecting endpoint on remote request");
                event_queue.push(new UdpProtocolEvent(UdpProtocolEvent.Event.Disconnected));
                disconnect_event_sent = true;
            }
        } else {
            UdpMsg.ConnectStatus remote_status = msg.payload.input.connect_status;
            // TODO: update peer connect status "array" here:
        }

        int last_received_frame_number = last_received_input.frame;
        if(msg.payload.input.num_inputs > 0) {
            int offset = 0;
            int num_inputs = msg.payload.input.num_inputs;
            int current_frame = msg.payload.input.start_frame;
            int[] inputs = new int[num_inputs];
            System.arraycopy(msg.payload.input.inputs, 0, inputs, 0, inputs.length);

            for(int input : inputs) {
                boolean useInputs = current_frame == last_received_input.frame + 1;
                last_received_input.input = input;;

                if(useInputs) {
                    last_received_input.frame = current_frame;
//                    UdpProtocolEvent event = new UdpProtocolEvent(UdpProtocolEvent.Event.Input);
//                    event.input.input = new GameInput(last_received_input.frame, last_received_input.input);
//                    event_queue.push(event);
                    System.out.println("Sending frame: " + last_received_input.frame + " to emulator queue. (but not really)");
                    state.running.last_input_packet_recv_time = System.nanoTime();
                } else {
                    System.out.println("Skipping past frame: " + last_received_input.frame + " current is " +  current_frame);
                }

                current_frame++;
            }
        }

        if(pending_output.front() != null) {
            System.out.println("pending output front frame " + pending_output.front().frame);
            System.out.println("last acked frame: " + msg.payload.input.ack_frame);
        }
        System.out.println("pending output size: " + pending_output.size());
        while(pending_output.size() > 0 && pending_output.front().frame < msg.payload.input.ack_frame) {
            System.out.println("removing pending output frame: " + pending_output.front().frame);
            last_acked_input = new GameInput(pending_output.front().frame, pending_output.front().frame);
            pending_output.pop();
        }
        return true;
	}

    private boolean onQualityReport(UdpMsg msg) {
        UdpMsg reply = new UdpMsg(UdpMsg.MsgType.QualityReply);
        reply.payload.qualrep.pong = msg.payload.qualrpt.ping;
        remote_frame_advantage = msg.payload.qualrpt.frame_advantage;
        sendMsg(reply);
        return true;
    }

    private boolean onQualityReply(UdpMsg msg) {
        round_trip_time = System.nanoTime() - msg.payload.qualrep.pong;
        return true;
    }
	
    public int getRemoteMagic() {
    	return remote_magic_number;
    }
    
	public SocketAddress getAddress() {
		return client_addr;
	}

	public UdpProtocolEvent getEvent() {
		if(event_queue.size() == 0) { return null; }
		UdpProtocolEvent event = event_queue.front();
		event_queue.pop();
		return event;
	}
	
	public GameInput getInput() {
		if(input_queue == null || input_queue.isEmpty()) { return null; }
		return input_queue.pop();
	}
	
	public void sendInput(GameInput input, SocketAddress dst) {
		if(udp != null) {
			if(current_state == ConnectState.Running) {
				pending_output.push(input);
			}
		}
		sendPendingOutput();
	}

	public void sendPendingOutput() {
		 UdpMsg msg = new UdpMsg(UdpMsg.MsgType.Input);
	        int i, j, offset = 0;
	        // TODO: something for inputs?

	        if(pending_output.size() > 0) {
	            msg.payload.input.start_frame = pending_output.front().frame;
	            for(j = 0; j < pending_output.size; j++) {
	                GameInput current = pending_output.item(j);
	                msg.payload.input.inputs[j] = current.input;
	                last_sent_input = current;
	            }
	        } else {
	            msg.payload.input.start_frame = 0;
	        }

	        msg.payload.input.ack_frame = last_received_input.frame;
	        msg.payload.input.num_inputs = pending_output.size();
	        msg.payload.input.disconnect_requested = current_state == ConnectState.Disconnected;
	        // Copy local connect status into msg.payload.input.connect_status
	        if(local_connect_status == null) {
	            local_connect_status = new UdpMsg.ConnectStatus();
	            local_connect_status.disconnected = false;
	            local_connect_status.last_frame = 0;
	        }

	        sendMsg(msg);
	}
	
    @Override
    public boolean onLoopPoll(Object o) {
        if(udp == null) { return false; }
        long now = System.nanoTime();
        long next_interval;
        pumpSendQueue();
        switch(current_state) {
            case Connecting:
                
                break;
            case Connected:
                // if connected, then we're waiting on another player. 
            	// Send first sync request.
            	System.out.println("sending first sync request");
                current_state = ConnectState.Syncing;
                state.sync.round_trips_remaining = NUM_SYNC_PACKETS;
                sendSyncRequest();
            	break;
            case Syncing:
            	next_interval = (state.sync.round_trips_remaining == NUM_SYNC_PACKETS) ? SYNC_FIRST_RETRY_INTERVAL : SYNC_RETRY_INTERVAL;
                if (last_send_time > 0 && last_send_time + next_interval < now) {
                    System.out.printf("No luck syncing after %d ms... Re-queueing sync packet.\n", next_interval);
                    sendSyncRequest();
                }
                break;
            case Running:
            	if( state.running.last_input_packet_recv_time <= 0 ||
                	state.running.last_input_packet_recv_time + 
                	RUNNING_RETRY_INTERVAL < now) {
            		System.out.println(
        				"haven't exchanged packets in a while (last received: " +
						last_received_input.frame + ", last sent: " +
						last_sent_input.frame + "). Resending");
            		sendPendingOutput();
            		state.running.last_input_packet_recv_time = now;
            	}
            	
            	if( state.running.last_quality_report_time <= 0 ||
                        state.running.last_quality_report_time +
                            QUALITY_REPORT_INTERVAL < now) {
                        UdpMsg msg = new UdpMsg(UdpMsg.MsgType.QualityReport);
                        msg.payload.qualrpt.ping = System.nanoTime();
                        msg.payload.qualrpt.frame_advantage = local_frame_advantage;
                        sendMsg(msg);
                        state.running.last_quality_report_time = now;
                    }

                    if( state.running.last_network_stats_interval <= 0 ||
                        state.running.last_network_stats_interval +
                            NETWORK_STATS_INTERVAL < now) {
                        System.out.println("Update network stats");
                        state.running.last_network_stats_interval = now;
                    }
            	
                break;
            case Disconnected:
            	if(shutdown_timeout < now) {
            		System.out.println("shutting down connections");
            		udp = null;
            		shutdown_timeout = 0;
            	}
                break;
		case Synchronized:
			break;
		default:
			break;
        }
        return true;
    }

	public UdpMsg.ConnectStatus getRemoteStatus() {
		return remote_connect_status;
	}

	public int recommendFrameDelay() {
        return timeSync.recommend_frame_wait_durration(false);
	}

	public void setLocalFrameNumber(int local_frame) {
        if(last_received_input != null) {
            long remote_frame =
                (last_received_input.frame +
                (round_trip_time * 60 / 1000000000L));
            local_frame_advantage = (int)(remote_frame - local_frame);
        }
	}

	public ConnectState getCurrentState() {
		return current_state;
	}

	public void disconnect() {
		System.out.println("Disconnecting endpoint");
		current_state = ConnectState.Disconnected;
		shutdown_timeout = System.nanoTime() + UDP_SHUTDOWN_TIMER;
	}

	public Udp getUdp() {
		return udp;
	}
	
}

class QueueEntry {
    public long queue_time;
    public SocketAddress dest_addr;
    UdpMsg msg;

    public QueueEntry(long queue_time, SocketAddress in_addr, UdpMsg msg) {
        this.queue_time = queue_time;
        this.dest_addr = in_addr;
        this.msg = msg;
    }
}

class State {
    public Sync sync;
    public Running running;
    public State() {
        sync = new Sync();
        running = new Running();
    }

    public class Sync {
        public int round_trips_remaining;
        public int random;
    }
    
    public class Running {
    	public long last_quality_report_time;
    	public long last_network_stats_interval;
    	public long last_input_packet_recv_time;
    }
}
