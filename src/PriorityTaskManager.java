import java.util.LinkedList;
import java.util.Queue;


public class PriorityTaskManager {
	private Queue<DownloaderTask> waitDownTasks;
	private Queue<AnalyzerTask> waitAnalyzerTasks;
	
	private int m_MaxDownloaders;
	private int m_MaxAnalyzers;
	
	private int m_WorkingDownloaders = 0;
	private int m_WorkingAnalyzers = 0;
	
	private MyThreadPool threadPool;
	private Object lock = new Object();
	
	public PriorityTaskManager(int i_MaxDownloaders, int i_MaxAnalyzers) {		
		this.m_MaxDownloaders = i_MaxDownloaders;
		this.m_MaxAnalyzers = i_MaxAnalyzers;
		
		this.waitDownTasks = new LinkedList<DownloaderTask>();
		this.waitAnalyzerTasks = new LinkedList<AnalyzerTask>();
	}
	
	public void setThreadPool(MyThreadPool threadPool) {
		this.threadPool = threadPool;
	}
	
	public void addDownloaderTask(String url, CrawlerJobManager crawlerJobManager) {
		synchronized (lock) {
			DownloaderTask task = new DownloaderTask(url, crawlerJobManager);
			
			if (threadPool.executeIfFree(task))
				m_WorkingDownloaders++;
			else
				waitDownTasks.add(task);
		}
	} 
	
	public void addAnalyzerTask(String url, String content, String domain, CrawlerJobManager crawlerJobManager) {
		synchronized (lock) {
			AnalyzerTask task = new AnalyzerTask(url, content, domain, crawlerJobManager);
		
			if (threadPool.executeIfFree(task))
				m_WorkingAnalyzers++;
			else
				waitAnalyzerTasks.add(task);
		}
	}

	public void onTaskComplete(CrawlerTask task) {
		synchronized (lock) {
			if (task.getName().equals("Downloader"))
				m_WorkingDownloaders--;
			else
				m_WorkingAnalyzers--;
			
			if (!waitAnalyzerTasks.isEmpty()) {
				if(m_WorkingAnalyzers < m_MaxAnalyzers) {
					if(threadPool.executeIfFree(waitAnalyzerTasks.element())){
						waitAnalyzerTasks.poll();
						m_WorkingAnalyzers++;
					}
					return;
				}
			}
			
			if (!waitDownTasks.isEmpty()) {
				if(m_WorkingDownloaders < m_MaxDownloaders) {
					if(threadPool.executeIfFree(waitDownTasks.element())){
						waitDownTasks.poll();
						m_WorkingDownloaders++;
					}
				}
			}
		}
	}
}
