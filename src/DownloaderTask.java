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
			downloader = new Downloader(url);
			String content = downloader.getHTMLPageData();

			// add analyzer task
			crawlerJobManager.addAnalayzerTask(url, content, crawlerJobManager);
		} catch (Exception e) {
			System.out.println("Error with downloading " + url + ". Error message: " + e.getMessage());
			return;
		}
		
		System.out.println("Downloader ends downloading the URL " + url);
	}
}