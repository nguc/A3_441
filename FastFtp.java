
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
	String hostname; // localhost
	String serverName;
	int serverUdpPort;
	Socket tcpSocket;
	DatagramSocket clientUDP;
	InetAddress localAddress;
	DataOutputStream dataOut;
	DataInputStream dataIn;
	FileInputStream fIn;
	File file;
	Timer timer;	
	TxQueue queue;
	

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
	try
	{
		try 
		{
			// Open a TCP and UDP connection
			this.tcpSocket = new Socket (serverName, serverPort);	
			this.file = new File(PATHNAME + fileName);			
			long fileLength = file.length();
			
			if (fileLength == 0)
			{
				System.out.println("Empty file");
				tcpSocket.close();
				return;
			}
			else {
			int localPortNum = tcpSocket.getLocalPort();
			//System.out.println("The local port numb is " + localPortNum);
			//this.clientUDP = new DatagramSocket(localPortNum);
			this.clientUDP = new DatagramSocket();
			System.out.println("client udp socket has port num:  " + clientUDP.getPort());
			
			dataOut = new DataOutputStream(tcpSocket.getOutputStream());
			dataIn = new DataInputStream(tcpSocket.getInputStream());
			
			// Send filename, file length, and local UDP port to server over TCP
			dataOut.writeUTF(fileName);
			dataOut.flush();
			
			dataOut.writeLong(fileLength);
			dataOut.flush();
			
			dataOut.writeInt(clientUDP.getLocalPort());
			dataOut.flush();			
			System.out.println("Done handshake");
			
			// Get server UDP port number over TCP
			serverUdpPort = dataIn.readInt();
			InetAddress serverIP = InetAddress.getByName(serverName);		
			clientUDP.connect(serverIP,serverUdpPort);
			
			System.out.println("client udp socket " + clientUDP.isConnected());
			System.out.println("client udp socket has port num:  " + clientUDP.getPort());
			//int localPortNum = tcpSocket.getLocalPort();
			//clientUDP= new DatagramSocket(serverUdpPort);
			
			// start the receiver thread
			ACK_receiver ar = new ACK_receiver(this,clientUDP, true);
			Thread receiver = new Thread(ar);
			receiver.start();
			
			// make segments, send segment to queue and send segment to server	
			fIn = new FileInputStream(this.file);
			byte[] bytes = new byte[Segment.MAX_PAYLOAD_SIZE];
			Segment segment = null;
			int seqnum = 0;
			int read;
			
			while ((read = fIn.read(bytes)) != -1) 
			{	
				System.out.println("Reading file");
				if (read < Segment.MAX_PAYLOAD_SIZE)
				{
					byte[] payload = new byte[read];
					System.arraycopy(bytes, 0, payload, 0, read);		
					segment = new Segment(seqnum, payload);
				}
				else 
					segment = new Segment(seqnum, bytes);
				
				// if queue is full then wait
				while (queue.isFull()) 
				{
					//System.out.println("queue full");
					Thread.yield();
				}
				
				this.processSend(segment);
				
				if (seqnum > windowSize) 
					seqnum = 0;						
				else 
					seqnum ++;
			}
			
			while(!queue.isEmpty())
			{
				System.out.println("queue not empty");
				Thread.yield();
			}
			
			//dataOut.write(0);
			//dataOut.flush();
		}
			} finally { // clean up
						timer.cancel();
						//ar.done();
						//receiver.join();
						
						fIn.close();
						dataOut.close();
						dataIn.close();
			  }	
		} 
		catch (IOException e) {  System.out.println(e.getMessage());  } 
		catch(NullPointerException e) { LOGGER.log( Level.FINE, e.toString(), e); }
			
}


public synchronized void processSend(Segment seg) {
	// send seg to the UDP socket
	// add seg to the transmission queue
	// if this is the first segment in transmission queue, start the timer
	System.out.println("Processing packet to send");
	DatagramPacket sendPacket = new DatagramPacket(seg.getBytes(), seg.getBytes().length, localAddress, serverUdpPort);
		try
		{
			queue.add(seg);
			clientUDP.send(sendPacket);
				
			if (queue.size() == 1) {
				timer = new Timer(true);
				timer.schedule(new TimeoutHandler(this, this.queue) , this.rtoTimer);
			}
			// check what in the window
			else
			{
				Segment[] window = queue.toArray();
				for (int i =0; i < window.length; i++) {System.out.println(window[i].toString());}	
			}
			// -------------------------- //
			System.out.println("Sent packet");
	} catch (Exception e) {  LOGGER.log( Level.FINE, e.toString(), e ); }
}



public synchronized void processACK (Segment ack) {
	int acknum = ack.getSeqNum();
	
	
	if(queue.element() != null)
	{
		System.out.println("ack received! num: " + acknum);
		if (ack.getSeqNum() > queue.element().getSeqNum())
		{
			timer.cancel();
			while(true)
			{
				if(queue.element() == null)
					break;
				if(queue.element().getSeqNum() < ack.getSeqNum())
				{
					try
					{
						queue.remove();
					} catch (InterruptedException e) {   LOGGER.log( Level.FINE, e.toString(), e );  }
				}
				else
					break;
			}
			
			if (!queue.isEmpty())
			{
				timer = new Timer(true);
				timer.schedule(new TimeoutHandler(this, this.queue) , this.rtoTimer);
			}
		}
	}
	// Queue is empty
	else
		System.out.println("No more elements in the queue.");
}



public synchronized void processTimeout(Segment[] pending_segs) {
	// get the list of all pending segments from the transmission queue
	// go through the list and send all segments to the UDP socket
	// if there are any pending segments in transmission queue, start the timer
	int i = pending_segs.length;
	while (i > 0) 
	{
		byte[] bytes = pending_segs[i-1].getBytes();
		DatagramPacket sendPacket = new DatagramPacket(bytes, bytes.length, localAddress, serverUdpPort);
		try
		{
			clientUDP.send(sendPacket);
			//processSend(pending_segs[i]);
			if (!queue.isEmpty())
			{
				timer = new Timer(true);
				timer.schedule(new TimeoutHandler(this, queue), rtoTimer);
			}
		} catch(IOException e){  e.printStackTrace();  }
		  catch(IllegalArgumentException e){  e.printStackTrace(); }
		
		i--; // move to next segment in window
	}
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
