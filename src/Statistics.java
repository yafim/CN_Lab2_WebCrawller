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

	private String domain;
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
	private ArrayList<Integer> openedPorts;
	private double sumOfAllRTTTimes = 0; // millis
	private long numberOfRTT = 0;

	private Date m_CrawlerStartTime;

	public Statistics(String i_Domain, boolean isRespectedRobot, boolean isRequestedOpenPorts, Date i_CrawlerStartTime)
	{
		this.domain = i_Domain;
		this.isRespectedRobot = isRespectedRobot;
		this.isRequestedOpenPorts = isRequestedOpenPorts;
		this.m_CrawlerStartTime = i_CrawlerStartTime;

		this.connectedDomains = new ArrayList<String>();
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

	public void createResultFile() throws IOException{
		DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
		String fileName = domain + "_" + dateFormat.format(m_CrawlerStartTime);

		String path = m_Root + File.separator + "CrawlerResults" + File.separator + fileName + ".html";
		// Use relative path for Unix systems
		File f = new File(path);
		//			System.out.println(fileName + "created");

		//write to file
		Writer writer = null;

		try {
			writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(f), "utf-8"));
			writer.write(getCrawlerStatistics());
		} catch (IOException ex) {
			// report
		} finally {
			try {writer.close();} catch (Exception ex) {/*ignore*/}
		}

		f.getParentFile().mkdirs(); 
		f.createNewFile();
	}

	private String getCrawlerStatistics(){
		String listOfOpenPorts = "";
		StringBuilder str = new StringBuilder();
		str.append("<html><head></head><body>");
		//Did the crawler respect robots.txt or not.
		str.append("Robots respect: " + isRespectedRobot + "<br>" );

		//Number of images(from config.ini)
		str.append("Number of images: " + numberOfImages + "<br>");

		//Total size (in bytes) of images
		str.append("Total size (in kilobytes) of images: " + imagesSize + " <br>");

		//		Number of videos(from config.ini)
		str.append("Number of videos: " + numberOfVideos + "<br>");

		//		Total size (in bytes) of videos
		str.append("Total size (in kilobytes) of videos: " + videosSize + " <br>");

		//		Number of document (from config.ini)
		str.append("Number of documents: " + numberOfDocuments + " <br>");

		//		Total size (in bytes) of documents
		str.append("Total size (in kilobytes) of documents: " + documentsSize + " <br>");

		//		Number of pages(all detected files excluding images, videosand documents).
		str.append("Number of pages: " + numberOfPages + " <br>");

		//		Total size (in bytes) of pages
		str.append("Total size (in kilobytes) of pages: " + pagesSize + " <br>");

		//		Number of internal links(pointing into the domain)
		str.append("Number of internal links: " + numberOfInternalLinks + " <br>");

		//		Number of external links (pointing outside the domain)
		str.append("Number of external links: " + numberOfExternalLinks + " <br>");

		//		Number of domains the crawled domain is connected to
		str.append("Number of domains the crawled domain is connected to: " + numberOfDomains + " <br>");

		//		The domains the crawled domain is connected to
		str.append("The domains the crawled domain is connected to: <br>");

		for (String conDomain : connectedDomains) {
			str.append("\t" + conDomain + "<br>");
		}

		//		If requested, the opened ports.
		str.append("Is open ports requested: " + isRequestedOpenPorts + "<br>");
		if (isRequestedOpenPorts){
			for(int port : openedPorts){
				listOfOpenPorts += " " + port + " ";
			}

			str.append("The opened ports: " + listOfOpenPorts + "<br>");
		}
		
		//		Average RTT in milliseconds (Time passed from sending the HTTP request until received the HTTP response, excludingreading the response).
		str.append("Average RTT in milliseconds: " + getAvgRTTInMillis() + "<br>");

		str.append("<a href='../'>BACK</a><br></body></html>");

		return str.substring(0, str.length());
	}

	private double getAvgRTTInMillis() {
		return roundSize(sumOfAllRTTTimes / numberOfRTT);
	}

	public void setOpenPorts(ArrayList<Integer> openPorts) {
		this.openedPorts = openPorts;
	}

}
