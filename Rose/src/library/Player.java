package library;

import network.UdpProto;

/*
 * The idea behind this class is to have a way for
 * the server to access both, the protocol processing inputs,
 * and the input queue they are stored in.
 * The server will take the input queue from player 1
 * and send them to player 2, and visa versa.
 */
public class Player {
	private UdpProto protocol;
	private RingBuffer<InputQueue> input_queue;
	
	public Player(UdpProto protocol) {
		this.protocol = protocol;
		input_queue = new RingBuffer<>(128);
	}
}
