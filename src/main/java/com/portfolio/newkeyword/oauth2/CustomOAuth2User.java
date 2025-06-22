package com.portfolio.newkeyword.oauth2;

import com.portfolio.newkeyword.entity.User;
import lombok.Getter;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class CustomOAuth2User implements OAuth2User {
    @Getter
    private final User user;
    private final java.util.Map<String, Object> attributes;
    private final String nameAttributeKey;

    public CustomOAuth2User(User user, java.util.Map<String, Object> attributes, String nameAttributeKey) {
        this.user = user;
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
    }

    @Override
    public java.util.Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
        return user.getAuthorities();
    }

    @Override
    public String getName() {
        return (String) attributes.get(nameAttributeKey);
    }

}