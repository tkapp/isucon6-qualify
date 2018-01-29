package isucon6.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class Utils {

	public static Map<String, String> httpGet(String urlString) {

		Map<String, String> result = new HashMap<>();

		HttpURLConnection con = null;

		try {

			URL url = new URL(urlString);

			con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");

			result.put("status", String.valueOf(con.getResponseCode()));
			if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
				//
				return result;
			}

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"))) {

				StringBuilder body = new StringBuilder();
				while (reader.ready()) {
					body.append(reader.readLine());
				}

				result.put("body", body.toString());

				return result;
			}

		} catch (IOException e) {
			throw new RuntimeException("SystemException", e);
		}
	}

	public static Map<String, String> httpPost(String urlString, String data) {

		Map<String, String> result = new HashMap<>();

		HttpURLConnection connection = null;
		try {

			URL url = new URL(urlString);
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod("POST");

			try (OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream())) {
				out.write(data);
				out.flush();
			}

			result.put("status", String.valueOf(connection.getResponseCode()));
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				//
				return result;
			}

			try (InputStream in = connection.getInputStream()) {

				StringBuilder content = new StringBuilder();
				try (BufferedReader input = new BufferedReader(new InputStreamReader(in))) {
					String line = null;
					while ((line = input.readLine()) != null) {
						content.append(line);
					}
				}

				result.put("content", content.toString());
				return result;
			}

		} catch (Exception e) {
			throw new RuntimeException("SystemExeption", e);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	public static String hexDigest(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			String hex = String.format("%02x", b);
			sb.append(hex);
		}
		return sb.toString();
	}

	public static String getSha1Digest(String item) {

		try {

			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(item.getBytes());
			return hexDigest(digest);

		} catch (NoSuchAlgorithmException e) {
			//
			throw new RuntimeException("SystemException", e);
		}
	}

	public static String escapeHtml(String value) {
		return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("'", "&#x27;");
	}

	public static String urlEncode(String value) {

		try {
			String result = URLEncoder.encode(value, "UTF-8");
			result = result.replace("+", "%20");

			return result;

		} catch (Exception e) {
			throw new RuntimeException("SystemException", e);
		}
	}

}
