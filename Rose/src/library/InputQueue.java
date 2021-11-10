package library;

public class InputQueue {
    private static final int INPUT_QUEUE_LENGTH = 128;
    private int head;
    private int tail;
    private int length;
    private int frame_delay;
    private boolean first_frame;
    private int last_user_added_frame;
    private int first_incorrect_frame;
    private int last_frame_requested;
    private int last_added_frame;
    private GameInput prediction;
    private final GameInput[] inputs;
    private GameInput currentInput;

    public InputQueue() {
        head = 0;
        tail = 0;
        length = 0;
        frame_delay = 0;
        first_frame = true;
        last_user_added_frame   = GameInput.NULL_FRAME;
        first_incorrect_frame   = GameInput.NULL_FRAME;
        last_frame_requested    = GameInput.NULL_FRAME;
        last_added_frame        = GameInput.NULL_FRAME;

        prediction = new GameInput(GameInput.NULL_FRAME, 0);
        inputs = new GameInput[INPUT_QUEUE_LENGTH];
        for(int i = 0; i < inputs.length; i++) {
            inputs[i] = new GameInput(0,0);
        }
        currentInput = null;
    }

    public GameInput[] getInputList() {
        return inputs;
    }

    public GameInput getCurrentInput() { return currentInput; }

    public void addInput(GameInput gameInput) {
//        assert(last_user_added_frame == GameInput.NULL_FRAME || gameInput.getFrame() == last_user_added_frame + 1);
        last_user_added_frame = gameInput.frame;

        int new_frame = advanceQueueHead(gameInput.frame);
        if(new_frame != GameInput.NULL_FRAME) {
            addDelayedInputToQueue(gameInput, new_frame);
        } else {
            System.out.println("new frame not added: " + new_frame);
        }

        gameInput.frame = new_frame;
        currentInput = gameInput;
    }

    private void addDelayedInputToQueue(GameInput gameInput, int frame_number) {
//        System.out.println("adding input frame: " + frame_number + ". last added + 1:" + (last_added_frame + 1));
//        if(frame_number != last_added_frame + 1) {
//            System.out.println("incoming frame: " + frame_number + " last added frame " +
//                    (last_added_frame + 1));
//        }
//        assert(last_added_frame == GameInput.NULL_FRAME ||
//                frame_number == last_added_frame + 1);
//
//        assert(frame_number == 0 ||
//                inputs[getPreviousFrame(head)].getFrame() == frame_number - 1);

        inputs[head] = new GameInput(frame_number, gameInput.input);
        head = (head + 1) % INPUT_QUEUE_LENGTH;
        length++;
        first_frame = false;

        last_added_frame = frame_number;

        if(prediction.frame != GameInput.NULL_FRAME) {
            System.out.println("prediction frame: " + prediction.frame +
                    " incoming frame: " + frame_number);
//            assert(prediction.frame == frame_number);
            if( first_incorrect_frame == GameInput.NULL_FRAME &&
                !prediction.equals(gameInput)) {
                first_incorrect_frame = frame_number;
                System.out.println("marking first incorrect frame: " + first_incorrect_frame);
            }

            if( prediction.frame == last_frame_requested &&
                first_incorrect_frame == GameInput.NULL_FRAME) {
                prediction.frame = GameInput.NULL_FRAME;
            } else {
                prediction.frame++;
                System.out.println("incrementing prediciton frame " + prediction.frame);
            }
        }
    }

    private int advanceQueueHead(int frame) {
        int expected_frame = first_frame ? 0 : inputs[getPreviousFrame(head)].frame + 1;
        frame += frame_delay;
        if(expected_frame > frame) {
            return GameInput.NULL_FRAME;
        }

        while(expected_frame < frame) {
            GameInput last_input = inputs[getPreviousFrame(head)];
            addDelayedInputToQueue(last_input, expected_frame);
            expected_frame++;
        }

        assert(frame == 0 || frame == inputs[getPreviousFrame(head)].frame + 1);
        return frame;
    }

    private int getPreviousFrame(int offset) {
        return  offset == 0 ? INPUT_QUEUE_LENGTH - 1 : offset - 1;
    }

    public void discardConfirmedFrames(int frame) {
//        assert (frame >= 0);

        if(last_frame_requested != GameInput.NULL_FRAME) {
            frame = Math.min(frame, last_frame_requested);
        }

        if(frame >= last_added_frame) {
            tail = head;
        } else {
            int offset = frame - inputs[tail].frame + 1;
            tail = (tail + offset) % INPUT_QUEUE_LENGTH;
            length -= offset;
        }
    }

    public GameInput getInput(int requested_frame) {

        last_frame_requested = requested_frame;

        if(prediction.frame == GameInput.NULL_FRAME) {
            int offset = requested_frame - inputs[tail].frame;
            if(offset >= 0 && offset < length) {
            	System.out.println("no need to predict. Return inputs for frame: " +
            			inputs[offset].frame);
                offset = (offset + tail) % INPUT_QUEUE_LENGTH;
                return inputs[offset];
            }

            if(requested_frame == 0) {
                prediction.input = 0;
            } else if(last_added_frame == GameInput.NULL_FRAME) {
                prediction.input = 0;;
            } else {
            	System.out.println("setting prediction to previous input " +
        			inputs[getPreviousFrame(head)].input + " frame " + inputs[getPreviousFrame(head)].frame);
                prediction = new GameInput(inputs[getPreviousFrame(head)].frame, inputs[getPreviousFrame(head)].input);
            }
            prediction.frame++;
        }

        GameInput input = new GameInput(prediction.frame, prediction.input);
        input.frame = requested_frame;
        return input;
    }

    public int getFirstIncorrectFrame() {
        return first_incorrect_frame;
    }

    public void resetPrediction() {
        prediction.frame = GameInput.NULL_FRAME;
        first_incorrect_frame = GameInput.NULL_FRAME;
        last_frame_requested = GameInput.NULL_FRAME;
    }

    public void setFrameDelay(int frameDelay) {
        this.frame_delay = frameDelay;
    }
}
