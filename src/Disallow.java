import java.util.ArrayList;

import com.sun.org.apache.bcel.internal.generic.NEWARRAY;


public class Disallow {
	private String url;
	private ArrayList<String> allows;
	
	public Disallow(String url) {
		this.url = url;
		this.allows = new ArrayList<String>();
	}
	
	public void addAllow(String url) {
		allows.add(url);
	}
	
	public boolean isMatch(String url) {
		return isMatchRec(url, this.url);
	}
	
	public boolean isAllow(String url) {
		for(String allow : allows) {
			if(isMatchRec(url, allow)) {
				return true;
			}
		}
		return false;
	}
	
	
	private boolean isMatchRec(String urlWithoutDomain, String str) {
		int iStr = 0, iUrl = 0;
		int urlLen = urlWithoutDomain.length();
		int strLen = str.length();
		while (iUrl < urlLen) {
			if (iStr == strLen) {
				return true;
			}
			
			char strC = str.charAt(iStr);
			if (urlWithoutDomain.charAt(iUrl) == strC) {
				iUrl++;
				iStr++;
				continue;
			}
			
			if (strC == '$') {
				return false;
			}
			
			if (strC == '*') {
				iStr++;
				if (iStr == strLen)
					return true;
				
				strC = str.charAt(iStr);
				if (strC == '$')
					return true;
				
				while(iUrl < urlLen && iStr < strLen) {
					if (urlWithoutDomain.charAt(iUrl) == strC) {				
						String remainingStr = str.substring(iStr + 1);
						String remainingUrl = urlWithoutDomain.substring(iUrl + 1);
						if (isMatchRec(remainingStr, remainingUrl))
							return true;
					}
					iUrl++;
				}
				
				return false;
			}
			
			return false;
		}
		
		// url end
		String remainingStr = str.substring(iStr);
		if (remainingStr.equals("") || remainingStr.equals("$") || remainingStr.equals("*$") || remainingStr.equals("*")) {
			return true;
		}

		return false;
	}
	
	public void print() {
		System.out.println("Disallow: " + url);
		for(String allow : allows) {
			System.out.println("Allow: " + allow);
		}
	}

	public ArrayList<String> getAllows() {
		return allows;
	}
	
	public String getUrl() {
		return url;
	}
}
