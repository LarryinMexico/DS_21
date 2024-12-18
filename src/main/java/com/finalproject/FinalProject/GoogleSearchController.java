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
    private GoogleQueryService searchService;

    @GetMapping("/search")
    public ResponseEntity<List<String>> search(@RequestParam String keyword) {
        try {
            // 搜尋並返回電影名稱
            List<String> movieNames = searchService.extractMovieNames(
                searchService.fetchFromWebsites(searchService.query(keyword)) 
            );
            return ResponseEntity.ok(searchService.removeDuplicates(movieNames));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of("發生錯誤，請稍後再試"));
        }
    }

    @GetMapping("/movies/summary")
    public ResponseEntity<String> getMovieSummary(@RequestParam String movieName) {
        try {
            String summary = searchService.fetchWikipediaSummary(movieName);
            return ResponseEntity.ok(summary);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("發生錯誤，無法獲取簡介");
        }
    }
    
    @GetMapping("/movies/search")
    public ResponseEntity<Map<String, Object>> searchMovies(@RequestParam String keyword) {
        try {
            // 1. Google 搜尋，獲取前8個網站URL
            List<String> websites = searchService.query(keyword);

            // 2. 爬取每個網站的h2和h3標籤內容
            List<String> websiteTexts = searchService.fetchFromWebsites(websites);

            // 3. 從中提取電影名稱
            List<String> movieNames = searchService.extractMovieNames(websiteTexts);

            // 4. 計算電影名稱出現次數
            Map<String, Integer> movieScores = searchService.calculateMovieScore(movieNames);
            
            // 5. 其他人也搜尋了
            List<String> relatedSearches = searchService.fetchRelatedSearches(keyword);

            Map<String, Object> response = new HashMap<>();
            response.put("scores", movieScores);
            response.put("relatedSearches", relatedSearches);
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyMap());
        }
    }



    
}
