/*
 *  Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association
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

package org.forome.annotation.config.database;

import net.minidev.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DatabaseConfig {

	private final static String FIELD_PATH_HG37 = "hg37";
	private final static String FIELD_PATH_HG38 = "hg38";
	private final static String FIELD_PATH_FAVOR = "favor";

	public final Path hg37;
	public final Path hg38;

	public final Path favor;

	public DatabaseConfig(JSONObject parse) {
		if (parse.containsKey(FIELD_PATH_HG37)) {
			this.hg37 = Paths.get(parse.getAsString(FIELD_PATH_HG37)).toAbsolutePath();
			if (!Files.exists(hg37) || !Files.isDirectory(hg37)) {
				throw new RuntimeException("Exception database path hg37: " + hg37);
			}
		} else {
			this.hg37 = null;
		}

		if (parse.containsKey(FIELD_PATH_HG38)) {
			this.hg38 = Paths.get(parse.getAsString(FIELD_PATH_HG38)).toAbsolutePath();
			if (!Files.exists(hg38) || !Files.isDirectory(hg38)) {
				throw new RuntimeException("Exception database path hg38: " + hg38);
			}
		} else {
			this.hg38 = null;
		}

		if (parse.containsKey(FIELD_PATH_FAVOR)) {
			this.favor = Paths.get(parse.getAsString(FIELD_PATH_FAVOR)).toAbsolutePath();
			if (!Files.exists(favor) || !Files.isDirectory(favor)) {
				throw new RuntimeException("Exception database path favor: " + favor);
			}
		} else {
			this.favor = null;
		}
	}
}
