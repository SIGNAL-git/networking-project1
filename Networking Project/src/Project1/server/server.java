package Project1.server;

import java.io.*;
import java.net.*;
import java.nio.file.*;

import Project1.tcp.tcp_transport;
import Project1.snw.snw_transport;

public class server {

	static final Path FOLDER = Paths.get(Paths.get("").toAbsolutePath() + "/Project1/server_fl");
	static ServerSocket serverSock;
	
	static tcp_transport clientCMDSock;
	static tcp_transport clientFileSockTCP;
	
	static tcp_transport cacheCMDSock;
	static tcp_transport cacheFileSockTCP;
	
	static snw_transport fileSockSNW;
	
	public static void main(String[] args) throws IOException
	{
		// Translate input into usable components
		int serverPort = Integer.parseInt(args[0]);
		String transportType = args[1];
		
		if (!transportType.equals("tcp") && !transportType.equals("snw"))
		{
			System.out.println("Invalid transport");
			System.exit(0);
		}
		
		// Make a new server socket and bind it to the specified port
		serverSock = new ServerSocket(serverPort);
		System.out.println("Server is listening on port " + serverPort);
		
		if (transportType.equals("tcp"))
		{
			cacheCMDSock = new tcp_transport(serverSock.accept());
			cacheFileSockTCP = new tcp_transport(serverSock.accept());
			System.out.println("Cache connected: " + cacheCMDSock.toString());
			
			clientCMDSock = new tcp_transport(serverSock.accept());
			clientFileSockTCP = new tcp_transport(serverSock.accept());
			System.out.println("Client connected: " + cacheCMDSock.toString());
			
		}
		else if (transportType.equals("snw"))
		{
			cacheCMDSock = new tcp_transport(serverSock.accept());
			System.out.println("Cache connected: " + cacheCMDSock.toString());
			clientCMDSock = new tcp_transport(serverSock.accept());
			System.out.println("Client connected: " + clientCMDSock.toString());
			
			fileSockSNW = new snw_transport(new DatagramSocket(serverPort));
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
			// Check for commands
			if (clientCMDSock.hasData())
			{
				data = clientCMDSock.get();
				cmd = new String(data);
				
				// Debugging
				System.out.println("Client Command: " + cmd);
				
				// Act on command
				if (cmd.equals("quit"))
				{
					quit(transportType);
				}
				else if (cmd.substring(0, 3).equals("put"))
				{
					if (transportType.equals("tcp"))
					{
						getTCP(cmd);
					}
					else if (transportType.equals("snw"))
					{
						getSNW(cmd);
					}
				}
			}
			else if (cacheCMDSock.hasData())
			{
				data = cacheCMDSock.get();
				cmd = new String(data);
				
				// Debugging
				System.out.println("Cache Command: " + cmd);
				
				// Act on command
				if (cmd.substring(0, 3).equals("get"))
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
	}
	
	public static void putTCP(String cmd) throws IOException
	{
		File file = new File(FOLDER + "/" + cmd.substring(4));
		
		if (file.exists() && !file.isDirectory())
		{
			cacheFileSockTCP.put(file);
			cacheCMDSock.put("File delivered from server.");
			System.out.println("File uploaded: " + file.getName());
		}
		else
		{
			cacheCMDSock.put("File not found.");
			System.out.println("Request for file failed: " + file.getName());
		}
	}
	
	public static void getTCP(String cmd) throws IOException
	{
		File file = new File(FOLDER + "/" + cmd.substring(4));
		
		clientFileSockTCP.get(file);
		
		clientCMDSock.put("File successfully uploaded.");
		
		System.out.println("File downloaded: " + file.getName());
	}
	
	public static void putSNW(String cmd) throws IOException
	{
		File file = new File(FOLDER + "/" + cmd.substring(4));
		InetAddress cacheIP = cacheCMDSock.getIP();
		int cachePort;

		// Check if file is valid
		if (file.exists() && !file.isDirectory())
		{
			System.out.println("Uploading " + file.getPath());
			
			// Tell cache to start receiving packets
			cacheCMDSock.put("OK");
			
			cachePort = Integer.parseInt(new String(cacheCMDSock.get()));
			
			// Send file
			fileSockSNW.setDestination(cacheIP, cachePort);
			int error = fileSockSNW.put(file);
			if (error == -1) // Did not receive ACK
			{
				cacheCMDSock.put("Did not receive ACK. Terminating.");
			}
			else // File successfully uploaded
			{
				cacheCMDSock.put("File delivered from server.");
				System.out.println("File uploaded: " + file.getName());
			}
		}
		else // Invalid file
		{
			cacheCMDSock.put("File not found.");
			System.out.println("Request for file failed: " + file.getName());
		}
	}
	
	public static void getSNW(String cmd) throws IOException
	{
		File file = new File(FOLDER + "/" + cmd.substring(4));
		
		System.out.println("Downloading file: " + file.getName());
		
		// Get file
		int error = fileSockSNW.get(file);
		if (error == -1)
		{
			clientCMDSock.put("Did not receive data. Terminating.");
		}
		else if (error == -2)
		{
			clientCMDSock.put("Data transmission terminated prematurely.");
		}
		else
		{
			System.out.println("Successfully downloaded file: " + file.getName());
			clientCMDSock.put("File uploaded successfully.");
		}
	}
	
	public static void quit(String transportType) throws IOException
	{
		clientCMDSock.quit();
		cacheCMDSock.quit();
		
		if (transportType.equals("tcp"))
		{
			clientFileSockTCP.quit();
			cacheFileSockTCP.quit();
		}
		else if (transportType.equals("snw"))
		{
			fileSockSNW.quit();
		}
		
		serverSock.close();
		
		System.out.println("Sockets closed.");
		System.exit(0);
	}
}