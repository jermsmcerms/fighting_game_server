package tests;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.ThreadLocalRandom;

import library.Poll;
import library.UdpCallbacks;
import network.Udp;
import network.UdpMsg;
import network.UdpProto;

public abstract class ServerBuilder implements UdpCallbacks {
	protected static final int PORT_NUM = 1234;
	protected static final int NUM_PLAYERS = 2;

	// Networking objects
	protected Udp udp;
	protected Poll poll;
	protected UdpProto[] endpoints;

	// timers for maintaining a fixed interval
	protected long now, next;

	protected int server_magic_number;
	protected int num_connections;

	public ServerBuilder() throws IOException {
		udp = new Udp(PORT_NUM, this);
		poll = udp.getPoll();
		
		endpoints = new UdpProto[2];
		

		do {
			server_magic_number = ThreadLocalRandom.current().nextInt();
		} while(server_magic_number <= 0);
	}

	public void run() {
		System.out.println("Now listening for connections on port " + PORT_NUM);
		now = next = System.currentTimeMillis();
	}
	
	@Override
	public void onMsg(SocketAddress from, UdpMsg msg) {
				
	}
}
