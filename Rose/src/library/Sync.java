package library;

import java.util.ArrayList;
import java.util.Arrays;

public class Sync {
    private final int max_prediction_frames;
    private boolean inRollBack;
    public int frame_count;
    private int last_confirmed_frame;
//    private GgpoCallbacks callbacks;
    public ArrayList<InputQueue> input_queues;
    public SavedState savedState;

    public Sync() {
        inRollBack = false;
        frame_count = 0;
        last_confirmed_frame = -1;
        max_prediction_frames = 10;

        input_queues = new ArrayList<InputQueue>(2);
        for(int i = 0; i < 2; i++) {
            input_queues.add(new InputQueue(i));
        }

        savedState = new SavedState();
    }

//    public void setCallbacks(GgpoCallbacks callbacks) {
//        this.callbacks = callbacks;
//    }

    public boolean isInRollback() { return inRollBack; }

    public boolean addLocalInput(int i, GameInput gameInput) {
        int frames_behind = frame_count - last_confirmed_frame;
        if( frame_count >= max_prediction_frames &&
            frames_behind >= max_prediction_frames) {
            return false;
        }

        if(frame_count == 0) {
            saveCurrentFrame();
        }

        gameInput.frame = frame_count;
        input_queues.get(i).addInput(gameInput);
        return true;
    }

    public void setLastConfirmedFrame(int frame) {
        last_confirmed_frame = frame;
        if(last_confirmed_frame > 0) {
            for(int i = 0; i < input_queues.size(); i++) {
                input_queues.get(i).discardConfirmedFrames(frame - 1);
            }
        }
    }

    public int[] syncInputs() {
        GameInput[] inputs = new GameInput[2];
        int[] syncedInputs;
        for(int i = 0; i < 2; i++) {
            inputs[i] = input_queues.get(i).getInput(frame_count);
            if(inputs[i] == null) { return null; }
        }
        syncedInputs = new int[inputs.length];
        for(int i = 0; i < 2; i++) {
            syncedInputs[i] = inputs[i].input;
        }
        return syncedInputs;
    }

    public void addRemoteInput(int i, GameInput input) {
        input_queues.get(i).addInput(input);
    }

    public void incrementFrame() {
        frame_count++;
        saveCurrentFrame();
    }

    public void saveCurrentFrame() {
        SavedState.SavedFrame state = savedState.frames[savedState.head];
        state.frame = frame_count;
//        SaveGameState sgs = (SaveGameState)callbacks.saveGameState();
//        state.cbuf = sgs.obj_data.length;
//        state.buf = new byte[state.cbuf];
//        System.arraycopy(sgs.obj_data, 0, state.buf, 0, state.cbuf);
//        state.checkSum = sgs.checksum;
        savedState.head = (savedState.head + 1) % savedState.frames.length;
    }

//    public void checkSimulation(long timeout) {
//        int seek_to = checkSimulationConsistency();
//        if(seek_to >= 0) {
//            adjustSimulation(seek_to);
//        }
//    }

//    private void adjustSimulation(int seek_to) {
//        int framecount = frame_count;
//        int count = frame_count - seek_to;
//        inRollBack = true;
//        loadFrame(seek_to);
//        resetPrediction(frame_count);
//        for(int i = 0; i < count; i++) {
//            callbacks.advanceFrame(0);
//        }
//        assert(framecount == frame_count);
//        inRollBack = false;
//    }

//    public void loadFrame(int frame) {
//        if(frame == frame_count) {
//            return;
//        }
//        savedState.head = findSavedFrameIndex(frame);
//        if(savedState.head >= 0) {
//            System.out.println("head location: " + savedState.head);
//            if(savedState.head >= savedState.frames.length) {
//                System.out.println("Exception incoming.");
//            }
//            SavedState.SavedFrame state = savedState.frames[savedState.head];
//            callbacks.loadFrame(state.buf, state.cbuf);
//            frame_count = state.frame;
//            savedState.head = (savedState.head + 1) % savedState.frames.length;
//        }
//    }

    private int findSavedFrameIndex(int frame) {
        int i;
        for(i = 0; i < savedState.frames.length; i++) {
            if(savedState.frames[i].frame == frame) {
                break;
            }
        }
        System.out.println("looking for frame " + frame);
        System.out.println("Saved frames");
        if(i < 0 || i >= savedState.frames.length) {
            for(i = 0; i < savedState.frames.length; i++) {
                System.out.println("Index: " + i + " frame " + savedState.frames[i].frame);
            }
        }
        return i;
    }

    private void resetPrediction(int frame) {
        for(int i = 0; i < 2; i++) {
            input_queues.get(i).resetPrediction(frame);
        }
    }

    private int checkSimulationConsistency() {
        int first_incorrect_frame = GameInput.NULL_FRAME;
        for(int i = 0; i < 2; i++) {
            int incorrect = input_queues.get(i).getFirstIncorrectFrame();
            if( incorrect != GameInput.NULL_FRAME &&
                first_incorrect_frame == GameInput.NULL_FRAME ||
                incorrect < first_incorrect_frame) {
                first_incorrect_frame = incorrect;
            }
        }

        return first_incorrect_frame;
    }

    public SavedState.SavedFrame[] getAllSavedFrames() {
        return savedState.frames;
    }

    public SavedState.SavedFrame getLastSavedFrame() {
        int i = savedState.head - 1;
        if(i < 0) {
            i = savedState.frames.length - 1;
        }
        return savedState.frames[i];
    }
}
