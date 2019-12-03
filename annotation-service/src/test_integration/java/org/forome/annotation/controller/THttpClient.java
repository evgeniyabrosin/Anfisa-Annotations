/*
 Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association

 Developed by Vladimir Ulitin and Michael Bouzinier

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

	 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package org.forome.annotation.controller;

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
