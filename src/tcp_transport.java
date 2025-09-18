import java.io.*;
import java.net.*;

public class tcp_transport {
	
	Socket sock;
	BufferedInputStream downStream;
	BufferedOutputStream upStream;
	
	public tcp_transport(Socket sock) throws IOException
	{
		this.sock = sock;
		downStream = new BufferedInputStream(this.sock.getInputStream());
		upStream = new BufferedOutputStream(this.sock.getOutputStream());
	}
	
	public int put(byte[] data) throws IOException
	{
		upStream.write(data);
		upStream.flush();
		
		return 0;
	}
	
	public int put(String data) throws IOException
	{
		upStream.write(data.getBytes());
		upStream.flush();

		return 0;
	}
	
	public int put(File file) throws IOException
	{
		FileInputStream fReader = new FileInputStream(file);
		
		while (fReader.available() != 0)
		{
			upStream.write(fReader.read());
		}
		fReader.close();
		upStream.flush();
		
		return 0;
	}
	
	public byte[] get() throws IOException
	{
		waitForData();
		
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		byte[] data;
		
		while (hasData())
		{
			bytes.write(downStream.read());
		}
		data = bytes.toByteArray();
		bytes.reset();
		return data;
	}
	
	public void get(File file) throws IOException
	{
		waitForData();
		
		FileOutputStream fWriter = new FileOutputStream(file);
		
		// Overwrite local file with received file
		if (file.exists())
		{
			file.delete();
		}
		
		// Read file
		while (downStream.available() != 0)
		{
			fWriter.write(downStream.read());
			fWriter.flush();
		}
		fWriter.close();
	}
	
	public int quit() throws IOException
	{
		sock.close();
		return 0;
	}
	
	public void waitForData() throws IOException
	{
		while (!hasData()) {};
	}
	
	public boolean hasData() throws IOException
	{
		if (downStream.available() != 0)
		{
			return true;
		}
		
		return false;
	}
	
	public InetAddress getIP() throws IOException
	{
		return sock.getInetAddress();
	}
	
	public int getPort() throws IOException
	{
		return sock.getPort();
	}
	
	public int gletPort() throws IOException
	{
		return sock.getLocalPort();
	}
	
	public String toString()
	{
		return sock.toString();
	}
}
