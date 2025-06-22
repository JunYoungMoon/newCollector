package com.portfolio.newkeyword.repository;

import com.portfolio.newkeyword.entity.User;
import com.portfolio.newkeyword.entity.UserKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserKeywordRepository extends JpaRepository<UserKeyword, Long> {

    List<UserKeyword> findByUserAndIsActiveTrue(User user);

    List<UserKeyword> findByUser(User user);

    Optional<UserKeyword> findByUserAndKeyword(User user, String keyword);

    @Query("SELECT uk FROM UserKeyword uk WHERE uk.isActive = true")
    List<UserKeyword> findAllActiveKeywords();

    boolean existsByUserAndKeyword(User user, String keyword);
}