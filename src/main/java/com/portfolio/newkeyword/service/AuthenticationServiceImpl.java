package com.portfolio.newkeyword.service;

import com.portfolio.newkeyword.entity.User;
import com.portfolio.newkeyword.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 서비스 구현체
 * Security 설정과 독립적으로 동작
 */
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User processOAuth2User(String email, String name, String providerId, User.AuthProvider provider) {
        return userRepository.findByProviderIdAndAuthProvider(providerId, provider)
                .map(user -> updateExistingUser(user, email, name))
                .orElseGet(() -> createNewOAuth2User(email, name, providerId, provider));
    }

    @Override
    @Transactional
    public User registerLocalUser(String username, String email, String password) {
        validateUserRegistration(username, email);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setAuthProvider(User.AuthProvider.LOCAL);

        return userRepository.save(user);
    }

    @Override
    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    private User updateExistingUser(User user, String email, String name) {
        user.setEmail(email);
        user.setUsername(name);
        return userRepository.save(user);
    }

    private User createNewOAuth2User(String email, String name, String providerId, User.AuthProvider provider) {
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setUsername(name);
        newUser.setProviderId(providerId);
        newUser.setAuthProvider(provider);
        return userRepository.save(newUser);
    }

    private void validateUserRegistration(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }
    }
}