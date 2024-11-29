package com.finalproject.FinalProject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

import org.springframework.stereotype.Service;


@Service
public class GoogleQueryService {
	 public Map<String, String> searchGoogle(String keyword) throws IOException {
	        GoogleQuery googleQuery = new GoogleQuery(keyword);
	        return googleQuery.query();
	    }
	    
	 // 您原本的 GoogleQuery 類別可以作為內部類別，或者作為單獨的類別
	 private class GoogleQuery {
	        private String searchKeyword;
	        private String url;
	        private String content;
	        
	        public GoogleQuery(String searchKeyword) {
	            this.searchKeyword = searchKeyword;
	            try {
	            	// This part has been specially handled for Chinese keyword processing. 
	                // You can comment out the following two lines 
	                // and use the line of code in the lower section. 
	                // Also, consider why the results might be incorrect 
	                // when entering Chinese keywords.
	                String encodeKeyword=java.net.URLEncoder.encode(searchKeyword,"utf-8");
	                this.url = "https://www.google.com/search?q="+encodeKeyword+"&oe=utf8&num=20";

	                // this.url = "https://www.google.com/search?q="+searchKeyword+"&oe=utf8&num=20";
	            } catch (Exception e) {
	                System.out.println(e.getMessage());
	            }
	        }
	        
	        private String fetchContent() throws IOException {
	            String retVal = "";
	            URL u = new URL(url);
	            URLConnection conn = u.openConnection();
	            conn.setRequestProperty("User-agent", "Chrome/107.0.5304.107");
	            InputStream in = conn.getInputStream();
	            InputStreamReader inReader = new InputStreamReader(in, "utf-8");
	            BufferedReader bufReader = new BufferedReader(inReader);
	            String line = null;
	            while((line = bufReader.readLine()) != null) {
	                retVal += line;
	            }
	            return retVal;
	        }
	        public Map<String, String> query() throws IOException {
	            if(content == null) {
	                content = fetchContent();
	            }
	            Map<String, String> retVal = new HashMap<>();
	            
	            Document doc = Jsoup.parse(content);
	            Elements lis = doc.select("div");
	            lis = lis.select(".kCrYT");
	            
	            for(Element li : lis) {
	                try {
	                    String citeUrl = li.select("a").get(0).attr("href").replace("/url?q=", "");
	                    String title = li.select("a").get(0).select(".vvjwJb").text();
	                    if(title.equals("")) {
	                        continue;
	                    }
	                    System.out.println("Title: " + title + " , url: " + citeUrl);
	                    retVal.put(title, citeUrl);
	                } catch (IndexOutOfBoundsException e) {
	                    //e.printStackTrace();
	                }
	            }
	            return retVal;
	            
	        }
	    }
}
