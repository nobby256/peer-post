package com.github.nobby256.peerpost.controller;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.nobby256.mattermost.client4.MattermostClientEx;
import com.github.nobby256.mattermost.client4.model.CommandRequest;
import com.github.nobby256.peerpost.config.AppProperties;
import com.github.nobby256.peerpost.helper.MatterMostHelper;

import net.bis5.mattermost.model.Channel;
import net.bis5.mattermost.model.CommandResponse;
import net.bis5.mattermost.model.CommandResponseType;
import net.bis5.mattermost.model.Post;
import net.bis5.mattermost.model.PostList;
import net.bis5.mattermost.model.Reaction;
import net.bis5.mattermost.model.ReactionList;
import net.bis5.mattermost.model.User;

@Transactional(rollbackFor = Exception.class)
@RestController
@RequestMapping("/peer/report")
public class PeerReportCommand {

	private static final String USAGE = "** Slash Command Help **\n\n  /peer-report [YYYY/MM/DD]\n\n  - 日付は省略可能です。\n\n  - 日付を省略した場合は今週の月曜日からの集計となります。\n\n  - 集計期間は指定した日から現在まで。";

	@Autowired
	private MatterMostHelper helper;
	private String postChannelName;

	@Autowired
	public void setAppProperties(AppProperties props) {
		postChannelName = props.getChannelName();
	}

	@GetMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE)
	public CommandResponse index(HttpServletRequest httpRequest) throws Exception {
		CommandRequest request = CommandRequest.of(httpRequest);

		MattermostClientEx client = helper.getClient();

		LocalDate from = null;
		//引数チェック
		{
			String errorMessage = null;

			String[] args;
			String text = request.getText();
			if (StringUtils.isBlank(text)) {
				args = new String[0];
			} else {
				args = request.getText().split("\\s");
			}
			if (args.length == 0) {
				LocalDate today = LocalDate.now();
				DayOfWeek dow = today.getDayOfWeek();
				int delta = dow.getValue() - DayOfWeek.MONDAY.getValue();
				from = today.plusDays(-1 * delta);
			} else if (args.length == 1) {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
				try {
					from = LocalDate.parse(args[0], formatter);
				} catch (Exception ex) {
					errorMessage = "有効な日付ではありません。\n\n" + USAGE;
				}
			} else {
				errorMessage = USAGE;
			}
			if (errorMessage != null) {
				String userId = request.getUserId();
				String channelId = request.getChannelId();
				client.createEphemeralPost(userId, new Post(channelId, errorMessage));
				return null;
			}
		}

		Date fromDate = Date.from(from.atStartOfDay(ZoneId.systemDefault()).toInstant());

		Map<String, Integer> fromCountMap = new HashMap<>();
		Map<String, Integer> toCountMap = new HashMap<>();
		Map<String, Integer> reactionCountMap = new HashMap<>();
		Map<String, Integer> hashTagCountMap = new HashMap<>();
		Map<String, String> displayNameMap = new HashMap<>();
		{
			Channel channel = client.getChannelByName(postChannelName, request.getTeamId()).readEntity();

			//特定チャンネル、指定日付以降のPostを取得する
			PostList postList = client.getPostsSince(channel.getId(), fromDate.getTime()).readEntity();
			Map<String, Post> postMap = postList.getPosts();

			for (String postId : postMap.keySet()) {
				Post post = postMap.get(postId);
				Map<String, Object> propMap = post.getProps();

				//プロパティにpeer-postというキー格納されているか否か
				Object value = propMap.get("peer-post");
				if (value == null) {
					continue;
				}
				String[] ids = ((String) value).split(":");
				addCount(fromCountMap, ids[0]);
				addCount(toCountMap, ids[1]);
				displayNameMap.put(ids[0], null);
				displayNameMap.put(ids[1], null);

				//プロパティにpeer-post-hashtagsというキーが格納されているか否か
				value = propMap.get("peer-post-hashtags");
				if (value != null) {
					for (String hashtag : ((String) value).split(" ")) {
						addCount(hashTagCountMap, hashtag);
					}
				}

				//リアクションを付けたユーザーをすべて取得
				ReactionList reactionList = client.getReactions(postId).readEntity();
				reactionList.forEach((Reaction reaction) -> {
					String reactionUserId = reaction.getUserId();
					displayNameMap.put(reactionUserId, null);
					addCount(reactionCountMap, reactionUserId);
				});
			}
		}
		//userIdをディスプレイ名に変換する
		{
			for (String userId : displayNameMap.keySet()) {
				User user = client.getUser(userId).readEntity();
				String displayName = helper.getDisplayName(user);
				displayNameMap.put(userId, displayName);
			}
		}
		//===========================================
		//レポート出力
		//==========
		String report;
		{
			//昇順でソートする
			List<Pair<String, Integer>> toList = sortCountMap(toCountMap);
			List<Pair<String, Integer>> fromList = sortCountMap(fromCountMap);
			List<Pair<String, Integer>> reactionList = sortCountMap(reactionCountMap);
			List<Pair<String, Integer>> hashTagList = sortCountMap(hashTagCountMap);

			StringBuilder builder = new StringBuilder();
			String title = String.format("## ピア投稿ランキング %s～", new SimpleDateFormat("yyyy/MM/dd").format(fromDate));

			builder.append(title).append("\n\n");
			builder.append("褒められた人\n\n");
			builder.append("| 名前 | 回数 |\n");
			builder.append("| :--- | ---: |\n");
			for (Pair<String, Integer> pair : toList) {
				String displayName = displayNameMap.get(pair.getKey());
				builder.append("|" + displayName + "|" + pair.getValue() + "|\n");
			}

			builder.append("\n\n");
			builder.append("褒めた人\n\n");
			builder.append("| 名前 | 回数 |\n");
			builder.append("| :--- | ---: |\n");
			for (Pair<String, Integer> pair : fromList) {
				String displayName = displayNameMap.get(pair.getKey());
				builder.append("|" + displayName + "|" + pair.getValue() + "|\n");
			}

			builder.append("\n\n");
			builder.append("リアクションした人\n\n");
			builder.append("| 名前 | 回数 |\n");
			builder.append("| :--- | ---: |\n");
			for (Pair<String, Integer> pair : reactionList) {
				String displayName = displayNameMap.get(pair.getKey());
				builder.append("|" + displayName + "|" + pair.getValue() + "|\n");
			}

			builder.append("\n\n");
			builder.append("人気ハッシュタグ\n\n");
			builder.append("| ハッシュタグ | 回数 |\n");
			builder.append("| :--- | ---: |\n");
			for (Pair<String, Integer> pair : hashTagList) {
				String hashTag = pair.getKey();
				builder.append("|" + hashTag + "|" + pair.getValue() + "|\n");
			}

			report = builder.toString();
		}

		CommandResponse response = new CommandResponse();
		response.setResponseType(CommandResponseType.Ephemeral);
		response.setChannelId(request.getChannelId());
		User botUser = client.getMe().readEntity();
		response.setUsername(helper.getDisplayName(botUser));
		response.setIconUrl(client.getUserProfileImageUrl(botUser.getId()));
		response.setText(report);

		return response;
	}

	protected List<Pair<String, Integer>> sortCountMap(Map<String, Integer> map) {
		List<Pair<String, Integer>> pairList = new ArrayList<Pair<String, Integer>>();
		for (Map.Entry<String, Integer> entry : map.entrySet()) {
			pairList.add(ImmutablePair.of(entry.getKey(), entry.getValue()));
		}
		pairList.sort((a, b) -> {
			return a.getValue().compareTo(b.getValue()) * -1;//降順にする
		});
		return pairList;
	}

	protected void addCount(Map<String, Integer> map, String key) {
		Integer count = map.get(key);
		if (count == null) {
			map.put(key, 1);
		} else {
			map.put(key, count + 1);
		}
	}
}
