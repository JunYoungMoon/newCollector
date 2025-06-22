package com.portfolio.newkeyword.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "news_articles",
        indexes = {
                @Index(name = "idx_user_keyword", columnList = "user_id, keyword"),
                @Index(name = "idx_collected_at", columnList = "collected_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String keyword;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "news_url", nullable = false, unique = true)
    private String newsUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type")
    private UserNewsSource.NewsSourceType sourceType;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @Column(name = "image_url")
    private String imageUrl;

    @PrePersist
    protected void onCreate() {
        this.collectedAt = LocalDateTime.now();
    }

    public NewsArticle(User user, String keyword, String title, String content,
                       String newsUrl, UserNewsSource.NewsSourceType sourceType) {
        this.user = user;
        this.keyword = keyword;
        this.title = title;
        this.content = content;
        this.newsUrl = newsUrl;
        this.sourceType = sourceType;
    }
}