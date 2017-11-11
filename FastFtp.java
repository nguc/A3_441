
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
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import cpsc441.a3.shared.*;

public class FastFtp {
	
	private static final Logger LOGGER = Logger.getLogger( FastFtp.class.getName() );
	
	int windowSize;
	int rtoTimer;
	String serverName;
	String PATHNAME = System.getProperty("user.dir") + "\\";
	String hostname; // Chi-Desktop @  home

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
		}
		catch (UnknownHostException ex)
		{
		    System.out.println("Hostname can not be resolved");
		}

			this.windowSize = windowSize;
			this.rtoTimer = rtoTimer;
			
			/*try
			{
				InetAddress  ip = InetAddress.getLocalHost();
				this.serverName = ip.getHostName();
				System.out.println("server name: " + serverName);
			}catch (UnknownHostException e) { e.printStackTrace(); }
			*/
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
			Socket tcpSocket = new Socket (serverName, serverPort);
			DatagramSocket udpSocket = new DatagramSocket ();
			//System.out.println("sockets opened");
			File file = new File(PATHNAME + fileName);
			
			
			//System.out.println("File: " + PATHNAME +fileName);
			long fileLength = file.length();
			//System.out.println("file length: " + fileLength);
			byte[] payload = new byte[(int) file.length()];
			try
			{
				FileInputStream fIn = new FileInputStream(file);
				fIn.read(payload);
				
				for (int i = 0; i < payload.length; i++)
				{
					System.out.print((char) payload[i]);
				}
			} 
			catch (FileNotFoundException e) { System.out.println(e.getMessage()); }
			catch (IOException e) { System.out.println(e.getMessage()); }
			
			
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
				
				// Get server UDP port number over TCP
				int serverUdpPort = dataIn.readInt();
				InetAddress serverIP = InetAddress.getByName("localhost");
				//System.out.println("\nserver Ip: " + serverIP);
				
				// send packet thread
				int seqnum = 0;
				try
				{
					Segment segment = new Segment(seqnum, payload);
					DatagramPacket sendPacket = new DatagramPacket(segment.getBytes(), segment.getBytes().length, serverIP, serverUdpPort);
					udpSocket.send(sendPacket);
					System.out.println("Sent packet");
					
					// Recieve packet back
					byte[] buffer = new byte[8*1024];
					DatagramPacket replyPacket = new DatagramPacket (buffer, buffer.length);
					udpSocket.receive(replyPacket);
					Segment ackseg = new Segment(buffer);
					int acknum = ackseg.getSeqNum();
					System.out.println(new String(replyPacket.getData()));
					
					
					udpSocket.close();
					tcpSocket.close();
					
				} catch (Exception e) {LOGGER.log( Level.SEVERE, e.toString(), e ); }
				
			}catch(IOException e) { LOGGER.log( Level.SEVERE, e.toString(), e); }
			
			
			
			
		}
		catch (Exception e) { LOGGER.log( Level.SEVERE, e.toString(), e); }
		
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
