package isucon6.web;

public class Entry {

	public String keyword;

	public String hash;

	public String link;

	public String content;

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
