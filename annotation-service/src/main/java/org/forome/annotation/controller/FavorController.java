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
import net.minidev.json.JSONObject;
import org.forome.annotation.Main;
import org.forome.annotation.controller.utils.ResponseBuilder;
import org.forome.annotation.exception.ExceptionBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;


@Controller
@RequestMapping(value = { "/favor" })
public class FavorController {

	//	private static final Path RESULT_PATH = Paths.get("/mnt/data/favor/anfisa");
	private static final Path RESULT_PATH = Paths.get("/data/tmp/favor");

	@RequestMapping(value = "info")
	public ResponseEntity getInfo(HttpServletRequest request) {
		long countFile;
		try (Stream<Path> files = Files.list(RESULT_PATH)) {
			countFile = files.count();
		} catch (IOException e) {
			Main.crash(e);
			return null;
		}

		JSONObject out = new JSONObject();
		out.put("size", countFile);
		return ResponseBuilder.build(out);
	}

	@RequestMapping(value = "variant")
	public ResponseEntity getVariant(HttpServletRequest request) {
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

		Path file;
		if (ord == 0) {
			file = RESULT_PATH.resolve("favor_anfisa.json.gz");
		} else {
			file = RESULT_PATH.resolve("favor_anfisa." + ord + ".json.gz");
		}

		if (!Files.exists(file)) {
			throw ExceptionBuilder.buildInvalidValueException("ord");
		}

		JSONObject out = new JSONObject();
		out.put("path", file.toAbsolutePath().toString());
		return ResponseBuilder.build(out);
	}
}
