package com.finalproject.FinalProject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class GoogleQueryService {

    // Google 搜尋
	public List<String> query(String keyword) throws IOException {
		//幫使用者輸入加上「電影」
		if (!keyword.endsWith("電影")) {
	        keyword += " 電影";
	    }
		
	    String searchUrl = "https://www.google.com/search?q=" + java.net.URLEncoder.encode(keyword, "utf-8") + "&num=15";
	    Document doc = Jsoup.connect(searchUrl).userAgent("Chrome/107.0.5304.107").get();
	    Elements links = doc.select("div.kCrYT a");

	    return links.stream()
	        .map(link -> link.attr("href").replace("/url?q=", "").split("&")[0])
	        .filter(url -> url.startsWith("http://") || url.startsWith("https://")) // 確保是完整 URL
	        .filter(url -> !url.contains("google.com")) // 過濾 Google 內部連結
	        .filter(this::isValidUrl) // 驗證 URL 是否有效
	        .limit(8) //抓前八個網頁結果
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
	
    // 驗證維基百科頁面是否有效
    private boolean isValidWikipediaPage(String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            return !doc.title().contains("標題無效") && !doc.title().contains("Wikipedia does not have an article");
        } catch (IOException e) {
            return false;
        }
    }

    // 爬取網站的h2 h3標籤內容
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

    // 抓《》中的中文字沒有的話就直接加入
	public List<String> extractMovieNames(List<String> rawMovieNames) {
	    List<String> movieNames = new ArrayList<>();
	    Pattern pattern = Pattern.compile("《([\u4e00-\u9fff]+)》"); //有書名號的截取格式
	    
	    for (String name : rawMovieNames) {
	        Matcher matcher = pattern.matcher(name);
	        
	        // 如果有書名號，取其中的字
	        if (matcher.find()) {
	            String movieName = matcher.group(1);
	            movieNames.add(movieName);
	        } else {
	            // 如果沒有書名號，檢查是否包含中文字
	            Pattern chinesePattern = Pattern.compile("[\u4e00-\u9fff]+");
	            Matcher chineseMatcher = chinesePattern.matcher(name);
	            
	            while (chineseMatcher.find()) {
	                String potentialMovieName = chineseMatcher.group();
	                // 過濾太短的字串
	                if (potentialMovieName.length() >= 2) {
	                    movieNames.add(potentialMovieName);
	                }
	            }
	        }
	    }
	    return movieNames;
	}
	
    // 從維基百科抓取第一段介紹
    public String fetchWikipediaSummary(String movieName) throws IOException {
        // 清理電影名稱並構建維基百科 URL
        String cleanedName = cleanMovieName(movieName);
        String wikiUrl = "https://zh.wikipedia.org/zh-tw/" + java.net.URLEncoder.encode(cleanedName, "UTF-8");

        if (!isValidWikipediaPage(wikiUrl)) { // 驗證網頁是否有效
            return "找不到維基百科頁面。";
        }

        // 爬取第一段內容
        return fetchFirstParagraph(wikiUrl);
    }

    // 去除電影名稱中的英文及空格，僅保留中文
    public static String cleanMovieName(String movieName) {
        return movieName.replaceAll("[^\\p{IsHan}]", "").trim();
    }
    
    // 用set去除重複電影
    public List<String> removeDuplicates(List<String> movieNames){
    	Set<String> unique = new LinkedHashSet<>(movieNames);
    	return new ArrayList<>(unique);
    }
    
    // 只爬取維基百科中第一段的文字內容 
    // 改成爬取第二段！！！！！！！！！！！！！！
    private String fetchFirstParagraph(String wikiUrl) throws IOException {
        Document wikiDoc = Jsoup.connect(wikiUrl).get();
        Element paragraph = wikiDoc.selectFirst("p"); 

        if (paragraph == null) {
            return "找不到簡介內容。";
        }
        String cleanContent = paragraph.text().replaceAll("\\[\\d+\\]", ""); // 去除維基百科中的註釋
        return cleanContent.trim();
    }
    
    
    //計算分數部分//
    
    // 計算電影出現次數
    public Map<String, Integer> calculateMovieScore(List<String> rawMovieNames) {
        // 在計算分數之前，不要去除重複項
        Map<String, Integer> movieScoreMap = new LinkedHashMap<>();
        
        for (String name : rawMovieNames) {
            // 只保留中文
            String cleanedName = cleanMovieName(name);
            
            // 確保電影名稱不為空且長度大於等於2
            if (!cleanedName.isEmpty() && cleanedName.length() >= 2) {
                // 如果電影名稱存在分數+1 否則初始化為1
                movieScoreMap.put(cleanedName, movieScoreMap.getOrDefault(cleanedName, 0) + 1);
            }
        }
        
        // 按分數降序排序
        return movieScoreMap.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .collect(Collectors.toMap(
                Map.Entry::getKey, 
                Map.Entry::getValue, 
                (e1, e2) -> e1, 
                LinkedHashMap::new
            ));
    }
    
    // 計算電影名稱出現次數
    public Map<String, Integer> extractAndScoreMovies(List<String> movieNames) {
        return movieNames.stream()
                .map(movie -> cleanMovieName(movie)) // 清洗名稱（保留中文）
                .collect(Collectors.toMap(
                        movieName -> movieName,   // 名稱作為鍵
                        movieName -> 1,           // 初始分數 1
                        Integer::sum,             // 分數累加
                        LinkedHashMap::new        // 保持順序
                ));
    }

    
    // 同時返回電影分數和去重複項目後的電影名稱
    public Map<String, Object> processMovies(List<String> websites) throws IOException {
        List<String> allTexts = fetchFromWebsites(websites);       // 抓取h2 h3元素
        List<String> allMovieNames = extractMovieNames(allTexts);  // 提取電影名稱

        // 用原始清單計算分數
        Map<String, Integer> movieScores = calculateMovieScore(allMovieNames);

        // 過濾無效的結果
        Map<String, Integer> filteredScores = filterResults(movieScores);

        List<String> uniqueMovies = new ArrayList<>(filteredScores.keySet()); // 要顯示在前端的電影名稱

        // 返回過濾後的分數和電影名稱
        Map<String, Object> result = new HashMap<>();
        result.put("scores", filteredScores);
        result.put("uniqueMovies", uniqueMovies);
        return result;
    }

    
    // 切換頁數
    public List<String> paginateResults(List<String> movieNames, int page, int pageSize) {
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, movieNames.size());
        if (start >= movieNames.size()) {
            return Collections.emptyList(); // 若超過範圍，回傳空清單
        }
        return movieNames.subList(start, end);
    }
    
    // 其他人也搜尋了
    public List<String> fetchRelatedSearches(String keyword) throws IOException {
        String searchUrl = "https://www.google.com/search?q=" + java.net.URLEncoder.encode(keyword, "utf-8");
        Document doc = Jsoup.connect(searchUrl).userAgent("Chrome/107.0.5304.107").get();

        // 選擇 <b> 標籤作為「其他人也搜尋了」的內容
        Elements relatedSearches = doc.select("b");
        
        // 獲取最多 8 個相關搜尋
        return relatedSearches.stream()
            .map(Element::text)
            .limit(8) // 限制為最多 8 筆
            .collect(Collectors.toList());
    }

    // 過濾搜尋結果
    // 定義要過濾的關鍵字
    private static final List<String> EXCLUDE_KEYWORDS = Arrays.asList("推薦", "排名", "片單", "整理", "分享");

    public static Map<String, Integer> filterResults(Map<String, Integer> movieScores) {
        return movieScores.entrySet().stream()
            .filter(entry -> isValidResult(entry.getKey(), entry.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    // 檢查是否為有效的電影結果
    private static boolean isValidResult(String movieName, int score) {
        // 1. 過濾分數過高的項目 (設定閾值，例如15分)
        if (score > 15) {
            return false;
        }
        // 2. 過濾包含關鍵字的項目
        for (String keyword : EXCLUDE_KEYWORDS) {
            if (movieName.contains(keyword)) {
                return false;
            }
        }
        return true;
    }
    
 
    
}
