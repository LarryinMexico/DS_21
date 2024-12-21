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
	// private static final String TRUEMOVIE_SEARCH_URL = "https://cse.google.com/cse";
    // private static final String TRUEMOVIE_PARAMS = "cx=partner-pub-0456971433098520:q4tljkv0qgf";
    
    // Google 搜尋
	public List<String> query(String keyword) throws IOException {
		//幫使用者輸入加上「電影」
		if (!keyword.endsWith("電影")) {
	        keyword += " 電影";
	    }
		
	    String searchUrl = "https://www.google.com/search?q=" + java.net.URLEncoder.encode(keyword, "utf-8") + "&num=10";
	    Document doc = Jsoup.connect(searchUrl).userAgent("Chrome/107.0.5304.107").get();
	    Elements links = doc.select("div.kCrYT a");

	    return links.stream()
	        .map(link -> link.attr("href").replace("/url?q=", "").split("&")[0])
	        .filter(url -> url.startsWith("http://") || url.startsWith("https://")) // 確保是完整 URL
	        .filter(url -> !url.contains("google.com")) // 過濾 Google 內部連結
	        .filter(this::isValidUrl) // 驗證 URL 是否有效
	        .limit(5) //抓前5個網頁結果
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
	
    // 從維基百科抓取
	public String fetchWikipediaSummary(String movieName) throws IOException {
	    // 清理電影名稱並構建維基百科 URL
	    String cleanedName = cleanMovieName(movieName);
	    String wikiUrl = "https://zh.wikipedia.org/zh-tw/" + java.net.URLEncoder.encode(cleanedName, "UTF-8");

	    if (!isValidWikipediaPage(wikiUrl)) {
	        // 如果維基百科搜尋失敗，改用 Google 搜尋
	        return searchMovieIntroFromGoogle(cleanedName);
	        // return "那我也沒辦法了";
	    }
	    // 爬取第一段內容
	    return fetchFirstParagraph(wikiUrl);
	}
	// 新增的 Google 搜尋電影簡介方法
	private String searchMovieIntroFromGoogle(String movieName) throws IOException {
	    String searchQuery = movieName + " 電影簡介";
	    String searchUrl = "https://www.google.com/search?q=" + java.net.URLEncoder.encode(searchQuery, "utf-8");
	    
	    Document doc = Jsoup.connect(searchUrl)
	        .userAgent("Chrome/107.0.5304.107")
	        .get();
	    
	    // 嘗試獲取搜尋結果的第一個鏈接
	    Elements searchResults = doc.select("div.kCrYT a");
	    if (!searchResults.isEmpty()) {
	        String firstResultUrl = searchResults.first()
	            .attr("href")
	            .replace("/url?q=", "")
	            .split("&")[0];
	            
	        // 確保URL有效
	        if (firstResultUrl.startsWith("http")) {
	            try {
	                Document resultDoc = Jsoup.connect(firstResultUrl).get();
	                
	                // 先嘗試獲取第一個 p 元素
	                Element firstP = resultDoc.select("p").stream()
	                    .filter(p -> !p.text().trim().isEmpty())
	                    .findFirst()
	                    .orElse(null);
	                    
	                if (firstP != null) {
	                    return firstP.text();
	                }
	                
	                // 如果沒有找到合適的 p 元素，嘗試獲取 br 元素後的文字
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

    // 去除電影名稱中的英文及空格，僅保留中文
    public static String cleanMovieName(String movieName) {
        return movieName.replaceAll("[^\\p{IsHan}]", "").trim();
    }
    
    // 用set去除重複電影
    public List<String> removeDuplicates(List<String> movieNames){
    	Set<String> unique = new LinkedHashSet<>(movieNames);
    	return new ArrayList<>(unique);
    }
    
    // 爬取維基百科中第二段的文字內容 
    private String fetchFirstParagraph(String wikiUrl) throws IOException {
        Document wikiDoc = Jsoup.connect(wikiUrl).get();
        Elements paragraphs = wikiDoc.select("p");  // 選取所有段落
        // 找到第一個非空的段落後的下一個段落
        Element second = null;
        int validParagraphCount = 0;
        
        for (Element p : paragraphs) {
            if (!p.text().trim().isEmpty()) {
                validParagraphCount++;
                if (validParagraphCount == 2) {
                    second = p;
                    break;
                }
            }
        }

        if (second == null) {
            return "找不到簡介內容。";
        }
        String cleanContent = second.text().replaceAll("\\[\\d+\\]", ""); // 去除註釋
        return cleanContent.trim();
    }
    
    //計算分數部分
    public Map<String, Integer> calculateMovieScore(List<String> rawMovieNames) {
        // 使用 LinkedHashMap 來保持插入順序
        Map<String, Integer> movieScoreMap = new LinkedHashMap<>();
        
        for (String name : rawMovieNames) {
            String cleanedName = cleanMovieName(name);
            if (!cleanedName.isEmpty() && cleanedName.length() >= 2) {
                movieScoreMap.put(cleanedName, movieScoreMap.getOrDefault(cleanedName, 0) + 1);
            }
        }
        
        return movieScoreMap;
    }

    
    // 同時返回電影分數和去重複項目後的電影名稱
    // 修改 processMovies 方法包含排序功能
    public Map<String, Object> processMovies(List<String> websites) throws IOException {
        List<String> allTexts = fetchFromWebsites(websites);
        List<String> allMovieNames = extractMovieNames(allTexts);
        // 用原始清單計算分數
        Map<String, Integer> movieScores = calculateMovieScore(allMovieNames);
        // 過濾無效的結果
        Map<String, Integer> filteredScores = filterResults(movieScores);

        // 使用stream進行排序，確保分數由高到低
        LinkedHashMap<String, Integer> sortedScores = filteredScores.entrySet()
            .stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));

        Map<String, Object> result = new HashMap<>();
        result.put("scores", sortedScores);
        result.put("uniqueMovies", new ArrayList<>(sortedScores.keySet()));
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
        Document doc = Jsoup.connect(searchUrl)
            .userAgent("Chrome/107.0.5304.107")
            .get();

        // 尋找「意見回饋」後的相關搜尋
        Elements relatedSearches = doc.select("span:contains(意見回饋) ~ div b");
        
        // 如果找不到，嘗試其他可能的選擇器
        if (relatedSearches.isEmpty()) {
            relatedSearches = doc.select("div.card-section b");
        }
        
        return relatedSearches.stream()
            .map(Element::text)
            .filter(text -> !text.isEmpty())
            .limit(5)  // 限制顯示數量
            .collect(Collectors.toList());
    }
    


    // 過濾搜尋結果//
    private static final List<String> EXCLUDE_KEYWORDS = Arrays.asList(
    		"推薦", "排名", "片單", "整理", "分享", "必看", "電影", "鐵證", "線上看", "可能", "好評", "上映", "完結篇", "即使", "更多", "禮物", "也在看");

    public Map<String, Integer> filterResults(Map<String, Integer> movieScores) {
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
