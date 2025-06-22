package com.portfolio.newkeyword.oauth2;

public class GoogleOAuth2UserInfo implements OAuth2UserInfo {
    private final java.util.Map<String, Object> attributes;

    public GoogleOAuth2UserInfo(java.util.Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }
}