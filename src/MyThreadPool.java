import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class MyThreadPool {
	private ArrayList<MyThread> m_availableThreads;
	private ArrayList<MyThread> m_busyThreads;
	private int threadsNum;

	private PriorityTaskManager m_taskManager;

	public MyThreadPool(int threadsNum, PriorityTaskManager i_taskManager) {
		// Number of threads that will be created in the thread pool
		this.threadsNum = threadsNum;
		
		this.m_taskManager = i_taskManager;
		initializeThreads();
	}

	private void initializeThreads() {
		// Creating to array list for available and busy threads
		this.m_availableThreads = new ArrayList<MyThread>();
		this.m_busyThreads = new ArrayList<MyThread>();

		// Filling the threads array with 10 threads
		for (int counter = 0; counter < this.threadsNum; counter++) {
			// Each thread that is add is actually a Mythread object that
			// surrounds the thread, that way i can communicate between the
			// thread pool
			// and manage my threads between being in busy thread list and
			// available
			// thread list
			this.m_availableThreads.add(new MyThread(this));
		}
	}

	private synchronized MyThread getAvailableThread() {
		// Returning the first available thread
		synchronized (m_availableThreads) {
			if (!m_availableThreads.isEmpty()) {
				return m_availableThreads.remove(0);
			}
		}

		return null;
	}

	// /**
	// * This method get an available thread for the client who just
	// * got connected to the server, if the is no available thread it adds
	// * him in the waiting client list. After getting a thread for the client
	// * it will send him to connection runnable there his request will be
	// handled)
	// * @param clientSocket
	// */
	public synchronized boolean executeIfFree(CrawlerTask task) {
		MyThread selectedThread = getAvailableThread();
		// If no thread available add the client to the waiting list
		if (selectedThread == null) {
			return false;
		}
		// Adding the selected thread to the busy Thread list
		synchronized (m_busyThreads) {
			m_busyThreads.add(selectedThread);
		}
		selectedThread.execute(task);
		return true;
	}

	// Bonus feature for shutting down the server in case we want to.
	// I have implemented this for bonus and for future use if we want to
	// Some how insert shutting down
	public void shutdown() {
		synchronized (m_availableThreads) {
			for (MyThread thread : m_availableThreads) {
				thread.stop();
			}
		}

		synchronized (m_busyThreads) {
			for (MyThread thread : m_busyThreads) {
				thread.stop();
			}
		}
	}

	/**
	 * This method happens when a client finish it request, when that happens we
	 * check if there a client waiting in the client list. if we have one we
	 * using the thread that just finished with the current client to run the
	 * waiting client command.
	 * 
	 * @param myThread
	 */
	public synchronized void onTaskComplete(MyThread myThread, CrawlerTask finishedTask) {
		synchronized (m_taskManager) {
			synchronized (m_busyThreads) {	
				synchronized (m_availableThreads) {
					m_busyThreads.remove(myThread);
					m_availableThreads.add(myThread);
					m_taskManager.onTaskComplete(finishedTask);					
				}
			}					
		}
	}
	
	public synchronized boolean isAllThreadsFree() {	
		synchronized (m_busyThreads) {
			return m_busyThreads.isEmpty();
		}
	}

	public int getBusyNum() {
		synchronized (m_busyThreads) {
			return m_busyThreads.size();
		}
	}
}
