import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

public class ConnectionRunnable implements Runnable{
	
	//Lab2
	private Downloader m_Downloader;
	
	private Socket m_clientSocket = null;
	private boolean m_isRun = true;
	private MyThread myThread;
	private Thread thread;

	private boolean m_IsHTTPRequestReady = false;
	private String m_Root;
	private String m_DefaultPage;
	private boolean m_IsChunked = false;
	
	private DataOutputStream m_OutToClient;
	private String m_HTTPRequest;
	public HTTPRequest m_HttpRequest;

	public ConnectionRunnable(MyThread myThread, String i_Root, String i_DeafultPage) {
		this.myThread = myThread;
		this.m_Root = i_Root;
		this.m_DefaultPage = i_DeafultPage;
		
		// Lab2
		m_Downloader = new Downloader();

	}

	public synchronized void run() {
		this.thread = Thread.currentThread();
		//While the thread is running if the is no client then it
		//tells him to wait until he will call to handle a client
		while(m_isRun) {
			if (m_clientSocket != null) {
				runTaskForClient();
			}
			else {
				synchronized (thread) {
					try {
						thread.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	InputStream input;
	OutputStream output;
	private void runTaskForClient() {		
		try {
			m_HttpRequest = new HTTPRequest(m_Root, m_DefaultPage);
			input  = m_clientSocket.getInputStream();
			output = m_clientSocket.getOutputStream();

			BufferedReader m_In = new BufferedReader (new InputStreamReader(input));
			m_OutToClient = new DataOutputStream (output);

			m_HTTPRequest = "";

			String s = "";
			int c;

			while (true){
				
				// read file by char
				while ((c = m_In.read()) != -1 && !m_IsHTTPRequestReady) {
					s += (char)c;
					if ((char) c == '\r'){
						char _c = (char)m_In.read();
						if (_c == '\n'){
							s += _c;
							m_HTTPRequest = buildHTTPRequest(m_HTTPRequest, s);
							s = "";
							break;
						}
					}
				}
				
				if (m_IsHTTPRequestReady){
					HashMap<String, Object> hm;

					hm = m_HttpRequest.handleHttpRequest(m_HTTPRequest, m_IsChunked, m_In);

					// TODO: Clean and delete some stuff here.
					String head = (String)hm.get("HEADER");
					byte[] html = (byte[]) hm.get("Content");


					System.out.println(head);
					m_OutToClient.writeBytes(head);
					
					if (html != null){
						m_OutToClient.write(html);
					}

/*					if (m_HttpRequest.getHTMLParams() != null){
						if (m_IsChunked){
							m_OutToClient.writeBytes(Integer.toHexString(m_HttpRequest.getHTMLParams().length()));
							m_OutToClient.writeBytes("\r\n");
						}
						m_OutToClient.writeBytes(m_HttpRequest.getHTMLParams());
					}*/
					
					// TODO: Consider moving this method to HTTPRequest.
					if (m_IsChunked){
						if (m_HttpRequest.isOK()){
							m_HttpRequest.readFileByChunks(m_OutToClient);
						}
						else if (m_HttpRequest.isNotFound()){
							String fnfMessage = m_HttpRequest.getNotFoundMessage;
							int iBytesToRead = fnfMessage.length();
							String hexBytesToRead = Integer.toHexString(iBytesToRead);

							m_OutToClient.writeBytes(hexBytesToRead);
							m_OutToClient.writeBytes("\r\n");
							
							m_OutToClient.writeBytes(fnfMessage.toString());
							m_OutToClient.writeBytes("\r\n");

						}
						m_OutToClient.writeBytes("0");
						m_OutToClient.writeBytes("\r\n");
						m_OutToClient.writeBytes("\r\n");
					}
					
					
					//Lab2
					if(!m_HttpRequest.getHTMLParams().isEmpty()){
						m_Downloader.initParams(m_HttpRequest.getHTMLParams());
						m_HttpRequest.printDictionary(m_Downloader.getHashedURL());
					}
					
					clearRequestedData();
				}
			}
		} catch (Exception e){
			// General exception with relevant message. There are many 
			// possible exceptions and we let the server handle them.
		//	System.err.println("ERROR! " + e.getMessage());
			
		} finally{
			try {
				m_OutToClient.close();
				output.close();
				input.close();

				//Finish handling client
				myThread.onClientCommComplete();
			} catch (Exception e) {
				// error in closing streams
				System.out.println(e.getMessage());
			}
		}
	}

	private String buildHTTPRequest(String hTTPRequest, String i_String) {
		
		if (i_String.contains("chunked") && i_String.contains("yes")){
			m_IsChunked = true;
		}
		if (i_String.equals("\r\n")){
			m_IsHTTPRequestReady = true;
			return hTTPRequest;
		}
		hTTPRequest += i_String;
		return hTTPRequest;
	}
	
	private void clearRequestedData() throws IOException{
		m_IsChunked = false;
		m_HTTPRequest = "";
		m_HttpRequest.clear();
		m_IsHTTPRequestReady = false;
		
	}

	public boolean isBusy() {
		return m_clientSocket != null;
	}

	/**
	 * Notifying the sleeping thread that it has a client request to handle 
	 * @param clientSocket
	 * @throws IOException 
	 */
	public void communicate(Socket clientSocket){// throws IOException {		
		this.m_clientSocket = clientSocket;

		synchronized (thread) {
			thread.notify();
		}
	}

	/**
	 * Stops the client request upon calling by changing the running
	 * mode of the variable m_isRun to false 
	 */
	public void stop() {
		this.m_isRun = false;
		synchronized (thread) {
			thread.notify();
		}
	}
}