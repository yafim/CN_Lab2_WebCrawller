import java.io.File;
import java.io.UnsupportedEncodingException;


public class DownloaderTask implements CrawlerTask{
	private String url;
	private CrawlerJobManager crawlerJobManager;
	
	public DownloaderTask(String url, CrawlerJobManager crawlerJobManager) {
		this.url = url;
		this.crawlerJobManager = crawlerJobManager;
	}

	public CrawlerJobManager getCrawlerJobManager() {
		return crawlerJobManager;
	}
	
	@Override
	public void doTask() {
		System.out.println("Downloader start downloading " + url);
		
		Downloader downloader;
		try {
			downloader = new Downloader(url);
			String content = downloader.getHTMLPageData();
		
			// add analyzer task
			crawlerJobManager.addAnalayzerTask(url, content, crawlerJobManager);
		} catch (Exception e) {
			System.out.println("Error with downloading " + url + ". Print stack trace:");
			e.printStackTrace();
		}
		
	}
}