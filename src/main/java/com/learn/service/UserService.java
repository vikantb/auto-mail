package com.learn.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class UserService {

	@Autowired
	private OAuth2AuthorizedClientService authorizedClientService;

	public String getAccessToken() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated() && auth instanceof OAuth2AuthenticationToken) {
			String registrationId = getClientRegistrationId(); // Specify the registration ID of your OAuth2 client

			OAuth2User oauth2User = (OAuth2User) auth.getPrincipal();
			OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(registrationId,
					oauth2User.getName());
			OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
			String tokenValue = accessToken.getTokenValue();
			return tokenValue;
		} else {
			return null;
		}

	}

	public Object getUserDetails() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (!auth.isAuthenticated()) {
			return null;
		}

		return auth.getPrincipal();
	}

	public String getClientRegistrationId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication instanceof OAuth2AuthenticationToken) {
			OAuth2AuthenticationToken oauth2Authentication = (OAuth2AuthenticationToken) authentication;
			return oauth2Authentication.getAuthorizedClientRegistrationId();
		}
		return null;
	}
}
