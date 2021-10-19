package network;

public class UdpMsg {
	private byte[] data;
	public enum MsgType {
		PacketTest
	}
	
	public MsgType type;
	public PacketTest packetTest;
	
	public UdpMsg(MsgType type) {
		this.type = type;
		switch(this.type) {
		case PacketTest:
			packetTest = new PacketTest();
			break;
		}
	}
	
	public static class PacketTest {
		byte testByte = 1;
	}
	
	public byte[] getData() {
		if(data == null) {
			return buildMsg();
		}
		return data;
	}
	
	private byte[] buildMsg() {
		data = new byte[getPacketSize(type)];
		switch(type) {
		case PacketTest:
			if(packetTest != null) {
				data[0] = packetTest.testByte;
			}
		}
		return data;
	}
	
	private int getPacketSize(MsgType type) {
		int size = 0;
		switch(type) {
			case PacketTest:
				size = 1;
				break;
		}
		return size;
	}
}
