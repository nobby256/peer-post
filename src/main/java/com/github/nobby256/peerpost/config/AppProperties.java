package com.github.nobby256.peerpost.config;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@ConfigurationProperties(prefix = "peer-post")
@Data
public class AppProperties {
	@NotNull
	private List<String> hashtags;
	@NotNull
	private String channelName;
}
