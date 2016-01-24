import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiThreadedClass implements Runnable {
	protected int m_Port;
	protected String m_Root;
	protected ServerSocket serverSocket;
	protected boolean isStopped = false;
	protected MyThreadPool threadPool;
	protected String m_DefaultPage;
	private PriorityTaskManager m_taskManager;
	
	public MultiThreadedClass(int i_Port, String i_Root, String i_DefaultPage){
		this.m_Root = i_Root;
		this.m_Port = i_Port;
		this.m_DefaultPage = i_DefaultPage;
	}

	/**
	 * starting to run the server here after therad.start was activated
	 */
	public void run(){
		openServerSocket();
		while(!isStopped){
			//At the beginning no client has connected so the client
			//socket is obviously null
			Socket clientSocket = null;
			try {
				//The thread that running this server stops at this line
				//and waits for a client to connect.
				clientSocket = this.serverSocket.accept();
			} catch (IOException e) {
				System.err.println(e.getMessage());
				System.out.println("SOCKET CLOSED : " + serverSocket.isClosed());
				if(isStopped) {
					System.out.println("Server Stopped.") ;
					break;
				}
				throw new RuntimeException(
						"Error accepting client connection", e);
			}
			
			//!!!!!Something will happen here that will start everything and will add the task of the first
			//downloader with the first url that is given. we will not have sockets here!!!
			////////////////////////this.m_taskManager.addDownloaderTask(url)            
		}
		//Just in case we would like to give an option to shut down the server i
		//implemented the shut down method.
		this.threadPool.shutdown();
		System.out.println("Server Stopped.") ;
	}

	/**
	 * Creating a thread pool for the 10 threads that will
	 * handle all clients here and starting the server. 
	 */
	public void startTheServer(int i_MaxThreads) {
		m_taskManager = new PriorityTaskManager();
		threadPool = new MyThreadPool(i_MaxThreads, m_Root, m_DefaultPage, m_taskManager);
		m_taskManager.setThreadPool(threadPool);
		new Thread(this).start();
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
}