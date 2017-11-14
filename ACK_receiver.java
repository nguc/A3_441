import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.logging.Level;
import java.util.logging.Logger;
import cpsc441.a3.shared.*;


public class ACK_receiver implements Runnable {
	
	private static final Logger LOGGER = Logger.getLogger( FastFtp.class.getName() );
	DatagramSocket udp;
	TxQueue queue;
	Boolean done = false;
	
	public ACK_receiver (int port, TxQueue queue) {
		try {
			this.udp = new DatagramSocket(port); 
			this.queue = queue;
		} catch (Exception e) {  LOGGER.log( Level.FINE, e.toString(), e);  }
	}
	
	@Override
	public void run() {
		System.out.println("ACK is starting");
		// Receive packet back
		try 
		{
			System.out.println("Reply: ");
			while(!done) {
				byte[] buffer = new byte[1000];
				DatagramPacket replyPacket = new DatagramPacket (buffer, buffer.length);
				
				udp.receive(replyPacket);
				Segment ackseg = new Segment(buffer);
				System.out.print(new String(replyPacket.getData()));
			}
		} catch(Exception e) {  LOGGER.log( Level.FINE, e.toString(), e); }
		
	}
	
	public synchronized void processACK (Segment ack) {
		int acknum = ack.getSeqNum();
		System.out.println("ack: " + acknum);
		
		try {
			// if ack in window 
			if (acknum > queue.element().getSeqNum()) 
			{
				//cancel timer
				// remove all segments that are acked by this ack 
				// if there are any pending segments in queue, start timer
				queue.remove();
				
				if (!queue.isFull()) 
				{
					
				}

			}
			// if ack not in window do nothing
			
			
			
			
				
			
		} catch (Exception e) { LOGGER.log( Level.FINE, e.toString(), e);  }

	}

}
