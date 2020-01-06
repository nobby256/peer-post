package com.github.nobby256.mattermost.client4.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.Getter;

@Data
public class DialogSubmissionResponse {
	@JsonProperty("error")
	String error;

	@JsonProperty("errors")
	@Getter
	Map<String, String> errors = new HashMap<String, String>();

	public void putErrors(String elementName, String error) {
		errors.put(elementName, error);
	}

}
