package tests;

import lib.TimeSync;

public class TimeSyncTest implements TestInterface {
	public TimeSyncTest() {
		
	}
	
	public int runTest() {
		return 0;
	}
	
	public static void main(String[] args) {
		System.out.println("This is the time sync test");
		long now, next;
		now = next = System.currentTimeMillis();
		
		TimeSync timeSync = new TimeSync();
		
		while(true) {
			now = System.currentTimeMillis();
			timeSync.doPoll(Math.max(0, next - now - 1));
			if(now >= next) {
				runFrame();
				next = now + (1000/60);
			}
		}
	}
	
	public static void runFrame() {
		
	}
}
