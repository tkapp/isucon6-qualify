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

	private static Map<String, String> contents = new HashMap<>();

	private static Map<String, List<Star>> stars = new HashMap<>();

	public static void init(Connection connection) throws SQLException {

		List<Map<String, Object>> entries = DBUtils.select(connection,
				"SELECT keyword FROM entry ORDER BY CHARACTER_LENGTH(keyword) DESC");

		for (Map<String, Object> kv : entries) {
			addKeyword(kv.get("keyword").toString());
		}

		contents = new HashMap<>();

		stars = new HashMap<>();
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

				contents = new HashMap<>();

				return true;
			}
		}

		keywords.add(keyword);

		Entry e = new Entry();
		e.setKeyword(keyword);
		entries.put(keyword, e);

		contents = new HashMap<>();

		return true;
	}

	public static List<String> getKeywords() {
		return keywords;
	}

	public static void removeKeyword(String keyword) {
		keywords.remove(keyword);
		entries.remove(keyword);
		stars.remove(keyword);
		contents = new HashMap<>();
	}

	public static String getContent(String keyword) {
		return contents.get(keyword);
	}

	public static void setContent(String keyword, String content) {
		contents.put(keyword, content);
	}

	public static List<Star> getStars(String keyword) {
		return stars.get(keyword);
	}

	public static void addStar(String keyword, String userName) {

		List<Star> stars = getStars(keyword);

		if (stars == null) {
			stars = new ArrayList<>();
		}

		Star star = new Star();
		star.setKeyword(keyword);
		star.setUser_name(userName);

		stars.add(star);


		Cache.stars.put(keyword, stars);
	}

	public static void setStars(String keyword, List<Star> stars) {
		Cache.stars.put(keyword, stars);
	}
}
