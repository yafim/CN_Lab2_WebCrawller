import java.net.Socket;

public class MyThread {
	private Thread thread;
	private ConnectionRunnable runnable;
	private MyThreadPool myThreadPool;

	public MyThread(MyThreadPool myThreadPool, String i_Root,
			String i_DefaultPage) {
		this.myThreadPool = myThreadPool;
		this.runnable = new ConnectionRunnable(this, i_Root, i_DefaultPage);
		this.thread = new Thread(this.runnable);
		// Tell the thread to start running and in case we have no client it
		// will
		// switch to wait mode on the method run.
		this.thread.start();
	}

	/**
	 * returns true if the threads runs a client request.
	 * 
	 * @return
	 */
	public boolean isBusy() {
		return runnable.isBusy();
	}

	/**
	 * Execute the client command through the connection runnable
	 * 
	 * @param clientSocket
	 */
	public void execute(CrawlerTask task) {
		runnable.doTask(task);
	}

	/**
	 * Informing the thread pool that this thread has finished\ handling the
	 * client request.
	 */
	public void onTaskComplete() {
		myThreadPool.onTaskComplete(this);
	}

	/**
	 * Making the thread waiting until he is needed for a new client.
	 */
	public void waitForClient() {
		synchronized (thread) {
			try {
				thread.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * part of the bonus features, stopping the client request in case shut down
	 * method was activated.
	 */
	public void stop() {
		this.runnable.stop();
	}

}
