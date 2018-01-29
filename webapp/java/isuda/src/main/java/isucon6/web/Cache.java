package isucon6.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Cache {

	private static List<String> keywords = new ArrayList<>();

	public static void init(Connection connection) throws SQLException {

		List<Map<String, Object>> entries = DBUtils.select(connection,
				"SELECT keyword FROM entry ORDER BY CHARACTER_LENGTH(keyword) DESC");

		List<String> keywords = new ArrayList<>();
		for (Map<String, Object> kv : entries) {
			keywords.add(kv.get("keyword").toString());
		}
		Cache.keywords = keywords;
	}

	public static boolean addKeyword(String keyword) {

		if (keywords.contains(keyword)) {
			return false;
		}

		keywords = new ArrayList<>(keywords);

		int length = keyword.length();
		for (int i = 0; i < keywords.size(); i++) {
			if (length >= keywords.get(i).length()) {
				keywords.add(i, keyword);
				return true;
			}
		}

		keywords.add(keyword);

		return true;
	}

	public static List<String> getKeywords() {
		return keywords;
	}

	public static void removeKeyword(String keyword) {
		keywords.remove(keyword);
	}
}
