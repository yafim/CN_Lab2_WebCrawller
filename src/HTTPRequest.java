import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * This class holds the necessarily logic to use web server program.
 * @authors Yafim Vodkov 308973882 , Nir Tahan 305181166
 */
public class HTTPRequest {
//	private String m_HTMLParams;
	private HashMap<String, String> m_HTMLParams;
	private BufferedReader m_In;
	/** Errors and messages to the client */
	private final String ERR_NOT_IMPEMENTED = "501 Not Implemented";
	private final String ERR_FILE_NOT_FOUND = "404 Not Found";
	private final String ERR_BAD_REQUEST = "400 Bad Request";
	private final String ERR_INTERNAL_SRV_ERR = "500 Internal Server Error";
	private final String OK_MSG = "200 OK";
	private final String FORBIDDEN_ERR_MSG = "403 Forbidden";
	private final String m_ForbiddenMessageHTML = FORBIDDEN_ERR_MSG + "<br><a href='/'>BACK</a>";

	/** HTTP request variables */
	private String m_HTTPRequest = null;
	private String[] m_SplitHTTPRequest = null;	
	private HashMap<String, Object> m_HTTPAdditionalInformation = null;
	private HashMap<String, Object> m_RequestedVariables = null;

	// HTTP request variables 
	private File m_RequestedFileFullPath;
	private HttpMethod m_HTTPMethod;
	private String m_HttpVersion;
	
	/** Response variables */
	// Requested file
	private byte[]  m_RequestedFileContent;
	private HashMap<String, Object> m_HTTPResponse = null;
	private String m_FileExtension;
	private boolean m_IsImage = false;
	private boolean m_IsForbiddenErr = false;

	/** Read file variables */	
	private static FileInputStream m_FileInputStream = null;
	private static HashMap<String, Object> m_FileParmas = null;
	private int m_RequestedVariablesLength;

	/** Root of the server */
	private String m_Root;
	private String m_DefaultPage;

	private boolean m_IsValidRequest = false;
	private static boolean m_IsValidReferer = true;

	// Default is 200
	private String m_ResponseMessage = OK_MSG;

	private boolean m_IsChunked = false;
	
	private int m_BytesToRead = 1024;
	
	// Date format 
	private final SimpleDateFormat m_Sdf = 
			new SimpleDateFormat("EEE, MMM d, yyyy hh:mm:ss a z");
	private Date m_CurrentTime;
	
	/**
	 * Constructor
	 */
	public HTTPRequest(String i_Root, String i_DefaultPage){
		// Add '\' in case there isn't
		this.m_Root = (i_Root + "\\"); 
		this.m_DefaultPage = i_DefaultPage;
	}

	/**
	 * Get Config.ini file parameters 
	 * @param i_File
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static HashMap<String, Object> getConfigFileParams(File i_File) throws UnsupportedEncodingException{
		byte[] bFile = readFile(i_File);
		String sFile = byteArrayToString(bFile);
		m_FileParmas = stringToDictionary(sFile, "=", new HashMap<String, Object>());

		return m_FileParmas;
	}

	/**
	 * Get the variables after post message sent
	 */
	public void handlePostVariables(BufferedReader i_In){
		int c;
		int i;
		boolean skip = false;
		String sVariables = "";
		String sNumberOfBytesToRead = "";
		try{
			sNumberOfBytesToRead = m_HTTPAdditionalInformation.get("Content-Length").toString().replaceAll("\\s+","");
		} catch (NullPointerException npe){
			// No params was given
			skip = true;
			sVariables = "";
			sNumberOfBytesToRead = "0";
		}

		int numberOfBytesToRead = Integer.parseInt(sNumberOfBytesToRead);
		

		try {
			// read numberOfBytes from the current buffer
			for (i = numberOfBytesToRead; i > 0 && !skip ; i--){
				c = i_In.read();
				sVariables += (char)c;
			}

			getVariables(sVariables);

		} 
		
		catch (Exception e) {
			// Shouldn't get here
			System.out.println("Problem with reading from buffer");
			System.out.println("Error message: " + e.getMessage());
		}
	}

	/**
	 * Handle HTTP request.
	 * @param i_HTTPRequest
	 * @throws Exception
	 */
	public HashMap<String, Object> handleHttpRequest(String i_HTTPRequest, boolean i_IsChunked, BufferedReader i_In) throws Exception{
		m_IsChunked = i_IsChunked;
		m_HTTPRequest = i_HTTPRequest;
		m_In = i_In;
		// print HTTP request to console
	//	System.out.println(m_HTTPRequest);


		try{
			splitHttpRequest(m_HTTPRequest);
			parseHTTPAdditionalInformation();
			
			// Print the request
			System.out.println(m_SplitHTTPRequest[0]); 

			initHttpRequestParams();

			tryParseVariables();
			
		} catch (NoVariablesException nve){
			// No variables to parse...
		} catch (BadRequestException bre){
			m_ResponseMessage = bre.getMessage();
			createResponseHeader();
		}
		
		//TODO: Other flag
//		if (isPramsInfoForm()){
			parseInfoFormParams();
//		}
		buildResponseMessage();
		return m_HTTPResponse;
	}
	
	private void parseInfoFormParams(){
			// get form variables
			handlePostVariables(m_In);

			String htmlParams = "";
			HashMap<String, Object> requestedVariables = getVariablesAsBytes();
			
			m_HTMLParams = new HashMap<String,String>();

			for (Map.Entry<String,Object> entry : requestedVariables.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue().toString();	
				
				m_HTMLParams.put(key, value);
				htmlParams += key + " : <input type=\"text\" value=\"" + 
				value + "\"> <br>";
				
		//		System.out.println("Key: " + key + "Value: " + value); //debug
			}
				m_RequestedVariablesLength = htmlParams.length();
//				m_HTMLParams = htmlParams;
	}

	/**
	 * Split HTTP request into 2 parts: 1. request 2. additional parameters.
	 * @param i_HTTPRequest
	 * @throws BadRequestException 
	 * @throws FileNotFoundException 
	 */
	private void splitHttpRequest(String i_HTTPRequest) throws BadRequestException{
		m_SplitHTTPRequest = i_HTTPRequest.split(System.lineSeparator(), 2);
		verifyGivenPath(m_SplitHTTPRequest[0]);
	}

	/**
	 * Checks if the URL that was given is OK and safe to open.
	 * @throws BadRequestException 
	 * @throws FileNotFoundException 
	 */
	private void verifyGivenPath(String i_Path) throws BadRequestException{
		m_SplitHTTPRequest[0] = i_Path.replaceAll("\\.\\.", "");

		String[] sSplittedRequest = m_SplitHTTPRequest[0].split(" ");
		sSplittedRequest[1] = sSplittedRequest[1].split("\\?")[0].replaceAll("(//*)", "/");
		if (sSplittedRequest.length != 3){
			throw new BadRequestException();
		} 
		else {
			// TODO: Consider moving this logic to tryParseVariables or updating here the m_RequestedFileFullPath
			m_SplitHTTPRequest[0] = sSplittedRequest[0] + " " + sSplittedRequest[1] + " " + sSplittedRequest[2];
			m_IsValidRequest = true;
		}
	}

	/**
	 * Try parse variables. If exist parse and return true, Otherwise return false.
	 * @return
	 * @throws NoVariablesException 
	 */
	private void tryParseVariables() throws NoVariablesException{

		try{
			String fileName = m_RequestedFileFullPath.getName();
			String[] variables = fileName.split("\\?");
			
			if (variables.length == 1){
				throw new NoVariablesException("No variables to parse");
			}
			else {
				updateRequestedFileFullPath();
				getVariables(variables[1]);
			}
		} catch (Exception e){
			//TODO: Should'nt get here..? probably. -> Check!
		}

	}

	/**
	 * There must be variables after the file name, so delete it.
	 * @param i_FilePath
	 */
	private void updateRequestedFileFullPath(){
		String sFullPath = m_RequestedFileFullPath.toString().split("\\?")[0];
		m_RequestedFileFullPath = new File(sFullPath);
	}

	/**
	 * Get variables from path as dictionary.
	 * @param i_Variables
	 */
	private void getVariables(String i_Variables){

		String[] s = i_Variables.split("&");

		m_RequestedVariables = new HashMap<String, Object>();
		for (String str : s){
			m_RequestedVariables = stringToDictionary(str, "=", m_RequestedVariables);
			m_RequestedVariablesLength += str.length();
		}
		
	}

	/**
	 * Return variables as bytes
	 * @return
	 */
	public HashMap<String, Object> getVariablesAsBytes(){
		return m_RequestedVariables;
	}

	/**
	 * Initialise HTTP request variables
	 * @throws Exception
	 */
	private void initHttpRequestParams() throws Exception{

		String[] sString = m_SplitHTTPRequest[0].split(" ");
		try{
			// get the method
			m_HTTPMethod = HttpMethod.valueOf(sString[0]);

			// get the file
			boolean defaultPageGiven = (sString[1].equals("/"));

			m_RequestedFileFullPath = (defaultPageGiven) ? new File(m_Root + m_DefaultPage)
			: new File(m_Root + sString[1]);
			
			// get HTTP version
			m_HttpVersion = sString[2];
		} catch (IllegalArgumentException e){
			m_ResponseMessage = ERR_BAD_REQUEST;
			createResponseHeader();
		} catch(ArrayIndexOutOfBoundsException oobe){
			m_ResponseMessage = ERR_BAD_REQUEST;
			createResponseHeader();
		} catch(NullPointerException npe){
			throw new Exception(ERR_INTERNAL_SRV_ERR);
		}

	}

	/**
	 * Split string to dictionary
	 * @param i_String
	 */
	private static HashMap<String, Object> stringToDictionary(String i_String, String i_Separator, HashMap<String, Object> i_Dictionary){
		boolean isNull = false;
		boolean requestFlag = false;

		for (String s : i_String.split(System.lineSeparator())) {
			isNull = s == System.lineSeparator() || s.equals("") || s.equals(" ") || s.equals(null);
			
			if (s.contains("Referer")){
//				System.err.println(s.replaceAll("\\s", "").split(":", 2)[1].equals("http://localhost:8080/"));
				m_IsValidReferer = s.replaceAll("\\s", "").split(":", 2)[1].equals("http://localhost:8080/");
			}
			
			/** Check for body */
			if (requestFlag){
				i_Dictionary.put("RequestBody", s);
				break;
			}

			if (!isNull){
				i_Dictionary.put(s.split(i_Separator)[0], s.split(i_Separator)[1]);
			} 
			else {
				requestFlag = true;
			}
		}

		return i_Dictionary;
	}

	/**
	 * Byte[] to String
	 * @param i_Bytes file content in bytes
	 * @return string
	 * @throws UnsupportedEncodingException
	 */
	public static String byteArrayToString(byte[] i_Bytes) throws UnsupportedEncodingException{
		return new String(i_Bytes, "UTF-8");
	}
	
	/**
	 * Return true if file is image, Otherwise false.
	 * @param i_FileExtension file to check
	 * @return isImage
	 */
	private boolean isImage(String i_FileExtension){
		boolean isImage = false;
		try{
			SupportedFiles.valueOf(i_FileExtension);
			isImage = true;
		}
		catch (IllegalArgumentException iae){
			isImage = false;
		}

		return isImage;
	}
	
	/**
	 * Read bytes from file
	 * @param i_File file to read
	 * @return file data in bytes
	 */
	private static byte[] readFile(File i_File){
		byte[] bFile = null;
		try{
			m_FileInputStream = new FileInputStream(i_File);
			bFile = new byte[(int)i_File.length()];

			while(m_FileInputStream.available() != 0){
				m_FileInputStream.read(bFile, 0, bFile.length);
			}
		}
		catch(FileNotFoundException fnf){
			// If you get here, there must be a problem with the config file.
			System.err.println(fnf.getMessage()); // debug
		}
		catch (IOException ioe){
			// Stream closed
//			System.err.println(ioe.getMessage()); // debug
			
		}
		finally{
			try {
				m_FileInputStream.close();
			} catch (Exception e) {
				System.out.println("Something went wrong...");
			}
		}
		return bFile;
	}

	/**
	 * Start building the response based on the method.
	 * @throws Exception 
	 */
	private void buildResponseMessage() throws Exception{
		if (m_HTTPMethod != null){
			switch(m_HTTPMethod){
			case GET :
				buildResponseMessage(false, true);
				break;
			case POST :
				buildResponseMessage(false, true);
				break;
			case HEAD :
				buildResponseMessage(false, true);
				break;
			case TRACE:
				buildResponseMessage(false, false);
				break;
			}
		}
	}

	/**
	 * Build response message.
	 * 1. Check the requested file and read its content to byte[] 
	 *    m_RequestedFileContent.
	 * 2. Create http response header.
	 * 3. If needed include file content to header.
	 * @param i_PrintFileContent
	 * @throws Exception 
	 */
	private void buildResponseMessage(boolean i_PrintFileContent, boolean i_IncludeConetnt) throws Exception{
		try{
			if (m_HTTPMethod != HttpMethod.POST || !m_IsValidReferer){
				if (m_RequestedFileFullPath.getName().equals("execResult.html")){
					throw new Exception("403");
				}
			}
			
			m_IsForbiddenErr = false;
			handleFileRequest();
			createResponseHeader();

			if (i_IncludeConetnt){
				buildResponseContent();
			}

			if (i_PrintFileContent){
				System.out.println(m_HTTPResponse.get("Content"));
			}

		} catch (UnsupportedEncodingException e) {
			// Should'nt get here
			m_ResponseMessage = ERR_INTERNAL_SRV_ERR;
			createResponseHeader();
		} catch(FileNotFoundException fnfe){
			m_Error = true;
			m_ResponseMessage = ERR_FILE_NOT_FOUND;
			m_RequestedFileContent = ERR_FILE_NOT_FOUND.getBytes();
			createResponseHeader();
		} catch (NullPointerException npe){
			throw new Exception(ERR_INTERNAL_SRV_ERR);
		} catch (ArrayIndexOutOfBoundsException aofe){
			// Should'nt get here
			System.out.println(aofe.getMessage());
			System.out.println("Something went wrong...");
		} catch (FileNotImplementedException fnse){
			m_ResponseMessage = ERR_NOT_IMPEMENTED;
			createResponseHeader();
		} catch (Exception e){
			if (e.getMessage().equals("403")){
				m_IsForbiddenErr = true;
				m_ResponseMessage = FORBIDDEN_ERR_MSG;
				
				m_RequestedFileContent = m_ForbiddenMessageHTML.getBytes();
				createResponseHeader();
				
			}
			else {
				System.err.println(e.getMessage());
			}
		}
	}

	/**
	 * If file exists and supported by the server open it, 
	 * Otherwise return 404 message
	 * @throws UnsupportedEncodingException 
	 * @throws FileNotFoundException 
	 * @throws FileNotImplementedException 
	 */
	private void handleFileRequest() throws ArrayIndexOutOfBoundsException, UnsupportedEncodingException, FileNotFoundException, FileNotImplementedException{
		try{
			m_FileExtension = m_RequestedFileFullPath.getName().split("\\.")[1];
		} catch (Exception e){
			throw new FileNotFoundException();
		}
		
		m_IsImage = isImage(m_FileExtension);

		// Check the file
		if (isSupportedFormat(m_FileExtension)){
			if (isExists(m_RequestedFileFullPath)){
				// open file...
				if (!m_IsChunked){
					m_RequestedFileContent = readFile(m_RequestedFileFullPath);
				}
			}
			else {
				m_ResponseMessage = ERR_FILE_NOT_FOUND;
				if (!m_IsChunked){
					m_RequestedFileContent = ERR_FILE_NOT_FOUND.getBytes();
					createResponseHeader();
				}
			}
		} else {
			throw new FileNotImplementedException();
		}
	}
	
	/**
	 * Returns true if file format supported by the server.
	 * @param i_FileExtension file extension
	 * @return isSupported
	 * @throws FileNotImplementedException 
	 */
	private boolean isSupportedFormat(String i_FileExtension) throws FileNotImplementedException{
		boolean isSupported = false;
		try{
			SupportedFiles.valueOf(i_FileExtension);		
			isSupported = true;
		}
		catch (IllegalArgumentException iae){
			isSupported = false;
			throw new FileNotImplementedException();
		}

		return isSupported;
	}

	/**
	 * True if file exists
	 * @param i_FileFullPath file to check
	 * @return
	 */
	private boolean isExists(File i_FileFullPath){
		return i_FileFullPath.exists();
	}

	/**
	 * Get all the additional information in dictionary
	 */
	private void parseHTTPAdditionalInformation(){
		m_HTTPAdditionalInformation = stringToDictionary(m_SplitHTTPRequest[1], ":", new HashMap<String, Object>());
	}
boolean m_Error = false; // TODO
	/**
	 * Create HTTP response header.
	 * 1. Set content-type
	 * 2. Set content-length
	 */
	private void createResponseHeader(){
		m_HTTPResponse = new HashMap<String, Object>();

		String contentType = (m_ResponseMessage != ERR_FILE_NOT_FOUND) ? getContentType()
				: "text/html";
		if (m_IsForbiddenErr){
			contentType = "text/html";
		}
		
		if (!m_IsChunked){

			String contentLength = getContentLength();

			buildResponseHeader(contentType, contentLength);
			if (m_Error){
				buildResponseContent();
			}
			else if (m_IsForbiddenErr){
				buildResponseContent();
			}

		}
		else {
			buildResponseChunkedHeader(contentType);
		}

	}

	/**
	 * Attach requested content to the HTTP response
	 */
	private void buildResponseContent(){
		m_HTTPResponse.put("Content", m_RequestedFileContent);
	}

	/**
	 * Build response header
	 */
	private void buildResponseHeader(String i_ContentType, String i_ContentLength){
		String headerResponse = (m_IsValidRequest) ? m_HttpVersion + " " + m_ResponseMessage : 
		"HTTP/1.1 " + m_ResponseMessage;
		
		String sHeader = String.format(
		"%s\r\nDate: %s\r\ncontent-type: %s\r\ncontent-length: %s\r\n\r\n", 
		headerResponse,
		getTimestamp(),
		i_ContentType,
		i_ContentLength
		);
		
		m_HTTPResponse.put("HEADER", sHeader);
	}
	/**
	 * Build response header for chunk encoding
	 * @param i_ContentType
	 */
	private void buildResponseChunkedHeader(String i_ContentType){
		String headerResponse = (m_IsValidRequest) ? m_HttpVersion + " " + m_ResponseMessage : 
		"HTTP/1.1 " + m_ResponseMessage;
		String sHeader = String.format(
				"%s\r\nDate: %s\r\nContent-Type: %s\r\nTransfer-Encoding: chunked\r\n\r\n", 
				headerResponse,
				getTimestamp(),
				i_ContentType
				);
		m_HTTPResponse.put("HEADER", sHeader);
	}
	
	/**
	 * Get local GMT time stamp
	 * @return
	 */
	private String getTimestamp(){
		m_CurrentTime = new Date();

		m_Sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return m_Sdf.format(m_CurrentTime);
	}
	
	/**
	 * Update parameters after post method
	 * @return
	 */
	public HashMap<String, Object> updateParams(){
		createResponseHeader();
		return m_HTTPResponse;
	}

	/**
	 * Get content type
	 * @return content type
	 */
	private String getContentType(){
		String contentType = null;
		try{
			SupportedFiles fileExtension = SupportedFiles.valueOf(m_FileExtension);
			switch(fileExtension){
			case html:
				contentType = (HttpMethod.TRACE == m_HTTPMethod) ? "message/http" 
						: SupportedFiles.html.getContentType();
				break;
			case bmp:
				contentType = SupportedFiles.bmp.getContentType();
				break;
			case jpg:
				contentType = SupportedFiles.jpg.getContentType();
				break;
			case gif:
				contentType = SupportedFiles.gif.getContentType();
				break;
			case png:
				contentType = SupportedFiles.png.getContentType();
				break;
			case ico:
				contentType = SupportedFiles.ico.getContentType();
				break;
			case txt:
				contentType = SupportedFiles.txt.getContentType();
				break;
			}
		} catch (Exception iae){
			contentType = "application/octet-stream";
		}

		return contentType;
	}

	/**
	 * Clear all fields
	 */
	public void clear(){
		/** HTTP request variables */
		m_IsValidReferer = true;
		m_IsValidRequest = false;
		m_HTTPRequest = "";
		m_SplitHTTPRequest = null;	
		m_HTTPAdditionalInformation.clear();
		if (m_RequestedVariables != null){
			m_RequestedVariables.clear();
			m_RequestedVariables = null;
		}

		// http request variables 
		m_RequestedFileFullPath = null;
		m_HTTPMethod = null;
		m_HttpVersion = "";

		/** Response variables */
		// Requested file
		m_RequestedFileContent = null;
		m_HTTPResponse.clear();
		m_HTTPResponse = null;
		m_FileExtension = "";
		m_ResponseMessage = OK_MSG;
		m_IsChunked = false;
		m_IsImage = false;


		/** Read file variables */	
		try {
			m_FileInputStream.close();
		} catch (IOException e) {
			System.out.println("cant close filestream");
		}
		if (m_FileParmas != null){
			m_FileParmas.clear();
		}

		m_RequestedVariablesLength = 0;
		
		m_HTMLParams = null;

	}

	/**
	 * Get content length as string
	 * @return content length
	 */
	private String getContentLength(){
		int contentLength = (m_RequestedFileContent != null) ? m_RequestedFileContent.length : 0;
		contentLength += m_RequestedVariablesLength;
		
		if (m_IsForbiddenErr){
			contentLength = m_ForbiddenMessageHTML.length();
		}
		return contentLength + "";
	}

	/**
	 * Read file content by chunks
	 * @param i_FileToRead
	 * @throws Exception 
	 */
	public void readFileByChunks(DataOutputStream outToClient) throws Exception{
		int chunkSize = m_BytesToRead;
		m_FileInputStream = null; // just in case..
		try {
			m_FileInputStream = new FileInputStream(m_RequestedFileFullPath);

			byte[] buffer = new byte[chunkSize];
			int bytesRead;
			
			String hexNumber;
			
			while ((bytesRead = m_FileInputStream.read(buffer)) != -1) {			
				hexNumber = Integer.toHexString(bytesRead);
				
				outToClient.write(hexNumber.getBytes());
				outToClient.writeBytes("\r\n");
				
			    outToClient.write(buffer, 0, bytesRead);
				outToClient.writeBytes("\r\n");
				buffer = new byte[chunkSize];
			}
			return;
		}
		catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				m_FileInputStream.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	/** SOME GETTERS AND SETTERS */
	
	public File getRequestedFile(){return m_RequestedFileFullPath;}
	public int getBytesToRead(){return m_BytesToRead;}
	// Hard code for params_info.html form.
	public boolean isPramsInfoForm(){return m_RequestedFileFullPath.getName().equals("params_info.html");}
	public boolean isImage(){return m_IsImage;};
	public boolean isOK() {return m_ResponseMessage.equals(OK_MSG);}
	public boolean isNotFound() {return m_ResponseMessage.equals(ERR_FILE_NOT_FOUND);}
	public String getNotFoundMessage = ERR_FILE_NOT_FOUND;
//	public String getHTMLParams() {return m_HTMLParams;}
	public HashMap<String, String> getHTMLParams() {return m_HTMLParams;}
	
	/*************************** ---  DELETE  ---**********************************/
	/**
	 * Print dictionary<String, String> - DEBUG ONLY! 
	 * TODO: Delete this method.
	 * @param i_Dictionary
	 */
	public static void printDictionary(HashMap<String, String> i_Dictionary){
		for (Map.Entry<String,String> entry : i_Dictionary.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();

			System.out.println("KEY : " + key + " - VALUE : " + value);
		}
	}

	/**
	 * Debug also...
	 */
	public void printHTTPRequestParams(){
		System.out.println("Http Method: " + m_HTTPMethod);
		System.out.println("FULL FILE PATH: " + m_RequestedFileFullPath);
		System.out.println("HTTP VERSION: " + m_HttpVersion);
	}
	/*************************** ---  DELETE END ---*******************************/
}
