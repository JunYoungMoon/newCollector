package com.portfolio.newkeyword.controller;

import com.portfolio.newkeyword.config.CurrentUser;
import com.portfolio.newkeyword.entity.NewsArticle;
import com.portfolio.newkeyword.entity.User;
import com.portfolio.newkeyword.entity.UserNewsSource;
import com.portfolio.newkeyword.service.NewsService;
import com.portfolio.newkeyword.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequestMapping("/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    public String newsView(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserNewsSource.NewsSourceType sourceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Model model,
            @CurrentUser User user) {

        Pageable pageable = PageRequest.of(page, size);

        Page<NewsArticle> newsPage;
        if (keyword != null || sourceType != null || startDate != null || endDate != null) {
            newsPage = newsService.getUserNewsWithFilters(user, keyword, sourceType, startDate, endDate, pageable);
        } else {
            newsPage = newsService.getUserNews(user, pageable);
        }

        // 통계 정보 추가
        Map<String, Object> statistics = newsService.getUserNewsStatistics(user);

        model.addAttribute("newsPage", newsPage);
        model.addAttribute("statistics", statistics);
        model.addAttribute("currentKeyword", keyword);
        model.addAttribute("currentSourceType", sourceType);
        model.addAttribute("currentStartDate", startDate);
        model.addAttribute("currentEndDate", endDate);
        model.addAttribute("sourceTypes", UserNewsSource.NewsSourceType.values());

        return "news/list";
    }

    @GetMapping("/detail")
    public String newsDetail(@CurrentUser User user, @RequestParam Long id, Model model) {
        NewsArticle article = newsService.getNewsArticle(id);

        // 권한 확인 - 해당 사용자의 뉴스인지 체크
        if (!article.getUser().getId().equals(user.getId())) {
            return "redirect:/news?error=unauthorized";
        }

        model.addAttribute("article", article);
        return "news/detail";
    }

    @GetMapping("/statistics")
    public String newsStatistics(@CurrentUser User user, Model model) {
        Map<String, Object> statistics = newsService.getUserNewsStatistics(user);

        model.addAttribute("statistics", statistics);
        return "news/statistics";
    }
}