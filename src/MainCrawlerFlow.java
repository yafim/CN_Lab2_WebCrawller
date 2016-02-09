import java.util.Date;


public class MainCrawlerFlow implements Runnable {
	private ClientCommunication clientCommunication;
	private MultiThreadedClass server;
	private ExtensionsChecker extensionChecker;

	public MainCrawlerFlow(ClientCommunication clientCommunication, MultiThreadedClass server) {
		this.clientCommunication = clientCommunication;
		this.server = server;
		this.extensionChecker = clientCommunication.getExtensionChecker();
	}
	
	@Override
	public void run() {
		Date crawlerStartTime = new Date();
		String requestedUrl = clientCommunication.getRequestedUrl();
		boolean isRespectedRobotFile = clientCommunication.isRobotFileRespected();
		boolean isRequestedTcpPortsScan = clientCommunication.isTCPOpenPortsRequested();
		String[] robotsFileContent = clientCommunication.getRobotsFileContent(requestedUrl);
		
		// change it
		CrawlerJobManager crawlerManager = new CrawlerJobManager(server, requestedUrl, isRespectedRobotFile, isRequestedTcpPortsScan, extensionChecker);
		crawlerManager.start(robotsFileContent, crawlerStartTime);
		
		if (isRequestedTcpPortsScan)
			crawlerManager.setOpenPorts(clientCommunication.getOpenPorts());
	}
}
