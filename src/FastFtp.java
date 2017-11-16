
/**
 * FastFtp Class 
 *
 */

import java.io.DataInputStream;   
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import cpsc441.a3.shared.*;

public class FastFtp{
	
	private static final Logger LOGGER = Logger.getLogger( FastFtp.class.getName() );
	
	String PATHNAME = System.getProperty("user.dir") + "\\";
	int rtoTimer;
	int windowSize;
	String fileName;
	String hostname; // localhost
	String serverName;
	int serverUdpPort;
	Socket tcpSocket;
	DatagramSocket clientUDP;
	DataOutputStream dataOut;
	DataInputStream dataIn;
	FileInputStream fIn;
	Timer timer;	
	TxQueue queue;
	Thread receiver;
	

	/**
     * Constructor to initialize the program 
     * 
     * @param windowSize	Size of the window for Go-Back_N in terms of segments
     * @param rtoTimer		The time-out interval for the retransmission timer
     */
	public FastFtp(int windowSize, int rtoTimer) {
		// to be completed
		queue = new TxQueue(windowSize);
		this.windowSize = windowSize;
		this.rtoTimer = rtoTimer;		
	}
	

    /**
     * Sends the specified file to the specified destination host:
     * 1. send file/connection infor over TCP
     * 2. start receving thread to process coming ACKs
     * 3. send file segment by segment
     * 4. wait until transmit queue is empty, i.e., all segments are ACKed
     * 5. clean up (cancel timer, interrupt receving thread, close sockets/files)
     * 
     * @param serverName	Name of the remote server
     * @param serverPort	Port number of the remote server
     * @param fileName		Name of the file to be trasferred to the rmeote server
     */
	public void send(String serverName, int serverPort, String fileName) {
		// to be completed
		this.fileName = fileName;
		try 
		{
			// Open a TCP and UDP connection
			this.tcpSocket = new Socket (serverName, serverPort);	
			this.clientUDP = new DatagramSocket(8888);
			
			File file = new File(PATHNAME + fileName);		
			long fileLength = file.length();
				
			dataOut = new DataOutputStream(tcpSocket.getOutputStream());
			dataIn = new DataInputStream(tcpSocket.getInputStream());
		
			try 
			{ 
				// Send filename, file length, and local UDP port to server over TCP
				dataOut.writeUTF(fileName);								
				dataOut.writeLong(fileLength);
				dataOut.writeInt(clientUDP.getLocalPort());
				dataOut.flush();			
					
				// Get server UDP port number over TCP
				serverUdpPort = dataIn.readInt();
				this.clientUDP.connect(tcpSocket.getInetAddress(), serverUdpPort);
				fIn = new FileInputStream(file);
				
				// start the receiver thread
				receiver = new Thread(new ACK_receiver(this, clientUDP));
				receiver.start();
				
				// make segments, send segment to queue and send segment to server	
				byte[] bytes = new byte[Segment.MAX_PAYLOAD_SIZE];
				Segment segment = null;
				int seqnum = 0;
				int read;
				
				while ((read = this.fIn.read(bytes)) != -1) 
				{	
					if (read < Segment.MAX_PAYLOAD_SIZE)
					{
						byte[] payload = new byte[read];
						System.arraycopy(bytes, 0, payload, 0, read);		
						segment = new Segment(seqnum, payload);
					}
					else 
						segment = new Segment(seqnum, bytes);
					
					while (queue.isFull()) 
					{
						Thread.yield();
					}
					
					this.processSend(segment);
					seqnum ++;
				}
				
				while(!queue.isEmpty())
				{
					Thread.yield();
				}
				
				cleanUp();
				
			} catch (Exception e) { LOGGER.log( Level.FINE, e.toString(), e); }
		} 
		catch (IOException e) {  System.out.println(e.getMessage());  } 
		catch(NullPointerException e) { LOGGER.log( Level.FINE, e.toString(), e); }			
}


public synchronized void processSend(Segment seg) {
	// send seg to the UDP socket
	// add seg to the transmission queue
	// if this is the first segment in transmission queue, start the timer
	DatagramPacket sendPacket = new DatagramPacket(seg.getBytes(), seg.getBytes().length);
		try
		{
			this.queue.add(seg);
			clientUDP.send(sendPacket);
				
			if (this.queue.size() == 1) {
				timer = new Timer(true);
				timer.schedule(new TimeoutHandler(this, this.queue) , this.rtoTimer);
			}
	} catch (Exception e) {  LOGGER.log( Level.FINE, e.toString(), e ); }
}



public synchronized void processACK (Segment ack) {
	int acknum = ack.getSeqNum();
	
	if(this.queue.element() != null)
	{
		System.out.println("ack received! num: " + acknum);
		if (ack.getSeqNum() > this.queue.element().getSeqNum())
		{
			timer.cancel();
			while(true)
			{
				if(this.queue.element() == null)
					break;
				if(this.queue.element().getSeqNum() < ack.getSeqNum())
				{
					try
					{
						this.queue.remove();
					} catch (InterruptedException e) {   LOGGER.log( Level.FINE, e.toString(), e );  }
				}
				else
					break;
			}
			
			if (!this.queue.isEmpty())
			{
				timer = new Timer(true);
				timer.schedule(new TimeoutHandler(this, this.queue) , this.rtoTimer);
			}
		}
	}
	
	// Else queue is empty do nothing
}


public synchronized void processTimeout(Segment[] pending_segs) {
	// get the list of all pending segments from the transmission queue
	// go through the list and send all segments to the UDP socket
	// if there are any pending segments in transmission queue, start the timer
	int i = pending_segs.length;
	
	while (i > 0) 
	{
		byte[] bytes = pending_segs[i-1].getBytes();
		DatagramPacket sendPacket = new DatagramPacket(bytes, bytes.length);
		try
		{
			clientUDP.send(sendPacket);
			if (!this.queue.isEmpty())
			{
				timer = new Timer(true);
				timer.schedule(new TimeoutHandler(this, this.queue), rtoTimer);
			}
		} catch(IOException e){  e.printStackTrace();  }
		  catch(IllegalArgumentException e){  e.printStackTrace(); }
		
		i--; // move to next segment in window
	}
}


public void cleanUp() {
	try 
	{
		timer.cancel();
		receiver.interrupt();					
		fIn.close();
		dataOut.close();
		dataIn.close();
		clientUDP.close();
		tcpSocket.close();
	} catch (Exception e) { e.printStackTrace(); }
	
}

    /**
     * A simple test driver
     * 
     */
	public static void main(String[] args) {
		// all srguments should be provided
		// as described in the assignment description 
		if (args.length != 5) {
			System.out.println("incorrect usage, try again.");
			System.out.println("usage: FastFtp server port file window timeout");
			System.exit(1);
		}
		
		// parse the command line arguments
		// assume no errors
		String serverName = args[0];
		int serverPort = Integer.parseInt(args[1]);
		String fileName = args[2];
		int windowSize = Integer.parseInt(args[3]);
		int timeout = Integer.parseInt(args[4]);
		
		// send the file to server
		FastFtp ftp = new FastFtp(windowSize, timeout);
		System.out.printf("sending file \'%s\' to server...\n", fileName);
		ftp.send(serverName, serverPort, fileName);
		System.out.println("file transfer completed.");
	}


	
}
