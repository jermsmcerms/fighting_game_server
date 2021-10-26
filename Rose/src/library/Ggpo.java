package library;

public interface Ggpo {
	default boolean startSession() { return true; }
	default boolean addPlayer() { return true; }
	default boolean startSyncTest() { return true; }
	default boolean startSpectating() { return true; }
	default boolean closeSession() { return true; }
	default boolean setFrameDelay() { return true; }
	default boolean idle(long timeout) { return true; }
	default boolean addLocalInput() { return true; }
	default boolean synchronizeInput() { return true; }
	default boolean disconncetPlayer() { return true; } 
	default boolean advanceFrame() { return true; }
	default boolean getNetworkStats() { return true; }
	default boolean setDisconnectTimeout() { return true; }
	default boolean setDisconnectNotifyStart() { return true; }
}
