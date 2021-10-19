package network;

import library.GameInput;

public class UdpProtocolEvent {
	public static enum Event {
		Unknown, Connected, Synchronizing, Synchronized,
		Input, Disconnected, NetworkInterrupted, NetworkResumed
	}
	
	private Event eventType;
	
	public Input input;
	public Synchronizing syncing;
	public NetworkInterrupted netInter;
	
	public UdpProtocolEvent(Event type) {
		this.eventType = type;
		switch(this.eventType) {
		case Input:
			input = new Input();
			break;
		case Synchronizing:
			syncing = new Synchronizing();
			break;
		case NetworkInterrupted:
			netInter = new NetworkInterrupted();
			break;
		default:
			break;
		}
	}
	
	public Event getEventType() { return eventType; }
	
	public static class Input {
		public GameInput input;
	}
	
	public static class Synchronizing {
		public int total, count;
	}
	
	public static class NetworkInterrupted {
		public int disconnect_timeout;
	}
}
