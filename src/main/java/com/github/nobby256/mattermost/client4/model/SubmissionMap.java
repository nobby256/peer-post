package com.github.nobby256.mattermost.client4.model;

import java.math.BigDecimal;
import java.util.HashMap;

public class SubmissionMap extends HashMap<String, String> {

	public BigDecimal getNumber(String key) {
		String value = get(key);
		if (value == null || value.length() == 0) {
			return null;
		}
		BigDecimal numValue = new BigDecimal(value);
		return numValue;
	}

	public String getString(String key) {
		return get(key);
	}

	public Boolean getBoolean(String key) {
		String value = get(key);
		if (value == null || value.length() == 0) {
			return null;
		}
		Boolean boolValue = Boolean.valueOf(value);
		return boolValue;
	}

}
