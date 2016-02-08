import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class ClientCommunication {
	private InputStream input;
	private OutputStream output;
	private Downloader m_Downloader;
	private Socket m_ClientSocket = null;
	private boolean m_IsHTTPRequestReady = false;
	private boolean m_IsChunked = false;
	private DataOutputStream m_OutToClient;
	private String m_HTTPRequest;
	private HTTPRequest m_HttpRequest;
	private String m_Root;
	private String m_DefaultPage;
	private ServerSocket m_ServerSocket;
	private int m_Port;
	
	//lab2

	//lab2end
	
	public ClientCommunication(String i_Root, int i_Port, String i_DefaultPage) {
		m_Root = i_Root;
		m_Port = i_Port;
		m_DefaultPage = i_DefaultPage;
	}
	
	public void doClientRequestFlow() {
		openServerSocket();
		waitToClientSocket();			
		
		try {
			m_HttpRequest = new HTTPRequest(m_Root, m_DefaultPage);
			input  = m_ClientSocket.getInputStream();
			output = m_ClientSocket.getOutputStream();

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

					hm = m_HttpRequest.handleHttpRequest(m_HTTPRequest, m_IsChunked, m_In, false, null);
					

					// TODO: Clean and delete some stuff here.
					String head = (String)hm.get("HEADER");
					byte[] html = (byte[]) hm.get("Content");


				//	System.out.println(head);


					if (m_HttpRequest.getHTMLParams() != null){
						String params = "";
						int sum = 0;
						for (Map.Entry<String,String> entry : m_HttpRequest.getHTMLParams().entrySet()) {
							String key = entry.getKey();
							String value = entry.getValue();
							sum += key.length()+value.length();
						}
						params = Integer.toString(sum);
//						System.out.println(params);
						
						if (m_IsChunked){
							m_OutToClient.writeBytes(Integer.toHexString(params.length()));
							m_OutToClient.writeBytes("\r\n");
						}
						m_OutToClient.writeBytes(params);
					}

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
					
					//LAB 2
					if(!m_HttpRequest.getHTMLParams().isEmpty()){
						String responseMessage = "";
						String filePath = "";
						int size = 0;
						boolean success = false;
						try{
							m_Downloader = new Downloader();
							m_Downloader.initParams(m_HttpRequest.getHTMLParams());
			//			size = m_Downloader.getFileSizeFromURL("www.chesedu.org/#NavigationMenu_SkipLink");
						
							//	size = m_Downloader.getFileSizeFromURL("http://techslides.com/demos/sample-videos/small.mp4");
		//					size = m_Downloader.getFileSizeFromURL("http://www.israelbar.org.il/newsletter_register.asp");
//							size = m_Downloader.getFileSizeFromURL("www.ynet.co.il");
							responseMessage = "<h1>Crawler started successfully</h1><br>";
							
							success = true;
						} 
						catch (Exception e){
							responseMessage = "<h1>Crawler failed to start because: " + e.getMessage() + "</h1><br>";
						}
						finally {
							if (success){								
								final File folder = new File(m_Root + File.separator + "CrawlerResults");
								listFilesForFolder(folder);

								for(String fileName : listFilesForFolder(folder)){
									filePath = "CrawlerResults" + File.separator + fileName;
									responseMessage += "<a href='" + filePath + "'>" + fileName + "</a><br>";
								}
								responseMessage += "<a href='../'>BACK</a>\r\n\r\n";
							
							int newLength = responseMessage.length();
							String newHeader = head.substring(0, head.indexOf("content-length")) + "content-length: " + newLength + "\r\n\r\n" + responseMessage;
							
							m_OutToClient.writeBytes(newHeader);
							}
							else {
								hm = null;
								String i_HTTPRequest = "GET / HTTP/1.1\r\nHost: localhost:8080\r\n\r\n";
								byte[] responseMessageAsBytes = new byte[responseMessage.length()];
								responseMessageAsBytes = responseMessage.getBytes();
								hm = m_HttpRequest.handleHttpRequest(i_HTTPRequest, false, m_In, true, responseMessageAsBytes);
								String head1 = (String)hm.get("HEADER");
								byte[] html1 = (byte[]) hm.get("Content");
								
								m_OutToClient.writeBytes(head1);
								if (html1 != null){
									m_OutToClient.write(html1);
								}
							}
						}
						break;											
					} else {
					//	System.out.println(head);
						m_OutToClient.writeBytes(head);
						if (html != null){
							m_OutToClient.write(html);
						}
					}
					//END LAB2
//					System.out.println("here");
					clearRequestedData();
					//return;
					//System.out.println("clear");
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
				//		myThread.onClientCommComplete();
			} catch (Exception e) {
				// error in closing streams
				System.out.println(e.getMessage());
			}
		}
	}
	
	// remove from here
	
	public ArrayList<String> listFilesForFolder(final File folder) {
		ArrayList<String> listOfFiles = new ArrayList<>();
		
	    for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	            listFilesForFolder(fileEntry);
	        } else {
	            listOfFiles.add(fileEntry.getName());
	        }
	    }
	    
	    return listOfFiles;
	}
	
	// remove until here

	private void waitToClientSocket() {
		//At the beginning no client has connected so the client
		//socket is obviously null
		try {
			//The thread that running this server stops at this line
			//and waits for a client to connect.
			m_ClientSocket = this.m_ServerSocket.accept();
		} catch (IOException e) {
			System.err.println(e.getMessage());			
			throw new RuntimeException(
					"Error accepting client connection", e);
		}
	}

	private void openServerSocket() {
		try {
			this.m_ServerSocket = new ServerSocket(this.m_Port);
		} catch (IOException e) {
			throw new RuntimeException("Cannot open port " + m_Port, e);
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
	
	public String getRequestedUrl() {
		return m_Downloader.getRequestedUrl();
	}
	
	public boolean isTCPOpenPortsRequested() {
		return m_Downloader.isTCPOpenPortsRequested();
	}
	
	public boolean isRobotFileRespected() {
		return m_Downloader.isRobotFileRespected();
	}
	
	public String[] getRobotsFileContent() {
		return m_Downloader.getRobotsFile().split("\n");
	}

}
