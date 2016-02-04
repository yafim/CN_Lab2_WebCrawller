import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.Socket;
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
	private String m_HTMLPageData;

	private int m_chunkedFileSize;
	private int m_Redirections = 0;
	private int m_RequestedFileSize = 0;

	private boolean m_IsChunked = false;
	private boolean m_ErrorFound = false;
	private boolean m_Robots = false;

	public HashMap<String, String> getHeaders(){return this.m_URLHeaders;}
	public String getHTMLPageDataWithoutScripts(){return this.m_HTMLPageDataWithoutScripts;}
	public String getHTMLPageData() {return this.m_HTMLPageData;}
	public String getRobotsFile() {return this.m_RobotsFile;}
	public int getContentLength() {return (m_IsChunked) ? this.m_chunkedFileSize : this.m_HTMLPageData.length();}
	public boolean isRobotsEnabled() {return !this.m_RobotsFile.isEmpty();}

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
		checkRobotsFile();
	}

	public Downloader(){

	}

	
	
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

		m_Socket = new Socket(m_URL, PORT);

		m_URLHeaders = new HashMap<String, String>();

		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(m_Socket.getOutputStream()))); 
		out.println("GET " + m_RequestedFile + " HTTP/1.1");
		out.println("Host:" + m_Host);
		out.println();
		out.flush(); 

		BufferedReader reader = new BufferedReader(new InputStreamReader(m_Socket.getInputStream(), "UTF8")); 

		getHTTPRequestData(reader, onlyHeaders);

		reader.close(); 
		m_Socket.close();
	}

	/**
	 * Check if site contains robots.txt file, if so get it.
	 * @throws Exception 
	 */
	private void checkRobotsFile() throws Exception{
		try {
			m_Robots = true;
			getHTTPRequestData(false, m_URL + "/robots.txt");
		} catch (IOException e) {
			throw new Exception("No robots file");
		} finally{
			m_Robots = false;
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
		//	System.out.println(i_Header);
		try{
			splittedString = i_Header.replaceAll("\\s","").split(":", 2);
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
		if(i_URL.contains("https://")){
			throw new Exception("https does not supported");
		}
		toReturn = (i_URL.contains("http://")) ? i_URL.replace("http://", "") : i_URL;

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
	 * @throws IOException
	 */
	private void setHTMLPageData(BufferedReader i_Reader) throws IOException{
		String line = "";
		boolean isHTMLBody = false;

		while ((line = i_Reader.readLine()) != null) {

			//		System.out.println(line);
			if (m_Robots){
				if (line.isEmpty()){
					break;
				}
				m_RobotsFile += line;
			}
			else {
				m_HTMLPageData += line;
			}

			if (line.contains("body")){
				isHTMLBody = true;
			}
			if (isHTMLBody){
				m_HTMLPageDataWithoutScripts += line;	
				//System.out.println(line);
			}

			if (line.contains("</body>") || line.contains("</html>")){
				isHTMLBody = false;
				break;
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


		getHTTPRequestData(true, args);

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

		try{
			while((read = i_Reader.read(buffer)) != -1){
				fileSize += read;
				str.append(buffer, 0, read);
				buffer = new char[BUFFER_SIZE];

				if(read < BUFFER_SIZE){
					m_chunkedFileSize = fileSize;
					if (m_Robots){
						//m_RequestedFile = str.substring(0, str.length());
						m_RobotsFile = str.substring(0, str.length());
					}
					else {
						if(!onlyHeaders){
							m_HTMLPageData = str.substring(0, str.length());
							//	System.out.println(m_HTMLPageData);
						} else {
							m_RequestedFileSize = fileSize;
						}
					}
					i_Reader.close();
					break;
				}
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
		return i_FilePath.substring(i_FilePath.indexOf("/"));
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
		checkRobotsFile();
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
		return m_IsRobots;
	}
	
}
