import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.logging.Level;
import java.util.logging.Logger;
import cpsc441.a3.shared.*;


public class ACK_receiver implements Runnable {
	
	private static final Logger LOGGER = Logger.getLogger( FastFtp.class.getName() );
	
	DatagramSocket udpSocket = null;
	FastFtp ftp;
	Boolean running = false;
	
	public ACK_receiver (FastFtp fftp, DatagramSocket socket, boolean status) {
		this.ftp = fftp;
		this.udpSocket= socket;
		this.running = status;
		System.out.println(udpSocket.isConnected());
	}
	
	@Override
	public void run() {	
		// Receive packet back
			while(this.running) 
			{
				byte[] buffer = new byte[1000];
				try 
				{
					DatagramPacket replyPacket = new DatagramPacket (buffer, buffer.length);					
					udpSocket.receive(replyPacket);
					System.out.print(new String(replyPacket.getData()));
					Segment ackseg = new Segment(buffer);			
					ftp.processACK(ackseg);				
				} catch(Exception e) {  LOGGER.log( Level.FINE, e.toString(), e); }
				
			}
		
	}

}
