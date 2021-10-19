package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;

public class ClientMain {
	private int playerNumber;
	private DatagramChannel dgc;
	private ByteBuffer buffer;

	public ClientMain() throws IOException {
		dgc = DatagramChannel.open();
		dgc.configureBlocking(false);
		dgc.socket().bind(null);
		
		buffer = ByteBuffer.allocate(32);
	}
		
	public void sendMsg(byte[] msg) throws IOException {
		buffer.clear();
		buffer.put(msg);
		buffer.flip();
		System.out.println("Sending: " + Arrays.toString(buffer.array()));
		dgc.send(buffer, new InetSocketAddress("localhost", 1234));
	}
	
	public void recvMsg() throws IOException {
		while(true) {
			buffer.clear();
			SocketAddress server_addr = dgc.receive(buffer);
			if(server_addr == null) { break; }
			if(buffer.array()[0] == 2) {
				playerNumber = buffer.array()[1];
				System.out.println("Player number " + playerNumber);
				System.exit(0);
			}
			server_addr = null;
		}
	}
	
	public static void main(String[] args) {
		ClientMain client = null;
		try { client = new ClientMain(); } 
		catch (IOException e) { e.printStackTrace(); }
		
		// Connect request loop
		while(true) {
			try {
				if(client != null) {	
					client.sendSyncRequest();
					client.recvMsg();
				}
			} catch(IOException e) { e.printStackTrace(); System.exit(0); } 
		}
		
		// input loop
//		while(true) {
//			try {
//				if(client != null) {	
//					client.sendMsg();
//					client.recvMsg();
//				}
//			} catch(IOException e) { e.printStackTrace(); System.exit(0); } 
//		}
	}

	private void sendSyncRequest() throws IOException {
		byte[] syncReq = { 1, 0, 0, 0, 0, 0, 0, 0};
		sendMsg(syncReq);		
	}
}