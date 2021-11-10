package tests;

import java.io.IOException;
import java.net.SocketAddress;

import network.Udp;
import network.UdpMsg;
import network.UdpMsg.MsgType;

public class UdpPacketTest implements TestInterface {
	private Udp udp;
	
	public UdpPacketTest() throws IOException {
		udp = new Udp();
	}
	
	public int runTest() {
		int failures = 0;
		if(udp == null) {
			System.out.println("udp not initialized");
			failures++;
		}
		
		if(!udp.getDatagramChannel().isOpen()) {
			System.out.println("Datagram channel has not been opened");
			failures++;
		}
		
		if(udp.getRemoteAddress() == null) {
			System.out.println("Remote address not estabilshed");
			failures++;
		} 
		
		if(!udp.onLoopPoll(null)) {
			System.out.println("Did not recieve message from remote.");
			failures++;
		} else {
			// Message received. Now, send reply's to the client.
			long start, now, maxTime;
	    	maxTime = 3000;
	    	now = start = System.currentTimeMillis();
	    	while(now - start <= maxTime) {
	    		now = System.currentTimeMillis();
				udp.sendTo(new UdpMsg(MsgType.PacketTest));
	    	}
		}
		
		return failures;
	}
	
	public static void main(String[] args) {
		try {
			UdpPacketTest test = new UdpPacketTest();
			int failures = test.runTest();
			if(failures > 0) {
				System.out.println(failures + " test failed!");
			} else {
				System.out.println("all tests passed");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
