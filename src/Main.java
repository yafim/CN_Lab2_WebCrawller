import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import org.omg.PortableServer.Servant;

// TODO: DELETE THIS CLASS
public class Main {

	private static final String r_PathToConfigFile = "c:\\serverroot\\config.ini";
	private static final File sr_ConfigFile = new File(r_PathToConfigFile);
	private static HashMap<String, Object> m_ConfigFileParams = null;

	private static int m_Port;
	private static String m_Root;
	private static String m_DefaultPage;
	private static int m_MaxThreads; 
	
	// New variables for lab2
	private static int m_MaxDownloaders;
	private static int m_MaxAnalyzers;
	private static ArrayList<String> m_ImageExtensions;
	private static ArrayList<String> m_VideoExtensions;
	private static ArrayList<String> m_DocumentExtensions;
	
	public static void main(String[] args){
		
		try {
			m_ConfigFileParams = HTTPRequest.getConfigFileParams(sr_ConfigFile);
			initParams();
//			printParams();
			
			MultiThreadedClass server = new MultiThreadedClass(m_Port, m_Root, m_DefaultPage);
			server.startTheServer(m_MaxThreads);
			System.out.println("Listening on port " + m_Port);
			
		} catch (UnsupportedEncodingException e) {
			System.err.println("Please check " + sr_ConfigFile.getName() + " file");
		} catch (Exception e){
			System.err.println("Please check " + sr_ConfigFile.getName() + " file");
		}
		
	}		
	
	/**
	 * Initial parameters from Config.ini file.
	 */
	private static void initParams(){
		m_Port = Integer.parseInt(m_ConfigFileParams.get("port").toString());
		m_Root = m_ConfigFileParams.get("root").toString();
		m_DefaultPage = m_ConfigFileParams.get("defaultPage").toString();
		m_MaxThreads = Integer.parseInt(m_ConfigFileParams.get("maxThreads").toString());
		
		//Lab2
		m_MaxDownloaders = Integer.parseInt(m_ConfigFileParams.get("maxDownloaders").toString());
		m_MaxAnalyzers = Integer.parseInt(m_ConfigFileParams.get("maxAnalyzers").toString());
		m_ImageExtensions = setExtentionsToArray(m_ConfigFileParams.get("imageExtensions").toString());
		m_VideoExtensions = setExtentionsToArray(m_ConfigFileParams.get("videoExtensions").toString());
		m_DocumentExtensions= setExtentionsToArray(m_ConfigFileParams.get("documentExtensions").toString());
	}
	
	private static ArrayList<String> setExtentionsToArray(String i_Extentions){
		String[] extString = i_Extentions.split(",");
		
		ArrayList<String> toReturn = new ArrayList<String>();
		
		for(String str : extString){
			toReturn.add(str);
		}
		
		return toReturn;
	}
	
	/**
	 * Print for debug
	 */
	private static void printParams() {
		System.out.println("port: " + m_Port);
		System.out.println("root: " + m_Root);
		System.out.println("defaultPage: " + m_DefaultPage);
		System.out.println("maxThreads: " + m_MaxThreads);
		
		System.out.println("maxDownloaders: " + m_MaxDownloaders);
		System.out.println("maxAnalyzers: " + m_MaxAnalyzers);
		
		printArrayList(m_ImageExtensions);
		printArrayList(m_VideoExtensions);
		printArrayList(m_DocumentExtensions);
		
	}
	
	private static void printArrayList(ArrayList<String> i_ToPrint){
		System.out.println("== Start ==");
		for(String s : i_ToPrint){
			System.out.print(s + ", " );
		}
		System.out.println("== End ==");
	}
	
	
	
}