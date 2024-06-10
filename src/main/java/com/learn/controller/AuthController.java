package com.learn.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.learn.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class AuthController {

	@Autowired
	UserService userService;
	
	@GetMapping("/login")
	public String login() {
		return "login-view.html";
	}

	@GetMapping("/login-auth")
	public String loginInitiate() {
		return "redirect:/oauth2/authorization/google";
	}

	@GetMapping("/login-success")
	public String loginSuccess() {
		String accessToken = userService.getAccessToken();
		return accessToken != null ? "redirect:/home" : "redirect:/login";
	}

	@GetMapping("/home")
	public String home() {
		return "home-view.html";
	}
}
