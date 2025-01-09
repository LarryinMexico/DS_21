package com.finalproject.FinalProject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api")
public class GoogleSearchController {
    @Autowired
    private CrawlerService crawlerService;
    
    @Autowired
    private MovieProcessingService movieProcessingService;
    
    @Autowired
    private WikiService wikipediaService;
    
    // 進行電影搜索或一般搜索
    @GetMapping("/movies/search")
    public ResponseEntity<Map<String, Object>> searchMovies(@RequestParam String keyword) {
        try {
        	Map<String, Object> response = new HashMap<>();
            boolean isMovieCategory = crawlerService.isMovieCategory(keyword);
            List<String> relatedSearches = crawlerService.fetchRelatedSearches(keyword);
            
            if (isMovieCategory) { // 電影搜索
            	
                // 1.Google搜尋，獲得前5個網站URL
                List<String> websites = crawlerService.Moviequery(keyword);
                
                // 插入：爬取子網頁
                System.out.println("\n===== 開始爬取網頁及其子網頁 =====");
                crawlerService.crawlSubpages(keyword);
                System.out.println("===== 子網頁爬取完成 =====\n");
                
                // 2.爬取每個網站的h2和h3標籤內容作為電影名稱來源
                List<String> websiteTexts = crawlerService.fetchFromWebsites(websites);
                // 3.將提取到的電影名稱作處理
                List<String> movieNames = movieProcessingService.extractMovieNames(websiteTexts);
                // 4.計算電影名稱出現次數
                Map<String, Integer> movieScores = movieProcessingService.calculateMovieScore(movieNames);
                // 5.清理搜尋結果
                Map<String, Integer> filteredScores = movieProcessingService.filterResults(movieScores);
                
                response.put("scores", filteredScores);
                response.put("isMovieCategory", true);
            } else { // 一般搜索
            	
                List<Map<String, String>> searchResults = crawlerService.generalSearch(keyword);
                response.put("generalResults", searchResults);
                response.put("isMovieCategory", false);
            }
            
            // 將結果Map回傳
            response.put("relatedSearches", relatedSearches);
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyMap());
        }
    }

    // 抓取電影簡介
    @GetMapping("/movies/summary")
    public ResponseEntity<String> getMovieSummary(@RequestParam String movieName) {
        try {
            String summary = wikipediaService.fetchWikipediaSummary(movieName);
            return ResponseEntity.ok(summary);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("發生錯誤，無法獲取簡介");
        }
    }
    
    // 印出子網頁
    @GetMapping("/movies/subpages")
    public ResponseEntity<String> crawlSubpages(@RequestParam String keyword) {
        try {
            crawlerService.crawlSubpages(keyword);
            return ResponseEntity.ok("子網頁爬取完成");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("爬取中發生錯誤: " + e.getMessage());
        }
    }

    
}
