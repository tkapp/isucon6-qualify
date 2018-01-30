package isucon6.web;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class Entry {

	public static Entry create(ResultSet rs) {

		try {
			Entry entry = new Entry();
			entry.keyword = rs.getString("keyword");
			entry.description = rs.getString("description");

			return entry;
		} catch (SQLException e) {
			throw new RuntimeException("SystemException", e);
		}
	}

	public String keyword;

	public String hash;

	public String link;

	public String description;

	public String html;

	public List<Map<String, Object>> stars;

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getHtml() {
		return html;
	}

	public void setHtml(String html) {
		this.html = html;
	}

	public List<Map<String, Object>> getStars() {
		return stars;
	}

	public void setStars(List<Map<String, Object>> stars) {
		this.stars = stars;
	}

	public String getKeyword() {
		return keyword;
	}


	@Override
	public String toString() {
		return keyword;
	}

	public void setKeyword(String keyword) {

		this.keyword = keyword;

		hash = Utils.createHash(keyword);

		String url = "/keyword/" + Utils.urlEncode(keyword);
		link = String.format("<a href=\"%s\">%s</a>", url, Utils.escapeHtml(keyword));
	}
}
