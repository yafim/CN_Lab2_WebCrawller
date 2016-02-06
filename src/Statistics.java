import java.util.ArrayList;


public class Statistics {
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
	
	public synchronized void addImage(int imageSize)
	{
		this.imagesSize += imageSize;
		this.numberOfImages++;
	}
	
	public synchronized void addVideo(int videoSize) 
	{
		this.videosSize += videoSize;
		this.numberOfVideos++;
	}
	
	public synchronized void addDocument(int documentSize) 
	{
		this.documentsSize += documentSize;
		this.numberOfDocuments++;
	}
	
	public synchronized void addPage(int pageSize) 
	{
		this.pagesSize += pageSize;
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
	
	public synchronized void addRTT(int timeOfRTT)
	{
		this.sumOfAllRTTTimes += timeOfRTT;
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
}
