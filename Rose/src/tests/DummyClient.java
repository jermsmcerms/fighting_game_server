package tests;

import java.util.concurrent.ThreadLocalRandom;

import library.ConnectState;
import library.GameInput;

public class DummyClient {
	private int frame_count;
	private InputReceiveTest test_callbacks;
    private int input;
    private int frame_timer;
    
	public DummyClient(InputReceiveTest test_callbacks) {
		this.test_callbacks = test_callbacks;
        input = getRandomInput();
	}
	
	public void runFrame() {
		if(test_callbacks.getConnectionState() == ConnectState.Running) {
			if(frame_timer > 5) {
                input = getRandomInput();
                frame_timer = 0;
            }
            frame_timer++;
            System.out.println("adding frame: " + frame_count);
			addLocalInupt(frame_count, input);
			frame_count++;
		}
		test_callbacks.doPoll(0);
	}
	
    private int getRandomInput() {
        int retval = 1;
        int randomInt = ThreadLocalRandom.current().nextInt(-1, 8);
        if(randomInt == -1) {
            return 0;
        }
        return retval << randomInt;
    }
	
	private void addLocalInupt(int frame_count, int i) {
		test_callbacks.sendInput(new GameInput(frame_count, i));
	}
}
