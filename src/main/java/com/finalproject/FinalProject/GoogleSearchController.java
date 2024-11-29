package com.finalproject.FinalProject;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GoogleSearchController {
	@Autowired
    private GoogleQueryService searchService;
    
    @GetMapping("/search")
    public ResponseEntity<Map<String, String>> search(@RequestParam String keyword) {
        try {
            Map<String, String> results = searchService.searchGoogle(keyword);
            return ResponseEntity.ok(results);
        } catch (IOException e) {
            // 可以根據需求返回適當的錯誤處理
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}