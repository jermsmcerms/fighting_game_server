package server;

import java.io.IOException;
import java.net.SocketAddress;

import library.ConnectState;
import library.GGPO;
import library.GGPOEventCode;
import library.GGPOSession;
import library.GameInput;
import library.GgpoEvent;
import library.Sync;
import network.UdpMsg;
import network.UdpProto;
import network.UdpProtocolEvent;
import tests.ServerBuilder;

public class SingleClientServer extends ServerBuilder implements GGPOSession {
	private static final int RECOMMENDATION_INTERVAL = 240;
	private int next_recommended_sleep;
	private Sync sync;
    private UdpMsg.ConnectStatus[] local_connect_status;
	private boolean synchronizing;
	private boolean connecting;

	public SingleClientServer(GGPO callbacks, int frame_delay) throws IOException {
		super();
		sync = new Sync();
		sync.setCallbacks(callbacks);
		sync.setFrameDelay(0, frame_delay);
		local_connect_status = new UdpMsg.ConnectStatus[2];
        for(int i = 0; i < local_connect_status.length; i++) {
        	local_connect_status[i] = new UdpMsg.ConnectStatus();
        	local_connect_status[i].disconnected = false;
        	local_connect_status[i].last_frame = -1;
        }
	}
	
	public ConnectState getConnectionState() {
		if(endpoints[0] == null) {
			return ConnectState.Invalid;
		}
		return endpoints[0].getCurrentState();
	}
	
	
	@Override
	public void onMsg(SocketAddress from, UdpMsg msg) {
		// For this test, I only want one end point on the server.
		// Note: 	This is b/c I want to test if the client is send 
		//			multiple inputs in a single message.
		for(int i = 0; i < endpoints.length - 1; i++) {
			if(endpoints[i] == null) {
				endpoints[i] = new UdpProto(msg, udp, poll, from, server_magic_number);
				return;
			}  else if(endpoints[i].handlesMsg(from)) {
				endpoints[i].onMsg(msg, 0);
				return;
			}
		}
	}

	@Override
	public boolean doPoll(long timeout) {
		if(!sync.isInRollback()) {
			poll.pump(0);

			if(	endpoints[0] != null && 
				endpoints[0].getCurrentState() == ConnectState.Running) {
				processUdpProtocolEvents();
				sync.checkSimulation(timeout);
				int current_frame = sync.getFrame();
				endpoints[0].setLocalFrameNumber(current_frame);
				
				int total_min_confirmed = poll2players(current_frame);
                if (total_min_confirmed >= 0) {
                    assert (total_min_confirmed != Integer.MAX_VALUE);
                    sync.setLastConfirmedFrame(total_min_confirmed);
                }
				
				if(current_frame > next_recommended_sleep) {
	                int interval =
	                    Math.max(0, endpoints[0].recommendFrameDelay());
	                
	                if(interval > 0) {
	                	try {
	                		System.out.println("sleep for " + interval + " frames.");
	                        Thread.sleep(1000L * interval / 60);
	                    } catch (InterruptedException e) {
	                        e.printStackTrace();
	                    }
	                    next_recommended_sleep = current_frame + RECOMMENDATION_INTERVAL;
	                }
	            }
			}
		}
		return true;
	}

	@Override
	public boolean addLocalInput(int input) {
		if(sync.isInRollback()) {
            System.out.println("in rollback");
            return false;
        }
		
        if(synchronizing) {
            System.out.println("synchronizing");

            return false;
        }

        if(!sync.addLocalInput(0, input)) {
            System.out.println("Prediction threshold reached");
            return false;
        }
        
		local_connect_status[0].last_frame = sync.getFrame();
		endpoints[0].sendInput(new GameInput(sync.getFrame(), input), local_connect_status[0]);
		return true;
	}

	@Override
	public int[] syncInput() {
        return sync.syncInputs();
	}

	@Override
	public void incrementFrame() {
		doPoll(0);
    	System.out.println("end of frame: " + sync.frame_count);
		sync.incrementFrame();
	}

	@Override
	public void setFrameDelay() {
		
	}

	@Override
	public void setDisconnectTimeout(int timeout) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDisconnectNotifyStart(int timeout) {
		// TODO Auto-generated method stub
		
	}
	
	public void processUdpProtocolEvents() {
        UdpProtocolEvent event = endpoints[0].getEvent();
        GgpoEvent ggpoEvent;
        while(event != null) {
            switch(event.getEventType()) {
                case Connected:
                    if( endpoints[0].getConnectState() == ConnectState.Connected &&
                		endpoints[0].getConnectState() != ConnectState.Syncing) {
                    	endpoints[0].synchronize();
                        synchronizing = true;
                        connecting = false;
                    }
                    break;
                case Synchronized:
                    checkInitialSync();
                    break;
                case Input:
                    // TODO: Don't send ggpo event. Add input to the sync's
                    //       input queue if now is a good time.
                    if(!local_connect_status[1].disconnected) {
                        int current_frame = local_connect_status[1].last_frame;
                        int new_remote = event.input.input.frame;
                        sync.addRemoteInput(1, event.input.input);
                        local_connect_status[1].last_frame =
                            event.input.input.frame;
                    }
                    break;
			default:
				break;
            }

            event = endpoints[0].getEvent();
        }
    }

	private void checkInitialSync() {
		if(synchronizing) {
            if(endpoints[0].isInitialized() &&
                !endpoints[0].isSynchronized() &&
                !local_connect_status[1].disconnected) {
                return;
            }
        }

//        GgpoEvent event = new GgpoEvent(GGPOEventCode.GGPO_EVENTCODE_RUNNING);
        // TODO: handle first running event
        synchronizing = false;		
	}
	
	private int poll2players(int frame) {
		int total_min_confirmed = Integer.MAX_VALUE;
        for(int i = 0; i < local_connect_status.length; i++) {
            // TODO: Get peer connect status and disconnect if needed.
            if(!local_connect_status[i].disconnected) {
                total_min_confirmed =
                    Math.min(local_connect_status[i].last_frame,
                        total_min_confirmed);
            }
        }
        return total_min_confirmed;
	}
}
