package com.learn.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class CurrentUser {

	public static final ThreadLocal<UserDto> local = ThreadLocal.withInitial(() -> new UserDto());

    private OAuth2AuthorizedClientService authorizedClientService;
	
	private CurrentUser(){

	}

	public static String getAccessToken() {
		return null;
	}
}
