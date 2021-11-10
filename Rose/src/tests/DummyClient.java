package tests;

import java.util.concurrent.ThreadLocalRandom;

import library.ConnectState;
import library.GameInput;
import network.UdpMsg;

public class DummyClient {
	private int frame_count;
	private InputReceiveTest test_callbacks;
    private int input;
    private int frame_timer;
    private UdpMsg.ConnectStatus[] local_connect_status;
    
	public DummyClient(InputReceiveTest test_callbacks) {
		this.test_callbacks = test_callbacks;
        input = getRandomInput();
        local_connect_status = new UdpMsg.ConnectStatus[2];
        for(int i = 0; i < local_connect_status.length; i++) {
        	local_connect_status[i] = new UdpMsg.ConnectStatus();
        	local_connect_status[i].disconnected = false;
        	local_connect_status[i].last_frame = -1;
        }
	}
	
	public void runFrame() {
		if(test_callbacks.getConnectionState() == ConnectState.Running) {
			if(frame_timer > 5) {
                input = getRandomInput();
                frame_timer = 0;
            }
            frame_timer++;
			addLocalInupt(frame_count, input);
			advanceFrame();
		}
	}
	
    private void advanceFrame() {
    	System.out.println("end of frame: " + frame_count);
    	test_callbacks.doPoll(0);
		frame_count++;
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
		local_connect_status[0].last_frame = frame_count;
		test_callbacks.sendInput(new GameInput(frame_count, i), local_connect_status[0]);
	}

	public int getFrame() {
		return frame_count;
	}
}
