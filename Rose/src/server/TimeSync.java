package server;

import library.GameInput;

public class TimeSync {
	public static final int FRAME_WINDOW_SIZE = 40;
	public static final int MIN_UNIQUE_FRAMES = 10;
	public static final int MIN_FRAME_ADVANTAGE = 3;
	public static final int MAX_FRAME_ADVANTAGE = 9;
	private int count = 0;
	
	private int[] local;
	private int[] remote;
	private GameInput []last_inputs;
	
    public TimeSync() {
        local = new int[FRAME_WINDOW_SIZE];
        remote = new int[FRAME_WINDOW_SIZE];
        last_inputs = new GameInput[MIN_UNIQUE_FRAMES];
    }

    public void advance_frame(GameInput input, int advantage, int radvantage) {
        last_inputs[input.frame % last_inputs.length] = input;
        local[input.frame % local.length] = advantage;
        remote[input.frame % remote.length] = radvantage;
    }

    public int recommend_frame_wait_duration(boolean require_idle_input) {
        // Average our local and remote frame advantages
        int i, sum = 0;
        float advantage, radvantage;
        for (i = 0; i < local.length; i++) {
            sum += local[i];
        }
        advantage = sum / (float)local.length;

        sum = 0;
        for (i = 0; i < remote.length; i++) {
            sum += remote[i];
        }
        radvantage = sum / (float)remote.length;

        count++;

        // See if someone should take action.  The person furthest ahead
        // needs to slow down so the other user can catch up.
        // Only do this if both clients agree on who's ahead!!
        if (advantage >= radvantage) {
            return 0;
        }

        // Both clients agree that we're the one ahead.  Split
        // the difference between the two to figure out how long to
        // sleep for.
        int sleep_frames = (int)(((radvantage - advantage) / 2) + 0.5);

//        System.out.printf("iteration %d:  sleep frames is %d\n", count, sleep_frames);

        // Some things just aren't worth correcting for.  Make sure
        // the difference is relevant before proceeding.
        if (sleep_frames < MIN_FRAME_ADVANTAGE) {
            return 0;
        }

        // Make sure our input had been "idle enough" before recommending
        // a sleep.  This tries to make the emulator sleep while the
        // user's input isn't sweeping in arcs (e.g. fireball motions in
        // Street Fighter), which could cause the player to miss moves.
        if (require_idle_input) {
            for (i = 1; i < last_inputs.length; i++) {
                if (!last_inputs[i].equals(last_inputs[0])) {
                    return 0;
                }
            }
        }

        // Success!!! Recommend the number of frames to sleep and adjust
        return Math.min(sleep_frames, MAX_FRAME_ADVANTAGE);
    }
}
