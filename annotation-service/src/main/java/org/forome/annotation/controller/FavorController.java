/*
 *  Copyright (c) 2020. Vladimir Ulitin, Partners Healthcare and members of Forome Association
 *
 *  Developed by Vladimir Ulitin and Michael Bouzinier
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.forome.annotation.controller;

import com.google.common.base.Strings;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONAware;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.forome.annotation.Service;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.favor.struct.out.JMetadata;
import org.forome.annotation.service.database.rocksdb.favor.FavorDatabase;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;


@Controller
@RequestMapping(value = { "/favor" })
public class FavorController {

	private final static Logger log = LoggerFactory.getLogger(FavorController.class);

	@RequestMapping(value = "info")
	public ResponseEntity getInfo(HttpServletRequest request) {
		Service service = Service.getInstance();
		FavorDatabase favorDatabase = service.getDatabaseConnectService().getFavorDatabase();
		if (favorDatabase == null) {
			throw ExceptionBuilder.buildInvalidOperation("error init favor");
		}

		JSONObject out = new JMetadata().toJSON();
		out.put("variants", favorDatabase.getSize());
		return build(out);
	}

	@RequestMapping(value = "variant")
	public ResponseEntity getVariant(HttpServletRequest request) throws RocksDBException {
		Service service = Service.getInstance();
		FavorDatabase favorDatabase = service.getDatabaseConnectService().getFavorDatabase();
		if (favorDatabase == null) {
			throw ExceptionBuilder.buildInvalidOperation("error init favor");
		}

		String sOrd = request.getParameter("ord");
		if (Strings.isNullOrEmpty(sOrd)) {
			throw ExceptionBuilder.buildInvalidValueException("ord");
		}
		int ord;
		try {
			ord = Integer.parseInt(sOrd);
		} catch (Throwable e) {
			throw ExceptionBuilder.buildInvalidValueException("ord");
		}

		String out = favorDatabase.getRecord(ord);
		if (out == null) {
			throw ExceptionBuilder.buildInvalidValueException("ord");
		}

		return build(out);
	}

	@RequestMapping(value = "variants")
	public ResponseEntity getVariants(HttpServletRequest request) throws RocksDBException {
		Service service = Service.getInstance();
		FavorDatabase favorDatabase = service.getDatabaseConnectService().getFavorDatabase();
		if (favorDatabase == null) {
			throw ExceptionBuilder.buildInvalidOperation("error init favor");
		}

		String sOrds = request.getParameter("seq");
		if (Strings.isNullOrEmpty(sOrds)) {
			throw ExceptionBuilder.buildInvalidValueException("seq");
		}
		int[] ords;
		try {
			JSONArray dataPostVariables = (JSONArray) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(sOrds);
			ords = dataPostVariables.stream().map(o -> (Number) o).mapToInt(value -> value.intValue()).toArray();
		} catch (Throwable e) {
			log.warn("Exception parse request", e);
			throw ExceptionBuilder.buildInvalidValueException("seq");
		}

		StringJoiner out = new StringJoiner(",", "[", "]");
		for (int ord: ords) {
			String record = favorDatabase.getRecord(ord);
			if (record == null) {
				throw ExceptionBuilder.buildInvalidValueException("seq: " + ord);
			}
			out.add(record);
		}

		return build(out.toString());
	}

	@RequestMapping(value = "titles")
	public ResponseEntity getTitles(HttpServletRequest request) throws RocksDBException, ParseException {
		Service service = Service.getInstance();
		FavorDatabase favorDatabase = service.getDatabaseConnectService().getFavorDatabase();
		if (favorDatabase == null) {
			throw ExceptionBuilder.buildInvalidOperation("error init favor");
		}

		String sOrds = request.getParameter("seq");
		if (Strings.isNullOrEmpty(sOrds)) {
			throw ExceptionBuilder.buildInvalidValueException("seq");
		}
		int[] ords;
		try {
			JSONArray dataPostVariables = (JSONArray) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(sOrds);
			ords = dataPostVariables.stream().map(o -> (Number) o).mapToInt(value -> value.intValue()).toArray();
		} catch (Throwable e) {
			log.warn("Exception parse request", e);
			throw ExceptionBuilder.buildInvalidValueException("seq");
		}

		JSONArray out = new JSONArray();
		for(int ord: ords) {
			String record = favorDatabase.getRecord(ord);
			if (record == null) {
				throw ExceptionBuilder.buildInvalidValueException("seq: " + ord);
			}

			JSONObject jRecord = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(record);
			String label = ((JSONObject)jRecord.get("__data")).getAsString("label");

			out.add(new JSONObject(){{
				put("no", ord);
				put("lb", label);
				put("cl", "grey");
			}});
		}

		return build(out.toString());
	}

	public static ResponseEntity build(JSONAware out) {
		return build(out.toString());
	}

	public static ResponseEntity build(String out) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		headers.setCacheControl("no-cache, no-store, must-revalidate");
		headers.setPragma("no-cache");
		headers.setExpires(0);

		return new ResponseEntity(out.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);
	}
}
