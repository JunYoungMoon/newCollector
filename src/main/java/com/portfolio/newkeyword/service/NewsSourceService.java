package com.portfolio.newkeyword.service;

import com.portfolio.newkeyword.entity.User;
import com.portfolio.newkeyword.entity.UserNewsSource;
import com.portfolio.newkeyword.repository.UserNewsSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsSourceService {

    private final UserNewsSourceRepository userNewsSourceRepository;

    @Transactional
    public UserNewsSource addNewsSource(User user, UserNewsSource.NewsSourceType sourceType) {
        if (userNewsSourceRepository.existsByUserAndSourceType(user, sourceType)) {
            throw new RuntimeException("News source already exists for this user");
        }

        UserNewsSource userNewsSource = new UserNewsSource(user, sourceType);
        return userNewsSourceRepository.save(userNewsSource);
    }

    @Transactional
    public void removeNewsSource(User user, Long sourceId) {
        UserNewsSource newsSource = userNewsSourceRepository.findById(sourceId)
                .orElseThrow(() -> new RuntimeException("News source not found"));

        if (!newsSource.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to news source");
        }

        userNewsSourceRepository.delete(newsSource);
    }

    @Transactional
    public void toggleNewsSourceStatus(User user, Long sourceId) {
        UserNewsSource newsSource = userNewsSourceRepository.findById(sourceId)
                .orElseThrow(() -> new RuntimeException("News source not found"));

        if (!newsSource.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to news source");
        }

        newsSource.setIsActive(!newsSource.getIsActive());
        userNewsSourceRepository.save(newsSource);
    }

    public List<UserNewsSource> getUserNewsSources(User user) {
        return userNewsSourceRepository.findByUser(user);
    }

    public List<UserNewsSource> getActiveUserNewsSources(User user) {
        return userNewsSourceRepository.findByUserAndIsActiveTrue(user);
    }

    public List<UserNewsSource.NewsSourceType> getAllAvailableSourceTypes() {
        return Arrays.asList(UserNewsSource.NewsSourceType.values());
    }
}