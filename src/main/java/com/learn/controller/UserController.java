package com.learn.controller;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.learn.service.UserService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/user")
public class UserController {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserController.class);

	@Autowired
	private UserService userService;

	@Autowired
	private RestTemplate restTemplate;
	
	@GetMapping("/")
	public String home() throws IOException {
		// Read the content of the HTML file
        ClassPathResource resource = new ClassPathResource("static/index.html");
        Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
        String htmlContent = FileCopyUtils.copyToString(reader);
        // Return the HTML content
        return htmlContent;
	}

	@GetMapping("/login-success")
	public Object login(HttpSession session) {
		log.info("login success.");
		return getUserDetails();
	}

	@GetMapping("/details")
	public Object getUserDetails() {
		return userService.getUserDetails();
	}

	@GetMapping("/access-token")
	public String getAccessToken() {
		return userService.getAccessToken();
	}

	@GetMapping("/profile-image")
	public byte[] getProfileImage(@AuthenticationPrincipal OAuth2User oauth2User) {
		if (oauth2User != null) {
			String profileImageUrl = (String) oauth2User.getAttribute("picture"); // Assuming "picture" attribute
																					// contains the profile image URL
			if (profileImageUrl != null) {
				return restTemplate.getForObject(profileImageUrl, byte[].class);
			}
		}
		// Return a default profile image or handle the case when the image URL is not
		// available
		return null;
	}

}
