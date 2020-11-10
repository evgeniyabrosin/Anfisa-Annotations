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

package org.forome.annotation.data.gnomad.datasource.http;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.forome.annotation.data.anfisa.struct.AnfisaExecuteContext;
import org.forome.annotation.data.gnomad.datasource.GnomadDataSource;
import org.forome.annotation.data.gnomad.utils.GnomadUtils;
import org.forome.annotation.data.gnomad.utils.СollapseNucleotideSequence;
import org.forome.annotation.exception.AnnotatorException;
import org.forome.annotation.service.source.DataSource;
import org.forome.annotation.service.source.tmp.GnomadDataResponse;
import org.forome.annotation.struct.SourceMetadata;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.utils.variant.MergeSequence;
import org.forome.astorage.core.liftover.LiftoverConnector;
import org.forome.core.struct.Assembly;
import org.forome.core.struct.Chromosome;
import org.forome.core.struct.Interval;
import org.forome.core.struct.Position;
import org.forome.core.struct.sequence.Sequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class GnomadDataSourceHttp implements GnomadDataSource {

	private final static Logger log = LoggerFactory.getLogger(GnomadDataSourceHttp.class);

	private final LiftoverConnector liftoverConnector;
	private final DataSource dataSource;

	public GnomadDataSourceHttp(
			LiftoverConnector liftoverConnector,
			DataSource dataSource
	) {
		this.liftoverConnector = liftoverConnector;
		this.dataSource = dataSource;
	}

	@Override
	public List<GnomadDataResponse> getData(
			AnfisaExecuteContext context,
			Assembly assembly,
			Variant variant,
			Chromosome chromosome,
			int sPosition,
			String sRef,
			String sAlt,
			String fromWhat
	) {
		СollapseNucleotideSequence.Sequence sequence = СollapseNucleotideSequence.collapseRight(
				new Position(chromosome, sPosition), sRef, sAlt
		);
		Position pos37 = liftoverConnector.toHG37(assembly, sequence.position);

		boolean isSNV = (sequence.ref.length() == 1 && sequence.alt.length() == 1);

		if (pos37 == null) {
			if (assembly == Assembly.GRCh38) {
				return tryFindRefertData(context, variant, fromWhat);
			} else {
				return Collections.emptyList();
			}
		}

		List<JSONObject> records = getRecord(
				pos37,
				sequence.ref, sequence.alt,
				fromWhat, isSNV
		);
		if (records.isEmpty() && assembly == Assembly.GRCh38) {
			List<GnomadDataResponse> resultTryFindRefertData = tryFindRefertData(context, variant, fromWhat);
			if (!resultTryFindRefertData.isEmpty()) {
				return resultTryFindRefertData;
			}
		}

		if (records.isEmpty() && !isSNV) {
			pos37 = new Position(pos37.chromosome, pos37.value - 1);
			records = getRecord(
					pos37,
					sequence.ref, sequence.alt,
					fromWhat, isSNV
			);
			if (records.isEmpty() && assembly == Assembly.GRCh38) {
				List<GnomadDataResponse> resultTryFindRefertData = tryFindRefertData(context, variant, fromWhat);
				if (!resultTryFindRefertData.isEmpty()) {
					return resultTryFindRefertData;
				}
			}
		}

		List<GnomadDataResponse> dataResponses = new ArrayList<>();
		for (JSONObject record : records) {
			String diff_ref_alt = GnomadUtils.diff(sequence.ref, sequence.alt);
			if (Objects.equals(diff_ref_alt, GnomadUtils.diff(record.getAsString("REF"), record.getAsString("ALT")))
					||
					GnomadUtils.diff3(record.getAsString("REF"), record.getAsString("ALT"), diff_ref_alt)
			) {
				dataResponses.add(build(pos37, record));
			}
		}
		return dataResponses;
	}

	private List<JSONObject> getRecord(
			Position pos37,
			String ref,
			String alt,
			String fromWhat,
			boolean isSNV
	) {
		List<JSONObject> jRecords = getData(pos37);
		if (jRecords == null) {
			return Collections.emptyList();
		}

		List<JSONObject> records;
		if (isSNV) {
			records = jRecords.stream()
					.filter(item ->
							item.getAsString("REF").equals(ref) && item.getAsString("ALT").equals(alt)
					).collect(Collectors.toList());
		} else {
			records = jRecords.stream()
					.filter(item ->
							item.getAsString("REF").contains(ref) && item.getAsString("ALT").contains(alt)
					).collect(Collectors.toList());
		}

		if (fromWhat != null) {
			if (!(fromWhat.equals("e") || fromWhat.equals("g"))) {
				throw new RuntimeException("Not support many fromWhat");
			}
			records = records.stream()
					.filter(item -> item.getAsString("SOURCE").equals(fromWhat))
					.collect(Collectors.toList());
		}

		return records;
	}


	/**
	 * Эта особенность ищется только в том с лучае, если мы обрабатываем hg38 вариант
	 * <p>
	 * Пытаемся обработать следующую ситеацию (на примере chr2:73448098-73448100 TCTC>T (hg38))
	 * Есть варианты, который различаются у половины людей:
	 * GGCTGTAAGTTCTC___TAGAAACTACTACTGGTC    (А)
	 * GGCTGTAAGTTCTCCTCTAGAAACTACTACTGGTC    (Б)
	 * <p>
	 * Всемье, у мамы (А), а у папы (Б). Когда мы обрабатываем по 19-й сборке, то находится вариант у папы (у мамы референс).
	 * Этот, папин, вариант находится в gnomAD, где написано, что он встречается у половины людей, и, соответственно,
	 * мы исключаем его из рассморения(понимаем, что он не вреден).
	 * <p>
	 * Когда мы обрабатываем в 38-й сборке, то определяется мамин вариант, потому что, то что у папы стало стандартом (референсом).
	 * Беда в том, что, поскольку gnomAD построен на 19-й сборке, маминого варианта мы не находим, поскольку он совпадал с 19-м стандартом.
	 * Поэтому, мы не знаем, что мамин вариант не редкий, а есть у половины людей.
	 * Соответственно можем подумать, что мамин вариант возможно опасен
	 *
	 * @return
	 */
	private List<GnomadDataResponse> tryFindRefertData(
			AnfisaExecuteContext context, Variant variant, String fromWhat
	) {
		Assembly assembly = context.anfisaInput.mCase.assembly;
		if (assembly != Assembly.GRCh38) throw new IllegalArgumentException();
		Chromosome chromosome = variant.chromosome;

		Sequence sequence38 = dataSource.getSource(Assembly.GRCh38).getFastaSequence(
				Interval.of(variant.chromosome,
						variant.getStart() - 1,
						variant.getStart() + Math.max(variant.getRef().length(), variant.getStrAlt().length()) + 2
				)
		);

		String mergeSequence;
		try {
			mergeSequence = new MergeSequence(sequence38).merge(variant);
		} catch (AnnotatorException ex) {
			log.error("Exception build mergeSequence: {}", ex.toString());
			return Collections.emptyList();
		}

		Position sequence19Start = liftoverConnector.toHG19(new Position(chromosome, sequence38.interval.start));
		Position sequence19End = liftoverConnector.toHG19(new Position(chromosome, sequence38.interval.end));
		if (sequence19Start == null || sequence19End == null || sequence19Start.value > sequence19End.value) {
			return Collections.emptyList();
		}
		if (Math.abs(sequence19End.value - sequence19Start.value) > 1000) {
			return Collections.emptyList();
		}

		Sequence sequence19 = dataSource.getSource(Assembly.GRCh37).getFastaSequence(
				Interval.of(chromosome, sequence19Start.value, sequence19End.value)
		);
		if (sequence19 == null) {
			return Collections.emptyList();
		}

		//Проверяем, что при наложеная мутация на ref (hg38) мы получим ref (hg19) - обязательное услови
		if (!sequence19.getValue().equals(mergeSequence)) {
			return Collections.emptyList();
		}

		for (int pos = sequence19Start.value; pos <= sequence19End.value; pos++) {
			Position iPosition = new Position(chromosome, pos);
			List<JSONObject> jRecords = getRecord(
					iPosition,
					variant.getStrAlt(),
					variant.getRef(),
					fromWhat,
					false
			);
			if (!jRecords.isEmpty()) {
				return jRecords.stream()
						.map(jsonObject -> buildRevert(iPosition, jsonObject))
						.collect(Collectors.toList());
			}
		}

		return Collections.emptyList();
	}


	private List<JSONObject> getData(Position pos37) {
		JSONArray jRecords = dataSource.getSource(Assembly.GRCh37).getGnomad(pos37);
		if (jRecords == null) {
			return null;
		}
		return jRecords.stream()
				.map(o -> (JSONObject) o)
				.collect(Collectors.toList());
	}

	@Override
	public List<SourceMetadata> getSourceMetadata() {
		return Collections.emptyList();
	}

	@Override
	public void close() {

	}

	private static GnomadDataResponse build(Position pos37, JSONObject record) {
		Map<String, Object> columns = new HashMap<>();

		columns.put("CHROM", pos37.chromosome.getChar());
		columns.put("POS", pos37.value);
		columns.put("REF", record.get("REF"));
		columns.put("ALT", record.get("ALT"));

		columns.put("AC", record.get("AC"));
		columns.put("AF", record.get("AF"));
		columns.put("AN", record.get("AN"));

		addGroup(columns, record, "oth");
		addGroup(columns, record, "amr");
		addGroup(columns, record, "raw");
		addGroup(columns, record, "fin");
		addGroup(columns, record, "afr");
		addGroup(columns, record, "nfe");
		addGroup(columns, record, "eas");
		addGroup(columns, record, "asj");
		addGroup(columns, record, "female");
		addGroup(columns, record, "male");

		columns.put("nhomalt", record.get("nhomalt"));
		columns.put("hem", record.get("hem"));

		return new GnomadDataResponse(columns);
	}

	private static void addGroup(Map<String, Object> columns, JSONObject record, String group) {
		JSONObject jGroup = (JSONObject) record.get(group);
		if (jGroup == null) return;
		columns.put("AC_" + group, jGroup.get("AC"));
		columns.put("AF_" + group, jGroup.get("AF"));
		columns.put("AN_" + group, jGroup.get("AN"));
	}

	private static GnomadDataResponse buildRevert(Position pos37, JSONObject record) {
		Map<String, Object> columns = new HashMap<>();

		columns.put("CHROM", pos37.chromosome.getChar());
		columns.put("POS", pos37.value);
		columns.put("REF", record.get("REF"));
		columns.put("ALT", record.get("ALT"));

		Number nAN = record.getAsNumber("AN");
		columns.put("AN", nAN);

		Number nAC = record.getAsNumber("AC");
		if (nAN != null && nAC != null) {
			columns.put("AC", nAN.longValue() - nAC.longValue());
		} else {
			columns.put("AC", null);
		}

		Number nAF = record.getAsNumber("AF");
		if (nAF != null) {
			columns.put("AF", 1 - nAF.doubleValue());
		} else {
			columns.put("AF", null);
		}

		addGroupRevert(columns, record, "oth");
		addGroupRevert(columns, record, "amr");
		addGroupRevert(columns, record, "raw");
		addGroupRevert(columns, record, "fin");
		addGroupRevert(columns, record, "afr");
		addGroupRevert(columns, record, "nfe");
		addGroupRevert(columns, record, "eas");
		addGroupRevert(columns, record, "asj");
		addGroupRevert(columns, record, "female");
		addGroupRevert(columns, record, "male");

		columns.put("nhomalt", null);
		columns.put("hem", null);

		return new GnomadDataResponse(columns);
	}

	private static void addGroupRevert(Map<String, Object> columns, JSONObject record, String group) {
		JSONObject jGroup = (JSONObject) record.get(group);
		if (jGroup == null) return;

		Number nAN = jGroup.getAsNumber("AN");
		columns.put("AN_" + group, nAN);

		Number nAC = jGroup.getAsNumber("AC");
		if (nAN != null && nAC != null) {
			columns.put("AC_" + group, nAN.longValue() - nAC.longValue());
		} else {
			columns.put("AC_" + group, null);
		}

		Number nAF = jGroup.getAsNumber("AF");
		if (nAF != null) {
			columns.put("AF_" + group, 1 - nAF.doubleValue());
		} else {
			columns.put("AF_" + group, null);
		}
	}

}
