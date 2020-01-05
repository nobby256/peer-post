package com.github.nobby256.peerpost.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class MattermostDao {
	@Autowired
	JdbcTemplate jdbcTemplate;

	public void updateHashtag(String postId, String hashtags) {
		jdbcTemplate.update("update posts set Hashtags = ? where Id = ?", hashtags, postId);
	}

}
