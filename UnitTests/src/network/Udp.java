package network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import network.UdpMsg.MsgType;

public class Udp {
	private ByteBuffer rec_buf;
	private DatagramChannel dgc;
	private SocketAddress raddr;
	
	public Udp() throws IOException {
		rec_buf = ByteBuffer.allocate(8);
		dgc = DatagramChannel.open();
		dgc.configureBlocking(false);
		dgc.socket().bind(new InetSocketAddress(1234));
	}
	
    public boolean sendTo(UdpMsg msg) {
    	byte[] data = msg.getData();
        try {
        	dgc.send(ByteBuffer.wrap(data), raddr);
        	return true;
        }
        catch(IOException e) { 
        	e.printStackTrace();
        	return false;
    	}
    }
    
    public boolean onLoopPoll(Object cookie) {
    	long start, now, maxTime;
    	maxTime = 30000000000L;
    	now = start = System.nanoTime();
    	while(now - start <= maxTime) {
    		now = System.nanoTime();
	        try {
	        	rec_buf.clear();
				SocketAddress rec_addr = dgc.receive(rec_buf);
				
				if(rec_addr == null) { 
					System.out.println(
						"Message not received. Will wait for " + 
						((maxTime - (now - start))/1000000000L) + 
						" more seconds");
				}
				else { 
					System.out.println("received message from: " + rec_addr);
					return true;
				} 
	        } catch (IOException e) {
				e.printStackTrace();
			}
    	}
		return false;
    }
	
	public DatagramChannel getDatagramChannel() {
		return dgc;
	}
	
	public SocketAddress getRemoteAddress() {
		return raddr;
	}
}
