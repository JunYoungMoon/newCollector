package com.portfolio.newkeyword.service;

import com.portfolio.newkeyword.entity.NewsArticle;
import com.portfolio.newkeyword.entity.User;
import com.portfolio.newkeyword.entity.UserNewsSource;
import com.portfolio.newkeyword.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsArticleRepository newsArticleRepository;

    public Page<NewsArticle> getUserNews(User user, Pageable pageable) {
        return newsArticleRepository.findByUserOrderByCollectedAtDesc(user, pageable);
    }

    public Page<NewsArticle> getUserNewsByKeyword(User user, String keyword, Pageable pageable) {
        return newsArticleRepository.findByUserAndKeywordOrderByCollectedAtDesc(user, keyword, pageable);
    }

    public Page<NewsArticle> getUserNewsBySource(User user, UserNewsSource.NewsSourceType sourceType, Pageable pageable) {
        return newsArticleRepository.findByUserAndSourceTypeOrderByCollectedAtDesc(user, sourceType, pageable);
    }

    public Page<NewsArticle> getUserNewsWithFilters(User user, String keyword,
                                                    UserNewsSource.NewsSourceType sourceType,
                                                    LocalDateTime startDate, LocalDateTime endDate,
                                                    Pageable pageable) {
        return newsArticleRepository.findByUserWithFilters(user, keyword, sourceType, startDate, endDate, pageable);
    }

    public Map<String, Object> getUserNewsStatistics(User user) {
        Map<String, Object> stats = new HashMap<>();

        // 전체 뉴스 개수
        long totalCount = newsArticleRepository.countByUser(user);
        stats.put("totalCount", totalCount);

        // 키워드별 통계
        List<Object[]> keywordStats = newsArticleRepository.countByKeywordForUser(user);
        Map<String, Long> keywordCounts = new HashMap<>();
        for (Object[] stat : keywordStats) {
            keywordCounts.put((String) stat[0], (Long) stat[1]);
        }
        stats.put("keywordCounts", keywordCounts);

        // 뉴스 소스별 통계
        List<Object[]> sourceStats = newsArticleRepository.countBySourceTypeForUser(user);
        Map<String, Long> sourceCounts = new HashMap<>();
        for (Object[] stat : sourceStats) {
            UserNewsSource.NewsSourceType sourceType = (UserNewsSource.NewsSourceType) stat[0];
            sourceCounts.put(sourceType.getDisplayName(), (Long) stat[1]);
        }
        stats.put("sourceCounts", sourceCounts);

        return stats;
    }

    public NewsArticle getNewsArticle(Long articleId) {
        return newsArticleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("News article not found"));
    }
}
