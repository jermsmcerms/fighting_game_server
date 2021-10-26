package server;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;

import library.Poll;
import library.UdpCallbacks;
import network.Udp;
import network.UdpMsg;
import network.UdpProto;

/*
 * This entire class will probably be re written once my tests are done.
 */
public class Server implements UdpCallbacks {
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
	}
	

	@Override
	public void onMsg(SocketAddress from, UdpMsg msg) {

	}
	
	public static void main(String[] args) {

	}
}
