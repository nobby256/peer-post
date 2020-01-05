package com.github.nobby256.peerpost.helper.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import net.bis5.mattermost.model.PostAction;

@Data
public class TypeSupportedPostAction extends PostAction {

	@JsonProperty("type")
	private String type;
	@JsonProperty("options")
	private List<Option> options;

	@Data
	public static class Option {
		@JsonProperty("text")
		private String text;
		@JsonProperty("value")
		private String value;

		public Option(String text, String value) {
			this.text = text;
			this.value = value;
		}
	}

}
