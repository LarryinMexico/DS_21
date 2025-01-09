package com.finalproject.FinalProject;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// 清理電影名稱以及評分排名相關的處理
@Service
public class MovieProcessingService {
	public CrawlerService craw;
	
    // 針對書名號的處理《》中的中文字，沒有的話就直接加入
	public List<String> extractMovieNames(List<String> rawMovieNames) {
	    List<String> movieNames = new ArrayList<>();
	    Pattern pattern = Pattern.compile("《([\u4e00-\u9fff]+)》"); //有書名號的截取格式
	    
	    for (String name : rawMovieNames) {
	        Matcher matcher = pattern.matcher(name);
	        
	        // 如果有書名號取其中的字
	        if (matcher.find()) {
	            String movieName = matcher.group(1);
	            movieNames.add(movieName);
	        } else {
	            // 如果沒書名號，檢查是否包含中文字
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
	
    // 去除電影名稱中的英文空格，只保留中文
    public static String cleanMovieName(String movieName) {
        return movieName.replaceAll("[^\\p{IsHan}]", "").trim();
    }
    
    // 用沒有去重的列表中清理過的電影名稱計算分數
    public Map<String, Integer> calculateMovieScore(List<String> rawMovieNames) {
        // 使用LinkedHashMap保持插入順序
        Map<String, Integer> movieScoreMap = new LinkedHashMap<>();
        
        for (String name : rawMovieNames) {
            String cleanedName = cleanMovieName(name);
            if (!cleanedName.isEmpty() && cleanedName.length() >= 2) {
                movieScoreMap.put(cleanedName, movieScoreMap.getOrDefault(cleanedName, 0) + 1);
            }
        }
        return movieScoreMap;
    }
    
    // 返回電影分數和去重項目後的電影名稱，並且排序。此返回結果會傳p到前端
    public Map<String, Object> processMovies(List<String> websites) throws IOException {
        List<String> allTexts = craw.fetchFromWebsites(websites);
        List<String> allMovieNames = extractMovieNames(allTexts);
        // 用原始清單計算分數
        Map<String, Integer> movieScores = calculateMovieScore(allMovieNames);
        // 過濾不要的結果
        Map<String, Integer> filteredScores = filterResults(movieScores);

        // 將Map轉換為List
        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(filteredScores.entrySet());
        quickSort(entryList, 0, entryList.size() - 1);
        
        // 創建排序後的LinkedHashMap
        LinkedHashMap<String, Integer> sortedScores = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : entryList) {
            sortedScores.put(entry.getKey(), entry.getValue());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("scores", sortedScores);
        result.put("uniqueMovies", new ArrayList<>(sortedScores.keySet()));
        return result;
    }
    
    // quick sort方法：用來排序電影分數
    private void quickSort(List<Map.Entry<String, Integer>> list, int low, int high) {
        if (low < high) {
            int pi = partition(list, low, high);
            quickSort(list, low, pi - 1);
            quickSort(list, pi + 1, high);
        }
    }
    private int partition(List<Map.Entry<String, Integer>> list, int low, int high) {
        int pivot = list.get(high).getValue();
        int i = (low - 1);
        
        for (int j = low; j < high; j++) {
            if (list.get(j).getValue() > pivot) {
                i++;
                Collections.swap(list, i, j); // 直接使用swap
            }
        }
        Collections.swap(list, i + 1, high);
        return i + 1;
    }
    
    
    // 過濾方式：包含要去除的字或是分數過高（暫定10)
    
    // 定義不要包含的字
    private static final List<String> KEYWORDS = Arrays.asList(
    		"推薦", "排名", "獎", "整理", "分享", "必看", "鐵證", "絕對",
    		"看", "可能", "好評", "上映", "完結篇", "即使", "更多", "禮物",
    		"租", "不錯過", "獨家", "片", "榜", "登入", "吧", "劇", "輸入", 
    		"大賽", "頻", "首頁", "列表", "下載", "特輯", "發送", "搜尋", "院",
    		"全部", "透過", "熱門", "免費", "兒童", "怎麼說", "提醒", "回到",
    		"感動", "其他", "正式", "導演", "最新消息", "留言", "影迷", "延伸閱讀",
    		"為人民服務", "爽度破表", "文化", "汽車", "分類", "但卻怕", "結語",
    		"網友激推", "教你", "懶人包", "盤點", "影音平台");
    
    // 然後過濾
    public Map<String, Integer> filterResults(Map<String, Integer> movieScores) {
        return movieScores.entrySet().stream()
            .filter(entry -> isValidResult(entry.getKey(), entry.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    // 檢查是否為有效的電影：分數過高或是包含要去除的字
    private static boolean isValidResult(String movieName, int score) {
        // 分數過高
        if (score > 10) {
            return false;
        }
        // 包含關鍵字
        for (String keyword : KEYWORDS) {
            if (movieName.contains(keyword)) {
                return false;
            }
        }
        return true;
    }
    
    // 切換頁數
    public List<String> paginateResults(List<String> movieNames, int page, int pageSize) {
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, movieNames.size());
        if (start >= movieNames.size()) {
            return Collections.emptyList(); //若超過範圍回傳空清單
        }
        return movieNames.subList(start, end);
    }
    
	
}
