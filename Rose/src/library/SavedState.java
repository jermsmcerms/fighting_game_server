package library;

public class SavedState {
    private static final int MAX_PREDICTION_FRAMES = 10;
    public SavedFrame[] frames;
    public int head;

    public class SavedFrame {
        public byte[] buf;
        public int cbuf;
        public long checkSum;
        public int frame;

        public SavedFrame() {
            frame = -1;
        }
    }

    public SavedState() {
        head = 0;
        frames = new SavedFrame[MAX_PREDICTION_FRAMES + 2];
        for(int i = 0; i < frames.length; i++) {
            frames[i] = new SavedFrame();
        }
    }
}
