import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class AnalyzerTask extends CrawlerTask {
	private String url;
	private String domain;
	private String content;	
	private CrawlerJobManager crawlerJobManager;
	
	public AnalyzerTask(String url, String content, String domain, CrawlerJobManager crawlerJobManager) {
		super("Analyzer");
		this.url = url;		
		this.content = content;
		this.domain = domain;
		this.crawlerJobManager = crawlerJobManager;
	}

	@Override
	public void doTask() {
		Statistics stat = crawlerJobManager.getStatistics();
		
		System.out.println("Analyzer start analyze " + url);
		System.out.println("--- Content length: " + content.length());
		//content = "       <a    href \"hi\"   > <a .... href   =        \"www.google.com/bla\" .....>jhsdkjfhskdj</a> <a      href=\"nir@elbit100perHour.amen.co.il\">blll</a>";		
		removeWhiteSpacesAfterTagStart();
		//removeScriptTags();
		handleATags();		
		handleImgTags();
		//handleVideoTags();
		
		System.out.println("Finish analyze " + url);
//		stat.print();
	}
	
//	private void removeScriptTags() {
//		StringBuilder stringBuilder = new StringBuilder();
//		int len = content.length();
//		int index = 0;
//		int prevIndex = 0;
//		while(index < len) {
//			int indexOfScriptTag = content.indexOf("<script ", index);
//			if (indexOfScriptTag == -1) {
//				indexOfScriptTag = content.indexOf("<script>", index);
//				if (indexOfScriptTag == -1) {
//					stringBuilder.append(content.substring(prevIndex));
//					content = stringBuilder.toString();
//					return;
//				}
//				else {
//					
//				}
//			}
//				
//		}
//	}

	private void handleImgTags() {
		Statistics stat = crawlerJobManager.getStatistics();		

		
		String imgRegex = "<img[^>]+src\\s*=\\s*[\'\"]([^\'\"]+)[\'\"][^>]*>";
		Pattern p = Pattern.compile(imgRegex);
		
		Matcher m = p.matcher(content);
		ArrayList<String> imagesUrls = new ArrayList<String>();
		 
		while (m.find()) {
			imagesUrls.add(m.group(1));
		}
		
		System.out.println("All images:");
		System.out.println(imagesUrls);

		for(String imageUrl : imagesUrls) {
			int lastIndexOfDot = imageUrl.lastIndexOf('.');
			if (lastIndexOfDot == -1)
				continue;
		
			String extension = imageUrl.substring(lastIndexOfDot + 1);
			if(crawlerJobManager.isImageExtension(extension)) {
				String fullFilePath = getFullFilePath(imageUrl);
				int imageSize = crawlerJobManager.calcFileSize(fullFilePath);				
				stat.addImage(imageSize);
			}
		}		
	}

	private String getFullFilePath(String fileUrl) {		
		if (fileUrl.startsWith("/"))
			return url + fileUrl;
		
		return fileUrl;
	}

	private void handleATags() {
		Statistics stat = crawlerJobManager.getStatistics();
		
		ArrayList<String> aLinks = getALinks();	
		System.out.println("Number of link Analyzer extracted from a given URL: " + url + " is " + aLinks.size());
		
		System.out.println("All links:");
		System.out.println(aLinks);
		for(String linkUrl : aLinks) {
			if(isInternalLink(linkUrl)) {				
				if (linkUrl.startsWith("http://") == false && linkUrl.startsWith("https://") == false) {
					if (linkUrl.startsWith("/")) 
						linkUrl = domain + linkUrl;
					else {
						int lastIndexOfSlash = url.lastIndexOf("/");
						if (lastIndexOfSlash == -1)
							linkUrl = url + "/" + linkUrl; 
						else {
							int indexOfDot = url.indexOf('.', lastIndexOfSlash);
							if (indexOfDot == -1) {
								if (lastIndexOfSlash == (url.length() - 1))
									linkUrl = url + linkUrl;
								else
									linkUrl = url + "/" + linkUrl;
							}
							else {
								String urlWithoutPage = url.substring(0, lastIndexOfSlash + 1);
								linkUrl = urlWithoutPage + linkUrl;
							}
						}
					}
				}
				
				if (crawlerJobManager.isInternalLinkExist(linkUrl) == false) {
					stat.incrementInternalLinks();
					crawlerJobManager.addDownloaderTask(linkUrl, crawlerJobManager);
				}
			} else {
				stat.incrementExternalLinks();
				int indexOfSlash = linkUrl.indexOf('/');
				if (indexOfSlash == -1) {
					stat.addConnectedDomain(linkUrl);
				}
				else
					stat.addConnectedDomain(linkUrl.substring(0, indexOfSlash));
			}
		}
	}

	private boolean isInternalLink(String linkUrl) {
		if (linkUrl.startsWith("/"))
			return true;
		
		if (linkUrl.startsWith("https://"))
			return false;
		
		if (linkUrl.startsWith("http://") == false)
			return true;
		
		// start with http://
		linkUrl = linkUrl.replace("http://", "");
		String domainWithoutHttp = domain.replace("http://", "");
		
		if (linkUrl.startsWith("www."))
			linkUrl = linkUrl.substring(4);
		
		if (domainWithoutHttp.startsWith("www."))
			domainWithoutHttp = domainWithoutHttp.substring(4);
		
		return linkUrl.startsWith(domainWithoutHttp);
		
//		if(linkUrl.startsWith("www.")) {
//			if (domainWithoutHttp.startsWith("www."))
//				return linkUrl.startsWith(domainWithoutHttp);
//			else 
//				return linkUrl.startsWith("www." + domainWithoutHttp);
//		}
//		else {
//			if(domainWithoutHttp.startsWith("www.")) {
//				linkUrl = "www." + linkUrl;
//				return linkUrl.startsWith(domainWithoutHttp);
//			}
//			else
//				return linkUrl.startsWith(domainWithoutHttp);						
//		}			
	}
	
	// <a .... href="urrrrrrl" .....>jhsdkjfhskdj</a>
	private ArrayList<String> getALinks() {
		ArrayList<String> linksInFile = new ArrayList<String>();
		int indexOfNextLink = 0;
		int lengthOfContent = content.length();
		int index = 0;
		
		while(index < lengthOfContent && index != -1) {
			indexOfNextLink = content.indexOf("<a ", index);
			if (indexOfNextLink == -1)
				return linksInFile;
			
			index = indexOfNextLink + 3;
			
			if (index == lengthOfContent)
				break;
			
			if (content.charAt(indexOfNextLink) == '>')
				continue;
				
			while(index < lengthOfContent && content.startsWith("href", index) == false) {					
				if (content.charAt(indexOfNextLink) == '>')	
					break;
				
				index++;
			}			
			
			if (index == lengthOfContent || content.startsWith("href", index) == false)
				break;
			
			index += 4; //href
			if (index == lengthOfContent)
				break;
			
			index = getNextNotTavIndex(content, index, ' ');
			if (index == -1)
				return linksInFile;
						
			if (content.charAt(index) != '=') {
				index = content.indexOf('>', index);
				if (index == -1)
					return linksInFile;
				continue;
			}
			
			
			index = getNextNotTavIndex(content, index + 1, ' ');
			if (content.charAt(index) != '"') {
				index = content.indexOf('>', index);
				if (index == -1)
					return linksInFile;
				continue;
			}
			index++;
			
			if (index == lengthOfContent)
				return linksInFile;
			
			int indexOfPar = content.indexOf('"', index);
			linksInFile.add(content.substring(index, indexOfPar));
			
			index = indexOfPar + 1;			
		}
		
		return linksInFile;
	}

	private int getNextNotTavIndex(String str, int fromIndex, char tav) {
		int len = str.length();
		if (fromIndex >= len)
			return -1;
		
		char c = content.charAt(fromIndex);
		while(c == tav) {					
			fromIndex++;
			if (fromIndex == len)
				return -1;
			
			c = content.charAt(fromIndex);
		}
		
		return fromIndex;
	}

	private void removeWhiteSpacesAfterTagStart() {
		StringBuilder contentAfterTrimming = new StringBuilder("");
		for (int index = 0, len = content.length(); index < len; index++) {
			char c  = content.charAt(index);
			contentAfterTrimming.append(c);
			
			if(c == '<') {										
				index++;				
				if (index == len)
					break;
				
				c = content.charAt(index);
				while(c == ' ') {					
					index++;
					if (index == len)
						break;
					
					c = content.charAt(index);
				}					
				
				if (index != len)
					contentAfterTrimming.append(c);
			}						
		}
		
		content = contentAfterTrimming.toString();
	}
	
	
}
