import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sun.misc.Cleaner;

public class MultiThreadedClass implements Runnable {
	protected int m_Port;
	protected String m_Root;
	protected ServerSocket serverSocket;
	protected boolean isStopped = false;
	protected MyThreadPool threadPool;
	protected String m_DefaultPage;
	private PriorityTaskManager m_taskManager;
	private int m_MaxDownloaders;
	private int m_MaxAnalyzers;
	private CrawlerJobManager m_CrawlerJob;
	private boolean m_isServerRun = true;
	private boolean m_isCrawlingMode = false;
	private ClientCommunication clientCommunication;

	public MultiThreadedClass(int i_Port, String i_Root, String i_DefaultPage){
		this.m_Root = i_Root;
		this.m_Port = i_Port;
		this.m_DefaultPage = i_DefaultPage;
	}

	public void setClientCommunication(ClientCommunication clientCommunication) {
		this.clientCommunication = clientCommunication;
	}
	
	/**
	 * starting to run the server here after therad.start was activated
	 */
	public void run(){
		//		openServerSocket();
		while(m_isServerRun) {
			if(m_isCrawlingMode) {
				while(threadPool.isAllThreadsFree() == false){
					try {
						Thread.sleep(1000);
						System.out.println("Busy num is " + threadPool.getBusyNum());
						m_CrawlerJob.printStatistics();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				System.out.println(5000);
				System.out.println("~~~~~~~~~~~~~Busy num is " + threadPool.getBusyNum());
				m_CrawlerJob.printStatistics();
				m_CrawlerJob.createResultFile();
				
				m_isCrawlingMode = false;
				clientCommunication.onFinishCrawling();
			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				System.out.println("Server failed");
				stop();
				return;
			}
		}

		//			//At the beginning no client has connected so the client
		//			//socket is obviously null
		//			Socket clientSocket = null;
		//			try {
		//				//The thread that running this server stops at this line
		//				//and waits for a client to connect.
		//				clientSocket = this.serverSocket.accept();
		//			} catch (IOException e) {
		//				System.err.println(e.getMessage());
		//				System.out.println("SOCKET CLOSED : " + serverSocket.isClosed());
		//				if(isStopped) {
		//					System.out.println("Server Stopped.") ;
		//					break;
		//				}
		//				throw new RuntimeException(
		//						"Error accepting client connection", e);
		//			}
		//			
		//			//!!!!!Something will happen here that will start everything and will add the task of the first
		//			//downloader with the first url that is given. we will not have sockets here!!!
		//			////////////////////////this.m_taskManager.addDownloaderTask(url)            
		//		}
		//		//Just in case we would like to give an option to shut down the server i
		//		//implemented the shut down method.
		//		this.threadPool.shutdown();
		//		System.out.println("Server Stopped.") ;
	}

	/**
	 * Creating a thread pool for the 10 threads that will
	 * handle all clients here and starting the server. 
	 * @param m_MaxAnalyzers 
	 * @param m_MaxDownloaders 
	 */
	public void startTheServer(int i_MaxThreads, int i_MaxDownloaders, int i_MaxAnalyzers) {
		this.m_MaxDownloaders = i_MaxDownloaders;
		this.m_MaxAnalyzers = i_MaxAnalyzers;
		m_taskManager = new PriorityTaskManager(i_MaxDownloaders, i_MaxAnalyzers);
		threadPool = new MyThreadPool(i_MaxThreads, m_taskManager);
		m_taskManager.setThreadPool(threadPool);		
	}

	public synchronized void stop(){
		this.isStopped = true;
		try {
			this.serverSocket.close();
		} catch (IOException e) {
			throw new RuntimeException("Error closing server", e);
		}
	}

	/**
	 * Creating a socket for the server
	 */
	private void openServerSocket() {
		try {
			this.serverSocket = new ServerSocket(this.m_Port);
		} catch (IOException e) {
			throw new RuntimeException("Cannot open port " + m_Port, e);
		}
	}

	public void startCrawlerFlow(CrawlerJobManager crawlerJob) {
		this.m_isCrawlingMode = true;
		this.m_CrawlerJob = crawlerJob;
		String domain = crawlerJob.getDomain();
		addDownloaderTask(domain, crawlerJob);
		crawlerJob.handleDisrespectingRobots();
		new Thread(this).start();
	}

	public void addDownloaderTask(String url, CrawlerJobManager crawlerJob) {
		m_taskManager.addDownloaderTask(url, crawlerJob);
	}

	public void addAnalayzerTask(String url, String content, String domain, CrawlerJobManager crawlerManager) {
		m_taskManager.addAnalyzerTask(url, content, domain, crawlerManager);

	}




}