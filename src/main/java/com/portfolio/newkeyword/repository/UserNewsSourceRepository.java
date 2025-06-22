package com.portfolio.newkeyword.repository;

import com.portfolio.newkeyword.entity.User;
import com.portfolio.newkeyword.entity.UserNewsSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserNewsSourceRepository extends JpaRepository<UserNewsSource, Long> {

    List<UserNewsSource> findByUserAndIsActiveTrue(User user);

    List<UserNewsSource> findByUser(User user);

    Optional<UserNewsSource> findByUserAndSourceType(User user, UserNewsSource.NewsSourceType sourceType);

    boolean existsByUserAndSourceType(User user, UserNewsSource.NewsSourceType sourceType);
}