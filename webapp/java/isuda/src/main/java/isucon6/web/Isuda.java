package isucon6.web;

import static spark.Spark.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
		get("/stars", getStars);
		post("/stars", postStars);

		exception(Exception.class, (exception, request, response) -> {

			try {
				exception.printStackTrace();
				Connection connection = (Connection) request.attribute("connection");
				connection.rollback();
			} catch (SQLException e) {
				throw new RuntimeException("SystemException", e);
			}
		});

		afterAfter((request, response) -> {
			try {
				Connection connection = (Connection) request.attribute("connection");
				connection.close();
			} catch (SQLException e) {
				throw new RuntimeException("SystemException", e);
			}
		});
	}

	public static Route getInitialize = (request, response) -> {

		Connection connection = DBUtils.getConnection(request);

		DBUtils.execute(connection, "DELETE FROM entry WHERE id > 7101");
		DBUtils.execute(connection, "TRUNCATE isutar.star");

		Cache.init(connection);

		return "ok";
	};

	public static Route getIndex = (request, response) -> {

		setName(request);

		int PER_PAGE = 10;
		int page = (request.queryParams("page") == null) ? 1 : Integer.parseInt(request.queryParams("page"));

		Connection connection = DBUtils.getConnection(request);
		List<Entry> entries = DBUtils.select(connection,
				"SELECT * FROM entry ORDER BY updated_at DESC LIMIT ? OFFSET ?", Entry::create, PER_PAGE, (page - 1) * PER_PAGE);

		for (Entry entry : entries) {
			entry.html = htmlify(entry.keyword, entry.description, request);
			entry.stars= loadStars(entry.keyword, request);
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

		Cache.addKeyword(keyword);

		connection.commit();

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

		connection.commit();

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

		String keyword = request.params("keyword");

		setName(request);

		if (keyword == null || "".equals(keyword)) {
			response.status(400);
			return "";
		}

		Connection connection = DBUtils.getConnection(request);
		Entry entry = getKeyword(keyword, connection);

		if (entry == null) {
			System.out.println("get 404 -> " + keyword);
			response.status(404);
			return "";
		}

		entry.html = htmlify(entry.keyword, entry.description, request);
		entry.stars = loadStars(entry.keyword, request);

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

		Cache.removeKeyword(keyword);

		connection.commit();

		response.redirect("/");
		return null;
	};

	public static Route getStars = (request, response) -> {

		Map<String, List<Map<String, Object>>> result = getStars(request.queryParams("keyword"), request);

		response.type("text/json");
		return JSON.encode(result);
	};

	public static Route postStars = (request, response) -> {

		String keyword = request.queryParams("keyword");

		Connection connection = DBUtils.getConnection(request);

		if (StringUtils.isEmpty(keyword)) {
			keyword = request.params("keyword");
		}

		Entry entry = getKeyword(keyword, connection);
		if (entry == null) {
			halt(404);
		}

		String user = request.queryParams("user");
		if (StringUtils.isEmpty(user)) {
			user = request.params("user");
		}

		DBUtils.execute(connection, "INSERT INTO isutar.star (keyword, user_name, created_at) VALUES (?, ?, NOW())",
				keyword, user);

		Cache.addStar(keyword, user);

		connection.commit();

		response.type("text/json");
		return "{result: \"ok\"}";
	};

	private static boolean authenticate(Request request) {

		if (request.session().attribute("userId") == null) {
			return false;
		}

		return true;
	}

	synchronized private static String htmlify(String keyword, String content, Request request) throws SQLException {

		if (content == null || "".equals(content)) {
			return "";
		}

		String result = Cache.getContent(keyword);

		if (result != null) {
			return result;
		}

		result = content;

		Map<String, String> kw2link = new HashMap<>();
		List<String> existsKeywords = new ArrayList<String>();

		List<String> keywords = Cache.getKeywords();

		result = Utils.replaceEach(content, keywords, (kw) -> {
			Entry entry = Cache.entries.get(kw);
			existsKeywords.add(entry.hash);
			kw2link.put(entry.hash, entry.link);
			return entry.hash;
		});

		result = Utils.escapeHtml(result);

		result = Utils.replaceEach(result, existsKeywords, (hash) -> {
			return kw2link.get(hash);
		});

		result = StringUtils.replace(result, "\n", "<br />");

		Cache.setContent(keyword, result);

		return result;

	}

	private static List<Map<String, Object>> loadStars(String keyword, Request request) throws SQLException {

		Map<String, List<Map<String, Object>>> stars = getStars(keyword, request);

		return stars.get("stars");

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

	private static Entry getKeyword(String keyword, Connection connection) throws SQLException {
		return DBUtils.selectOne(connection, "SELECT * FROM entry WHERE keyword = ?", Entry::create, keyword);
	}

	private static Map<String, List<Map<String, Object>>> getStars(String keyword, Request request)
			throws SQLException {

		Map<String, List<Map<String, Object>>> stars = Cache.getStars(keyword);

		if (stars == null) {
			Connection connection = DBUtils.getConnection(request);

			List<Map<String, Object>> sqlResult = DBUtils.select(connection,
					"SELECT * FROM isutar.star WHERE keyword = ?", keyword);

			stars = new HashMap<>();
			stars.put("stars", sqlResult);

			Cache.setStars(keyword, stars);
		}

		return stars;
	}
}
