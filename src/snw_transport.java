import java.io.*;
import java.net.*;
import java.util.Arrays;

public class snw_transport {
	
	DatagramSocket sock;
	InetAddress IP;
	int port;
	
	public snw_transport(DatagramSocket sock) throws IOException
	{
		this.sock = sock;
		
		// Set 1 second timeout
		sock.setSoTimeout(1000);
	}
	
	public void setDestination(InetAddress IP, int port)
	{
		this.IP = IP;
		this.port = port;
	}
	
	public void setDestination(DatagramPacket packet)
	{
		this.IP = packet.getAddress();
		this.port = packet.getPort();
	}
	
	public int put(byte[] data) throws IOException
	{
		DatagramPacket outPacket = new DatagramPacket(data, data.length, IP, port);
		sock.send(outPacket);
		
		return 0;
	}
	
	public int put(String message) throws IOException
	{
		byte[] data = message.getBytes();
		DatagramPacket outPacket = new DatagramPacket(data, data.length, IP, port);
		sock.send(outPacket);
		
		return 0;
	}
	
	public int put(File file) throws IOException
	{	
		FileInputStream fReader = new FileInputStream(file);
		DatagramPacket inPacket = null;
		byte[] buffer;
		int bufferLength;
		
		// Send LEN packet
		put("LEN:" + file.length());
		
		while (fReader.available() != 0)
		{
			// Send datagram
			if (fReader.available() >= 1000)
			{
				bufferLength = 1000;
			}
			else
			{
				bufferLength = fReader.available();
			}

			buffer = new byte[bufferLength];
			fReader.read(buffer, 0, bufferLength);
			put(buffer);
			
			// Receive response
			inPacket = get();
			if (inPacket == null) // Did not get response
			{
				fReader.close();
				return -1;
			}
			else
			{
				// Finished sending data
				if (new String(inPacket.getData()).equals("FIN"))
				{
					fReader.close();
					return 0;
				}
			}
		}
		
		// Finished reading file
		fReader.close();
		return 0;
	}
	
	public int get(File file) throws IOException
	{
		FileOutputStream fWriter = new FileOutputStream(file);
		DatagramPacket inPacket = null;

		// Wait for initial LEN packet
		while (inPacket == null)
		{
			inPacket = get();
		}

		// Save return address for responses
		setDestination(inPacket);
		
		int len = Integer.parseInt(new String(inPacket.getData()).substring(4));
		
		if (len > 0)
		{
			// Get first datagram
			inPacket = get();
			if (inPacket == null) // Did not receive data 
			{
				fWriter.close();
				return -1;
			}
			
			while (len > 0)
			{
				fWriter.write(inPacket.getData());
				len -= inPacket.getLength();
				if (len == 0) // Got expected length
				{
					put("FIN");
					
					fWriter.close();
					return 0;
				}
				else // Not done reading
				{
					put("ACK");
					
					inPacket = get();
					if (inPacket == null) // Premature termination
					{
						fWriter.close();
						return -2;
					}
				}
			}
		}
		
		fWriter.close();
		return 0;
	}
	
	public DatagramPacket get() throws IOException
	{
		byte[] buffer = new byte[1000];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		
		try {
			sock.receive(packet);
			
			// Truncate empty space
			packet.setData(Arrays.copyOfRange(buffer, 0, packet.getLength()));
			
			return packet;
		}
		catch (SocketTimeoutException s)
		{
			return null;
		}
	}
	
	public InetAddress getIP() throws IOException
	{
		return sock.getLocalAddress();
	}
	
	public int getPort() throws IOException
	{
		return sock.getLocalPort();
	}
	
	public void quit() throws IOException
	{
		sock.close();
	}
	
	public String toString()
	{
		return sock.toString();
	}
}
