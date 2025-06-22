package com.portfolio.newkeyword.service;

import com.portfolio.newkeyword.entity.NewsArticle;
import com.portfolio.newkeyword.entity.User;
import com.portfolio.newkeyword.entity.UserKeyword;
import com.portfolio.newkeyword.entity.UserNewsSource;
import com.portfolio.newkeyword.repository.NewsArticleRepository;
import com.portfolio.newkeyword.repository.UserKeywordRepository;
import com.portfolio.newkeyword.repository.UserNewsSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsCollectorService {

    private final UserKeywordRepository userKeywordRepository;
    private final UserNewsSourceRepository userNewsSourceRepository;
    private final NewsArticleRepository newsArticleRepository;

    @Scheduled(cron = "0 0 * * * *") // 매시간 정각 실행
    public void collectNewsScheduled() {
        log.info("Starting scheduled news collection at {}", LocalDateTime.now());

        List<UserKeyword> activeKeywords = userKeywordRepository.findAllActiveKeywords();

        for (UserKeyword userKeyword : activeKeywords) {
            User user = userKeyword.getUser();
            List<UserNewsSource> activeSources = userNewsSourceRepository.findByUserAndIsActiveTrue(user);

            for (UserNewsSource source : activeSources) {
                collectNewsAsync(user, userKeyword.getKeyword(), source.getSourceType());
            }

            // 마지막 수집 시간 업데이트
            userKeyword.setLastCollectedAt(LocalDateTime.now());
            userKeywordRepository.save(userKeyword);
        }

        log.info("Completed scheduled news collection at {}", LocalDateTime.now());
    }

    @Async
    public CompletableFuture<Void> collectNewsAsync(User user, String keyword, UserNewsSource.NewsSourceType sourceType) {
        try {
            collectNews(user, keyword, sourceType);
        } catch (Exception e) {
            log.error("Error collecting news for user: {}, keyword: {}, source: {}",
                    user.getUsername(), keyword, sourceType, e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Transactional
    public void collectNews(User user, String keyword, UserNewsSource.NewsSourceType sourceType) {
        try {
            String searchUrl = buildSearchUrl(sourceType, keyword);
            Document doc = Jsoup.connect(searchUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();

            List<NewsArticle> articles = parseArticles(doc, user, keyword, sourceType);

            for (NewsArticle article : articles) {
                // 중복 체크
                if (!newsArticleRepository.findByNewsUrl(article.getNewsUrl()).isPresent()) {
                    newsArticleRepository.save(article);
                    log.debug("Saved new article: {}", article.getTitle());
                }
            }

        } catch (IOException e) {
            log.error("Failed to collect news for keyword: {} from source: {}", keyword, sourceType, e);
        }
    }

    private String buildSearchUrl(UserNewsSource.NewsSourceType sourceType, String keyword) {
        return sourceType.getSearchUrl() + keyword;
    }

    private List<NewsArticle> parseArticles(Document doc, User user, String keyword, UserNewsSource.NewsSourceType sourceType) {
        Elements articleElements;

        switch (sourceType) {
            case NAVER:
                articleElements = doc.select("div.news_wrap");
                return parseNaverArticles(articleElements, user, keyword, sourceType);
            case DAUM:
                articleElements = doc.select("div.item-box");
                return parseDaumArticles(articleElements, user, keyword, sourceType);
            case GOOGLE:
                articleElements = doc.select("article");
                return parseGoogleArticles(articleElements, user, keyword, sourceType);
            default:
                articleElements = doc.select("article, div.news-item, div.article");
                return parseGenericArticles(articleElements, user, keyword, sourceType);
        }
    }

    private List<NewsArticle> parseNaverArticles(Elements elements, User user, String keyword, UserNewsSource.NewsSourceType sourceType) {
        return elements.stream()
                .limit(10) // 최대 10개 기사
                .map(element -> {
                    try {
                        Element titleElement = element.selectFirst("a.news_tit");
                        Element contentElement = element.selectFirst("div.news_dsc");

                        if (titleElement != null) {
                            String title = titleElement.text();
                            String url = titleElement.attr("href");
                            String content = contentElement != null ? contentElement.text() : "";

                            return new NewsArticle(user, keyword, title, content, url, sourceType);
                        }
                        return null;
                    } catch (Exception e) {
                        log.warn("Error parsing Naver article", e);
                        return null;
                    }
                })
                .filter(article -> article != null)
                .toList();
    }

    private List<NewsArticle> parseDaumArticles(Elements elements, User user, String keyword, UserNewsSource.NewsSourceType sourceType) {
        return elements.stream()
                .limit(10)
                .map(element -> {
                    try {
                        Element titleElement = element.selectFirst("a.tit-wrap");
                        Element contentElement = element.selectFirst("p.desc");

                        if (titleElement != null) {
                            String title = titleElement.text();
                            String url = titleElement.attr("href");
                            String content = contentElement != null ? contentElement.text() : "";

                            return new NewsArticle(user, keyword, title, content, url, sourceType);
                        }
                        return null;
                    } catch (Exception e) {
                        log.warn("Error parsing Daum article", e);
                        return null;
                    }
                })
                .filter(article -> article != null)
                .toList();
    }

    private List<NewsArticle> parseGoogleArticles(Elements elements, User user, String keyword, UserNewsSource.NewsSourceType sourceType) {
        return elements.stream()
                .limit(10)
                .map(element -> {
                    try {
                        Element titleElement = element.selectFirst("h3 a, h4 a");

                        if (titleElement != null) {
                            String title = titleElement.text();
                            String url = titleElement.attr("href");
                            String content = "";

                            Element summaryElement = element.selectFirst("p");
                            if (summaryElement != null) {
                                content = summaryElement.text();
                            }

                            return new NewsArticle(user, keyword, title, content, url, sourceType);
                        }
                        return null;
                    } catch (Exception e) {
                        log.warn("Error parsing Google article", e);
                        return null;
                    }
                })
                .filter(article -> article != null)
                .toList();
    }

    private List<NewsArticle> parseGenericArticles(Elements elements, User user, String keyword, UserNewsSource.NewsSourceType sourceType) {
        return elements.stream()
                .limit(10)
                .map(element -> {
                    try {
                        Element titleElement = element.selectFirst("h1, h2, h3, h4, a");

                        if (titleElement != null) {
                            String title = titleElement.text();
                            String url = titleElement.attr("href");
                            String content = element.text();

                            if (url.isEmpty() && titleElement.tagName().equals("a")) {
                                url = titleElement.attr("href");
                            }

                            return new NewsArticle(user, keyword, title, content, url, sourceType);
                        }
                        return null;
                    } catch (Exception e) {
                        log.warn("Error parsing generic article", e);
                        return null;
                    }
                })
                .filter(article -> article != null)
                .toList();
    }
}