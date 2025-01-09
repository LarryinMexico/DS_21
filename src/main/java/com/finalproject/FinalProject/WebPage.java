package com.finalproject.FinalProject;

import java.util.ArrayList;
import java.util.List;

public class WebPage {
    private String url;
    private List<WebPage> subPages;
    
    public WebPage(String url) {
        this.url = url;
        this.subPages = new ArrayList<>();
    }
    
    public String getUrl() {
        return url;
    }
    
    public List<WebPage> getSubPages() {
        return subPages;
    }
    
    public void addSubPage(WebPage subPage) {
        if (subPages.size() < 10) { // 子網頁太多了抓十個就好
            subPages.add(subPage);
        }
    }
    
    @Override
    public String toString() {
        return generateTreeString(0);
    }
    
    private String generateTreeString(int depth) {
        StringBuilder sb = new StringBuilder();
        String indent = "  ".repeat(depth);
        
        sb.append(indent).append("└─ ").append(url).append("\n");
        for (WebPage subPage : subPages) {
            sb.append(subPage.generateTreeString(depth + 1));
        }
        return sb.toString();
    }
}