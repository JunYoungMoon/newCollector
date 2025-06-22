package com.portfolio.newkeyword.repository;

import com.portfolio.newkeyword.entity.NewsArticle;
import com.portfolio.newkeyword.entity.User;
import com.portfolio.newkeyword.entity.UserNewsSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    Page<NewsArticle> findByUserOrderByCollectedAtDesc(User user, Pageable pageable);

    Page<NewsArticle> findByUserAndKeywordOrderByCollectedAtDesc(User user, String keyword, Pageable pageable);

    Page<NewsArticle> findByUserAndSourceTypeOrderByCollectedAtDesc(User user, UserNewsSource.NewsSourceType sourceType, Pageable pageable);

    @Query("SELECT na FROM NewsArticle na WHERE na.user = :user " +
            "AND (:keyword IS NULL OR na.keyword = :keyword) " +
            "AND (:sourceType IS NULL OR na.sourceType = :sourceType) " +
            "AND (:startDate IS NULL OR na.collectedAt >= :startDate) " +
            "AND (:endDate IS NULL OR na.collectedAt <= :endDate) " +
            "ORDER BY na.collectedAt DESC")
    Page<NewsArticle> findByUserWithFilters(
            @Param("user") User user,
            @Param("keyword") String keyword,
            @Param("sourceType") UserNewsSource.NewsSourceType sourceType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    Optional<NewsArticle> findByNewsUrl(String newsUrl);

    @Query("SELECT COUNT(na) FROM NewsArticle na WHERE na.user = :user")
    long countByUser(@Param("user") User user);

    @Query("SELECT na.keyword, COUNT(na) FROM NewsArticle na WHERE na.user = :user GROUP BY na.keyword")
    List<Object[]> countByKeywordForUser(@Param("user") User user);

    @Query("SELECT na.sourceType, COUNT(na) FROM NewsArticle na WHERE na.user = :user GROUP BY na.sourceType")
    List<Object[]> countBySourceTypeForUser(@Param("user") User user);
}