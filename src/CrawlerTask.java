
public abstract class CrawlerTask {	
	private String m_Name;
	
	public CrawlerTask(String i_Name) {
		this.m_Name = i_Name;
	}
	
	public String getName() {
		return this.m_Name;
	}
	
	public abstract void doTask();
}


