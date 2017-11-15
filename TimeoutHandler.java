import java.util.TimerTask;

import cpsc441.a3.shared.*;

public class TimeoutHandler extends TimerTask {
	
	FastFtp mainThread;
	TxQueue queue;
	
	public TimeoutHandler (FastFtp sender, TxQueue q) {
		this.mainThread = sender;
		this.queue = q;
	}

	@Override
	public void run() {
		//System.out.println("Timer task started");
		Segment[] pending_seg = queue.toArray();
		this.mainThread.processTimeout(pending_seg);
	}
	
}
