import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class Statistics {
	private String m_Root = "C:\\serverroot";
	
	private boolean isRespectedRobot;
	private boolean isRequestedOpenPorts;
	private int numberOfImages = 0;
	private int imagesSize = 0;
	private int numberOfVideos = 0;
	private int videosSize = 0;
	private int numberOfDocuments = 0;
	private int documentsSize = 0;
	private int numberOfPages = 0;
	private int pagesSize = 0;
	private int numberOfInternalLinks = 0; 
	private int numberOfExternalLinks = 0;
	private int numberOfDomains = 0;
	private ArrayList<String> connectedDomains;
	private ArrayList<String> openedPorts;
	private int sumOfAllRTTTimes = 0;
	private int numberOfRTT = 0;
	
	public Statistics(boolean isRespectedRobot, boolean isRequestedOpenPorts)
	{
		this.isRespectedRobot = isRespectedRobot;
		this.isRequestedOpenPorts = isRequestedOpenPorts;
		this.connectedDomains = new ArrayList<String>();
		this.openedPorts = new ArrayList<String>();
	}
	
	public void addImage(int imageSize)
	{
		this.imagesSize += imageSize;
		this.numberOfImages++;
	}
	
	public void addVideo(int videoSize) 
	{
		this.videosSize += videoSize;
		this.numberOfVideos++;
	}
	
	public void addDocument(int documentSize) 
	{
		this.documentsSize += documentSize;
		this.numberOfDocuments++;
	}
	
	public void addPage(int pageSize) 
	{
		this.pagesSize += pageSize;
		this.numberOfPages++;
	}
	
	public void addConnectedDomain(String domainName)
	{
		if (connectedDomains.contains(domainName))
			return;
		
		this.numberOfDomains++;
		this.connectedDomains.add(domainName);
	}
	
	public void addPort(String portNumber)
	{
		this.openedPorts.add(portNumber);
	}
	
	public void addRTT(int timeOfRTT)
	{
		this.sumOfAllRTTTimes += timeOfRTT;
		this.numberOfRTT++;
	}
	
	public void incrementInternalLinks() 
	{
		this.numberOfInternalLinks++;
	}
	
	public void incrementExternalLinks() 
	{
		this.numberOfExternalLinks++;
	}

	public void print() {
		System.out.println("The number of images is:" + numberOfImages);
		System.out.println("The size of all images is:" + imagesSize);
		System.out.println("The number of all videos is:" + numberOfVideos);
		System.out.println("The size of all videos is:" + videosSize);
		System.out.println("The number of all Documents is:" + numberOfDocuments);
		System.out.println("The size of all Documents is:" + documentsSize);
		System.out.println("The number of all pages is:" + numberOfPages);
		System.out.println("The size of all pages is:" + pagesSize);
		System.out.println("The number of all internal links is:" + numberOfInternalLinks);
		System.out.println("The number of all external links is:" + numberOfExternalLinks);
		System.out.println("The number of all domains is:" + numberOfDomains);
		System.out.println("The number of RTT is:" + numberOfRTT);
		System.out.println("The sum of all RTT times is:" + sumOfAllRTTTimes);
		if(isRespectedRobot) {
			System.out.println("Robots.txt file is respected");
		} else {
			System.out.println("Robots.txt file is not respected");
		}
	}
	
	private void createResultFile(String domain) throws IOException{
		Date date = new Date();
		DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
		String fileName = domain + "_" + dateFormat.format(date);

		String path = m_Root + File.separator + "CrawlerResults" + File.separator + fileName + ".txt";
		// Use relative path for Unix systems
		File f = new File(path);
		
		//write to file
		Writer writer = null;

		try {
		    writer = new BufferedWriter(new OutputStreamWriter(
		          new FileOutputStream(f), "utf-8"));
		 //   writer.write(getCrawlerStatistics());
		} catch (IOException ex) {
		  // report
		} finally {
		   try {writer.close();} catch (Exception ex) {/*ignore*/}
		}
		
		f.getParentFile().mkdirs(); 
		f.createNewFile();
	}
	
}
