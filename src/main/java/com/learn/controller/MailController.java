package com.learn.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.learn.service.MailService;

@RestController
public class MailController {

	@Autowired
	MailService mailService;

	@GetMapping("/{user-id}")
	public void getMessages(@PathVariable("user-id") String userId) throws IOException {
		mailService.deleteMessageFromUser(userId);
		return;
	}
	
	@GetMapping("/all-mail-ids")
	public Object getMailIds() throws IOException {
		return mailService.getEmailAddressesFromInbox();
	}

}
