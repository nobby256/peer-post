package com.github.nobby256.peerpost.config;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@ConfigurationProperties(prefix = "mattermost")
@Data
public class MattermostProperties {
	@NotNull
	private String url;
	@NotNull
	private String accessToken;
	private String logLevel;
}
