package com.github.nobby256.mattermost.client4;

import java.util.logging.Level;

import com.github.nobby256.mattermost.client4.model.DialogRequest;
import com.github.nobby256.mattermost.client4.model.DialogResponse;

import net.bis5.mattermost.client4.ApiResponse;
import net.bis5.mattermost.client4.MattermostClient;

public class MattermostClientEx extends MattermostClient {

	private String url;

	public MattermostClientEx(String url, Level logLevel) {
		super(url, logLevel);
		this.url = url;
	}

	public MattermostClientEx(String url) {
		super(url);
		this.url = url;
	}

	public ApiResponse<DialogResponse> openDialog(DialogRequest data) {
		return doApiPost(getOpenDialogRoute(), data, DialogResponse.class);
	}

	public String getIntegrationActionsRoute() {
		return "/actions";
	}

	public String getDialogsRoute() {
		return getIntegrationActionsRoute() + "/dialogs";
	}

	public String getOpenDialogRoute() {
		return getDialogsRoute() + "/open";
	}

	public String getUserProfileImageUrl(String userId) {
		return url + API_URL_SUFFIX + getUserProfileImageRoute(userId);
	}

	public String getUrl() {
		return url;
	}
	public String getApiUrl() {
		return url + API_URL_SUFFIX;
	}

}
