import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.logging.Level;
import java.util.logging.Logger;
import cpsc441.a3.shared.*;


public class ACK_receiver implements Runnable {
	
	private static final Logger LOGGER = Logger.getLogger( FastFtp.class.getName() );
	DatagramSocket udpSocket;
	TxQueue queue;
	
	public ACK_receiver (DatagramSocket socket, TxQueue queue) {
		this.udpSocket = socket; 
		this.queue = queue;
	}
	
	@Override
	public void run() {
		System.out.println("ACK is starting");
		// Receive packet back
		try 
		{
			System.out.println("Reply: ");
			byte[] buffer = new byte[8*1024];
			DatagramPacket replyPacket = new DatagramPacket (buffer, buffer.length);
			udpSocket.receive(replyPacket);
			Segment ackseg = new Segment(buffer);
			int acknum = ackseg.getSeqNum();
			System.out.print(new String(replyPacket.getData()));
			
			if (acknum > queue.element().getSeqNum()) {
				queue.remove();
			}
			
		} catch(Exception e) {  LOGGER.log( Level.FINE, e.toString(), e); }
		
		
	}

}
