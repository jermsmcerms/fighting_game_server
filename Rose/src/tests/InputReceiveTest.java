package tests;

import java.io.IOException;
import java.net.SocketAddress;

import library.ConnectState;
import library.GameInput;
import network.UdpMsg;
import network.UdpProto;

public class InputReceiveTest extends ServerBuilder {
	private static final int RECOMMENDATION_INTERVAL = 240000000;
	DummyClient dc;
	private int next_recommended_sleep;
	
	public InputReceiveTest() throws IOException {
		super();
		dc = new DummyClient(this);
	}
	
	@Override
	public void run() {
		super.run();
		while(true) {
			now = System.nanoTime();
			doPoll(Math.max(0, next - now - 1));
			if(now >= next) {
				dc.runFrame();
			// increase the amount time passed by one frame in nanoseconds
			next = now + (1000000000L/60);
			}
		}
	}
	
	public void doPoll(long timeout) {
		poll.pump(0);
		if(	endpoints[0] != null && 
			endpoints[0].getCurrentState() == ConnectState.Running) {
			int current_frame = dc.getFrame();
			endpoints[0].setLocalFrameNumber(current_frame);
			if(current_frame > next_recommended_sleep) {
                int interval =
                    Math.max(0, endpoints[0].recommendFrameDelay());
                
                if(interval > 0) {
                    System.out.println("try to sleep for: " + 
                        (1000000000 * interval / 60) + " frames");
                    next_recommended_sleep = current_frame + RECOMMENDATION_INTERVAL;
                }
            }
		}
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
	
	public static void main(String[] args) {
		try {
			InputReceiveTest irt = new InputReceiveTest();
			irt.run();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	public void sendInput(GameInput gameInput, UdpMsg.ConnectStatus local_connect_status) {
		if(endpoints[0] != null) {
			System.out.println("sending input frame: " + gameInput.frame + " input " + gameInput.input);
			endpoints[0].sendInput(gameInput, endpoints[0].getAddress(), local_connect_status);
		}
	}

	public ConnectState getConnectionState() {
		if(endpoints[0] == null) {
			return ConnectState.Invalid;
		}
		return endpoints[0].getCurrentState();
	}
}
