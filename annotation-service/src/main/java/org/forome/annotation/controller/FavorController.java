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

import net.minidev.json.JSONObject;
import org.forome.annotation.controller.utils.ResponseBuilder;
import org.forome.annotation.exception.ExceptionBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@Controller
@RequestMapping(value = { "/favor" })
public class FavorController {

	@RequestMapping(value = "info")
	public ResponseEntity getInfo(HttpServletRequest request) {
		JSONObject out = new JSONObject();
		out.put("packs", 3);
		return ResponseBuilder.build(out);
	}

	@RequestMapping(value = "pack/{id}")
	public ResponseEntity getPack(@PathVariable("id") int id) {
		Path parent = Paths.get("/mnt/data/favor/anfisa");
		Path file;
		if (id == 0) {
			file = parent.resolve("favor_anfisa.json.gz");
		} else {
			file = parent.resolve("favor_anfisa." + id + ".json.gz");
		}

		if (!Files.exists(file)) {
			throw ExceptionBuilder.buildInvalidValueException("id");
		}

		JSONObject out = new JSONObject();
		out.put("path", file.toAbsolutePath().toString());
		return ResponseBuilder.build(out);
	}
}
