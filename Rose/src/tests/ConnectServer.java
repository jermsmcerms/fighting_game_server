package tests;

import java.io.IOException;
import java.net.SocketAddress;

import network.UdpMsg;
import network.UdpMsg.MsgType;
import network.UdpProto;

/*
 * Connect server is a network test to validate that a connection with a client
 * can be established. It will wait for a message until stopped manually.
 * Once a message is received and a reply sent, the test passes.
 */
public class ConnectServer extends ServerBuilder {	
	// Testing values
	private boolean connect_request_received;
	private boolean connect_reply_sent;
	
	public ConnectServer() throws IOException {
		super();
	}
	
	/*
	 * Run listens for a connection from one client. Once a connect request
	 * is received, a connect reply is sent. After the reply is sent, the
	 * test concludes. Finally the program terminates.
	 */
	@Override
	public void run() {
		System.out.println("Now listening for connections on port " + PORT_NUM);
		now = next = System.nanoTime();
		while(!connect_request_received && !connect_reply_sent) {
			now = System.nanoTime();
			poll.pump(0); // pump messages "between" frames
			// this determines if enough time has passes to process a single frame
			// of a game state.
			if(now >= next) {
				poll.pump(0); // pump messages at the "end" of the frame.
				if(connect_request_received && connect_reply_sent) {
					connect_request_received = connect_reply_sent = false;
					// Check the end points to see if a reply has been sent
					for(int i = 0; i < endpoints.length; i++) {
						if(endpoints[i] != null) {
							connect_reply_sent = endpoints[i].connection_reply_sent;
						}
					}
				}
				// increase the amount time passed by one frame in nanoseconds
				next = now + (1000000000L/60); 
			}
		}
		System.out.println("Connection test passed. Exiting");
		System.exit(0);
	}
	
	/*
	 * Implementation of the UDP class's onMsg function.
	 * Creates a new end point if the message received is a connection request
	 * @param from 	a socket address containing the IP and port numbers of the sender.
	 * @param msg 	a UDP packet 
	 */
	@Override
	public void onMsg(SocketAddress from, UdpMsg msg) {
		connect_request_received = msg.hdr.type == MsgType.ConnectReq.ordinal();
		for(int i = 0; i < endpoints.length; i++) {
			if(endpoints[i] == null) {
				endpoints[i] = new UdpProto(msg, udp, poll, from, server_magic_number);
				return;
			}
		}
	}
	
	/*
	 * Driver function
	 */
	public static void main(String[] args) {
		ConnectServer ss;
		try {
			ss = new ConnectServer();
			ss.run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
