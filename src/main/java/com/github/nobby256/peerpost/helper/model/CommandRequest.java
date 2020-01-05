package com.github.nobby256.peerpost.helper.model;

import javax.servlet.http.HttpServletRequest;

import lombok.Data;

@Data
public class CommandRequest {

	private String channelId;
	private String channelName;
	private String command;
	private String responseUrl;
	private String teamDomain;
	private String teamId;
	private String text;
	private String token;
	private String triggerId;
	private String userId;
	private String userName;

	public static CommandRequest of(HttpServletRequest request) {
		CommandRequest commandRequest = new CommandRequest();
		commandRequest.setChannelId(request.getParameter("channel_id"));
		commandRequest.setChannelName(request.getParameter("channel_name"));
		commandRequest.setCommand(request.getParameter("command"));
		commandRequest.setResponseUrl(request.getParameter("response_url"));
		commandRequest.setTeamDomain(request.getParameter("team_domain"));
		commandRequest.setTeamId(request.getParameter("team_id"));
		commandRequest.setText(request.getParameter("text"));
		commandRequest.setToken(request.getParameter("token"));
		commandRequest.setTriggerId(request.getParameter("trigger_id"));
		commandRequest.setUserId(request.getParameter("user_id"));
		commandRequest.setUserName(request.getParameter("user_name"));
		return commandRequest;
	}
}
