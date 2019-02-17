package ru.processtech.forome.annotation.controller;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class THttpClient {

	private final String uri;
	private String session = null;

	public THttpClient(final String uri) {
		this.uri = uri;
	}

	public String logon(final String login, final String password) throws Exception {
		String response = execute("logon/login", new HashMap<String, String>() {{
			put("login", login);
			put("password", password);
		}});

		JSONObject jResponse = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(response);
		JSONObject jDataResponse = (JSONObject) jResponse.get("data");
		session = jDataResponse.getAsString("session");
		return session;
	}

	public String execute(String path, Map<String, String> variables) throws Exception {
		HttpClient client = HttpClientBuilder.create().build();
		HttpPost httpPost = new HttpPost(this.uri + path);
		httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

		List<NameValuePair> nvps = new ArrayList<>();
		if (session != null) {
			nvps.add(new BasicNameValuePair("session", session));
		}
		for (Map.Entry<String, String> entry : variables.entrySet()) {
			nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
		}
		httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));

		HttpResponse response = client.execute(httpPost);
		return EntityUtils.toString(response.getEntity());
	}
}
