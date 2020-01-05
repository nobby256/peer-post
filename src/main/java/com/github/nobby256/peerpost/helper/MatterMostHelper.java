package com.github.nobby256.peerpost.helper;

import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.nobby256.peerpost.config.MattermostProperties;

import net.bis5.mattermost.client4.ApiResponse;
import net.bis5.mattermost.client4.hook.IncomingWebhookClient;
import net.bis5.mattermost.model.IncomingWebhookRequest;
import net.bis5.mattermost.model.Team;
import net.bis5.mattermost.model.User;

@Component
public class MatterMostHelper {

	@Autowired
	private MattermostProperties props;
	@Autowired
	private HttpServletRequest servletRequest;

	public MattermostClientEx getClient(boolean useAccessToken) {
		String url = props.getUrl();
		String token = props.getAccessToken();
		String logLevel = props.getLogLevel();

		MattermostClientEx client;
		if (StringUtils.isNotEmpty(logLevel)) {
			Level level = Level.parse(logLevel);
			client = new MattermostClientEx(url, level);
		} else {
			client = new MattermostClientEx(url);
		}
		if (useAccessToken) {
			client.setAccessToken(token);
		}
		return client;
	}

	public MattermostClientEx getClient() {
		return getClient(true);
	}

	public ApiResponse<Boolean> postByIncomingWebhook(IncomingWebhookRequest payload) {
		throw new NotImplementedException("postByIncomingWebhook(IncomingWebhookRequest payload)は未実装です");
	}

	public ApiResponse<Boolean> postByIncomingWebhook(String url, IncomingWebhookRequest payload) {
		String logLevel = props.getLogLevel();

		IncomingWebhookClient client = null;
		if (StringUtils.isNotEmpty(logLevel)) {
			Level level = Level.parse(logLevel);
			client = new IncomingWebhookClient(url, level);
		} else {
			client = new IncomingWebhookClient(url);
		}
		return client.postByIncomingWebhook(payload);
	}

	public ApiResponse<Boolean> postByDelayedCommandResponse(String responseUrl, IncomingWebhookRequest payload) {
		return postByIncomingWebhook(responseUrl, payload);
	}

	public String createCallbackUrl(String callbackUrl) {
		if (servletRequest != null) {
			callbackUrl = servletRequest.getScheme() + "://" + servletRequest.getServerName() + ":" + servletRequest.getServerPort() + servletRequest.getContextPath() + callbackUrl;
		}
		return callbackUrl;
	}

	public String getPermanentLinkUrl(String teamId, String postId) {
		MattermostClientEx client = getClient();
		Team team = client.getTeam(teamId).readEntity();
		return String.format("%s/%s/pl/%s", client.getUrl(), team.getName(), postId);

	}

	public String getDisplayName(User user) {
		String displayName = user.getNickname();
		if (StringUtils.isNotEmpty(displayName)) {
			return displayName;
		}

		String firstName = user.getFirstName();
		String lastName = user.getLastName();
		displayName = (StringUtils.isNotBlank(firstName) ? firstName : "") + " " + (StringUtils.isNotBlank(lastName) ? lastName : "");
		displayName = displayName.trim();
		if (StringUtils.isNotEmpty(displayName)) {
			return displayName;
		}

		return user.getUsername();
	}

}
