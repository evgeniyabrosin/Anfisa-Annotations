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

package org.forome.annotation.utils.vcf.merge.struct;

import java.nio.file.Files;
import java.nio.file.Path;

public class BgzipFile {

	public final Path file;
	public final Path fileIndex;

	public BgzipFile(Path file, Path fileIndex) {
		if (!Files.exists(file)) {
			throw new RuntimeException("File is not exist: " + file);
		}
		if (!Files.exists(fileIndex)) {
			throw new RuntimeException("File is not exist: " + fileIndex);
		}

		this.file = file;
		this.fileIndex = fileIndex;
	}
}
