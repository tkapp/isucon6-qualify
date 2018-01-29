package isucon6.web;

import static spark.Spark.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import freemarker.template.Configuration;
import net.arnx.jsonic.JSON;
import spark.ModelAndView;
import spark.Request;
import spark.Route;
import spark.embeddedserver.EmbeddedServers;
import spark.embeddedserver.jetty.EmbeddedJettyFactory;
import spark.template.freemarker.FreeMarkerEngine;

public class Isuda {

	public static void main(String[] args) throws Exception {

		EmbeddedServers.add(EmbeddedServers.Identifiers.JETTY, new EmbeddedJettyFactory(new IsudaJettyServer()));
		port(5000);

		staticFiles.externalLocation("../public");

		before("*", (request, response) -> {
			Connection connection = DBUtils.getConnection(request);
			request.attribute("connection", connection);
		});

		get("/", getIndex);
		get("/keyword/:keyword", getKeyword);
		post("/keyword", createKeyword);
		post("/keyword/:keyword", deleteKeyword);
		get("/initialize", getInitialize);
		get("/robots.txt", getRobotTxt);
		get("/register", getRegister);
		post("/register", postRegister);
		get("/login", getLogin);
		post("/login", postLogin);
		get("/logout", getLogout);

		after("*", (request, response) -> {
			Connection connection = (Connection) request.attribute("connection");
			connection.commit();
			connection.close();
		});

		exception(Exception.class, (exception, request, response) -> {

			try {
				Connection connection = (Connection) request.attribute("connection");
				exception.printStackTrace();
				connection.rollback();
				connection.close();
			} catch (SQLException e) {
				throw new RuntimeException("SystemException", e);
			}
		});
	}

	public static Route getInitialize = (request, response) -> {

		Connection connection = DBUtils.getConnection(request);

		DBUtils.execute(connection, "DELETE FROM entry WHERE id > 7101");
		Utils.httpGet(Config.isutarOrigin + "/initialize");

		return "ok";
	};

	public static Route getIndex = (request, response) -> {

		setName(request);

		int PER_PAGE = 10;
		int page = (request.queryParams("page") == null) ? 1 : Integer.parseInt(request.queryParams("page"));

		Connection connection = DBUtils.getConnection(request);
		List<Map<String, Object>> entries = select(connection,
				"SELECT * FROM entry ORDER BY updated_at DESC LIMIT ? OFFSET ?", PER_PAGE, (page - 1) * PER_PAGE);

		for (Map<String, Object> entry : entries) {
			entry.put("html", htmlify(entry.get("description").toString(), request));
			entry.put("stars", loadStars(entry.get("keyword").toString()));
		}

		int totalEntries = DBUtils.count(connection, "SELECT COUNT(*) AS count FROM entry");
		int lastPage = totalEntries / PER_PAGE;

		int[] pages = IntStream.range(Math.max(1, page - 5), Math.min(lastPage, page + 5)).toArray();

		Map<String, Object> model = new HashMap<>();
		model.put("entries", entries);
		model.put("page", page);
		model.put("pages", pages);
		model.put("lastPage", lastPage);
		model.put("userName", request.attribute("userName"));

		return render(model, "/index.ftl");
	};

	public static Route getRobotTxt = (request, response) -> {
		halt(404);
		return null;
	};

	public static Route createKeyword = (request, response) -> {

		setName(request);

		String keyword = request.queryParams("keyword");

		if (StringUtils.isEmpty(keyword)) {
			halt(400);
		}

		String userId = request.attribute("userId");

		String description = request.queryParams("description");

		if (isSpamContents(keyword) || isSpamContents(description)) {
			halt(400);
		}

		Connection connection = DBUtils.getConnection(request);
		DBUtils.execute(connection,
				"INSERT INTO entry (author_id, keyword, description, created_at, updated_at)"
						+ " VALUES (?,?,?,NOW(), NOW()) " + " ON DUPLICATE KEY UPDATE "
						+ " author_id = ?, keyword = ?, description = ?, updated_at = NOW() ",
				userId, keyword, description, userId, keyword, description);

		response.redirect("/");
		return null;
	};

	public static Route getRegister = (request, response) -> {

		setName(request);

		Map<String, Object> model = new HashMap<>();
		model.put("action", "register");
		model.put("userName", request.attribute("userName"));

		return render(model, "templates/register.ftl");
	};

	public static Route postRegister = (request, response) -> {

		String name = request.queryParams("name");
		String password = request.queryParams("password");

		if (StringUtils.isEmpty(name) || StringUtils.isEmpty(password)) {
			response.status(400);
			return null;
		}

		Connection connection = DBUtils.getConnection(request);
		String userId = register(connection, name, password);
		request.session().attribute("userId", userId);

		response.redirect("/");
		return null;
	};

	private static String register(Connection connection, String user, String password) throws SQLException {
		//
		String salt = RandomStringUtils.randomAlphanumeric(20);

		String hash = Utils.getSha1Digest(salt + "password");

		DBUtils.execute(connection, "INSERT INTO user (name, salt, password, created_at) VALUES (?, ?, ?, NOW())", user,
				salt, hash);

		Map<String, Object> selectResult = DBUtils.selectOne(connection, "SELECT LAST_INSERT_ID() AS last_insert_id");

		return selectResult.get("last_insert_id").toString();
	}

	public static Route getLogin = (request, response) -> {

		setName(request);

		Map<String, Object> model = new HashMap<>();
		model.put("action", "login");
		model.put("userName", request.attribute("userName"));

		return render(model, "/authenticate.ftl");
	};

	public static Route postLogin = (request, response) -> {

		String name = request.queryParams("name");
		String password = request.queryParams("password");

		Connection connection = DBUtils.getConnection(request);
		Map<String, Object> user = DBUtils.selectOne(connection, "SELECT * FROM user WHERE name = ?", name);

		if (user == null || !StringUtils.equals(user.get("password").toString(),
				Utils.getSha1Digest(user.get("salt").toString() + password))) {
			halt(403);
		}

		request.session().attribute("userId", user.get("id").toString());

		response.redirect("/");
		return null;

	};

	public static Route getLogout = (request, response) -> {

		request.session().invalidate();
		response.redirect("/");

		return null;
	};

	public static Route getKeyword = (request, response) -> {

		setName(request);

		String keyword = request.params("keyword");

		if (keyword == null || "".equals(keyword)) {
			response.status(400);
			return "";
		}

		Connection connection = DBUtils.getConnection(request);
		Map<String, Object> entry = DBUtils.selectOne(connection, "SELECT * FROM entry WHERE keyword = ?", keyword);

		if (entry == null) {
			response.status(404);
			return "";
		}

		entry.put("html", htmlify(entry.get("description").toString(), request));
		entry.put("stars", loadStars(entry.get("keyword").toString()));

		Map<String, Object> model = new HashMap<>();
		model.put("entry", entry);
		model.put("userName", request.attribute("userName"));

		return render(model, "/keyword.ftl");
	};

	public static Route deleteKeyword = (request, response) -> {

		if (authenticate(request)) {
			response.status(400);
			return null;
		}

		setName(request);

		String keyword = request.params("keyword");

		Connection connection = DBUtils.getConnection(request);
		Map<String, Object> entry = DBUtils.selectOne(connection, "SELECT * FROM entry WHERE keyword = ?", keyword);

		if (entry == null) {
			response.status(404);
			return null;
		}

		DBUtils.execute(connection, "DELETE FROM entry WHERE keyword = ?", keyword);

		response.redirect("/");
		return null;
	};

	private static boolean authenticate(Request request) {

		if (request.session().attribute("userId") == null) {
			return false;
		}

		return true;
	}

	private static List<Map<String, Object>> select(Connection connection, String sql, Object... params)
			throws SQLException {
		//
		try (PreparedStatement statement = connection.prepareStatement(sql)) {

			DBUtils.setParams(statement, params);

			try (ResultSet rs = statement.executeQuery()) {

				ResultSetMetaData metaData = rs.getMetaData();
				int columnCount = metaData.getColumnCount();

				List<Map<String, Object>> result = new ArrayList<>();

				while (rs.next()) {

					Map<String, Object> item = DBUtils.convertToMap(rs, metaData, columnCount);

					result.add(item);
				}

				return result;
			}
		}
	}

	private static String htmlify(String content, Request request) throws SQLException {

		if (content == null || "".equals(content)) {
			return "";
		}

		String result = content;

		Connection connection = DBUtils.getConnection(request);

		Map<String, String> kw2sha = new HashMap<>();

		List<Map<String, Object>> keywords = select(connection,
				"SELECT keyword FROM entry ORDER BY CHARACTER_LENGTH(keyword) DESC");

		String regex = String.format("(%s)", keywords.stream().map(k -> Pattern.quote(k.get("keyword").toString()))
				.collect(Collectors.joining("|")));

		Pattern re = Pattern.compile(regex.toString());

		Matcher m = re.matcher(content);

		StringBuffer sb = new StringBuffer();
		while (m.find()) {

			String k = m.group();
			String hash = "isuda_" + Utils.getSha1Digest(k);

			m.appendReplacement(sb, hash);

			kw2sha.put(k, hash);
		}
		m.appendTail(sb);

		result = sb.toString();

		result = Utils.escapeHtml(result);

		for (Map.Entry<String, String> kw : kw2sha.entrySet()) {

			String url = "/keyword/" + Utils.urlEncode(kw.getKey());
			String link = String.format("<a href=\"%s\">%s</a>", url, Utils.escapeHtml(kw.getKey()));

			result = result.replace(kw.getValue(), link);
		}

		result = result.replaceAll("\n", "<br />");

		return result;

	}

	private static Object loadStars(String keyword) {

		Map<String, String> result = Utils.httpGet(Config.isutarOrigin + "/stars?keyword=" + Utils.urlEncode(keyword));
		Map<String, Object> json = JSON.decode(result.get("body"));

		return (Object) json.get("stars");

	}

	private static String render(Map<String, Object> model, String path) {
		return getTemplateEngine().render(new ModelAndView(model, path));
	}

	private static FreeMarkerEngine getTemplateEngine() {

		Configuration freeMarkerConfiguration = new Configuration(Configuration.VERSION_2_3_26);
		freeMarkerConfiguration.setClassForTemplateLoading(Isuda.class, "/templates");
		freeMarkerConfiguration.setDefaultEncoding("UTF-8");
		FreeMarkerEngine freeMarkerEngine = new FreeMarkerEngine(freeMarkerConfiguration);

		return freeMarkerEngine;
	}

	private static boolean isSpamContents(String target) {

		Map<String, String> httpResult = Utils.httpPost(Config.isupamOrigin, "content=" + Utils.urlEncode(target));
		String content = httpResult.get("content");

		Map<String, Boolean> result = JSON.decode(content);

		if (!result.get("valid")) {
			return true;
		}

		return false;
	}

	private static void setName(Request request) throws SQLException {
		//
		Connection connection = DBUtils.getConnection(request);

		Object userId = request.session().attribute("userId");

		if (userId != null) {

			request.attribute("userId", userId);

			Map<String, Object> user = DBUtils.selectOne(connection, "SELECT name FROM user WHERE id = ?", userId);

			if (user == null) {
				halt(403);
			}

			request.attribute("userName", user.get("name"));
		} else {
			request.attribute("userName", "");
		}
	}
}
