import java.util.ArrayList;


public class CrawlerJobManager {
	private Statistics statistics;
	private RobotFileParser robotsParser;
	private MultiThreadedClass server;
	private String domain;
	private boolean isRespectedRobot;
	private boolean isRequestedOpenPorts;
	private ExtensionsChecker extensionChecker;

	private ArrayList<String> scrawledUrls;
	
	public CrawlerJobManager(MultiThreadedClass server, String domain, boolean isRespectedRobot, boolean isRequestedOpenPorts,
			ExtensionsChecker extensionChecker) {
		this.server = server;
		this.domain = domain;
		this.isRespectedRobot = isRespectedRobot;
		this.isRequestedOpenPorts = isRequestedOpenPorts;
		this.extensionChecker = extensionChecker;
		
		this.scrawledUrls = new ArrayList<String>();
		this.scrawledUrls.add(domain);
	}

	public void start(String[] robotsFileContent) {
		statistics = new Statistics(isRespectedRobot, isRequestedOpenPorts);
		parseRobots(robotsFileContent);		
		server.startCrawlerFlow(this);
	}

	public String getDomain() {
		return domain;
	}

	private void parseRobots(String[] robotsFileContent) {
		//String filePath = getCleanDomain() + "/robots.txt";
		robotsParser = new RobotFileParser(robotsFileContent, domain);
		robotsParser.parse();		
	}

	private String getCleanDomain() {
		int indexOfSlash = domain.replace("http://", "").indexOf("/");
		if (indexOfSlash == -1)
			return domain;
		return domain.substring(0, indexOfSlash);
	}
	
	public boolean isInternalLinkExist(String url) {
		return scrawledUrls.indexOf(url) != -1;
	}
	
	public void addDownloaderTask(String url, CrawlerJobManager crawlerManager) {
		if (url.startsWith("https://"))
			return;
		
		if (isInternalLinkExist(url))
			return;
		
		if (isRespectedRobot && robotsParser.isAllowedPage(url) == false)
			return;			
		
		scrawledUrls.add(url);
		server.addDownloaderTask(url, crawlerManager);
	}

	public void addAnalayzerTask(String url, String content, CrawlerJobManager crawlerManager) {
		server.addAnalayzerTask(url, content, domain, crawlerManager);
	}
		
	public boolean isImageExtension(String extension) {
		return extensionChecker.isImageExtension(extension);
	}
	
	public boolean isVideoExtension(String extension) {
		return extensionChecker.isVideoExtension(extension);
	}
	
	public boolean isDocumentsExtension(String extension) {
		return extensionChecker.isDocumentsExtension(extension);
	}

	//!!!!!NEED TO IMPLEMENT STILL!!!!!!!!
	public boolean isAllowedPage(String url) {
		if (isRespectedRobot)
			return robotsParser.isAllowedPage(url);
		return true;
	}
	
	public Statistics getStatistics() {
		return statistics;
	}
	
	public int calcFileSize(String fileUrl) {
		return 0;
	}

	public void printStatistics() {
		statistics.print();
	}	
}
