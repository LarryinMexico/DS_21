package com.finalproject.FinalProject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


// 搜尋並爬取電影名稱
@Service
public class CrawlerService {
	
	// 添加緩存
	private static final Logger logger = LoggerFactory.getLogger(CrawlerService.class);
	
    // 電影類別列表，使用者輸入如果為列表中關鍵字，則進行評分搜尋
    private static final Set<String> MOVIE_CATEGORIES = new HashSet<>(Arrays.asList(
        "動作", "冒險", "喜劇", "劇情", "恐怖", "驚悚", "科幻", "奇幻",
        "動畫", "家庭", "愛情", "浪漫", "犯罪", "懸疑", "戰爭", "歷史",
        "傳記", "音樂", "歌舞", "運動", "武俠", "西部", "災難", "溫馨",
        "青春", "校園", "特務", "黑幫", "醫療", "職場", "警匪", "政治"
    ));
    
    // 是否為電影類別
    public boolean isMovieCategory(String keyword) {
        return MOVIE_CATEGORIES.stream()
            .anyMatch(category -> keyword.contains(category));
    }
    
    // 一般Google搜尋
    public List<Map<String, String>> generalSearch(String keyword) throws IOException {
    	String searchUrl = "https://www.google.com/search?q=" + 
    			java.net.URLEncoder.encode(keyword, "utf-8") + "&oe=utf8&num=20";
            
        String content = fetchContent(searchUrl);
        Document doc = Jsoup.parse(content);
        Elements searchResults = doc.select("div.kCrYT");
        List<Map<String, String>> results = new ArrayList<>();
        
        for (Element result : searchResults) {
            try {
                Element titleElement = result.select("a .vvjwJb").first();
                String href = result.select("a").first().attr("href");
                
                if (titleElement != null && !titleElement.text().isEmpty()) {
                    String title = titleElement.text();
                    // Clean up the URL
                    String url = href.replace("/url?q=", "").split("&")[0];
                    
                    Map<String, String> resultMap = new HashMap<>();
                    resultMap.put("title", title);
                    resultMap.put("url", url);
                    results.add(resultMap);
                }
            } catch (Exception e) {
                logger.error("Error processing search result", e);
            }
        }
        
        return results.stream()
            .distinct()
            .limit(10)
            .collect(Collectors.toList());
    }
    private String fetchContent(String urlString) throws IOException {
        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();
        conn.setRequestProperty("User-agent", "Chrome/107.0.5304.107");
        
        StringBuilder content = new StringBuilder();
        try (InputStream in = conn.getInputStream();
             InputStreamReader inReader = new InputStreamReader(in, "utf-8");
             BufferedReader bufReader = new BufferedReader(inReader)) {
            
            String line;
            while ((line = bufReader.readLine()) != null) {
                content.append(line);
            }
        }
        
        return content.toString();
    }
	
    // Google電影搜尋
	@Cacheable(value = "movieCache", key = "#keyword") // 添加緩存
	public List<String> Moviequery(String keyword) throws IOException {
		// 幫使用者輸入加上「電影」
		if (!keyword.endsWith("電影")) {
	        keyword += " 電影";
	    }
		
	    String searchUrl = "https://www.google.com/search?q=" + java.net.URLEncoder.encode(keyword, "utf-8") + "&num=10";
	    Document doc = Jsoup.connect(searchUrl).userAgent("Chrome/107.0.5304.107").get();
	    Elements links = doc.select("div.kCrYT a");

	    return links.stream()
	        .map(link -> link.attr("href").replace("/url?q=", "").split("&")[0])
	        .filter(url -> url.startsWith("http://") || url.startsWith("https://")) // 確保是完整url
	        .filter(url -> !url.contains("google.com")) // 過濾內部連結
	        .filter(this::isValidUrl) // 驗證url是否有效
	        .limit(7) //用前7個網頁中的電影名稱
	        .collect(Collectors.toList());
	}
	
	// 驗證連結是否可用
	private boolean isValidUrl(String url) {
	    try {
	        Jsoup.connect(url).get(); 
	        return true;
	    } catch (IOException e) {
	        return false;
	    }
	}
	
	// 抓取網站中的h2 h3標籤
    public List<String> fetchFromWebsites(List<String> websites) throws IOException {
        List<String> MovieNames = new ArrayList<>();

        for (String site : websites) {
            try {
                Document doc = Jsoup.connect(site).get();
                Elements elements = doc.select("h2, h3");
                for (Element element : elements) {
                    MovieNames.add(element.text());
                }
            } catch (org.jsoup.HttpStatusException e) {
                System.err.println("HTTP error fetching URL: " + site + " (Status=" + e.getStatusCode() + ")");
            } catch (IOException e) {
                System.err.println("Error processing website: " + site);
                e.printStackTrace();
            }
        }

        return MovieNames;
    }
    
    // 其他人也搜尋了...
    public List<String> fetchRelatedSearches(String keyword) throws IOException {
        String searchUrl = "https://www.google.com/search?q=" + 
            java.net.URLEncoder.encode(keyword + " 電影", "utf-8") + "&hl=zh-TW&gl=tw";
            
        Document doc = Jsoup.connect(searchUrl)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .header("Accept-Language", "zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            .referrer("https://www.google.com")
            .followRedirects(true)
            .timeout(10000)
            .get();

        // 使用多個可能的選擇器
        Elements relatedSearches = new Elements();
        
        // 嘗試所有可能的選擇器
        relatedSearches.addAll(doc.select("div.card-section a:has(b)"));
        if (relatedSearches.isEmpty()) {
            relatedSearches.addAll(doc.select(".y6Uyqe .ngTNl.ggLgoc"));
        }
        if (relatedSearches.isEmpty()) {
            relatedSearches.addAll(doc.select(".dg6jd b"));
        }

        return relatedSearches.stream()
            .map(element -> element.select("b").text())
            .filter(text -> !text.isEmpty())
            .distinct()
            .limit(8)
            .collect(Collectors.toList());
    }
	
    
    // 緩存相關設定 //
    
    // 清除
    @CacheEvict(value = "movieCache", allEntries = true)
    public void clearCache() {
        logger.info("Clearing all caches");
    }
    
    // 定時清除
    @Scheduled(fixedRate = 24 * 60 * 60 * 1000) // 24小時執行一次
    @CacheEvict(value = "movieCache", allEntries = true)
    public void clearCacheScheduled() {
        logger.info("Scheduled cache clearing - executed");
    }
	
}
