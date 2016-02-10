import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Create HTTP Request and get file content by URL.
 */
public class Downloader{

	private final int TIMEOUT = 22;
	private final int PORT = 80;
	private final int MAX_REDIRECTIONS = 5;
	private final int BUFFER_SIZE = 1024;

	private HashMap<String, String> m_URLHeaders;

	private Socket m_Socket;

	private String m_URL;
	private String m_HTMLPageDataWithoutScripts;
	private String m_RequestedFile;
	private String m_RobotsFile;
	private String m_Host = "";
	private String m_OriginalHost;
	private String m_HTMLPageData;
	
	private long m_StartTime = 0;
	private long m_EstimatedRTTTime = 0;
	private long m_EstimatedRTTTimeForRobots = 0;
	private long m_RequestedFileRTTTime = 0;

	private int m_chunkedFileSize;
	private int m_Redirections = 0;
	private int m_RequestedFileSize = 0;
	private int m_ContentLength = 0;

	private boolean m_IsChunked = false;
	private boolean m_ErrorFound = false;
	private boolean m_Robots = false;
	private boolean m_IsFile = false;
	
	private ArrayList<Integer> m_OpenPorts;

	public HashMap<String, String> getHeaders(){return this.m_URLHeaders;}
	public String getHTMLPageDataWithoutScripts(){return this.m_HTMLPageDataWithoutScripts;}
	public String getHTMLPageData() {return this.m_HTMLPageData;}
	
	public String getRobotsFile(String i_URL){
		try {
			checkRobotsFile(i_URL);
		} catch (Exception e) {
			System.err.println("no robots.txt file");
			return null;
		}
		return this.m_RobotsFile;
	}
	
	public String getRequestedDomainName() {return this.m_OriginalHost.split("\\.")[1];}
	public int getContentLength() {return (m_IsChunked) ? this.m_chunkedFileSize : this.m_HTMLPageData.length();}
	public boolean isRobotsEnabled() {return !this.m_RobotsFile.isEmpty();}
	public ArrayList<Integer> getOpenPorts() {return this.m_OpenPorts;}
	public long getRTTTime() {return (this.m_EstimatedRTTTime);}
	public long getRobotsRTTTime() {return this.m_EstimatedRTTTimeForRobots;}
	public long getRequestedFileRTTTime() {return this.m_RequestedFileRTTTime;}
	private TimeoutTimer timer;
	private boolean m_IsRobots;
	private boolean m_IsTCP;

	/**
	 * Initialises a new instance of the Downloader class.
	 * @param i_URL
	 * @throws Exception 
	 */
	public Downloader(String i_URL) throws Exception{
		getHTTPRequestData(false, i_URL);
	}

	public Downloader(){}
	
	/**
	 * Get HTTP Request
	 * @param i_URL
	 * @throws Exception 
	 */
	private void getHTTPRequestData(boolean onlyHeaders, String... args) throws Exception {
		if (!onlyHeaders){
			m_URL = getFixedURL(args[0]);
		} else {
			m_URL = args[0];
			m_Host = args[0];
			m_RequestedFile = args[1];
		}
		
		if (m_StartTime == 0){
			m_StartTime = System.currentTimeMillis();
		}
		
		m_Socket = new Socket(m_URL, PORT);

		m_URLHeaders = new HashMap<String, String>();

		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(m_Socket.getOutputStream()))); 
		out.println("GET " + m_RequestedFile + " HTTP/1.1");
		out.println("Host:" + m_Host);
		out.println();
		out.flush(); 

		BufferedReader reader = new BufferedReader(new InputStreamReader(m_Socket.getInputStream(), "UTF8")); 
		
		measureRTTTime();
		
		if (!m_Robots && !m_IsFile){
			m_OriginalHost = m_Host;
		}
		
		getHTTPRequestData(reader, onlyHeaders);

		reader.close(); 
		m_Socket.close();
	}
	
	/**
	 * Measure RTT time based
	 */
	private void measureRTTTime(){
		if (!m_Robots){
			if (m_IsFile){
				m_RequestedFileRTTTime = System.currentTimeMillis() - m_StartTime;
			} else {
				m_EstimatedRTTTime = System.currentTimeMillis() - m_StartTime;
			}
		}
		else {
			m_EstimatedRTTTimeForRobots = System.currentTimeMillis() - m_StartTime;
		}
	}

	/**
	 * Check if site contains robots.txt file, if so get it.
	 * @throws Exception 
	 */
	private void checkRobotsFile(String i_URL) throws Exception{
		try {
			m_Robots = true;
			m_StartTime = 0;
			getHTTPRequestData(false, i_URL + "/robots.txt");
		} catch (IOException e) {
			throw new Exception("No robots file");
		} finally{
			m_Robots = false;
			m_StartTime = 0;
		}
	}

	/**
	 * Get all HTTP request data.
	 * @param i_Reader
	 * @throws Exception 
	 */
	private void getHTTPRequestData(BufferedReader i_Reader, boolean onlyHeaders) throws Exception{
		int c = 0;
		String header = "";

		int newLineFlag = 0;
		m_IsChunked = false;

		timer = new TimeoutTimer(TIMEOUT);
		
		while ((c = i_Reader.read()) != -1) {
			if (timer.timeOut){
				throw new Exception("Timeout...");
			}

			header += (char) c;
			if ((char) c == '\r'){
				c = (char)i_Reader.read();

				if (c == '\n'){
					newLineFlag++;

					header += (char) c;

					if (newLineFlag == 2){
						if (m_ErrorFound){
							sendHTTPRequestWithDifferentURL(m_URLHeaders.get("Location"), onlyHeaders);
						}
						else {
							if(!onlyHeaders){
								if (!m_IsChunked){
									//TODO: 
									//	setHTMLPageData(i_Reader);
									setHTMLPageData(i_Reader);
								}
								else {
									readFileByChunks(i_Reader, false);
								}
							}
							// must be file
							else { 
								if (m_IsChunked){
									readFileByChunks(i_Reader, true);
								}
							}
						}
						newLineFlag = 0;
						break;
					}

					setHeader(header);
					header = "";
				}
			} else {
				newLineFlag = 0;
			}
		}
	}

	/**
	 * Set header line
	 * @param i_Header
	 */
	private void setHeader(String i_Header){
		String[] splittedString;
		System.out.println(i_Header);
		try{
			splittedString = i_Header.replaceAll("\\s","").split(":", 2);
			if (splittedString[0].toLowerCase().equals("content-length")){
				m_ContentLength = Integer.parseInt(splittedString[1]);
			}
			m_URLHeaders.put(splittedString[0], splittedString[1]);
			if (splittedString[0].equals("Transfer-Encoding") && splittedString[1].equals("chunked")){
				m_IsChunked = true;
			}
		} catch (Exception e){
			splittedString = i_Header.split(" ", 2);
			if (splittedString[1].contains("302 Found") || 
					splittedString[1].contains("301 Moved Permanently")
					){
				m_ErrorFound = true;
			}
		}

	}

	/**
	 * In case if redirection, try with new URL
	 * @param i_URL
	 * @throws Exception 
	 */
	private void sendHTTPRequestWithDifferentURL(String i_URL, boolean onlyHeaders) throws Exception{
		resetHTTPParams();
		m_Redirections++;
		if (m_Redirections >= MAX_REDIRECTIONS){
			throw new Exception("To many redirections...");
		}
		if (!onlyHeaders){
			getHTTPRequestData(onlyHeaders, i_URL);
		} else {
			getFileSizeFromURL(i_URL);
		}
	}

	/**
	 * Reset all relevant fields
	 */
	private void resetHTTPParams(){
		m_HTMLPageDataWithoutScripts = "";
		m_URL = "";
		m_URLHeaders.clear();
		m_ErrorFound = false;
		m_RobotsFile = "";
		m_HTMLPageData = "";
		//	timer = null;
	}

	/**
	 * Fix URL representation
	 * @param i_URL
	 * @return fixed URL "www.domain"
	 * @throws Exception 
	 */
	private String getFixedURL(String i_URL) throws Exception{		
		String toReturn = "";

		String url;

		url = (i_URL.contains("%3A%2F%2F")) ? i_URL.replace("%3A%2F%2F", "://") : i_URL;
		url = url.replace("%2F", "/");

		if(url.contains("https://")){
			throw new Exception("https does not supported");
		}
		toReturn = (url.contains("http://")) ? url.replace("http://", "") : url;

		if (m_Host == ""){
			m_Host = toReturn.split("/", 2)[0];
		}

		if (toReturn.contains("/")) {
			m_RequestedFile = toReturn.substring(toReturn.indexOf("/"));

			toReturn = toReturn.substring(0, toReturn.indexOf('/'));
		}
		else {
			m_RequestedFile =  "/";
		}

		if (m_Host != ""){
			return m_Host;
		}
		return toReturn;
	}

	/**
	 * Get HTML content from HTTP response.
	 * @param i_Reader
	 * @throws Exception 
	 */
	private void setHTMLPageData(BufferedReader i_Reader) throws Exception{
		int c = 0;
		String line = "";
		String sHTMLTag = "</html>";
		String stringToCheck = "";
		//int contentLength = Integer.parseInt(m_URLHeaders.get("content-length"));
		m_IsChunked = false;

		while ((c = i_Reader.read()) != -1) {
			if (timer.timeOut){
				throw new Exception("Timeout...");
			}
			line += (char) c;

			if (line.length() >= sHTMLTag.length()){
				stringToCheck = line.substring(line.length() - sHTMLTag.length());
				if (m_Robots){
					if (line.length() >= m_ContentLength){
						m_RobotsFile = line;
						break;
					}
				}
				else {
					if (stringToCheck.contains("</html>")){
						m_HTMLPageData = line;
						break;
					}
				}
			}
		}
	}
	
	/**
	 * Get file size from URL.
	 * @param FilePath
	 * @return
	 * @throws Exception
	 */
	public int getFileSizeFromURL(String i_FilePath) throws Exception{
		String[] args = new String[2];
		String fixedFileURL = getFixedFileURL(i_FilePath);

		args[0] = getFileHost(fixedFileURL);
		args[1] = getFileFullName(fixedFileURL);

		m_IsFile = true;
		getHTTPRequestData(true, args);
		m_IsFile = false;
		
		System.out.println("here");
		return (m_IsChunked) ? m_RequestedFileSize : Integer.parseInt(m_URLHeaders.get("Content-Length"));
	}

	/**
	 * Read file by chunks
	 * @param i_Reader
	 * @param onlyHeaders
	 */
	private void readFileByChunks(BufferedReader i_Reader, boolean onlyHeaders){
		char[] buffer = new char[BUFFER_SIZE];
		int read;
		int fileSize = 0;
		StringBuilder str = new StringBuilder();
		boolean isEOF;

		try{
			while((read = i_Reader.read(buffer)) != 0){
				fileSize += read;
				str.append(buffer, 0, read);

				if (!m_Robots){
					m_HTMLPageData = (str.substring(0, str.length()));
				}
				else {
					m_RobotsFile = str.substring(0, str.length());
				}

				isEOF = (m_Robots) ? read < BUFFER_SIZE || read == 0 : m_HTMLPageData.contains("</html");

				if (isEOF){
					m_chunkedFileSize = fileSize;
					if (m_Robots){
						//m_RequestedFile = str.substring(0, str.length());
						//	m_RobotsFile = str.substring(0, str.length());

					}
					else {
						if(!onlyHeaders){
							//							m_HTMLPageData = str.substring(0, str.length());
							//System.out.println(m_HTMLPageData);
						} else {
							m_RequestedFileSize = fileSize;
						}
					}
					i_Reader.close();
					break;
				}

				buffer = new char[BUFFER_SIZE];
			}
			
		}
		catch (Exception e){
		}
	}

	/**
	 * Get file full name. Supports all file extensions.
	 * @param i_FilePath
	 * @return
	 */
	private String getFileFullName(String i_FilePath){
		String toReturn = "";
		try {
			toReturn = i_FilePath.substring(i_FilePath.indexOf("/"));
		} catch (Exception e) {
			toReturn = i_FilePath;
		}
		return toReturn;
	}

	/**
	 * Get the host of the file
	 * @param i_FilePath
	 * @return
	 */
	private String getFileHost(String i_FilePath){
		return i_FilePath.split("/", 2)[0];
	}

	/**
	 * Get fixed URL 
	 * @param FilePath
	 * @return
	 * @throws Exception 
	 */
	private String getFixedFileURL(String FilePath) throws Exception{
		if(FilePath.contains("https://")){
			throw new Exception("https does not supported");
		}
		String fixedFileURL = FilePath.contains("http://") ? FilePath.replace("http://", "") : FilePath;
		return fixedFileURL.contains("www.") ? fixedFileURL.replace("www.", "") : fixedFileURL;
	}
	
	public void initParams(HashMap<String, String> i_Params) throws Exception{
		parseParams(i_Params);
		getHTTPRequestData(false, m_URL);
		if (m_IsTCP){
			m_OpenPorts = new ArrayList<>();
			portScanner(m_URL);
		}
		System.out.println("Done");
	}

	private void parseParams(HashMap<String, String> i_Params) throws Exception{
		for (Map.Entry<String,String> entry : i_Params.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();

			switch(key){
			case "textBoxURL":
				m_URL = getFixedURL(value);
				break;
			case "checkBoxTCP":
				m_IsTCP = (value.equals("on"));
				break;
			case "checkBoxRobots":
				m_IsRobots = (value.equals("on"));
				break;						
			}

			if (m_URL == ""){
				throw new Exception("Bad URL was given");
			}
		}
	}

	public String getRequestedUrl() {
		return m_URL;
	}

	public boolean isTCPOpenPortsRequested() {
		return m_IsTCP;
	}

	public boolean isRobotFileRespected() {
		return !m_IsRobots;
	}
	
	private void portScanner(String i_URL){
		System.out.println("Scanning ports");
		for (int port = 1; port <= 50/*65535*/; port++) {
			try {

				Socket socket = new Socket();
				socket.connect(new InetSocketAddress(i_URL, port), 1000);
				socket.close();
				System.out.println("Port " + port + " is open");
				m_OpenPorts.add(port);

			} catch (Exception ex) {
			}
		}
		System.out.println("Finished");
	}

}
