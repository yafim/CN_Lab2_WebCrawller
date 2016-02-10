import java.util.Date;

public class DownloaderTask extends CrawlerTask{
	private String url;
	private CrawlerJobManager crawlerJobManager;
	
	public DownloaderTask(String url, CrawlerJobManager crawlerJobManager) {
		super("Downloader");
		this.url = url;
		this.crawlerJobManager = crawlerJobManager;
	}

	public CrawlerJobManager getCrawlerJobManager() {
		return crawlerJobManager;
	}
	
	public void doTask() {		
		System.out.println("Downloader starts downloading URL " + url);
		Downloader downloader;
		try {
//			Date start = new Date();
			downloader = new Downloader(url);
//			Date end = new Date();
//			long rtt = (end.getTime() - start.getTime());
			long rtt = downloader.getRTTTime();
			System.out.println("RTT is " + rtt);
			crawlerJobManager.getStatistics().addRTT(rtt);
			
			String content = downloader.getHTMLPageData();

			if (content == null) {
				return;
			}
			
			content = content.replaceAll("\r\n", " ").replace("\n", " ");
			
			// add analyzer task
			crawlerJobManager.addAnalayzerTask(url, content, crawlerJobManager);
		} catch (Exception e) {
			System.out.println("Error with downloading " + url + ". Error message: " + e.getMessage());
			return;
		}
		
		System.out.println("Downloader ends downloading the URL " + url);
	}
}