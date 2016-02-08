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
	private long numberOfImages = 0;
	private double imagesSize = 0; //KB
	private long numberOfVideos = 0;
	private double videosSize = 0; //KB
	private long numberOfDocuments = 0;
	private double documentsSize = 0; //KB
	private long numberOfPages = 0;
	private long pagesSize = 0; //KB
	private long numberOfInternalLinks = 0; 
	private long numberOfExternalLinks = 0;
	private long numberOfDomains = 0;
	private ArrayList<String> connectedDomains;
	private ArrayList<String> openedPorts;
	private long sumOfAllRTTTimes = 0;
	private long numberOfRTT = 0;
	
	public Statistics(boolean isRespectedRobot, boolean isRequestedOpenPorts)
	{
		this.isRespectedRobot = isRespectedRobot;
		this.isRequestedOpenPorts = isRequestedOpenPorts;
		this.connectedDomains = new ArrayList<String>();
		this.openedPorts = new ArrayList<String>();
	}
	
	public synchronized void addImage(long imageSizeInBytes)
	{
		this.imagesSize += (imageSizeInBytes / 1000.0);
		this.numberOfImages++;
	}
	
	public synchronized void addVideo(long videoSize) 
	{
		this.videosSize += (videoSize / 1000.0);
		this.numberOfVideos++;
	}
	
	public synchronized void addDocument(long documentSize) 
	{
		this.documentsSize += (documentSize / 1000.0);
		this.numberOfDocuments++;
	}
	
	public synchronized void addPage(long pageSize) 
	{
		this.pagesSize += (pageSize / 1000.0);
		this.numberOfPages++;
	}
	
	public synchronized void addConnectedDomain(String domainName)
	{
		if (connectedDomains.contains(domainName))
			return;
		
		this.numberOfDomains++;
		this.connectedDomains.add(domainName);
	}
	
	public synchronized void addPort(String portNumber)
	{
		this.openedPorts.add(portNumber);
	}
	
	public synchronized void addRTT(long rtt)
	{
		this.sumOfAllRTTTimes += rtt;
		this.numberOfRTT++;
	}
	
	public synchronized void incrementInternalLinks() 
	{
		this.numberOfInternalLinks++;
	}
	
	public synchronized void incrementExternalLinks() 
	{
		this.numberOfExternalLinks++;
	}

	private double roundSize(double size) {
		return Math.round(size * 100) / 100.0;
	}
	
	public void print() {
		System.out.println("The number of images is:" + numberOfImages);
		System.out.println("The size of all images is:" + roundSize(imagesSize));
		System.out.println("The number of all videos is:" + numberOfVideos);
		System.out.println("The size of all videos is:" + roundSize(videosSize));
		System.out.println("The number of all Documents is:" + numberOfDocuments);
		System.out.println("The size of all Documents is:" + roundSize(documentsSize));
		System.out.println("The number of all pages is:" + numberOfPages);
		System.out.println("The size of all pages is:" + roundSize(pagesSize));
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
