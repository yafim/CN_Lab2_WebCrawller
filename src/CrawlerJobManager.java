import java.util.ArrayList;
import java.util.Date;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;


public class CrawlerJobManager {
	private Statistics statistics;
	private RobotFileParser robotsParser;
	private MultiThreadedClass server;
	private String domain;
	private boolean isRespectedRobot;
	private boolean isRequestedOpenPorts;
	private ExtensionsChecker extensionChecker;

	private ArrayList<String> internalUrls;
	private ArrayList<String> externalUrls;
	
	public CrawlerJobManager(MultiThreadedClass server, String domain, boolean isRespectedRobot, boolean isRequestedOpenPorts,
			ExtensionsChecker extensionChecker) {
		this.server = server;
		this.domain = domain;
		this.isRespectedRobot = isRespectedRobot;
		this.isRequestedOpenPorts = isRequestedOpenPorts;
		this.extensionChecker = extensionChecker;
		
		this.internalUrls = new ArrayList<String>();
		this.externalUrls = new ArrayList<String>();
		
		this.internalUrls.add(domain);
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
		return internalUrls.indexOf(url) != -1;
	}
	
	public void addDownloaderTask(String url) {
		if (url.startsWith("https://"))
			return;
		
		if (isInternalLinkExist(url))
			return;
		
		if (isRespectedRobot && robotsParser.isAllowedPage(url) == false)
			return;			
		
		server.addDownloaderTask(url, this);
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

	public boolean isAllowedPage(String url) {
		if (isRespectedRobot)
			return robotsParser.isAllowedPage(url);
		return true;
	}
	
	public Statistics getStatistics() {
		return statistics;
	}
	
	public long calcFileSize(String fileUrl) {
		Downloader downloader = new Downloader();
		try {
			Date start = new Date();
			
			long fileSize = downloader.getFileSizeFromURL(fileUrl);
			
			Date end = new Date();
			long rtt = (end.getTime() - start.getTime())/1000/60;
			
			getStatistics().addRTT(rtt);
			return fileSize;
		} catch (Exception e) {
			System.out.println("Image file size process error for " + fileUrl +" --- " + e.getMessage());
		}
		return 0;
	}

	public void printStatistics() {
		statistics.print();
	}

	public String getFullFilePath(String fileUrl, String currentPageUrl) {		
		if(isInternalLink(fileUrl))		
			return fixInternalLink(fileUrl, currentPageUrl);
		return fileUrl;
	}
	
	private boolean isInternalLink(String linkUrl) {
		if (linkUrl.startsWith("/"))
			return true;
		
		if (linkUrl.startsWith("https://"))
			return false;
		
		if (linkUrl.startsWith("http://") == false)
			return true;
		
		// start with http://
		linkUrl = linkUrl.replace("http://", "");
		String domainWithoutHttp = domain.replace("http://", "");
		
		if (linkUrl.startsWith("www."))
			linkUrl = linkUrl.substring(4);
		
		if (domainWithoutHttp.startsWith("www."))
			domainWithoutHttp = domainWithoutHttp.substring(4);
		
		return linkUrl.startsWith(domainWithoutHttp);	
	}
	
	private String fixInternalLink(String linkUrl, String currentPageUrl) {
		if (linkUrl.startsWith("http://") == false && linkUrl.startsWith("https://") == false) {
			if (linkUrl.startsWith("/")) 
				linkUrl = domain + linkUrl;
			else {
				int lastIndexOfSlash = currentPageUrl.lastIndexOf("/");
				if (lastIndexOfSlash == -1)
					linkUrl = currentPageUrl + "/" + linkUrl; 
				else {
					int indexOfDot = currentPageUrl.indexOf('.', lastIndexOfSlash);
					if (indexOfDot == -1) {
						if (lastIndexOfSlash == (currentPageUrl.length() - 1))
							linkUrl = currentPageUrl + linkUrl;
						else
							linkUrl = currentPageUrl + "/" + linkUrl;
					}
					else {
						String urlWithoutPage = currentPageUrl.substring(0, lastIndexOfSlash + 1);
						linkUrl = urlWithoutPage + linkUrl;
					}
				}
			}
		}
		return linkUrl;
	}
	
	public void doLinkFlow(String linkUrl, String currentPageUrl) {
		if(isInternalLink(linkUrl)) {				
			linkUrl = fixInternalLink(linkUrl, currentPageUrl);
			handleInternalUrlByPurpose(linkUrl);
		} else {
			if(isExternalLinkExist(linkUrl) == false) {				
				addExternalLink(linkUrl);				
			}
		}
	}

	private void addInternalLink(String linkUrl) {
		internalUrls.add(linkUrl);
	}

	private boolean isExternalLinkExist(String linkUrl) {
		return externalUrls.contains(linkUrl);
	}
	
	private void addExternalLink(String linkUrl) {		
		int indexOfSlash = linkUrl.indexOf('/');
		if (indexOfSlash == -1) {
			statistics.addConnectedDomain(linkUrl);
		}
		else
			statistics.addConnectedDomain(linkUrl.substring(0, indexOfSlash));
	}

	public void handleDisrespectingRobots() {
		if (isRespectedRobot == false) {
			ArrayList<Disallow> disallows = robotsParser.getDisallows();
			for (Disallow disallow : disallows) {
				String disallowUrl = domain + disallow.getUrl();
				String fixedUrl = fixedRobotUrlForCrawling(disallowUrl);
				handleInternalUrlByPurpose(fixedUrl);
				
				ArrayList<String> allows = disallow.getAllows();
				for (String allow : allows) {
					String allowUrl = domain + allow;
					fixedUrl = fixedRobotUrlForCrawling(allowUrl);
					handleInternalUrlByPurpose(fixedUrl);					
				}
			}
		}
	}

	private String fixedRobotUrlForCrawling(String url) {
		if (url.endsWith("$"))
			return url.substring(0, url.length() - 1);
		StringBuilder builder = new StringBuilder();
		for (int i = 0, len = url.length(); i < len; i++) {
			char c = url.charAt(i);
			if (c != '*')
				builder.append(c);
		}
		
		return builder.toString();
	}

	private synchronized void handleInternalUrlByPurpose(String linkUrl) {		
		if (isInternalLinkExist(linkUrl) == false) {
			statistics.incrementInternalLinks();				
			
			int lastIndexOfSlash = linkUrl.lastIndexOf('/');
			int lastIndexOfDot = linkUrl.lastIndexOf('.');
			if (lastIndexOfSlash == -1 || lastIndexOfDot == -1 || lastIndexOfDot < lastIndexOfSlash) {
				long size = calcFileSize(linkUrl);
				statistics.addPage(size);
				addDownloaderTask(linkUrl);
				addInternalLink(linkUrl);
				return;
			}
		
			String extension = linkUrl.substring(lastIndexOfDot + 1);
			if(isImageExtension(extension)) {
				long imageSize = calcFileSize(linkUrl);				
				statistics.addImage(imageSize);
				addInternalLink(linkUrl);
				return;
			}
			
			if(isDocumentsExtension(extension)) {				
				long documentSize = calcFileSize(linkUrl);				
				statistics.addDocument(documentSize);
				addInternalLink(linkUrl);
				return;
			}
			
			if(isVideoExtension(extension)) {
				long videoSize = calcFileSize(linkUrl);				
				statistics.addVideo(videoSize);
				addInternalLink(linkUrl);
				return;
			}
			
			long size = calcFileSize(linkUrl);
			statistics.addPage(size);
			addDownloaderTask(linkUrl);
			addInternalLink(linkUrl);
		}
	}
}
