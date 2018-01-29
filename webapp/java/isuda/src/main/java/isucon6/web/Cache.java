package isucon6.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Cache {

	private static List<String> keywords = new ArrayList<>();

	public static Map<String, Entry> entries = new HashMap<>();

	public static void init(Connection connection) throws SQLException {

		List<Map<String, Object>> entries = DBUtils.select(connection,
				"SELECT keyword FROM entry ORDER BY CHARACTER_LENGTH(keyword) DESC");

		for (Map<String, Object> kv : entries) {
			addKeyword(kv.get("keyword").toString());
		}
	}

	public static boolean addKeyword(String keyword) {

		if (keywords.contains(keyword)) {
			return false;
		}

		int length = keyword.length();
		for (int i = 0; i < keywords.size(); i++) {
			if (length >= keywords.get(i).length()) {

				keywords.add(i, keyword);

				Entry e = new Entry();
				e.setKeyword(keyword);
				entries.put(keyword, e);
				return true;
			}
		}

		keywords.add(keyword);

		Entry e = new Entry();
		e.setKeyword(keyword);
		entries.put(keyword, e);

		return true;
	}

	public static List<String> getKeywords() {
		return keywords;
	}

	public static void removeKeyword(String keyword) {
		keywords.remove(keyword);
	}
}
