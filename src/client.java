import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Scanner;

public class client {

	static tcp_transport serverCMDSock;
	static tcp_transport serverFileSockTCP;
	
	static tcp_transport cacheCMDSock;
	static tcp_transport cacheFileSockTCP;
	
	static final Path FOLDER = Paths.get(Paths.get("").toAbsolutePath() + "/client_fl");
	static Scanner input = new Scanner(System.in);
	
	public static void main(String[] args) throws NumberFormatException, UnknownHostException, IOException
	{
		if (args.length != 5)
		{
			System.out.println("Usage: java client.java <server ip> <server port> <cache ip> <cache port> <tcp/udp>");
			System.exit(0);
		}

		// Translate input into usable components
		InetAddress serverIP = InetAddress.getByName(args[0]);
		int serverPort = Integer.parseInt(args[1]);
		InetAddress cacheIP = InetAddress.getByName(args[2]);
		int cachePort = Integer.parseInt(args[3]);
		String transportType = args[4];

		if (args[4] == "udp")
		{
			transportType = "snw";
		}
		
		if (transportType.equals("tcp"))
		{
			cacheCMDSock = new tcp_transport(new Socket(cacheIP, cachePort));
			cacheFileSockTCP = new tcp_transport(new Socket(cacheIP, cachePort));
			
			serverCMDSock = new tcp_transport(new Socket(serverIP, serverPort));
			serverFileSockTCP = new tcp_transport(new Socket(serverIP, serverPort));
		}
		else if (transportType.equals("snw"))
		{
			cacheCMDSock = new tcp_transport(new Socket(cacheIP, cachePort));
			serverCMDSock = new tcp_transport(new Socket(serverIP, serverPort));
		}
		else
		{
			System.out.println("Invalid transport type");
			System.out.println("Usage: java client.java <server ip> <server port> <cache ip> <cache port> <tcp/udp>");
			System.exit(0);
		}
		
		String cmd;
		
		while (true)
		{
			System.out.print("Enter command: ");
			cmd = input.nextLine();
			
			if (cmd.length() < 4)
			{
				System.out.println("Invalid input.");
			}
			else if (cmd.equals("quit"))
			{
				quit(transportType);
			}
			else if (cmd.substring(0, 3).equals("put"))
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
			else if (cmd.substring(0, 3).equals("get"))
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
			else
			{
				System.out.println("Invalid input.");	
			}
		}
	}
	
	/**
	 * Sends a file to the server via TCP
	 * @param cmd
	 * @throws IOException
	 */
	public static void putTCP(String cmd) throws IOException
	{
		File file = new File(FOLDER + "/" + cmd.substring(4));
		String feedback;
		System.out.println(file.getAbsolutePath());
		
		// Check if file is valid
		if (file.exists() && !file.isDirectory())
		{
			// Send file
			serverFileSockTCP.put(file);
			
			// Request for server to accept file
			serverCMDSock.put(cmd);
			
			// Wait for feedback
			feedback = new String(serverCMDSock.get());
			
			System.out.println("Server response: " + feedback);
		}
		else // Invalid file
		{
			// Message generated locally as file is local
			System.out.println("File not found.");
		}
	}
	
	/**
	 * Receives a file from the cache via TCP
	 * @param cmd
	 * @throws IOException
	 */
	public static void getTCP(String cmd) throws IOException
	{
		File file = new File(FOLDER + "/" + cmd.substring(4));
		String feedback;
		
		// Send request to download file
		cacheCMDSock.put(cmd);
		
		// Wait for feedback to confirm cache/server has file
		feedback = new String(cacheCMDSock.get());
		
		// File exists on server
		if (!(feedback).equals("File not found."))
		{
			// Get file
			cacheFileSockTCP.get(file);
		}
		
		// Prints "File not found." on failed get
		System.out.println("Cache response: " + feedback);
	}
	
	/**
	 * Sends a file to the server via UDP SNW
	 * @param cmd
	 * @throws IOException
	 */
	public static void putSNW(String cmd) throws IOException
	{
		snw_transport fileSockSNW = new snw_transport(new DatagramSocket());
		File file = new File(FOLDER + "/" + cmd.substring(4));
		String feedback;
		InetAddress serverIP = serverCMDSock.getIP();
		int serverPort = serverCMDSock.getPort();
		
		// Check if file is valid
		if (file.exists() && !file.isDirectory())
		{
			System.out.println("Uploading " + file.getPath());
			
			// Request for server to accept file
			serverCMDSock.put(cmd);
			
			// Send file
			fileSockSNW.setDestination(serverIP, serverPort);
			int error = fileSockSNW.put(file);
			if (error == -1)
			{
				System.out.println("Did not receive ACK. Terminating.");
			}
			else
			{
				// Wait for feedback
				feedback = new String(serverCMDSock.get());
				System.out.println("Server response: " + feedback);
			}
		}
		else // Invalid file
		{
			// Message generated locally as file is local
			System.out.println("File not found.");
		}
	}
	
	/**
	 * Receives a file from the cache via UDP SNW
	 * @param cmd
	 * @throws IOException
	 */
	public static void getSNW(String cmd) throws IOException
	{
		snw_transport fileSockSNW = new snw_transport(new DatagramSocket());
		File file = new File(FOLDER + "/" + cmd.substring(4));
		String feedback;
		
		// Send request to download file
		cacheCMDSock.put(cmd);
		
		// Wait for feedback to confirm cache/server has file
		feedback = new String(cacheCMDSock.get());
		
		// File exists on cache/server
		if (!(feedback).equals("File not found."))
		{
			cacheCMDSock.put(String.valueOf(fileSockSNW.getPort()));
			
			// Get file
			fileSockSNW.get(file);
			feedback = new String(cacheCMDSock.get());
			System.out.println("Cache response: " + feedback);
		}
		else
		{
			// Prints "File not found." on failed get
			System.out.println("Cache response: " + feedback);
		}
		
		fileSockSNW.quit();
	}
	
	/**
	 * Closes all sockets and exits the program
	 * @param transportType
	 * @throws IOException
	 */
	public static void quit(String transportType) throws IOException
	{
		serverCMDSock.put("quit");
		cacheCMDSock.put("quit");
		
		serverCMDSock.quit();
		cacheCMDSock.quit();
		
		if (transportType.equals("tcp"))
		{
			serverFileSockTCP.quit();
			cacheFileSockTCP.quit();
		}
		
		System.out.println("Sockets closed.");
		System.exit(0);
	}
}
