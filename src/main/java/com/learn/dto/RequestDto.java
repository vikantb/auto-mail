package com.learn.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.util.CollectionUtils;

import lombok.Data;

@Data
public class RequestDto {
	private String accessToken;
	private String query = "";
	private boolean selfFolder;
	private boolean removeAllLabels;
	private Map<String, String> filter;

	private List<String> emailIds;
	private List<String> addLabels;
	private List<String> removeLabels;
	private Long maxResult;

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public Long getMaxResult() {
		return this.maxResult;
	}

	public void setMaxResult(Long maxResult) {
		this.maxResult = maxResult;
	}

	public boolean getRemoveAllLabels() {
		return this.removeAllLabels;
	}

	public void getRemoveAllLabels(boolean removeAllLabels) {
		this.removeAllLabels = removeAllLabels;
	}

	public boolean getSelfFolder() {
		return this.selfFolder;
	}

	public void setselfFolder(boolean selfFolder) {
		this.selfFolder = selfFolder;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public Map<String, String> getFilter() {
		return filter;
	}

	public void setFilter(Map<String, String> filter) {
		this.filter = filter;
	}

	public List<String> getEmailIds() {
		return CollectionUtils.isEmpty(emailIds) ? new ArrayList<>() : emailIds;
	}

	public void setEmailIds(List<String> emailIds) {
		this.emailIds = emailIds;
	}

	public List<String> getAddLabels() {
		return CollectionUtils.isEmpty(addLabels) ? new ArrayList<>() : addLabels;
	}

	public void setAddLabels(List<String> addLabels) {
		this.addLabels = addLabels;
	}

	public List<String> getRemoveLabels() {
		return CollectionUtils.isEmpty(removeLabels) ? new ArrayList<>() : removeLabels;
	}

	public void setRemoveLabels(List<String> removeLabels) {
		this.removeLabels = removeLabels;
	}
}
