package com.github.nobby256.peerpost.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.nobby256.mattermost.client4.MattermostClientEx;
import com.github.nobby256.mattermost.client4.model.CommandRequest;
import com.github.nobby256.mattermost.client4.model.Dialog;
import com.github.nobby256.mattermost.client4.model.DialogRequest;
import com.github.nobby256.mattermost.client4.model.DialogSubmissionRequest;
import com.github.nobby256.mattermost.client4.model.DialogSubmissionResponse;
import com.github.nobby256.mattermost.client4.model.SubmissionMap;
import com.github.nobby256.mattermost.client4.model.Dialog.Element;
import com.github.nobby256.mattermost.client4.model.Dialog.Element.ElementType;
import com.github.nobby256.mattermost.client4.model.Dialog.Element.Option;
import com.github.nobby256.peerpost.config.AppProperties;
import com.github.nobby256.peerpost.dao.MattermostDao;
import com.github.nobby256.peerpost.helper.MatterMostHelper;

import net.bis5.mattermost.model.Channel;
import net.bis5.mattermost.model.Post;
import net.bis5.mattermost.model.SlackAttachment;
import net.bis5.mattermost.model.User;
import net.bis5.mattermost.model.UserList;

@Transactional(rollbackFor = Exception.class)
@RestController
@RequestMapping("/peer")
public class PeerPointController {

	private static final String USAGE = "** ピア投稿 Slash Command Help **\n\n  /peer @ユーザ名\n\n  - 複数人を指すメンションは指定できません。（ex: @all, @channel, @here）\n\n  - チーム内のメンバーのみ指定できます。";

	@Autowired
	private MatterMostHelper helper;
	@Autowired
	private MattermostDao dao;

	private String postChannelName;
	private List<Option> hashTagOptionList = new ArrayList<>();

	@Autowired
	public void setAppProperties(AppProperties props) {
		for (String hashtag : props.getHashtags()) {
			if (!hashtag.startsWith("#")) {
				hashtag = "#" + hashtag;
			}
			hashTagOptionList.add(new Option(hashtag, hashtag));
		}
		postChannelName = props.getChannelName();
	}

	@PostMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE)
	public void index(HttpServletRequest httpRequest) throws Exception {
		CommandRequest request = CommandRequest.of(httpRequest);

		MattermostClientEx client = helper.getClient();

		User targetUser = null;
		//引数チェック
		{
			String errorMessage = null;

			String mention = null;

			String[] args = request.getText().split("\\s");
			if (args.length != 1) {
				errorMessage = USAGE;
			} else {
				mention = args[0];
			}

			//メンションをユーザ名に修正
			String username = null;
			if (errorMessage == null) {
				if (!mention.startsWith("@")) {
					errorMessage = USAGE;
				} else if ("@all".equals(mention) || "@channel".equals(mention) || "@here".equals(mention)) {
					errorMessage = USAGE;
				} else {
					username = mention.substring(1);
				}
			}
			//ユーザーの存在確認
			if (errorMessage == null) {
				UserList userList = client.getUsersByUsernames(username).readEntity();
				if (userList.size() == 0) {
					errorMessage = "該当ユーザーを見つけることができませんでした。（" + mention + "）\n\n" + USAGE;
				} else if (userList.size() > 1) {
					errorMessage = "ユーザーを一人に絞り込むことが出来ませんでした。（" + mention + "）\n\n" + USAGE;
				} else {
					targetUser = userList.get(0);
				}
			}

			//エラーがあった場合は終了
			if (errorMessage != null) {
				String userId = request.getUserId();
				String channelId = request.getChannelId();
				client.createEphemeralPost(userId, new Post(channelId, errorMessage));
				return;
			}
		}

		User botUser = client.getMe().readEntity();

		//ダイアログオープン
		Dialog dialog = buildDialog(client, botUser, targetUser);
		String triggerId = request.getTriggerId();
		String callbackUrl = helper.createCallbackUrl("/peer/dialog_submit");
		client.openDialog(new DialogRequest(triggerId, callbackUrl, dialog));
	}

	@PostMapping(path = "/dialog_submit", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public DialogSubmissionResponse dialogSubmit(@RequestBody DialogSubmissionRequest request) {
		MattermostClientEx client = helper.getClient();

		//投稿者
		User postUser = client.getUser(request.getUserId()).readEntity();
		//BOT
		User botUser = client.getMe().readEntity();
		//対象ユーザー
		User targetUser = client.getUser(request.getState()).readEntity();

		SubmissionMap submission = request.getSubmission();
		//本文
		String text = submission.get(TEXT);
		//スタンプ
		String stamp = submission.get(STAMP);
		//ハッシュタグ
		String hashtag1 = submission.get(HASHTAG_1);
		String hashtag2 = submission.get(HASHTAG_2);
		String hashtag3 = submission.get(HASHTAG_3);
		List<String> hashList = new ArrayList<>();
		if (StringUtils.isNotBlank(hashtag1)) {
			hashList.add(hashtag1);
		}
		if (StringUtils.isNotBlank(hashtag2)) {
			hashList.add(hashtag2);
		}
		if (StringUtils.isNotBlank(hashtag3)) {
			if(!hashtag3.startsWith("#")) {
				hashtag3 = "#" + hashtag3;
			}
			hashList.add(hashtag3);
		}
		String hashtag = StringUtils.join(hashList, " ");

		String newPostId;
		{

			Post newPost = new Post();
			//===========================================
			//投稿チャンネル
			//==========
			{
				Channel channel = client.getChannelByName(postChannelName, request.getTeamId()).readEntity();
				newPost.setChannelId(channel.getId());
			}
			//===========================================
			//BOT
			//==========
			{
				newPost.setUserId(botUser.getId());
			}

			Map<String, Object> propMap = new HashMap<>();
			newPost.setProps(propMap);
			List<SlackAttachment> attachments = new ArrayList<SlackAttachment>();
			propMap.put("attachments", attachments);
			//===========================================
			//アタッチメント
			//==========
			{
				SlackAttachment attach = new SlackAttachment();
				attachments.add(attach);

				//投稿者
				{
					attach.setAuthorName(helper.getDisplayName(postUser));
					attach.setAuthorIcon(client.getUserProfileImageUrl(postUser.getId()));
				}

				{
					String fulltext = String.format("@%sさんへ\n%s\n%s", helper.getDisplayName(targetUser), text, hashtag);

					attach.setText(fulltext);
				}

				//===========================================
				//サムネイル
				//==========
				{
					attach.setThumbUrl(stamp);
				}
			}

			newPostId = client.createPost(newPost).readEntity().getId();
		}

		//===========================================
		//ハッシュタグの更新
		//==========
		{
			dao.updateHashtag(newPostId, hashtag);
		}

		//===========================================
		//完了メッセージ
		//==========
		{
			Post post = new Post();
			post.setChannelId(request.getChannelId());
			String permaLink = helper.getPermanentLinkUrl(request.getTeamId(), newPostId);
			String message = "[こちら](" + permaLink + ")に投稿しました。";
			post.setMessage(message);
			client.createEphemeralPost(postUser.getId(), post);
		}

		return new DialogSubmissionResponse();
	}

	public static final String TEXT = "text";
	public static final String STAMP = "stamp";
	public static final String HASHTAG_1 = "hashtag_1";
	public static final String HASHTAG_2 = "hashtag_2";
	public static final String HASHTAG_3 = "hashtag_3";

	public Dialog buildDialog(MattermostClientEx client, User botUser, User user) {
		Dialog dialog = new Dialog(helper.getDisplayName(user) + " さんへの投稿を作成します。");
		dialog.setState(user.getId());

		dialog.setIconUrl(client.getUserProfileImageUrl(botUser.getId()));

		dialog.setSubmitLabel("投稿する");

		Element elm;

		//メッセージ
		elm = dialog.addElement(ElementType.textarea, "メッセージ", TEXT);
		elm.setOptional(false);
		elm.setPlaceholder("ピンチだった時、さりげなくサポートに入ってくれてありがとう。");

		//スタンプ
		elm = dialog.addElement(ElementType.select, "スタンプ", STAMP);
		elm.setOptional(false);
		fillStampOptions(elm.getOptions());

		//チームハッシュタグ１
		elm = dialog.addElement(ElementType.select, "褒める理由（行動指針・VALUE）", HASHTAG_1);
		elm.setOptional(true);
		elm.getOptions().addAll(hashTagOptionList);

		//チームハッシュタグ２
		elm = dialog.addElement(ElementType.select, "褒める理由（行動指針・VALUE）", HASHTAG_2);
		elm.setOptional(true);
		elm.getOptions().addAll(hashTagOptionList);

		//チームハッシュタグ３
		elm = dialog.addElement(ElementType.text, "褒める理由（自由に一言）", HASHTAG_3);
		elm.setOptional(true);
		elm.setMinLength(0);
		elm.setMaxLength(30);

		return dialog;
	}

	protected void fillStampOptions(List<Dialog.Element.Option> options) {
		for (String[] pair : STAMPS) {
			options.add(new Option(pair[0], helper.createCallbackUrl(pair[1])));
		}
	}

	static final String[][] STAMPS = new String[][] {
			new String[] { "カッコいい", "/static/stamp/Group 8.1.png" },
			new String[] { "カワいい", "/static/stamp/Group 8.2.png" },
			new String[] { "ステキ", "/static/stamp/Group 8.3.png" },
			new String[] { "たしかに", "/static/stamp/Group 8.4.png" },
			new String[] { "それな", "/static/stamp/Group 8.5.png" },
			new String[] { "天才", "/static/stamp/Group 8.6.png" },
			new String[] { "つよつよ", "/static/stamp/Group 8.7.png" },
			new String[] { "わかる", "/static/stamp/Group 8.7-1.png" },
			new String[] { "GJ", "/static/stamp/Group 8.8.png" },
			new String[] { "いいね", "/static/stamp/Group 8.9.png" },
			new String[] { "優秀", "/static/stamp/Group 8.10.png" },
			new String[] { "がんばれ", "/static/stamp/Group 8.11.png" },
			new String[] { "神", "/static/stamp/Group 8.14.png" },
			new String[] { "早く寝ろ", "/static/stamp/Group 8.15.png" },
			new String[] { "さすが", "/static/stamp/Group 8.17.png" },
			new String[] { "養いたい", "/static/stamp/Group 8.18.png" },
			new String[] { "世界一", "/static/stamp/Group 8.18-1.png" },
			new String[] { "富", "/static/stamp/Group 8.19.png" },
			new String[] { "名声", "/static/stamp/Group 8.20.png" },
			new String[] { "力", "/static/stamp/Group 8.22.png" },
			new String[] { "卍", "/static/stamp/Group 8.23.png" },
			new String[] { "尊い", "/static/stamp/Group 8.24.png" },
			new String[] { "ワロタ", "/static/stamp/Group 8.25.png" },
			new String[] { "草", "/static/stamp/Group 8.25-1.png" },
			new String[] { "養われたい", "/static/stamp/Group 8.26.png" },
			new String[] { "すごい", "/static/stamp/Group 8.png" },
			new String[] { "は？", "/static/stamp/Group 45.png" },
			new String[] { "ハート", "/static/stamp/Heart.gif" }
	};

}
