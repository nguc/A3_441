
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

public class FastFtp implements Runnable{
	
	private static final Logger LOGGER = Logger.getLogger( FastFtp.class.getName() );
	
	String PATHNAME = System.getProperty("user.dir") + "\\";
	String hostname; // Chi-Desktop @  home
	String serverName;
	Socket tcpSocket;
	DatagramSocket udpSocket;
	InetAddress serverIP;
	int serverUdpPort;
	Timer timer;
	int rtoTimer;
	File file;
	TxQueue queue;
	

	/**
     * Constructor to initialize the program 
     * 
     * @param windowSize	Size of the window for Go-Back_N in terms of segments
     * @param rtoTimer		The time-out interval for the retransmission timer
     */
	public FastFtp(int windowSize, int rtoTimer) {
		// to be completed
		
		try
		{
		    InetAddress addr;
		    addr = InetAddress.getLocalHost();
		    this.hostname = addr.getHostName();
		    System.out.println("hostname: " + hostname);
		}
		catch (UnknownHostException ex) { System.out.println("Hostname can not be resolved"); }

			queue = new TxQueue(windowSize);
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
			// Open a TCP and UDP connection
			this.tcpSocket = new Socket (serverName, serverPort);
			this.udpSocket = new DatagramSocket ();
			
			file = new File(PATHNAME + fileName);
			//System.out.println("File: " + PATHNAME +fileName);
			long fileLength = file.length();
			//System.out.println("file length: " + fileLength);
			
			DataOutputStream dataOut = new DataOutputStream(tcpSocket.getOutputStream());
			DataInputStream dataIn = new DataInputStream(tcpSocket.getInputStream());
			
			try
			{
				// Send filename, file length, and local UDP port to server over TCP
				dataOut.writeUTF(fileName);
				dataOut.flush();
				
				dataOut.writeLong(fileLength);
				dataOut.flush();
				
				dataOut.writeInt(udpSocket.getLocalPort());
				dataOut.flush();
				
				System.out.println("Done handshake");
				// Get server UDP port number over TCP
				serverUdpPort = dataIn.readInt();
				serverIP = InetAddress.getByName("localhost");
				udpSocket.connect(serverIP,serverUdpPort);
				SocketAddress serverAddr = udpSocket.getRemoteSocketAddress();
				System.out.println("remote socket addr: " + serverAddr);
					
				Thread receiver = new Thread (new ACK_receiver(serverIP, serverUdpPort,  queue));
				Thread sender = new Thread (this);
				
				receiver.start();
				sender.start();	
				
				dataOut.close();
				dataIn.close();
				
				
				
			
			} catch (Exception e) { LOGGER.log( Level.FINE, e.toString(), e); }
		} 
		catch (FileNotFoundException e) {  System.out.println(e.getMessage());  } 
		catch(IOException e) { LOGGER.log( Level.FINE, e.toString(), e); }		
	}

	
@Override
public void run() {
	// TODO Auto-generated method stub
	// send packet thread
	int seqnum = 0;
	
	try
	{
		FileInputStream fIn = new FileInputStream(file);
		int c = 0;
		byte[] payload = new byte[1000];
		// make segments, send segment to queue and send segment to server
		while ((c = fIn.read(payload)) > 0) 
		{
			Segment segment = new Segment(seqnum, payload);
			processSend(segment);
			seqnum ++;
		}
		
		fIn.close();
		udpSocket.close();
		tcpSocket.close();
		
	} catch (Exception e) {LOGGER.log( Level.FINE, e.toString(), e ); }
	
}
	


public synchronized void processSend(Segment seg) {
	// send seg to the UDP socket
	// add seg to the transmission queue
	// if this is the first segment in transmission queue, start the timer
	try
	{
		
			DatagramPacket sendPacket = new DatagramPacket(seg.getBytes(), seg.getBytes().length, serverIP, serverUdpPort);
			if (queue.isFull()) 
			{
	
			}
			
			if (queue.isEmpty()) {
				timer = new Timer(true);
				timer.schedule(new TimeoutHandler(queue), rtoTimer);
			}
			
			else
			{
				queue.add(seg);
				
				Segment[] window = queue.toArray();
				for (int i =0; i < window.length; i++)
				{
					System.out.println("Seq Num: " + window[i].getSeqNum() );
					System.out.println("Payload: " + window[i].getPayload());
				}
				
				udpSocket.send(sendPacket);
				System.out.println("Sent packet");
			}		
	} catch (Exception e) {LOGGER.log( Level.FINE, e.toString(), e ); }
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
