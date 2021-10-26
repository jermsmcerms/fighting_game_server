package tests;

import java.io.IOException;
import java.net.SocketAddress;

import library.ConnectState;
import library.GameInput;
import network.UdpMsg;
import network.UdpProto;

public class InputReceiveTest extends ServerBuilder {
	DummyClient dc;
	
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

	public void sendInput(GameInput gameInput) {
		if(endpoints[0] != null) {
			System.out.println("sending input frame: " + gameInput.frame + " input " + gameInput.input);
			endpoints[0].sendInput(gameInput, endpoints[0].getAddress());
		}
	}

	public ConnectState getConnectionState() {
		if(endpoints[0] == null) {
			return ConnectState.Invalid;
		}
		return endpoints[0].getCurrentState();
	}
}
