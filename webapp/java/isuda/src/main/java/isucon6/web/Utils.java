package isucon6.web;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

public class Utils {

	private static Map<String, String> escapeMap;

	static {
		escapeMap = new HashMap<>();
		escapeMap.put("&", "&amp;");
		escapeMap.put("\"", "&quot;");
		escapeMap.put("<", "&lt;");
		escapeMap.put(">", "&gt;");
		escapeMap.put("'", "&#x27;");
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

	public static String escapeHtml(final String value) {

		String result = value;

		for (Map.Entry<String, String> kv : escapeMap.entrySet()) {
			result = StringUtils.replace(result, kv.getKey(), kv.getValue());
		}

		return result;
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

	public static String replaceEach(final String text, final List<String> searchList, Function<String, String> replace) {

		StringBuilder result = new StringBuilder(text);
		int currentPos = 0;
		List<String> nextSearchList = searchList;

		while (true) {

			int minPos = -1;
			String keyword = null;

			List<String> currenetSearchList = nextSearchList;
			nextSearchList = new ArrayList<>();

 			for (int i = 0; i < currenetSearchList.size(); i++) {

				int pos = result.indexOf(currenetSearchList.get(i), currentPos);

				if (pos == -1) {
					continue;
				} else if (minPos == -1 || minPos > pos) {
					minPos = pos;
					keyword = currenetSearchList.get(i);
				}

				nextSearchList.add(currenetSearchList.get(i));
			}

			if (minPos == -1) {
				return result.toString();
			}

			String replacement = replace.apply(keyword);
			result.replace(minPos, minPos + keyword.length(), replacement);
			currentPos = minPos + replacement.length();
		}
	}

	public static String createHash(String kw) {
		return "i_" + Utils.getSha1Digest(kw);
	}
}
