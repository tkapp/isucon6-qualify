package isucon6.web;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import spark.Request;

public class DBUtils {

	private static HikariDataSource ds;

	static {

		String url = "jdbc:mysql://" + Config.host + ":" + Config.port + "/" + Config.db
				+ "?useUnicode=true&characterEncoding=" + Config.charset;

		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(url);
		config.setUsername(Config.user);
		config.setPassword(Config.password);
		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "250");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		config.setMinimumIdle(60);
		config.setMaximumPoolSize(60);
		config.setAutoCommit(false);

		ds = new HikariDataSource(config);
	}

	public static Connection getConnection(Request request) {

		if (request.attribute("connection") != null) {
			return request.attribute("connection");
		}

		try {

			Connection connection = ds.getConnection();

			try (Statement statement = connection.createStatement()) {
				//
				statement.execute("SET SESSION sql_mode='TRADITIONAL,NO_AUTO_VALUE_ON_ZERO,ONLY_FULL_GROUP_BY'");
				statement.execute("SET NAMES utf8mb4");
				statement.execute("begin");
			}

			request.attribute("connection", connection);

			return connection;

		} catch (SQLException e) {
			//
			throw new RuntimeException("SystemException", e);
		}
	}

	public static Connection getConnection() throws SQLException {
		return ds.getConnection();
	}

	public static Connection getConnection2(Request request) {

		if (request.attribute("connection") != null) {
			return request.attribute("connection");
		}

		//
		try {
			//
			Class.forName("com.mysql.jdbc.Driver");

			String serverName = Config.host;
			String port = Config.port;
			String databaseName = Config.db;
			String user = Config.user;
			String password = Config.password;

			String url = "jdbc:mysql://" + serverName + ":" + port + "/" + databaseName
					+ "?useUnicode=true&characterEncoding=" + Config.charset;

			Connection connection = DriverManager.getConnection(url, user, password);
			connection.setAutoCommit(false);

			try (Statement statement = connection.createStatement()) {
				//
				statement.execute("SET SESSION sql_mode='TRADITIONAL,NO_AUTO_VALUE_ON_ZERO,ONLY_FULL_GROUP_BY'");
				statement.execute("SET NAMES utf8mb4");
			}

			request.attribute("connection", connection);

			return connection;

		} catch (ClassNotFoundException | SQLException e) {
			//
			throw new RuntimeException("SystemException", e);
		}
	}

	public static List<Map<String, Object>> select(Connection connection, String sql, Object... params)
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

	public static Map<String, Object> selectOne(Connection connection, String sql, Object... params)
			throws SQLException {

		try (PreparedStatement statement = connection.prepareStatement(sql)) {

			setParams(statement, params);

			try (ResultSet rs = statement.executeQuery()) {

				ResultSetMetaData metaData = rs.getMetaData();
				int columnCount = metaData.getColumnCount();

				if (rs.next()) {

					Map<String, Object> item = convertToMap(rs, metaData, columnCount);
					return item;

				} else {
					return null;
				}
			}
		}
	}

	public static int count(Connection connection, String sql, Object... params) throws SQLException {

		try (PreparedStatement statement = connection.prepareStatement(sql)) {

			setParams(statement, params);

			try (ResultSet rs = statement.executeQuery()) {
				rs.next();
				return rs.getInt(1);
			}
		}
	}

	public static int execute(Connection connection, String sql) throws SQLException {
		//
		return execute(connection, sql, new Object[0]);
	}

	public static int execute(Connection connection, String sql, Object... params) throws SQLException {
		//
		try (PreparedStatement statement = connection.prepareStatement(sql)) {

			setParams(statement, params);

			return statement.executeUpdate();
		}
	}

	public static void setParams(PreparedStatement statement, Object... params) throws SQLException {
		//
		for (int i = 0; i < params.length; i++) {
			if (params[i] instanceof Integer || params[i].getClass() == int.class) {
				statement.setInt(i + 1, (Integer) params[i]);
			} else if (params[i] instanceof BigInteger) {
				statement.setInt(i + 1, ((BigInteger) params[i]).intValue());
			} else {
				statement.setString(i + 1, (String) params[i]);
			}
		}
	}

	public static Map<String, Object> convertToMap(ResultSet rs, ResultSetMetaData metaData, int columnCount)
			throws SQLException {
		Map<String, Object> item = new HashMap<>();

		for (int i = 0; i < columnCount; i++) {
			item.put(metaData.getColumnName(i + 1), rs.getObject(i + 1));
		}
		return item;
	}

}
