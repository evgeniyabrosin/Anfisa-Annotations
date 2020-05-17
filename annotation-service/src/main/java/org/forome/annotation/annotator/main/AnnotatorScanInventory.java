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

package org.forome.annotation.annotator.main;

import org.forome.annotation.annotator.AnnotationConsole;
import org.forome.annotation.annotator.main.argument.ArgumentsScanInventory;
import org.forome.annotation.annotator.main.argument.ParserArgument;
import org.forome.annotation.inventory.Inventory;
import org.forome.annotation.logback.LogbackConfigure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AnnotatorScanInventory {

	public static void main(String[] args) {
		ArgumentsScanInventory arguments;
		try {
			ParserArgument argumentParser = new ParserArgument(args);
			arguments = (ArgumentsScanInventory) argumentParser.arguments;
		} catch (Throwable e) {
			getLazyLogger().error("Exception arguments parser", e);
			System.exit(2);
			return;
		}

		Inventory inventory;
		try {
			Path pathInventory = null;
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(
					Paths.get("."),
					file -> Inventory.PATTERN_FILE_INVENTORY.matcher(file.getFileName().toString()).matches())
			) {
				for (Path entry : stream) {
					if (pathInventory != null) {
						throw new RuntimeException("Uncertain situation, many file inventory");
					}
					pathInventory = entry;
				}
			}
			inventory = new Inventory.Builder(pathInventory).build();
		} catch (Throwable e) {
			getLazyLogger().error("Exception build inventory", e);
			System.exit(3);
			return;
		}

		if (inventory.logFile != null) {
			LogbackConfigure.setLogPath(inventory.logFile);
		}

		AnnotationConsole annotationConsole = new AnnotationConsole(
				arguments.config,
				inventory.caseName,
				inventory.assembly, inventory.casePlatform,
				inventory.famFile, inventory.patientIdsFile,
				inventory.cohortsFile,
				inventory.vcfFile, inventory.vepJsonFile,
				inventory.cnvFile,
				0,
				inventory.outFile,
				() -> arguments.getArguments()
		);
		annotationConsole.execute();
	}

	private static Logger getLazyLogger() {
		return LoggerFactory.getLogger(AnnotatorScanInventory.class);
	}
}
