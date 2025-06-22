package com.portfolio.newkeyword.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_news_sources")
@Getter
@Setter
@NoArgsConstructor
public class UserNewsSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NewsSourceType sourceType;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public UserNewsSource(User user, NewsSourceType sourceType) {
        this.user = user;
        this.sourceType = sourceType;
        this.isActive = true;
    }

    @Getter
    public enum NewsSourceType {
        NAVER("네이버", "https://search.naver.com/search.naver?where=news&query="),
        DAUM("다음", "https://search.daum.net/search?w=news&q="),
        GOOGLE("구글", "https://news.google.com/search?q="),
        CHOSUN("조선일보", "https://www.chosun.com/search/?query="),
        JOONGANG("중앙일보", "https://www.joongang.co.kr/search?keyword=");

        private final String displayName;
        private final String searchUrl;

        NewsSourceType(String displayName, String searchUrl) {
            this.displayName = displayName;
            this.searchUrl = searchUrl;
        }

    }
}