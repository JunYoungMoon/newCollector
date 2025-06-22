package com.portfolio.newkeyword.service;

import com.portfolio.newkeyword.entity.User;
import com.portfolio.newkeyword.entity.UserKeyword;
import com.portfolio.newkeyword.entity.UserNewsSource;
import com.portfolio.newkeyword.repository.UserKeywordRepository;
import com.portfolio.newkeyword.repository.UserNewsSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KeywordService {

    private final UserKeywordRepository userKeywordRepository;
    private final UserNewsSourceRepository userNewsSourceRepository;
    private final NewsCollectorService newsCollectorService;

    @Transactional
    public UserKeyword addKeyword(User user, String keyword) {
        if (userKeywordRepository.existsByUserAndKeyword(user, keyword)) {
            throw new RuntimeException("Keyword already exists for this user");
        }

        UserKeyword userKeyword = new UserKeyword(user, keyword);
        UserKeyword savedKeyword = userKeywordRepository.save(userKeyword);

        // 키워드 등록 즉시 뉴스 수집 실행
        List<UserNewsSource> activeSources = userNewsSourceRepository.findByUserAndIsActiveTrue(user);
        for (UserNewsSource source : activeSources) {
            newsCollectorService.collectNewsAsync(user, keyword, source.getSourceType());
        }

        return savedKeyword;
    }

    @Transactional
    public void removeKeyword(User user, Long keywordId) {
        UserKeyword keyword = userKeywordRepository.findById(keywordId)
                .orElseThrow(() -> new RuntimeException("Keyword not found"));

        if (!keyword.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to keyword");
        }

        userKeywordRepository.delete(keyword);
    }

    @Transactional
    public void toggleKeywordStatus(User user, Long keywordId) {
        UserKeyword keyword = userKeywordRepository.findById(keywordId)
                .orElseThrow(() -> new RuntimeException("Keyword not found"));

        if (!keyword.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to keyword");
        }

        keyword.setIsActive(!keyword.getIsActive());
        userKeywordRepository.save(keyword);
    }

    public List<UserKeyword> getUserKeywords(User user) {
        return userKeywordRepository.findByUser(user);
    }

    public List<UserKeyword> getActiveUserKeywords(User user) {
        return userKeywordRepository.findByUserAndIsActiveTrue(user);
    }
}