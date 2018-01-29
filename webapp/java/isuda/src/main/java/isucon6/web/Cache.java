package isucon6.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Cache {

	private static List<String> keywords = new ArrayList<>();

	private static Pattern cachePattern;

	public static void init(Connection connection) throws SQLException {

		cachePattern = null;

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
		cachePattern = null;

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

	public static void removeKeyword(String keyword) {
		cachePattern = null;
		keywords.remove(keyword);
	}

	public static String getRegex() {

		List<String> keywords = new ArrayList<>(Cache.keywords);
		String regex = String.format("(%s)",
				keywords.stream().map(k -> Pattern.quote(k)).collect(Collectors.joining("|")));

		return regex;
	}

	public static Pattern getPattern() {

		if (cachePattern == null) {
			String regex = getRegex();
			cachePattern = Pattern.compile(regex.toString());
		}

		return cachePattern;
	}
}
