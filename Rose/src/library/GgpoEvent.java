package library;

public class GgpoEvent {
    private final GGPOEventCode code;
    public Connected connected;
    public Synchronized synced;
    public Synchronizing synchronizing;
    public Disconnected disconnected;
    public TimeSync timeSync;
    public ConnectionInterrupted connectionInterrupted;
    public ConnectionResumed connectionResumed;

    public GgpoEvent(GGPOEventCode code) {
        this.code = code;
        switch (this.code) {
            case GGPO_EVENTCODE_CONNECTED_TO_SERVER:
                connected = new Connected();
                break;
            case GGPO_EVENTCODE_SYNCHRONIZED_WITH_SERVER:
                synchronizing = new Synchronizing();
                break;
            case GGPO_EVENTCODE_SYNCHRONIZING_WITH_SERVER:
                synced = new Synchronized();
                break;
            case GGPO_EVENTCODE_DISCONNECTED_FROM_SERVER:
                disconnected = new Disconnected();
                break;
            case GGPO_EVENTCODE_TIMESYNC:
                timeSync = new TimeSync();
                break;
            case GGPO_EVENTCODE_CONNECTION_INTERRUPTED:
                connectionInterrupted = new ConnectionInterrupted();
                break;
            case GGPO_EVENTCODE_CONNECTION_RESUMED:
                connectionResumed = new ConnectionResumed();
                break;
		case GGPO_EVENTCODE_RUNNING:
			break;
		default:
			break;
        }
    }

    public GGPOEventCode getCode() {
        return code;
    }

    public static class Connected {
        public int playerHandle;
    }

    public static class Synchronizing {
       public int playerHandle;
       public int count;
       public int total;
    }

    public static class Synchronized {
        public int player_handle;
    }

    public static class Disconnected {
        public int player_handle;
    }

    public static class TimeSync {
        public int frames_ahead;
    }

    public static class ConnectionInterrupted {
        public int player_handle;
        public int disconnect_timeout;
    }

    public static class ConnectionResumed {
        public int player_handle;
    }
}
