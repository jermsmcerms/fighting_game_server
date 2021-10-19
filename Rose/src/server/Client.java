package server;

import java.io.IOException;
import java.net.SocketAddress;

public class Client {
	public int frame;
	public RingBuffer<Integer> inputList;
	public SocketAddress client_addr;
	public ServerMain server;
	public static byte playerNumber = 1;
	public int sequence = 0;
	int input = 0;
	public boolean connected = false;
	public int type;
	public Client(ServerMain server, SocketAddress client_addr) {
		this.server = server;
		inputList = new RingBuffer<>(128);
		this.client_addr = client_addr;
		frame = -1;
	}
	
	public boolean handlesMsg(SocketAddress from) {
		return client_addr.equals(from);
	}
	
	public void onMsg(byte[] msg) throws IOException {
		type = 	((msg[0] & 0xFF) << 24) | 
	            ((msg[1] & 0xFF) << 16) | 
	            ((msg[2] & 0xFF) << 8 ) | 
	            (msg[3] & 0xFF);
		
		switch (type) {
		case 0: break;				// Invalid
		case 1: break;				// Connect Request
		case 2:						// Connect Reply 
			connected = true; 	
			break;
		case 3: 
			frame = ((msg[4] & 0xFF) << 24) | 
            		((msg[5] & 0xFF) << 16) | 
        			((msg[6] & 0xFF) << 8 ) | 
        			(msg[7] & 0xFF);
			
			input = ((msg[8] & 0xFF) << 24) | 
            		((msg[9] & 0xFF) << 16) | 
        			((msg[10] & 0xFF) << 8 ) | 
        			(msg[11] & 0xFF);
	
			inputList.push(input);
			break;				// Input
		}
		
//		switch(msg[0]) {
//		case 1:
//			byte[] reply = {2, playerNumber, 0, 0, 0, 0, 0, 0};
//			server.sendMsg(reply, client_addr);
//			playerNumber++;
//		}
//		sequence = ((msg[0] & 0xFF) << 24) | 
//	            ((msg[1] & 0xFF) << 16) | 
//	            ((msg[2] & 0xFF) << 8 ) | 
//	            ((msg[3] & 0xFF) << 0 );
//		
//		frame = ((msg[4] & 0xFF) << 24) | 
//	            ((msg[5] & 0xFF) << 16) | 
//	            ((msg[6] & 0xFF) << 8 ) | 
//	            ((msg[7] & 0xFF) << 0 );
	}
}
