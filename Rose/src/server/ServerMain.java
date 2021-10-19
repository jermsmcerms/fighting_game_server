package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class ServerMain {
	DatagramChannel dgc;
	ByteBuffer buffer;
	SocketAddress client_addr;
	int response;
	List<Integer> inputList;
	ArrayList<Client> clients;
	
	public ServerMain() throws IOException {
		dgc = DatagramChannel.open();
		dgc.configureBlocking(false);
		dgc.socket().bind(new InetSocketAddress(1234));
		buffer = ByteBuffer.allocate(32);
		client_addr = null;
		clients = new ArrayList<>(2);
	}
	
	public int getResponse() { return response; }

	public void sendMsg() throws IOException {
		if(client_addr == null) { return; }
		
//		for(int i = 0; i < clients.size(); i++) {
//			if(clients.get(i).handlesMsg(dest_addr)) {	
//				buffer.clear();
//				buffer.put(msg);
//				buffer.flip();
//				System.out.println("Sending " + Arrays.toString(buffer.array()));
//				dgc.send(buffer, dest_addr);
//				return;
//			}
//		}
		
		// Code for sending pending output. probably will move this into Client.java
		int size = clients.size();
		
		for(int i = 0; i < size; i++) {
			buffer.clear();
			// Reply that not all players are connected with an request echo
			if(size < 2) {
				buffer.putInt(1);
				buffer.flip();
			} else if(size == 2) {
				switch(clients.get(i).type) {
				case 0: break;
				case 1: 
					if(!clients.get(i).connected) {
						buffer.putInt(2);	// Connect Reply
						buffer.putInt(i+1);	// Player number
						buffer.flip();
						System.out.println("player " + (i + 1) + " assinged to " +
							clients.get(i).client_addr);
						clients.get(i).connected = true;
					}
					break;
				case 2: break; // Clients should not send connect reply's at this time
				case 3:
					// This should get player two's inputs and send them to player 1
					// and get player one's inputs and send them to player 2.
					int position = (i + 1) % size;
					if(!clients.get(position).inputList.empty()) {
						for(int j = 0; j < clients.get(position).inputList.size; j++){
							buffer.putInt(clients.get(position).type);
							buffer.putInt(clients.get(position).frame);
							buffer.putInt(clients.get(position).inputList.front());
							buffer.flip();
							clients.get(position).inputList.pop();
						}
					}
					break;
				}
			}
			System.out.println("sending " + Arrays.toString(buffer.array())
			 + " to " + clients.get(i).client_addr);
			dgc.send(buffer, clients.get(i).client_addr);
		}
	}
	
	public void recvMsg() throws IOException {
		buffer.clear();
		client_addr = dgc.receive(buffer);
		if(client_addr == null) return;
//		System.out.println("received " + Arrays.toString(buffer.array()));
		// need to check the message type.
		// keep track of the number of unique connectReq messages.
		// When that value reaches 2, assume both players are
		// connected and send connectReply messages.
		// Add clients to our list
		if(clients.isEmpty()) {
			clients.add(new Client(this, client_addr));
		} else if(clients.size() < 2) {
			for(int i = 0; i < clients.size(); i++) {
				if(!clients.get(i).client_addr.equals(client_addr)) { 
					clients.add(new Client(this, client_addr));
					break;
				}
			}
		}
		
		// add the input to the proper queue
		for(Client client : clients) {
			if(client.handlesMsg(client_addr)) {
				client.onMsg(buffer.array());
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
		ServerMain server = new ServerMain();
		System.out.println("listening on port " + 1234);
		while(true) {			
			server.recvMsg();
			server.sendMsg();
		}
	}
}
