package com.learn.config;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import com.learn.service.UserService;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		SecurityFilterChain response = http
				.authorizeHttpRequests(authorizeRequests -> authorizeRequests.requestMatchers("/login-auth").permitAll()
						.dispatcherTypeMatchers(DispatcherType.values()).permitAll()
						.requestMatchers(this::customRequestMatcher).authenticated())
				.csrf(csrf -> csrf.disable())
				.oauth2Login(oauth2Login -> oauth2Login.loginPage("/login").defaultSuccessUrl("/login-success"))
				.build();

		return response;
	}
	
	@Autowired
	UserService userService;

	private boolean customRequestMatcher(HttpServletRequest request) {
		try {
			return userService.getAccessToken() != null;
		} catch (Exception e) {
			log.error("Exception occured in customRequestMatcher:{}", ExceptionUtils.getStackTrace(e));
		}
		return false;
	}

}
