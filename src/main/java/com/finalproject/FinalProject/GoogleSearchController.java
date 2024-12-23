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
    
    @GetMapping("/movies/search")
    public ResponseEntity<Map<String, Object>> searchMovies(@RequestParam String keyword) {
        try {
            // 1.Google搜尋，獲得前5個網站URL
            List<String> websites = crawlerService.query(keyword);

            // 2.爬取每個網站的h2和h3標籤內容作為電影名稱來源
            List<String> websiteTexts = crawlerService.fetchFromWebsites(websites);

            // 3.將提取到的電影名稱作處理
            List<String> movieNames = movieProcessingService.extractMovieNames(websiteTexts);

            // 4.計算電影名稱出現次數
            Map<String, Integer> movieScores = movieProcessingService.calculateMovieScore(movieNames);
            
            // 5.清理搜尋結果
            Map<String, Integer> filteredScores = movieProcessingService.filterResults(movieScores);
            
            // 6.其他人也搜尋了（抓不到）
            List<String> relatedSearches = crawlerService.fetchRelatedSearches(keyword);
            
            // 將結果Map回傳
            Map<String, Object> response = new HashMap<>();
            response.put("scores", filteredScores);
            response.put("relatedSearches", relatedSearches);
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyMap());
        }
    }
    
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

    
}
