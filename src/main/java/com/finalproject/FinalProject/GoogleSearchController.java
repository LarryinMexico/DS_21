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
                searchService.fetchFromWebsites(searchService.query(keyword)) // 加上"電影"
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
    public ResponseEntity<Map<String, Integer>> searchMovies(@RequestParam String keyword) {
        try {
            // 1. 執行 Google 搜尋，獲取前 8 個網站的 URL
            List<String> websites = searchService.query(keyword);

            // 2. 爬取每個網站的 h2 和 h3 標籤內容
            List<String> websiteTexts = searchService.fetchFromWebsites(websites);

            // 3. 從內容中提取電影名稱
            List<String> movieNames = searchService.extractMovieNames(websiteTexts);

            // 4. 計算電影名稱出現次數並排序
            Map<String, Integer> movieScores = searchService.extractAndScoreMovies(movieNames);

            return ResponseEntity.ok(movieScores);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyMap());
        }
    }



    
}
