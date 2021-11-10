package library;

public interface GGPOSession {
	boolean doPoll(long timeout);
	boolean addLocalInput(int input);
	int[] syncInput();
	void incrementFrame();
	void setFrameDelay();
	void setDisconnectTimeout(int timeout);
	void setDisconnectNotifyStart(int timeout);
}
