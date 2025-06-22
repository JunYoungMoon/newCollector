package com.portfolio.newkeyword.service;

import com.portfolio.newkeyword.entity.User;

public interface AuthenticationService {

    /**
     * OAuth2 사용자 등록/업데이트
     */
    User processOAuth2User(String email, String name, String providerId, User.AuthProvider provider);

    /**
     * 일반 사용자 등록
     */
    User registerLocalUser(String username, String email, String password);

    /**
     * 사용자 조회
     */
    User findUserByUsername(String username);

    /**
     * 사용자 조회 (이메일)
     */
    User findUserByEmail(String email);
}