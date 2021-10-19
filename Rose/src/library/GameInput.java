package library;

import network.UdpMsg.ConnectStatus;

public class GameInput {
	public static final int NULL_FRAME = -1;
	public ConnectStatus connectStatus;
	public int frame;
	public int input;
	
	public GameInput() {}
	
	public GameInput(int frame, int input) {
		this.frame = frame;
		this.input = input;
	}
}
