package network;

import java.net.SocketAddress;
import java.util.Arrays;
import java.util.LinkedList;

import library.GameInput;
import library.IPollSink;
import library.Poll;
import library.RingBuffer;
import network.UdpMsg.ConnectStatus;
import network.UdpMsg.MsgType;
import server.Server;
import server.TimeSync;

public class UdpProto implements IPollSink {
//	private static final boolean DEBUG = false;
    private static final int NUM_SYNC_PACKETS = 5;
	private static final int MAX_SEQ_DISTANCE = 32768;
	private static final int QUALITY_REPORT_INTERVAL = 500000000;
    private static int playerNumber = 1;
    public static int numConnections = 0;
    
    private int next_send_seq;
    private RingBuffer<QueueEntry> send_queue;
    private RingBuffer<UdpProtocolEvent> event_queue;
    private RingBuffer<GameInput> pending_output;
    private LinkedList<GameInput> input_queue;
    private SocketAddress client_addr;
    private Udp udp;
    private int disconnect_timeout;
    private int disconnect_notify_start;
    private State state;
    private ConnectState current_state;
    private boolean isInitialized;
	private int next_recv_seq;
	private int local_frame_adv;
	private int remote_frame_advantage;
	private int local_frame_advantage;
    private TimeSync timeSync;
	private long round_trip_time;
	private GameInput last_received_input;
	public boolean startRequest;
	
	private UdpMsg.ConnectStatus remote_connect_status;
    
	public UdpProto() {
		isInitialized = false;
		timeSync = new TimeSync();
        remote_connect_status = new UdpMsg.ConnectStatus();
	}
    
    public void init(Udp udp, Poll poll, SocketAddress client_addr, 
    		int portNum, int disconnect_timeout, int disconnect_notify_start,
    		UdpMsg msg) {
    	this.udp = udp;
        this.disconnect_timeout = disconnect_timeout;
        this.disconnect_notify_start = disconnect_notify_start;
        this.client_addr = client_addr;
        send_queue = new RingBuffer<>(64);
        event_queue = new RingBuffer<>(64);
        pending_output = new RingBuffer<>(128);
        input_queue = new LinkedList<>();
        
        next_send_seq = 0;
        poll.registerLoop(this);

        state = new State();
        
        if(udp != null) {
        	current_state = ConnectState.Syncing;
        	state.sync.round_trips_remaining = NUM_SYNC_PACKETS;
        	onConnectReq(msg);
        }
        
		numConnections++;
        isInitialized = true;
    }

	private void sendMsg(UdpMsg msg) {
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
            handled = onStartReq(msg);
            break;
        case 4:
            handled = onStartRep(msg);
            break;
        case 5:
            handled = onInput(msg);
            break;
        case 6:
            handled = onQualityReport(msg);
            break;
        case 7:
            handled = onQualityReply(msg);
		}
		
		int seq = msg.hdr.sequenceNumber;
		if(	msg.hdr.type != MsgType.ConnectReq.ordinal() && 
			msg.hdr.type != MsgType.ConnectReply.ordinal()) {
			// TODO: reject messages from senders we don't expect
		}
		
		int skipped = seq - next_recv_seq;
		if(skipped > MAX_SEQ_DISTANCE) {
			return;
		}
		
		next_recv_seq = seq;
		if(msg.hdr.type < 0 || msg.hdr.type > 3) {
			onInvalid(msg);
		}
		
		if(handled) {
			// TODO: handle network resumed events
			//		 if a disconnect notification has been sent and
			//		 if the current state is running
		}
	}

	private void onInvalid(UdpMsg msg) {
		// TODO Auto-generated method stub
		
	}

	private boolean onConnectReq(UdpMsg msg) {
		// TODO: make sure we only handle requests form sources we expect
		UdpMsg reply = new UdpMsg(MsgType.ConnectReply);
//		System.out.println(Arrays.toString(msg.getData()));
		reply.payload.connRep.random_reply = msg.payload.connReq.random_request;
		reply.payload.connRep.playerNumber = playerNumber++;
		sendMsg(reply);
		return true;
	}
	
	private boolean onConnectRep(UdpMsg msg) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean onStartReq(UdpMsg msg) {
		System.out.println("Can the client start?");
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
		GameInput input = new GameInput();
		input.frame = msg.payload.input.frame;
		input.input = msg.payload.input.input;
		input.connectStatus = new ConnectStatus(
			msg.payload.input.connect_status.disconnected,
			msg.payload.input.connect_status.last_frame);
		
//		UdpProtocolEvent event = new UdpProtocolEvent(UdpProtocolEvent.Event.Input);
//		event.input.input = new GameInput();
//		event.input.input.frame = msg.payload.input.frame;
//		event.input.input.input = msg.payload.input.input;
//		event_queue.push(event);
		
		input_queue.push(input);
		
		last_received_input = new GameInput();
		last_received_input.frame = msg.payload.input.frame;
		last_received_input.input = msg.payload.input.input;
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
				UdpMsg msg = new UdpMsg(UdpMsg.MsgType.Input);
				msg.payload.input.connect_status.disconnected = input.connectStatus.disconnected;
				msg.payload.input.connect_status.last_frame = input.connectStatus.last_frame;
				msg.payload.input.frame = input.frame;
				msg.payload.input.input = input.input;
				
				udp.sendTo(msg, dst);
			}
		}
	}
	
    @Override
    public boolean onLoopPoll(Object o) {
        if(udp == null) { return false; }
    	
        long now = System.nanoTime();
    	
        pumpSendQueue();
        // TODO: handle different states, such as, syncing, running, or disconnecting
        switch(current_state) {
            case Syncing:
                break;
            case Running:
            	if(state.running.last_quality_report_time <= 0 ||
	                state.running.last_quality_report_time +
	                QUALITY_REPORT_INTERVAL < now) {
	                UdpMsg msg = new UdpMsg(UdpMsg.MsgType.QualityReport);
	                msg.payload.qualrpt.ping = System.nanoTime();
	                msg.payload.qualrpt.frame_advantage = local_frame_advantage;
	                sendMsg(msg);
	                state.running.last_quality_report_time = now;
            	}
            	break;
            case Disconnected:
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
            int remoteFrame =
                    (int)(last_received_input.frame +
                            (round_trip_time * 60 / 1000));
            local_frame_advantage = remoteFrame - local_frame;		
        }
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

enum ConnectState {
    Syncing, Synchronized, Running, Disconnected
}
