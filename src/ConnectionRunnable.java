public class ConnectionRunnable implements Runnable{
	private CrawlerTask m_task = null;
	private boolean m_isRun = true;
	private MyThread myThread;
	private Thread thread;

	public HTTPRequest m_HttpRequest;

	public ConnectionRunnable(MyThread myThread) {
		this.myThread = myThread;
	}

	public synchronized void run() {
		this.thread = Thread.currentThread();
		//While the thread is running if the is no client then it
		//tells him to wait until he will call to handle a client
		while(m_isRun) {
			if (m_task != null) {
				runTask();
			}
			else {
				synchronized (thread) {
					try {
						thread.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void runTask() {
		m_task.doTask();
		m_task = null;
		myThread.onTaskComplete();		
	}

	public boolean isBusy() {
		return m_task != null;
	}

	public void doTask(CrawlerTask task){// throws IOException {		
		this.m_task = task;

		synchronized (thread) {
			thread.notify();
		}
	}

	/**
	 * Stops the client request upon calling by changing the running
	 * mode of the variable m_isRun to false 
	 */
	public void stop() {
		this.m_isRun = false;
		synchronized (thread) {
			thread.notify();
		}
	}
}