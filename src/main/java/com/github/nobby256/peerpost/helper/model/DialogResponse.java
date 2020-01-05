package com.github.nobby256.peerpost.helper.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class DialogResponse {

	@JsonProperty("status")
	private String status;

}
