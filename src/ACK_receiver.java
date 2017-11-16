import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;
import cpsc441.a3.shared.*;


public class ACK_receiver implements Runnable {
	
	private static final Logger LOGGER = Logger.getLogger( FastFtp.class.getName() );
	
	DatagramSocket udpSocket = null;
	FastFtp ftp;
	boolean running = false;
	
	//public ACK_receiver (FastFtp fftp, DatagramSocket socket, InetAddress serverIP, int serverPort, boolean status) {
	public ACK_receiver (FastFtp fftp, DatagramSocket socket, boolean status) {
		this.ftp = fftp;
		this.udpSocket = socket;
		this.running = status;
		//udpSocket.connect(serverIP, serverPort);
		//System.out.println("ack udp socket has port num:  " + udpSocket.getPort());
		//System.out.println("ack udp socket is connected:  " + udpSocket.isConnected());
	}
	
	
	@Override
	public void run() {	
		System.out.println("reciever started");
		// Receive packet back
			while(this.running) 
			{
				byte[] buffer = new byte[Segment.MAX_PAYLOAD_SIZE];
				try 
				{	
					DatagramPacket replyPacket = new DatagramPacket (buffer, buffer.length);					
					udpSocket.receive(replyPacket);
					//System.out.println("Got ack");
					//System.out.print(new String(replyPacket.getData()));
					Segment ackseg = new Segment(buffer);
					ftp.processACK(ackseg);				
				} catch(Exception e) {  LOGGER.log( Level.FINE, e.toString(), e); running = false; }
			}
	}
	

}
