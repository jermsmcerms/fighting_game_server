package network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import library.IPollSink;
import library.Poll;
import library.UdpCallbacks;

public class Udp implements IPollSink {
	DatagramChannel dgc;
	Poll poll;
    private ByteBuffer recv_buffer;
    private UdpCallbacks callbacks;

	public Udp(int portNumber, UdpCallbacks callbacks) throws IOException {
		this.callbacks = callbacks;
		
		dgc = DatagramChannel.open();
		dgc.configureBlocking(false);
		dgc.socket().bind(new InetSocketAddress(portNumber));
		poll = new Poll();
		poll.registerLoop(this);
		recv_buffer = ByteBuffer.allocate(UdpMsg.MAX_COMPRESSED_BITS);
	}
	
    public Poll getPoll() { return poll; }

    public void sendTo(UdpMsg msg, SocketAddress dst) {
    	
        try { dgc.send(msg.getBuffer(), dst); }
        catch(IOException e) { e.printStackTrace(); }
    }


    //#region IPollSink Implementation
    @Override public boolean onLoopPoll(Object o) {
    	while(true) {
	        try {
	        	recv_buffer.clear();
				SocketAddress client_addr = dgc.receive(recv_buffer);
				
				if(client_addr == null) { break; }
				else { 
					callbacks.onMsg(client_addr, new UdpMsg(recv_buffer));
				}
				
				} catch (IOException e) {
				e.printStackTrace();
			}
    	}
		return true;
    }
    //#endregion
}
