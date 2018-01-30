package isucon6.web;

import java.sql.ResultSet;

public class Star {

	public static Star create(ResultSet rs) {
		try {
			Star star = new Star();
			star.keyword = rs.getString("keyword");
			star.user_name = rs.getString("user_name");
			return star;
		} catch (Exception e) {
			throw new RuntimeException("SystemException", e);
		}
	}

	private String keyword;

	private String user_name;

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public String getUser_name() {
		return user_name;
	}

	public void setUser_name(String user_name) {
		this.user_name = user_name;
	}
}
