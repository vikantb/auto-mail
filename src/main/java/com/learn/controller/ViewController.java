package com.learn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ViewController.class);
	
	@GetMapping("/")
	public String home() {
        return "index.html";
	}
}
