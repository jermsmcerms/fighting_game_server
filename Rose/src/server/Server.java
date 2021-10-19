package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import library.GameInput;
import library.Poll;
import network.Udp;
import network.UdpMsg;
import network.UdpProto;
import network.UdpProtocolEvent;

public class Server extends Udp.Callbacks {
	public static final int PORT_NUM = 1234;
	public static final int NUM_PLAYERS = 2;
	private ArrayList<UdpProto> endpoints;
	private Udp udp;
	private Poll poll;
	
	public Server() throws IOException {
		endpoints = new ArrayList<>(NUM_PLAYERS);
		for(int i = 0; i < NUM_PLAYERS; i++) {
			endpoints.add(new UdpProto());
		}
		udp = new Udp(PORT_NUM, this);
		poll = udp.getPoll();
	}
	
	public void idle(long timeout) {
		poll.pump(0);
		//pollUdpProtocolEvents();
		
	}
	
//	private void pollUdpProtocolEvents() {
//		for(int i = 0; i < NUM_PLAYERS; i++) {
//			if(endpoints.get(i) != null) {
//				UdpProtocolEvent event = endpoints.get(i).getEvent();
//				while(event != null) {
//					onUdpProtocolEvent(event, i);
//					event = endpoints.get(i).getEvent();
//				}
//			}
//		}
//	}
//
//	private void onUdpProtocolEvent(UdpProtocolEvent event, int i) {
//		// TODO handle other protocol events (first)
//		switch(event.getEventType()) {
//		case Input:
//			// TODO: check the connect status of the current client.
//			// TODO: then check if the current remote frame equals -1 or
//			//		 the event frame is one frame ahead of the current remote frame
//			// Add the input to the correct queue (i)
//			// TODO: update the connect status' frame for the current client with
//			//		 the events frame
//			break;
//		case Disconnected: break;
//		default: break;
//		}
//	}

	@Override
	public void onMsg(SocketAddress from, UdpMsg msg) {
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
	
	public static void main(String[] args) {
		System.out.println("Starting up server on port " + Server.PORT_NUM);
		try {
			Server server = new Server();
			long next, now;
			next = now = System.nanoTime();
			
			while(true) {
				now = System.nanoTime();
				server.idle(Math.max(0, next - now -1));
				server.broadcastInputs();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void broadcastInputs() {
		UdpProto player1 = endpoints.get(0);
		GameInput p1_input = player1.getInput();
		UdpProto player2 = endpoints.get(1);
		GameInput p2_input = player2.getInput();
		if(player1 != null && player2 != null) {
			if(p2_input != null) {
				player1.sendInput(p2_input, player1.getAddress());
			}
		}
		
		if(player2 != null && player1 != null) {
			if(p1_input != null) {
				player2.sendInput(p1_input, player2.getAddress());
			}
		}
	}
}
