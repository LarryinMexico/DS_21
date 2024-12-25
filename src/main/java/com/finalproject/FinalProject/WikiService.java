package com.finalproject.FinalProject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.Comparator;

// 從維基百科中爬取劇情簡介
@Service
public class WikiService {
	
	// 直接從維基百科建立url並抓取劇情內容
	 @Cacheable(value = "movieCache", key = "'wiki_' + #movieName") // 添加緩存
	public String fetchWikipediaSummary(String movieName) throws IOException {
	    // 用清理過的電影名稱建立維基百科url
	    String cleanedName = MovieProcessingService.cleanMovieName(movieName);
	    String wikiUrl = "https://zh.wikipedia.org/zh-tw/" + java.net.URLEncoder.encode(cleanedName, "UTF-8");

	    if (!isValidWiki(wikiUrl)) {
	        // 如果沒有該電影的維基百科連結，改用Google搜尋
	        return searchMovieIntroFromGoogle(cleanedName);
	    }
	    // 爬取劇情內容
	    return fetchParagraph(wikiUrl);
	}
	
    // 用來驗證維基頁面是否有效
    private boolean isValidWiki(String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            return !doc.title().contains("標題無效") && !doc.title().contains("Wikipedia does not have an article");
        } catch (IOException e) {
            return false;
        }
    }
    
    // 嘗試抓取維基百科中的「劇情」段落
    private String fetchParagraph(String wikiUrl) throws IOException {
        Document wikiDoc = Jsoup.connect(wikiUrl).get();
        Elements paragraphs = wikiDoc.select("p");
        
        for (Element p : paragraphs) {
            String text = p.text().trim();
            text = text.replaceAll("\\[\\d+\\]", ""); //移除註解
            
            // 如果段落文字超過100個字，則返回。避免抓到過短的劇情簡介
            if (!text.isEmpty() && text.length() >= 100) {
                return text;
            }
        }
        
        // 都不符合條件，返回最長段落
        return paragraphs.stream()
                .map(p -> p.text().replaceAll("\\[\\d+\\]", "").trim())
                .filter(text -> !text.isEmpty())
                .max(Comparator.comparingInt(String::length))
                .orElse("找不到簡介內容。");
    }
    
	// 嘗試直接用google搜尋劇情
	private String searchMovieIntroFromGoogle(String movieName) throws IOException {
	    String searchQuery = movieName + " 電影簡介";
	    String searchUrl = "https://www.google.com/search?q=" + java.net.URLEncoder.encode(searchQuery, "utf-8");
	    
	    Document doc = Jsoup.connect(searchUrl)
	        .userAgent("Chrome/107.0.5304.107")
	        .get();
	    
	    // 嘗試獲取第一個搜尋結果
	    Elements searchResults = doc.select("div.kCrYT a");
	    if (!searchResults.isEmpty()) {
	        String firstResultUrl = searchResults.first()
	            .attr("href")
	            .replace("/url?q=", "")
	            .split("&")[0];
	            
	        // 確保url有效
	        if (firstResultUrl.startsWith("http")) {
	            try {
	                Document resultDoc = Jsoup.connect(firstResultUrl).get();
	                
	                // 先嘗試獲取第一個p元素
	                Element firstP = resultDoc.select("p").stream()
	                    .filter(p -> !p.text().trim().isEmpty())
	                    .findFirst()
	                    .orElse(null);
	                    
	                if (firstP != null) {
	                    return firstP.text();
	                }
	                
	                // 如果沒有p，嘗試獲取br元素後的文字
	                Element firstBr = resultDoc.select("br").first();
	                if (firstBr != null && firstBr.parent() != null) {
	                    String text = firstBr.parent().text();
	                    if (!text.trim().isEmpty()) {
	                        return text;
	                    }
	                }
	            } catch (IOException e) {
	                return "無法獲取電影簡介";
	            }
	        }
	    }
	    return "無法獲取電影簡介";
	}
}
