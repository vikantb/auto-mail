package com.learn.controller;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import com.learn.auth.CurrentUser;
import com.learn.dto.RequestDto;
import com.learn.service.MailService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class MailController {

	@Autowired
	MailService mailService;

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MailController.class);

	@PostMapping("/delete")
	public void deleteMessages(@RequestBody RequestDto requestDto) throws IOException {
		log.info("deleteMessages Invoked. requestDto:{}", requestDto);
		mailService.deleteMessageFromUser(requestDto);
		return;
	}

	@PostMapping("/fetch-email-addresses")
	public Object getMailIds(@RequestBody RequestDto requestDto) throws IOException, GeneralSecurityException {
		return mailService.getEmailAddressesFromInbox(requestDto);
	}

	@PostMapping("/edit")
	public void moveMails(@RequestBody RequestDto requestDto) throws IOException {
		mailService.modifyMessage(requestDto);
	}

	@PostMapping("/emails")
	public List<Message> getMessages(@RequestBody RequestDto requestDto) throws IOException {
		return mailService.getMessageByFilter(requestDto);
	}

	@GetMapping("/labels")
	public List<Label> getUserLabels(@RequestParam String emailId) throws IOException {
		return mailService.getUserLabels(emailId);
	}

	@PostMapping("/update-label-name")
	public void updateLabelName(@RequestParam Integer errorCount) throws IOException {
		mailService.updateLabelName(errorCount);
	}

}
