import java.util.LinkedList;
import java.util.Queue;


public class PriorityTaskManager {
	private Queue<DownloaderTask> waitDownTasks;
	private Queue<AnalyzerTask> waitAnalyzerTasks;
	
	private MyThreadPool threadPool;
	
	public PriorityTaskManager() {		
		this.waitDownTasks = new LinkedList<DownloaderTask>();
		this.waitAnalyzerTasks = new LinkedList<AnalyzerTask>();
	}
	
	public void setThreadPool(MyThreadPool threadPool) {
		this.threadPool = threadPool;
	}
	
	public void addDownloaderTask(String url, CrawlerJobManager crawlerJobManager) {
		DownloaderTask task = new DownloaderTask(url, crawlerJobManager);
		
		if (threadPool.executeIfFree(task) == false)
			waitDownTasks.add(task);
	} 
	
	public void addAnalyzerTask(String url, String content, String domain, CrawlerJobManager crawlerJobManager) {
		AnalyzerTask task = new AnalyzerTask(url, content, domain, crawlerJobManager);
		
		if (threadPool.executeIfFree(task) == false)
			waitAnalyzerTasks.add(task);
	}

	public void onTaskComplete() {
		// TODO Auto-generated method stub
		// threadPool.
	}
}
