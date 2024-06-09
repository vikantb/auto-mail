package com.learn.auth;

import com.google.api.services.gmail.Gmail;

public class UserDto {

	private String accessToken;
	private String userName;
	private Gmail gmail;

	public String getAccessToken() {
		return accessToken;
	}
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public Gmail getGmail() {
		return gmail;
	}
	public void setGmail(Gmail gmail) {
		this.gmail = gmail;
	}

}
