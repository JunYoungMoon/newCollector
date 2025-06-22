package com.portfolio.newkeyword.controller;

import com.portfolio.newkeyword.entity.User;
import com.portfolio.newkeyword.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MainController 테스트")
class MainControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private MainController mainController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(mainController)
                .build();
    }

    @TestConfiguration
    @EnableWebSecurity
    static class TestConfig {

        @Bean
        @Primary
        public UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/", "/login", "/register").permitAll()
                            .requestMatchers("/dashboard").authenticated()
                            .anyRequest().authenticated()
                    )
                    .formLogin(form -> form
                            .loginPage("/login")
                            .permitAll()
                    )
                    .logout(logout -> logout.permitAll());

            return http.build();
        }

        @Bean
        @Primary
        public org.springframework.web.servlet.ViewResolver viewResolver() {
            return new org.springframework.web.servlet.view.InternalResourceViewResolver() {
                @Override
                public org.springframework.web.servlet.View resolveViewName(String viewName, java.util.Locale locale) {
                    // 실제 뷰 해석 없이 Mock View 반환
                    return new org.springframework.web.servlet.view.AbstractView() {
                        @Override
                        protected void renderMergedOutputModel(java.util.Map<String, Object> model,
                                                               jakarta.servlet.http.HttpServletRequest request,
                                                               jakarta.servlet.http.HttpServletResponse response) {
                            // 실제 렌더링은 수행하지 않음
                        }
                    };
                }
            };
        }
    }

    @Test
    @DisplayName("홈페이지 접근 - 비로그인 사용자")
    @WithAnonymousUser
    void home_AnonymousUser() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    @DisplayName("홈페이지 접근 - 로그인 사용자는 대시보드로 리다이렉트")
    @WithMockUser(username = "testuser")
    void home_AuthenticatedUser() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("로그인 페이지 접근")
    @WithAnonymousUser
    void loginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    @DisplayName("로그인 페이지 - 에러 파라미터 포함")
    @WithAnonymousUser
    void loginPageWithError() throws Exception {
        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attribute("error", "아이디 또는 비밀번호가 잘못되었습니다."));
    }

    @Test
    @DisplayName("회원가입 페이지 접근")
    @WithAnonymousUser
    void registerPage() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }

    @Test
    @DisplayName("회원가입 처리 - 성공")
    @WithAnonymousUser
    void registerUser_Success() throws Exception {
        // Given
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("newuser");
        mockUser.setEmail("newuser@example.com");

        when(userService.registerUser("newuser", "newuser@example.com", "password123"))
                .thenReturn(mockUser);

        // When & Then
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("email", "newuser@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("message", "회원가입이 완료되었습니다. 로그인해주세요."));

        verify(userService).registerUser("newuser", "newuser@example.com", "password123");
    }

    @Test
    @DisplayName("회원가입 처리 - 비밀번호 불일치")
    @WithAnonymousUser
    void registerUser_PasswordMismatch() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("email", "newuser@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "differentpassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attribute("error", "비밀번호가 일치하지 않습니다."));

        verify(userService, never()).registerUser(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("회원가입 처리 - 중복 사용자명")
    @WithAnonymousUser
    void registerUser_DuplicateUsername() throws Exception {
        // Given
        when(userService.registerUser("existinguser", "new@example.com", "password123"))
                .thenThrow(new RuntimeException("Username already exists"));

        // When & Then
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "existinguser")
                        .param("email", "new@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attribute("error", "Username already exists"));
    }

    @Test
    @DisplayName("대시보드 접근 - 로그인 사용자")
    @WithMockUser(username = "testuser")
    void dashboard_AuthenticatedUser() throws Exception {
        // Given
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");

        when(userService.findByUsername("testuser")).thenReturn(mockUser);

        // When & Then
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("user", mockUser));

        verify(userService).findByUsername("testuser");
    }

    @Test
    @DisplayName("대시보드 접근 - 비로그인 사용자는 로그인 페이지로 리다이렉트")
    @WithAnonymousUser
    void dashboard_AnonymousUser() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection());
    }
}