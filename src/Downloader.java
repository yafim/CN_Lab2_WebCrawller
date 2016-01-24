import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Downloader {
	// Variables
	private String m_URL;
	private HashMap<String, String> m_HashedUrl;
	
	public HashMap<String, String> getHashedURL(){return m_HashedUrl;}
	
	private String m_URLContent;
	private boolean m_IsTCP;
	private boolean m_IsRobots;
	
	public Downloader(){};
	
	private void setURLContent() throws IOException{
        URL url = new URL(m_URL);
        BufferedReader in = new BufferedReader(
        new InputStreamReader(url.openStream()));

        String inputLine;
        while ((inputLine = in.readLine()) != null){;
        	m_URLContent += inputLine;
        }
        
        in.close();
	}
	
	private void setURLAsHashMap(){
		if (m_HashedUrl == null){
			m_HashedUrl = new HashMap<String, String>();
		}
		
		m_HashedUrl.put(m_URL, m_URLContent);
		
		
		
	}
	
//	public void bla() {
//		try
//        {
//            Connection connection = Jsoup.connect(url).userAgent(USER_AGENT);
//            Document htmlDocument = connection.get();
//            this.htmlDocument = htmlDocument;
//            if(connection.response().statusCode() == 200) // 200 is the HTTP OK status code
//                                                          // indicating that everything is great.
//            {
//                System.out.println("\n**Visiting** Received web page at " + url);
//            }
//            if(!connection.response().contentType().contains("text/html"))
//            {
//                System.out.println("**Failure** Retrieved something other than HTML");
//                return false;
//            }
//            Elements linksOnPage = htmlDocument.select("a[href]");
//            System.out.println("Found (" + linksOnPage.size() + ") links");
//            for(Element link : linksOnPage)
//            {
//                this.links.add(link.absUrl("href"));
//            }
//            return true;
//        }
//        catch(IOException ioe)
//        {
//            // We were not successful in our HTTP request
//            return false;
//        }
//		
//	}
//	
	public void initParams(HashMap<String, String> i_Params){
		try{
			parseParams(i_Params);
			setURLContent();
			
			//TODO: Debug? 
			setURLAsHashMap();
		}
		catch (Exception e){
			System.out.println(e.getMessage());
		}
	}
	
	private void parseParams(HashMap<String, String> i_Params) throws Exception{
		for (Map.Entry<String,String> entry : i_Params.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			
			switch(key){
				case "textBoxURL":
					m_URL = getFixedURL(value);
					break;
				case "checkBoxTCP":
					m_IsTCP = (value.equals("on"));
					break;
				case "checkBoxRobots":
					m_IsRobots = (value.equals("on"));
					break;						
			}
			
			if (m_URL == ""){
				throw new Exception("Bad URL was given");
			}
		}
	}
	
	private String getFixedURL(String i_URLToFix){
		String fixedURL = "";
		if(i_URLToFix.contains("http%3A%2F%2F")){
			fixedURL = i_URLToFix.replace("http%3A%2F%2F", "HTTP://");
		}
		else if (i_URLToFix.contains("www.")){
			fixedURL = "HTTP://" + i_URLToFix;
		}
		
		return fixedURL;
	}
	
	/******************* DEBUG ***********************/
	
	/******************* END DEBUG ******************/
	
}
