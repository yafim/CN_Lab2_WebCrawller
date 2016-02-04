import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class RobotFileParser {
	private ArrayList<Disallow> disallows;
	private String domain;
	private String[] robotsFileContent;
	
	public RobotFileParser(String[] robotsFileContent, String domain) 
	{
		this.domain = domain;
		this.disallows = new ArrayList<Disallow>();
		this.robotsFileContent = robotsFileContent;
	}
	
	public void parse() {		
		int linesCounter = getUserAgentLineIndex(robotsFileContent);
		if (linesCounter == -1 || linesCounter + 1 == robotsFileContent.length)
			return;
		
		linesCounter++;
		String line = robotsFileContent[linesCounter]; 
		
		Disallow disallow = null;
		while(line.startsWith("User-agent") == false)
		{			
			if(line.startsWith("Disallow:")) 
			{
				//Because the length of "Disallow:" is 9
				
				int index = 9;
				disallow = new Disallow(line.substring(index).trim());
				disallows.add(disallow);				
			} 
			else {
				if(line.startsWith("Allow:"))	
				{
					//Because the length of "Allow:" is 6
					int index = 6;
					disallow.addAllow(line.substring(index).trim());				
				}
			}
			
			linesCounter++;
			if (linesCounter == robotsFileContent.length)
				break;
			
			line = robotsFileContent[linesCounter];
		}				
	}	
	
	private int getUserAgentLineIndex(String[] lines) {
		int linesCounter = 0;
		int len = lines.length;
		String currentString = lines[0];
		while(!currentString.equals("User-agent: *")) {
			linesCounter++;
			if (linesCounter == len)
				return -1;
			currentString = lines[linesCounter];
		}		
		
		return linesCounter;
	}

	public void print() {
		for (Disallow disallow : disallows) {
			disallow.print();
		}
	}
	
	public boolean isAllowedPage(String url) {
		String urlWithoutDomain = url;
		if (url.startsWith(domain)) {
			urlWithoutDomain = url.substring(domain.length());
		}		
	
		for(Disallow disallow : disallows) {
			if(disallow.isMatch(urlWithoutDomain)) {
				return disallow.isAllow(urlWithoutDomain);
			}
		}
		
		return true;
	}	
}
