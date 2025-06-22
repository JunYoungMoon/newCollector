package com.portfolio.newkeyword.service;

import com.portfolio.newkeyword.entity.User;
import com.portfolio.newkeyword.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setAuthProvider(User.AuthProvider.LOCAL);
    }

    @Test
    @DisplayName("사용자명으로 사용자 조회 성공")
    void loadUserByUsername_Success() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        UserDetails result = userService.loadUserByUsername("testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("존재하지 않는 사용자명으로 조회 시 예외 발생")
    void loadUserByUsername_NotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.loadUserByUsername("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: nonexistent");
    }

    @Test
    @DisplayName("사용자 등록 성공")
    void registerUser_Success() {
        // Given
        String username = "newuser";
        String email = "newuser@example.com";
        String password = "password123";
        String encodedPassword = "encodedPassword123";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });

        // When
        User result = userService.registerUser(username, email, password);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getPassword()).isEqualTo(encodedPassword);
        assertThat(result.getAuthProvider()).isEqualTo(User.AuthProvider.LOCAL);

        verify(userRepository).existsByUsername(username);
        verify(userRepository).existsByEmail(email);
        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("중복된 사용자명으로 등록 시 예외 발생")
    void registerUser_DuplicateUsername() {
        // Given
        String username = "existinguser";
        String email = "new@example.com";
        String password = "password123";

        when(userRepository.existsByUsername(username)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(username, email, password))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Username already exists");

        verify(userRepository).existsByUsername(username);
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("중복된 이메일로 등록 시 예외 발생")
    void registerUser_DuplicateEmail() {
        // Given
        String username = "newuser";
        String email = "existing@example.com";
        String password = "password123";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(username, email, password))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email already exists");

        verify(userRepository).existsByUsername(username);
        verify(userRepository).existsByEmail(email);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("OAuth2 사용자 생성 또는 업데이트 - 새 사용자")
    void createOrUpdateOAuth2User_NewUser() {
        // Given
        String email = "oauth@example.com";
        String name = "OAuth User";
        String providerId = "google123";
        User.AuthProvider provider = User.AuthProvider.GOOGLE;

        when(userRepository.findByProviderIdAndAuthProvider(providerId, provider))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(3L);
            return user;
        });

        // When
        User result = userService.createOrUpdateOAuth2User(email, name, providerId, provider);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getUsername()).isEqualTo(name);
        assertThat(result.getProviderId()).isEqualTo(providerId);
        assertThat(result.getAuthProvider()).isEqualTo(provider);

        verify(userRepository).findByProviderIdAndAuthProvider(providerId, provider);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("OAuth2 사용자 생성 또는 업데이트 - 기존 사용자")
    void createOrUpdateOAuth2User_ExistingUser() {
        // Given
        String email = "updated@example.com";
        String name = "Updated Name";
        String providerId = "google123";
        User.AuthProvider provider = User.AuthProvider.GOOGLE;

        User existingUser = new User();
        existingUser.setId(4L);
        existingUser.setEmail("old@example.com");
        existingUser.setUsername("Old Name");
        existingUser.setProviderId(providerId);
        existingUser.setAuthProvider(provider);

        when(userRepository.findByProviderIdAndAuthProvider(providerId, provider))
                .thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userService.createOrUpdateOAuth2User(email, name, providerId, provider);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(4L);
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getUsername()).isEqualTo(name);
        assertThat(result.getProviderId()).isEqualTo(providerId);
        assertThat(result.getAuthProvider()).isEqualTo(provider);

        verify(userRepository).findByProviderIdAndAuthProvider(providerId, provider);
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("사용자명으로 사용자 찾기 성공")
    void findByUsername_Success() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        User result = userService.findByUsername("testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("이메일로 사용자 찾기 성공")
    void findByEmail_Success() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        User result = userService.findByEmail("test@example.com");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("이메일로 사용자 찾기 실패 시 null 반환")
    void findByEmail_NotFound() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When
        User result = userService.findByEmail("nonexistent@example.com");

        // Then
        assertThat(result).isNull();
        verify(userRepository).findByEmail("nonexistent@example.com");
    }
}