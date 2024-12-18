package com.finalproject.FinalProject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GoogleQueryService {

    // Google 搜尋功能
	public List<String> query(String keyword) throws IOException {
	    String searchUrl = "https://www.google.com/search?q=" + java.net.URLEncoder.encode(keyword, "utf-8") + "&num=15";
	    Document doc = Jsoup.connect(searchUrl).userAgent("Chrome/107.0.5304.107").get();
	    Elements links = doc.select("div.kCrYT a");

	    return links.stream()
	        .map(link -> link.attr("href").replace("/url?q=", "").split("&")[0])
	        .filter(url -> url.startsWith("http://") || url.startsWith("https://")) // 確保是完整 URL
	        .filter(url -> !url.contains("google.com")) // 過濾 Google 內部連結
	        .filter(this::isValidUrl) // 驗證 URL 是否有效
	        .limit(8)
	        .collect(Collectors.toList());
	}

	private boolean isValidUrl(String url) {
	    try {
	        Jsoup.connect(url).get(); // 驗證連結是否可用
	        return true;
	    } catch (IOException e) {
	        return false;
	    }
	}

    // 爬取網站的 h2 和 h3 標籤內容
	public List<String> fetchFromWebsites(List<String> websites) throws IOException {
	    List<String> allTexts = new ArrayList<>();

	    for (String site : websites) {
	        try {
	            Document doc = Jsoup.connect(site).get();
	            Elements elements = doc.select("h2, h3");
	            for (Element element : elements) {
	                allTexts.add(element.text());
	            }
	        } catch (org.jsoup.HttpStatusException e) {
	            System.err.println("HTTP error fetching URL: " + site + " (Status=" + e.getStatusCode() + ")");
	        } catch (IOException e) {
	            System.err.println("Error processing website: " + site);
	            e.printStackTrace();
	        }
	    }

	    return allTexts;
	}


    // 從抓取的文字中提取《》內的電影名稱
    public List<String> extractMovieNames(List<String> texts) {
        List<String> movieNames = new ArrayList<>();
        for (String text : texts) {
            int start = text.indexOf('《');
            int end = text.indexOf('》');
            if (start != -1 && end != -1 && start < end) {
                movieNames.add(text.substring(start + 1, end).trim());
            }
        }
        return movieNames;
    }

    // 從維基百科抓取第一段介紹
    public String fetchWikipediaSummary(String movieName) throws IOException {
        // 清理電影名稱並構建維基百科 URL
        String cleanedName = cleanMovieName(movieName);
        String wikiUrl = "https://zh.wikipedia.org/zh-tw/" + java.net.URLEncoder.encode(cleanedName, "UTF-8");

        // 驗證 URL 是否有效
        if (!isValidWikipediaPage(wikiUrl)) {
            return "找不到維基百科頁面。";
        }

        // 爬取第一段內容
        return fetchFirstParagraph(wikiUrl);
    }

    // 去除電影名中的英文字母及後續內容
    public static String cleanMovieName(String movieName) {
        return movieName.replaceAll("\\s[A-Za-z].*$", "").trim();
    }
    
    // 去除重複電影
    public List<String> removeDuplicates(List<String> movieNames){
    	// Set中不會有重複項
    	Set<String> unique = new LinkedHashSet<>(movieNames);
    	return new ArrayList<>(unique);
    }

    // 驗證維基百科頁面是否有效
    private boolean isValidWikipediaPage(String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            return !doc.title().contains("標題無效") && !doc.title().contains("Wikipedia does not have an article");
        } catch (IOException e) {
            return false;
        }
    }
    
    // 只爬取維基百科中第一段的文字內容
    private String fetchFirstParagraph(String wikiUrl) throws IOException {
        Document wikiDoc = Jsoup.connect(wikiUrl).get();
        Element firstParagraph = wikiDoc.selectFirst("p"); // 抓取第一個p元素就好了

        if (firstParagraph == null) {
            return "找不到簡介內容。";
        }

        return firstParagraph.text().trim();
    }
    
    // 計算電影出現次數
    public Map<String, Integer> calculateMoviescore(List<String> movieNames){
    	Map<String, Integer> movieScoreMap = new LinkedHashMap<>();
    	for(String name: movieNames) {
    		movieScoreMap.put(name, movieScoreMap.getOrDefault(name, 0)+1);
    	}
    	
    	return movieScoreMap;
    }
    
    // 抓取到的h2,h3元：先提取，去除英文，計算分數
    
    //計算電影名稱出現次數
    public static Map<String, Integer> extractAndScoreMovies(List<String> movieNames) {
        Map<String, Integer> movieScoreMap = new LinkedHashMap<>(); // 使用 LinkedHashMap 保持順序

        for (String movieName : movieNames) {
            // 去除英文，只保留非英文字符
            String cleanedMovieName = cleanMovieName(movieName);
            // 若名稱為空則跳過
            if (cleanedMovieName.trim().isEmpty()) {
                continue;
            }
            // 計算電影出現次數
            movieScoreMap.put(cleanedMovieName, movieScoreMap.getOrDefault(cleanedMovieName, 1));
        }

        return movieScoreMap;
    }
    
    // 主邏輯：同時返回電影分數和去重後的電影名稱
    public Map<String, Object> processMovies(List<String> websites) throws IOException {
        List<String> allTexts = fetchFromWebsites(websites);       // 抓取 h2/h3 元素
        List<String> allMovieNames = extractMovieNames(allTexts);  // 提取電影名稱

        Map<String, Integer> movieScores = extractAndScoreMovies(allMovieNames); // 計算分數
        List<String> uniqueMovies = removeDuplicates(allMovieNames);            // 去重後的電影名稱

        // 組合結果：返回分數和唯一電影名稱
        Map<String, Object> result = new HashMap<>();
        result.put("scores", movieScores);
        result.put("uniqueMovies", uniqueMovies);
        return result;
    }
    
    
}
