import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnalyzerTask extends CrawlerTask {
	private String url;
	private String domain;
	private String content;
	private CrawlerJobManager crawlerJobManager;

	public AnalyzerTask(String url, String content, String domain,
			CrawlerJobManager crawlerJobManager) {
		super("Analyzer");
		this.url = url;
		this.content = content;
		this.domain = domain;
		this.crawlerJobManager = crawlerJobManager;
	}

	@Override
	public void doTask() {
		Statistics stat = crawlerJobManager.getStatistics();
		content = content.toLowerCase();
		System.out.println("Analyzer start analyze " + url);
		System.out.println("--- Content length: " + content.length());
		// content =
		// "       <a    href \"hi\"   > <a .... href   =        \"www.google.com/bla\" .....>jhsdkjfhskdj</a> <a      href=\"nir@elbit100perHour.amen.co.il\">blll</a>";
		removeWhiteSpacesAfterTagStart();
		// removeScriptTags();
		handleATags();
		handleImgTags();

		System.out.println("Finish analyze " + url);
		// stat.print();
	}

	// !!!!!this will be a bonus if e have time!!!!!!!
	private void removeScriptTags() {
		// String str =
		// "some text here<script language='javascript'>script text here</script>some text here";
		// String regex =
		// "/((<[\\s\\/]*script\\b[^>]*>)([^>]*)(<\\/script>))/i";
		//
		// Pattern p = Pattern.compile(regex);
		// Matcher m = p.matcher(str);
		//
		// while (m.find()) {
		// System.out.println(m.group(1));
		// }

		// StringBuilder stringBuilder = new StringBuilder();
		// int len = content.length();
		// int index = 0;
		// int prevIndex = 0;
		// while(index < len) {
		// int indexOfScriptTag = content.indexOf("<script ", index);
		// if (indexOfScriptTag == -1) {
		// indexOfScriptTag = content.indexOf("<script>", index);
		// if (indexOfScriptTag == -1) {
		// stringBuilder.append(content.substring(prevIndex));
		// content = stringBuilder.toString();
		// return;
		// }
		// else {
		//
		// }
		// }
		//
		// }
	}

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

		for (String imageUrl : imagesUrls) {
			int lastIndexOfDot = imageUrl.lastIndexOf('.');
			if (lastIndexOfDot == -1)
				continue;

			String extension = imageUrl.substring(lastIndexOfDot + 1);			
			if (crawlerJobManager.isImageExtension(extension)) {
				crawlerJobManager.doLinkFlow(imageUrl, url);
			}
		}
	}

	private void handleATags() {
		Statistics stat = crawlerJobManager.getStatistics();

		ArrayList<String> aLinks = getALinks();
		System.out
				.println("Number of link Analyzer extracted from a given URL: "
						+ url + " is " + aLinks.size());

		System.out.println("All links:");
		System.out.println(aLinks);
		for (String linkUrl : aLinks) {
			crawlerJobManager.doLinkFlow(linkUrl, url);
		}
	}

	// <a .... href="urrrrrrl" .....>jhsdkjfhskdj</a>
	private ArrayList<String> getALinks() {
		ArrayList<String> linksInFile = new ArrayList<String>();
		int indexOfNextLink = 0;
		int lengthOfContent = content.length();
		int index = 0;

		while (index < lengthOfContent && index != -1) {
			indexOfNextLink = content.indexOf("<a ", index);
			if (indexOfNextLink == -1)
				return linksInFile;

			index = indexOfNextLink + 3;

			if (index == lengthOfContent)
				break;

			if (content.charAt(indexOfNextLink) == '>')
				continue;

			while (index < lengthOfContent
					&& content.startsWith("href", index) == false) {
				if (content.charAt(indexOfNextLink) == '>')
					break;

				index++;
			}

			if (index == lengthOfContent
					|| content.startsWith("href", index) == false)
				break;

			index += 4; // href
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
		while (c == tav) {
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
			char c = content.charAt(index);
			contentAfterTrimming.append(c);

			if (c == '<') {
				index++;
				if (index == len)
					break;

				c = content.charAt(index);
				while (c == ' ') {
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
