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

package org.forome.annotation.annotator.recovery;

import net.minidev.json.JSONObject;
import org.forome.annotation.exception.AnnotatorException;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.iterator.json.JsonFileIterator;
import org.forome.annotation.iterator.vcf.VCFFileIterator;
import org.forome.annotation.processing.smavariant.SplitMAVariant;
import org.forome.annotation.struct.mavariant.MAVariant;
import org.forome.annotation.struct.variant.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class Recovery {

	private final static Logger log = LoggerFactory.getLogger(Recovery.class);

	private final Path vcfFile;
	private final Path recoveryAnfisaJson;

	public Recovery(Path vcfFile, Path recoveryAnfisaJson) {
		this.vcfFile = vcfFile;
		this.recoveryAnfisaJson = recoveryAnfisaJson;
	}

	public RecoveryResult execute(BufferedOutputStream bos) throws IOException {
		log.debug("Run recovery mode...");

		int offset = 0;
		int countRecords = 0;
		try (
				VCFFileIterator vcfFileIterator = new VCFFileIterator(vcfFile);
				InputStream isRecoveryAnfisaJson = Files.newInputStream(recoveryAnfisaJson);
				JsonFileIterator recoveryJsonIterator = new JsonFileIterator(isRecoveryAnfisaJson, recoveryAnfisaJson.getFileName().toString().contains(".gz_"))
		) {

			//Проверяем, что первая строка, это матаданные
			JSONObject jMetadata = recoveryJsonIterator.next();
			if (!jMetadata.getAsString("record_type").equals("metadata")) {
				throw new RuntimeException("Bad recovery file, need first line is metadata");
			}

			while (true) {
				MAVariant maVariant = vcfFileIterator.next();

				List<JSONObject> jRecoveryRecords = new ArrayList<>();

				for (Variant variant : SplitMAVariant.split(maVariant)) {
					JSONObject jRecoveryRecord = recoveryJsonIterator.next();

					//Валидируем
					JSONObject jRecoveryRecordFilters = (JSONObject) jRecoveryRecord.get("_filters");
					if (jRecoveryRecordFilters.getAsNumber("end").intValue() != variant.end) {
						throw new RuntimeException("Bad recovery file, not equals end position. " + variant.toString());
					}

					jRecoveryRecords.add(jRecoveryRecord);
				}

				//Заливаем данные
				for (JSONObject jRecoveryRecord : jRecoveryRecords) {
					bos.write(jRecoveryRecord.toJSONString().getBytes(StandardCharsets.UTF_8));
					bos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
				}
				offset++;
				countRecords +=jRecoveryRecords.size();
			}
		} catch (NoSuchElementException nee) {
			log.debug("Vcf file is end");
		} catch (AnnotatorException ae) {
			if (ExceptionBuilder.CODE_IO_ERROR.equals(ae.getCode())) {
				IOException ioException = (IOException) ae.getCause();
				if (ioException instanceof EOFException) {
					log.debug("Eof file " + ioException.getMessage());
				} else {
					throw new RuntimeException(ae);
				}
			} else {
				throw new RuntimeException(ae);
			}
		}

		log.debug("Run recovery mode...complete.");
		log.debug("Recovery variants: " + offset);
		log.debug("Recovery records: " + countRecords);
		return new RecoveryResult(
				offset, countRecords
		);
	}
}
