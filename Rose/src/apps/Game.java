package apps;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import library.ConnectState;
import library.GGPO;
import library.GgpoEvent;
import library.SaveGameState;
import server.SingleClientServer;

public class Game implements GGPO {
	private static final int DEFAULT_FRAME_DELAY  = 0;
	private int frame_timer;
	private int input;
	SingleClientServer scs;
	
	public Game() throws IOException {
		input = getRandomInput();
		scs = new SingleClientServer(this, DEFAULT_FRAME_DELAY);
		scs.setDisconnectTimeout(3000);
		scs.setDisconnectNotifyStart(1000);
	}
	
	public void idle(long timeout) {
		scs.doPoll(timeout);
	}
	
	public void runFrame() {
		if(scs.getConnectionState() == ConnectState.Running) {
			if(frame_timer > 5) {
                input = getRandomInput();
                frame_timer = 0;
            }
            frame_timer++;
			if(scs.addLocalInput(input)) {
				int[] inputs = scs.syncInput();
				if(inputs[0] != -1 && inputs[1] != -1) {
					scs.incrementFrame();					
				}
			}	
		}
	}
	
	@Override
	public boolean beginGame(String name) {
		return false;
	}

	@Override
	public SaveGameState saveGameState() {
		return null;
	}

	@Override
	public boolean loadFrame(byte[] buffer, int length) {
		return false;
	}

	@Override
	public boolean logGameState(String filename, String buffer) {
		return false;
	}

	@Override
	public Object freeBuffer(Object buffer) {
		return null;
	}

	@Override
	public boolean advanceFrame(int flags) {
		return false;
	}

	@Override
	public boolean onEvent(GgpoEvent event) {
		return false;
	}

	public static void main(String[] args) {
		try {
			Game game = new Game();
		
			long next, now;
			next = now = System.currentTimeMillis();
			
			while(true) {
				now = System.currentTimeMillis();
				game.idle(Math.max(0, next - now - 1));
				if(now >= next) {
					game.runFrame();
				// increase the amount time passed by one frame in nanoseconds
				next = now + (1000 /60);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private int getRandomInput() {
        int retval = 1;
        int randomInt = ThreadLocalRandom.current().nextInt(-1, 8);
        if(randomInt == -1) {
            return 0;
        }
        return retval << randomInt;
    }
}
