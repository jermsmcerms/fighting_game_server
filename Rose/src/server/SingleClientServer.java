package server;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import library.GGPOEventCode;
import library.GameInput;
import library.GgpoEvent;
import library.Poll;
import library.Sync;
import network.Udp;
import network.UdpMsg;
import network.UdpMsg.ConnectStatus;
import network.UdpMsg.MsgType;
import network.UdpProto;
import network.UdpProtocolEvent;

/*
 * This server should be used to only allow a single connection.
 * It then simulates a second client by randomly generated inputs
 * that can be accepted by the client's game state.
 */
public class SingleClientServer extends Udp.Callbacks {
	public static final int PORT_NUM = 1234;
	public static final int NUM_PLAYERS = 2;
	private static final int RECOMMENDATION_INTERVAL = 240;
	private ArrayList<UdpProto> endpoints;
	private Udp udp;
	private Poll poll;
	private UdpMsg.ConnectStatus local_connect_status;
	private UdpMsg.ConnectStatus remote_connect_status;
	private Sync sync;
	private int next_recommended_sleep;
	
	public SingleClientServer() throws IOException {
		endpoints = new ArrayList<>(NUM_PLAYERS);
		for(int i = 0; i < NUM_PLAYERS; i++) {
			endpoints.add(new UdpProto());
		}
		udp = new Udp(PORT_NUM, this);
		poll = udp.getPoll();
		
		local_connect_status = new UdpMsg.ConnectStatus();
		remote_connect_status = new UdpMsg.ConnectStatus();
		
		sync = new Sync();
		
		System.out.println("running dummy client server on port " + PORT_NUM);
	}

	public void runFrame() {
		// Randomly generate inputs between 1 and 2 to simulate left/right movement.
		int randomInput = ThreadLocalRandom.current().nextInt(3);
		boolean result = addLocalInput(randomInput);
		if(result) {
			int[] inputs = syncInputs();
			if(inputs != null) {
				advanceFrame(inputs); 	// <--- If this simulation idea works I might use this function
										// to route messages to the other real player.
										// Do nothing for now.
			}
		}
	}

	public void doPoll(long timeout) {
		poll.pump(0);
		if(getCanStart()) {
			pollUdpProtocolEvents();
            int current_frame = sync.frame_count;
            endpoints.get(0).setLocalFrameNumber(current_frame);
//                sync.checkSimulation(timeout);
            int total_min_confirmed = poll2Players();
            if(total_min_confirmed >= 0) {
                sync.setLastConfirmedFrame(total_min_confirmed);
            }

            // TODO: send time sync notifications if its proper to do so.
            if(current_frame > next_recommended_sleep) {
                int recommend_frame_delay = endpoints.get(0).recommendFrameDelay();
                int interval = Math.max(0, recommend_frame_delay);
                if(interval > 0) {
                    GgpoEvent event = new GgpoEvent(GGPOEventCode.GGPO_EVENTCODE_TIMESYNC);
                    event.timeSync.frames_ahead = interval;
                    try {
                    	System.out.println("sleeping for " + (interval *(1000/60)) + " frames");
                    	Thread.sleep(interval * (1000/60));
                    } catch(InterruptedException e) {
                    	e.printStackTrace();
                    }
                    next_recommended_sleep = current_frame + RECOMMENDATION_INTERVAL;
                }
            }
		}
	}
	
	private int poll2Players() {
		int total_min_confirmed = Integer.MAX_VALUE;
        if(endpoints.get(0).getRemoteStatus() != null) {
            remote_connect_status =
            		new UdpMsg.ConnectStatus(
            				endpoints.get(0).getRemoteStatus().disconnected, 
            				endpoints.get(0).getRemoteStatus().last_frame);

            total_min_confirmed = Math.min(
                    remote_connect_status.last_frame,
                    total_min_confirmed);
            return total_min_confirmed;
        }
        return -1;
	}

	public boolean getCanStart() {
		if(endpoints.get(0) == null || !endpoints.get(0).getIsInitialized()) {
			return false;
		}
		return endpoints.get(0).startRequest;
	}
	
	@Override
	public void onMsg(SocketAddress from, UdpMsg msg) {
		System.out.println("message from: " + from + " received");
		System.out.println("message type: " + MsgType.values()[msg.hdr.type]);
		// This is where we will determine when to sends messages to clients or whatever
		for(int i = 0; i < NUM_PLAYERS; i++) {
			if(!endpoints.get(i).getIsInitialized()) {
				endpoints.get(i).init(udp, poll, from, PORT_NUM, 3000, 1000, msg);
				return;
			} 
			else if(endpoints.get(i).handlesMsg(from)) {
				endpoints.get(i).onMsg(msg, i);
				return;
			}
		}
	}
	
	private void pollUdpProtocolEvents() {
		UdpProtocolEvent event = endpoints.get(0).getEvent();
		while(event != null) {
			processEvent(event);
			event = endpoints.get(0).getEvent();
		}
	}
	
	private void processEvent(UdpProtocolEvent event) {
		switch(event.getEventType()) {
		case Input:
			if(!local_connect_status.disconnected) {
				sync.addRemoteInput(1, event.input.input);
				if(remote_connect_status == null) {
					remote_connect_status = new UdpMsg.ConnectStatus();
				}
				
				remote_connect_status.last_frame = event.input.input.frame;
			}
		}
	}
	
	private void advanceFrame(int[] inputs) {
		// TODO Auto-generated method stub
		
	}

	private int[] syncInputs() {
		// TODO Auto-generated method stub
		return sync.syncInputs();
	}

	int frame = 0;
	private boolean addLocalInput(int randomInput) {
		GameInput input = new GameInput();
		input.frame = frame;
		input.input = randomInput;
		input.connectStatus = new ConnectStatus(false, frame);
		endpoints.get(0).sendInput(input, endpoints.get(0).getAddress());
		frame++;
		return true;
	}
	
	public static void main(String[] args) {
		try {
			SingleClientServer scs = new SingleClientServer();
			// first wait for the client to connect.
			long now, next;
			now = next = System.nanoTime();
			do {
				scs.doPoll(0);
			} while (!scs.getCanStart());
			while(true) {
				now = System.nanoTime();
				scs.doPoll(Math.max(0, next-now-1));
				if(now >= next) {
					scs.runFrame();
					now = next + (1000000/60);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
