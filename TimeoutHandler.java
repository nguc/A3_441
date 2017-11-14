import java.util.TimerTask;

import cpsc441.a3.shared.*;

public class TimeoutHandler extends TimerTask {
	
	TxQueue queue;
	
	public TimeoutHandler (TxQueue q) {
		this.queue = q;
	}

	@Override
	public void run() {
		Segment[] pending_seg = queue.toArray();
		
		
	}
	
	public synchronized void processTimeout(Segment[] pending_segs) {
		// get the list of all pending segments from the transmission queue
		// go through the list and send all segments to the UDP socket
		// if there are any pending segments in transmission queue, start the timer
		}

}
