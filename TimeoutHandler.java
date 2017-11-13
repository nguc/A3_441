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

}
