package com.github.nobby256.peerpost.helper.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class DialogSubmissionRequest {
	@JsonProperty("type")
	private String type;
	@JsonProperty("callback_id")
	private String callbackId;
	@JsonProperty("state")
	private String state;
	@JsonProperty("user_id")
	private String userId;
	@JsonProperty("channel_id")
	private String channelId;
	@JsonProperty("team_id")
	private String teamId;
	@JsonProperty("submission")
	private SubmissionMap submission = new SubmissionMap();
	@JsonProperty("cancelled")
	private boolean cancelled;
}
