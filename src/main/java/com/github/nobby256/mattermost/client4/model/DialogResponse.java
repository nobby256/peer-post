package com.github.nobby256.mattermost.client4.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class DialogResponse {

	@JsonProperty("status")
	private String status;

}
