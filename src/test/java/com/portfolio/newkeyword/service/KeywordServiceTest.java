package com.portfolio.newkeyword.service;

import com.portfolio.newkeyword.entity.User;
import com.portfolio.newkeyword.entity.UserKeyword;
import com.portfolio.newkeyword.entity.UserNewsSource;
import com.portfolio.newkeyword.repository.UserKeywordRepository;
import com.portfolio.newkeyword.repository.UserNewsSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KeywordService 테스트")
class KeywordServiceTest {

    @Mock
    private UserKeywordRepository userKeywordRepository;

    @Mock
    private UserNewsSourceRepository userNewsSourceRepository;

    @Mock
    private NewsCollectorService newsCollectorService;

    @InjectMocks
    private KeywordService keywordService;

    private User testUser;
    private UserKeyword testKeyword;
    private UserNewsSource testNewsSource;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testKeyword = new UserKeyword();
        testKeyword.setId(1L);
        testKeyword.setUser(testUser);
        testKeyword.setKeyword("테스트키워드");
        testKeyword.setIsActive(true);

        testNewsSource = new UserNewsSource();
        testNewsSource.setId(1L);
        testNewsSource.setUser(testUser);
        testNewsSource.setSourceType(UserNewsSource.NewsSourceType.NAVER);
        testNewsSource.setIsActive(true);
    }

    @Test
    @DisplayName("키워드 추가 성공")
    void addKeyword_Success() {
        // Given
        String keyword = "새키워드";
        List<UserNewsSource> activeSources = Arrays.asList(testNewsSource);

        when(userKeywordRepository.existsByUserAndKeyword(testUser, keyword)).thenReturn(false);
        when(userKeywordRepository.save(any(UserKeyword.class))).thenAnswer(invocation -> {
            UserKeyword saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        when(userNewsSourceRepository.findByUserAndIsActiveTrue(testUser)).thenReturn(activeSources);
        when(newsCollectorService.collectNewsAsync(eq(testUser), eq(keyword), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When
        UserKeyword result = keywordService.addKeyword(testUser, keyword);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getKeyword()).isEqualTo(keyword);
        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.getIsActive()).isTrue();

        verify(userKeywordRepository).existsByUserAndKeyword(testUser, keyword);
        verify(userKeywordRepository).save(any(UserKeyword.class));
        verify(userNewsSourceRepository).findByUserAndIsActiveTrue(testUser);
        verify(newsCollectorService).collectNewsAsync(testUser, keyword, UserNewsSource.NewsSourceType.NAVER);
    }

    @Test
    @DisplayName("중복 키워드 추가 시 예외 발생")
    void addKeyword_DuplicateKeyword() {
        // Given
        String keyword = "중복키워드";

        when(userKeywordRepository.existsByUserAndKeyword(testUser, keyword)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> keywordService.addKeyword(testUser, keyword))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Keyword already exists for this user");

        verify(userKeywordRepository).existsByUserAndKeyword(testUser, keyword);
        verify(userKeywordRepository, never()).save(any(UserKeyword.class));
        verify(newsCollectorService, never()).collectNewsAsync(any(), any(), any());
    }

    @Test
    @DisplayName("키워드 삭제 성공")
    void removeKeyword_Success() {
        // Given
        Long keywordId = 1L;

        when(userKeywordRepository.findById(keywordId)).thenReturn(Optional.of(testKeyword));

        // When
        keywordService.removeKeyword(testUser, keywordId);

        // Then
        verify(userKeywordRepository).findById(keywordId);
        verify(userKeywordRepository).delete(testKeyword);
    }

    @Test
    @DisplayName("존재하지 않는 키워드 삭제 시 예외 발생")
    void removeKeyword_NotFound() {
        // Given
        Long keywordId = 999L;

        when(userKeywordRepository.findById(keywordId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> keywordService.removeKeyword(testUser, keywordId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Keyword not found");

        verify(userKeywordRepository).findById(keywordId);
        verify(userKeywordRepository, never()).delete(any());
    }

    @Test
    @DisplayName("다른 사용자의 키워드 삭제 시 예외 발생")
    void removeKeyword_UnauthorizedAccess() {
        // Given
        Long keywordId = 1L;
        User otherUser = new User();
        otherUser.setId(2L);

        when(userKeywordRepository.findById(keywordId)).thenReturn(Optional.of(testKeyword));

        // When & Then
        assertThatThrownBy(() -> keywordService.removeKeyword(otherUser, keywordId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unauthorized access to keyword");

        verify(userKeywordRepository).findById(keywordId);
        verify(userKeywordRepository, never()).delete(any());
    }

    @Test
    @DisplayName("키워드 상태 토글 성공")
    void toggleKeywordStatus_Success() {
        // Given
        Long keywordId = 1L;
        boolean originalStatus = testKeyword.getIsActive();

        when(userKeywordRepository.findById(keywordId)).thenReturn(Optional.of(testKeyword));
        when(userKeywordRepository.save(testKeyword)).thenReturn(testKeyword);

        // When
        keywordService.toggleKeywordStatus(testUser, keywordId);

        // Then
        assertThat(testKeyword.getIsActive()).isEqualTo(!originalStatus);
        verify(userKeywordRepository).findById(keywordId);
        verify(userKeywordRepository).save(testKeyword);
    }

    @Test
    @DisplayName("사용자 키워드 목록 조회")
    void getUserKeywords_Success() {
        // Given
        List<UserKeyword> expectedKeywords = Arrays.asList(testKeyword);

        when(userKeywordRepository.findByUser(testUser)).thenReturn(expectedKeywords);

        // When
        List<UserKeyword> result = keywordService.getUserKeywords(testUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testKeyword);
        verify(userKeywordRepository).findByUser(testUser);
    }

    @Test
    @DisplayName("사용자 활성 키워드 목록 조회")
    void getActiveUserKeywords_Success() {
        // Given
        List<UserKeyword> expectedKeywords = Arrays.asList(testKeyword);

        when(userKeywordRepository.findByUserAndIsActiveTrue(testUser)).thenReturn(expectedKeywords);

        // When
        List<UserKeyword> result = keywordService.getActiveUserKeywords(testUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testKeyword);
        assertThat(result.get(0).getIsActive()).isTrue();
        verify(userKeywordRepository).findByUserAndIsActiveTrue(testUser);
    }
}