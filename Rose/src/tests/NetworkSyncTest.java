package tests;

import java.io.IOException;
import java.net.SocketAddress;

import library.ConnectState;
import network.UdpMsg;
import network.UdpProto;
import network.UdpProtocolEvent;

public class NetworkSyncTest extends ServerBuilder {
	private boolean shut_down;
	
	public NetworkSyncTest() throws IOException {
		super();
	}

	@Override
	public void run() {
		System.out.println("Now listening for connections on port " + PORT_NUM);
		now = next = System.currentTimeMillis();
		while(!shut_down) {
			now = System.currentTimeMillis();
			poll.pump(0);
			if(now >= next) {
				poll.pump(0);
				for(int i = 0; i < endpoints.length; i++) {
		            if(endpoints[i] != null) {
		                UdpProtocolEvent event = endpoints[i].getEvent();
		                while (event != null) {
		                    switch(event.getEventType()) {
		                    case Disconnected:
		                    	// If one player disconnects disconnect all players
		                    	for(UdpProto endpoint : endpoints) {
		                    		if(endpoint != null) {
		                    			endpoint.disconnect();
		                    		}
		                    	}
		                    	break;
							default:
								break;
		                    }
		                    event = endpoints[i].getEvent();
		                }
		            }
		        }
				if(endpoints[0] != null && endpoints[1] != null) {
					shut_down = 
						endpoints[0].getCurrentState() == ConnectState.Disconnected && 
						endpoints[1].getCurrentState() == ConnectState.Disconnected &&
						endpoints[0].getUdp() == null || endpoints[1].getUdp() == null;
				}
				next = now + (1000/60); 
			}
		}
	}

	@Override
	public void onMsg(SocketAddress from, UdpMsg msg) {
		if(endpoints[0] == null) {
			endpoints[0] = new UdpProto(msg, udp, poll, from, server_magic_number);
			num_connections++;
			return;
		}
		
		if(endpoints[1] == null && endpoints[0].getAddress() != from) {
			endpoints[1] = new UdpProto(msg, udp, poll, from, server_magic_number);
			num_connections++;
			return;
		}
				
		for(int i = 0; i < endpoints.length; i++) {
			if(num_connections >= NUM_PLAYERS) {
				if(endpoints[i].handlesMsg(from)) {
					endpoints[i].onMsg(msg, 0); // TODO: remove queue argument
					return;
				}
			}
		}
	}
	
	public static void main(String[] args) {
		NetworkSyncTest nst;
		try {
			nst = new NetworkSyncTest();
			nst.run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
