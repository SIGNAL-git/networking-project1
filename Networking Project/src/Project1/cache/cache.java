package Project1.cache;

import java.io.*;
import java.net.*;
import java.nio.file.*;

import Project1.tcp.tcp_transport;
import Project1.snw.snw_transport;

public class cache {

	static final Path FOLDER = Paths.get(Paths.get("").toAbsolutePath() + "/Project1/cache_fl");
	static ServerSocket cacheSock;
	
	static tcp_transport clientCMDSock;
	static tcp_transport clientFileSockTCP;
	static tcp_transport serverCMDSock;
	static tcp_transport serverFileSockTCP;
	
	static snw_transport fileSockSNW;
	
	public static void main(String[] args) throws IOException
	{
		// Translate input into usable components
		int cachePort = Integer.parseInt(args[0]);
		InetAddress serverIP = InetAddress.getByName(args[1]);
		int serverPort = Integer.parseInt(args[2]);
		String transportType = args[3];
		
		// Make a new server socket and bind it to the specified port
		cacheSock = new ServerSocket(cachePort);
		System.out.println("Cache is listening on port " + cachePort);
		
		// Wait for incoming connections
		if (transportType.equals("tcp"))
		{
			serverCMDSock = new tcp_transport(new Socket(serverIP, serverPort));
			serverFileSockTCP = new tcp_transport(new Socket(serverIP, serverPort));
			clientCMDSock = new tcp_transport(cacheSock.accept());
			clientFileSockTCP = new tcp_transport(cacheSock.accept());
			System.out.println("Client connected: " + clientCMDSock.toString());
			
		}
		else if (transportType.equals("snw"))
		{
			serverCMDSock = new tcp_transport(new Socket(serverIP, serverPort));
			clientCMDSock = new tcp_transport(cacheSock.accept());
			System.out.println("Client connected: " + clientCMDSock.toString());
			
			fileSockSNW = new snw_transport(new DatagramSocket(cachePort));
		}
		else
		{
			System.out.println("Invalid transport type");
			System.exit(0);
		}
		
		byte[] data;
		String cmd;
		
		while (true)
		{
			// Wait for command
			data = clientCMDSock.get();
			
			cmd = new String(data);
			
			// Debugging
			System.out.println("Command: " + cmd);
			
			// Act on command
			if (cmd.equals("quit"))
			{
				quit(transportType);
			}
			else if (cmd.substring(0, 3).equals("get"))
			{
				if (transportType.equals("tcp"))
				{
					putTCP(cmd);
				}
				else if (transportType.equals("snw"))
				{
					putSNW(cmd);
				}
			}
			
		}
	}
	
	public static void putTCP(String cmd) throws IOException
	{
		File file = new File(FOLDER + "/" + cmd.substring(4));
		String feedback;
		
		// File exists on cache
		if (file.exists() && !file.isDirectory())
		{
			clientFileSockTCP.put(file);
			clientCMDSock.put("File delivered from cache.");
			System.out.println("File uploaded: " + file.getName());
		}
		else if (!file.isDirectory()) // File does not exist on cache
		{
			// Request file from server
			feedback = getTCP(cmd);
			
			// File exists on server
			if (!(feedback.equals("File not found.")))
			{
				clientFileSockTCP.put(file);
				clientCMDSock.put(feedback);
				System.out.println("File uploaded: " + file.getName());
			}
			else // File does not exist on server
			{
				System.out.println("Request for file failed: " + file.getName());
				clientCMDSock.put("File not found.");
			}
		}
		else // Invalid request
		{
			System.out.println("Request for file failed: " + file.getName());
			clientCMDSock.put("File not found.");
		}
	}
	
	public static String getTCP(String cmd) throws IOException
	{
		File file = new File(FOLDER + "/" + cmd.substring(4));
		String feedback;
		
		System.out.println("Downloading file: " + file.getName());
		
		// Send request to download file
		serverCMDSock.put(cmd);
		
		// Wait for feedback to confirm file exists
		feedback = new String(serverCMDSock.get());
		
		// If file does not exist
		if (feedback.equals("File not found."))
		{
			return feedback;
		}
		
		// File exists, download it locally
		serverFileSockTCP.get(file);
		
		return feedback;
	}
	
	public static void putSNW(String cmd) throws IOException
	{
		File file = new File(FOLDER + "/" + cmd.substring(4));
		InetAddress clientIP = clientCMDSock.getIP();
		int clientPort;
		
		// Check if file exists on cache
		if (file.exists() && !file.isDirectory())
		{
			// Request for client to accept file
			clientCMDSock.put("OK");
			
			clientPort = Integer.parseInt(new String(clientCMDSock.get()));
			
			// Send file
			fileSockSNW.setDestination(clientIP, clientPort);
			int error = fileSockSNW.put(file);
			if (error == -1) // Did not receive ACK
			{
				clientCMDSock.put("Did not receive ACK. Terminating.");
			}
			else // File successfully uploaded
			{
				clientCMDSock.put("File delivered from cache.");
				System.out.println("File uploaded: " + file.getName());
			}
		}
		else if (!file.isDirectory()) // File does not exist on cache
		{
			// Get file from server
			String feedback = getSNW(cmd);

			if (feedback.equals("File not found.")) // File does not exist on server
			{
				clientCMDSock.put(feedback);
				System.out.println("Request for file failed: " + file.getName());
			}
			else // File exists on server and has been downloaded
			{
				feedback = new String(serverCMDSock.get());
				// Tell client to start receiving packets
				clientCMDSock.put("OK");
				
				clientPort = Integer.parseInt(new String(clientCMDSock.get()));
				
				// Send file to client
				fileSockSNW.setDestination(clientIP, clientPort);
				int error = fileSockSNW.put(file);
				if (error == -1) // Did not receive ACK
				{
					clientCMDSock.put("Did not receive ACK. Terminating.");
				}
				else // File successfully uploaded
				{
					clientCMDSock.put(feedback);
					System.out.println("File uploaded: " + file.getName());
				}
			}
		}
		else // Invalid file
		{
			clientCMDSock.put("File not found.");
		}
	}
	
	public static String getSNW(String cmd) throws IOException
	{
		File file = new File(FOLDER + "/" + cmd.substring(4));
		String feedback;
		
		// Send request to download file
		serverCMDSock.put(cmd);
		
		// Wait for feedback to confirm file download
		feedback = new String(serverCMDSock.get());
		
		// File exists on server
		if (!(feedback).equals("File not found."))
		{
			// Give port to send to
			serverCMDSock.put(String.valueOf(fileSockSNW.getPort()));
			
			// Get file
			System.out.println("Downloading file: " + file.getName());
			int error = fileSockSNW.get(file);
			if (error != 0) // File transfer failed for some reason
			{
				feedback = getSNW(cmd); // Try again
				return feedback;
			}
			else // Successfully downloaded file
			{
				return feedback;
			}
		}
		else // File does not exist on server
		{
			// Returns "File not found." on failed get
			return feedback;
		}
	}
	
	public static void quit(String transportType) throws IOException
	{
		clientCMDSock.quit();
		serverCMDSock.quit();
		
		if (transportType.equals("tcp"))
		{
			serverFileSockTCP.quit();
			clientFileSockTCP.quit();
		}
		else if (transportType.equals("snw"))
		{
			fileSockSNW.quit();
		}
		
		cacheSock.close();
		
		System.out.println("Sockets closed.");
		System.exit(0);
	}
}