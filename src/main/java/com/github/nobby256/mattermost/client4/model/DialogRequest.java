package com.github.nobby256.mattermost.client4.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class DialogRequest {
	/**
	 * trigger id.
	 */
	@JsonProperty("trigger_id")
	private String triggerId;

	/**
	 * The URL to send the submitted dialog payload to.
	 */
	@JsonProperty("url")
	private String url;

	@JsonProperty("dialog")
	private Dialog dialog;

	public DialogRequest(String triggerId, String callbackUrl, Dialog dialog) {
		this.triggerId = triggerId;
		this.url = callbackUrl;
		this.dialog = dialog;
	}

	public DialogRequest(String triggerId, String callbackUrl, String title) {
		this(triggerId, callbackUrl, new Dialog(title));
	}

}
