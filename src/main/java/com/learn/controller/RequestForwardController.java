package com.learn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RequestForwardController {
	
	/**
	 * map to home view incase of no request matched
	 */
	@GetMapping(value = "/")
    public String forward() {
        return "redirect:/home";
    }
}
