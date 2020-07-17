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
import net.minidev.json.parser.JSONParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.util.EntityUtils;
import org.forome.annotation.config.connector.base.AStorageConfigConnector;
import org.forome.annotation.data.anfisa.struct.AnfisaExecuteContext;
import org.forome.annotation.data.fasta.FastaSource;
import org.forome.annotation.data.gnomad.datasource.GnomadDataSource;
import org.forome.annotation.data.gnomad.struct.DataResponse;
import org.forome.annotation.data.gnomad.utils.GnomadUtils;
import org.forome.annotation.data.gnomad.utils.СollapseNucleotideSequence;
import org.forome.annotation.data.liftover.LiftoverConnector;
import org.forome.annotation.exception.AnnotatorException;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.struct.*;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.utils.variant.MergeSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * curl "localhost:8290/get?array=hg19&loc=18:67760501"
 * {"chrom": "chr18", "array": "hg19", "pos": 67760501, "Gerp": {"GerpN": 2.45, "GerpRS": -1.87}, "gnomAD": [{"ALT": "C", "REF": "A", "SOURCE": "g", "AC": 2, "AN": 31248, "AF": 6.4e-05, "nhomalt": 0, "faf95": 1.06e-05, "faf99": 1.096e-05, "male": {"AC": 1, "AN": 17400, "AF": 5.747e-05}, "female": {"AC": 1, "AN": 13848, "AF": 7.221e-05}, "afr": {"AC": 2, "AN": 8692, "AF": 0.0002301}, "amr": {"AC": 0, "AN": 842, "AF": 0}, "asj": {"AC": 0, "AN": 290, "AF": 0}, "eas": {"AC": 0, "AN": 1560, "AF": 0}, "fin": {"AC": 0, "AN": 3408, "AF": 0}, "nfe": {"AC": 0, "AN": 15380, "AF": 0}, "oth": {"AC": 0, "AN": 1076, "AF": 0}, "raw": {"AC": 2, "AN": 31416, "AF": 6.366e-05}, "hem": null}]}
 */
public class GnomadDataSourceHttp implements GnomadDataSource {

	private final static Logger log = LoggerFactory.getLogger(GnomadDataSourceHttp.class);

	private final LiftoverConnector liftoverConnector;
	private final FastaSource fastaSource;

	private final DatabaseConnectService.AStorage aStorage;

	private final RequestConfig requestConfig;
	private final PoolingNHttpClientConnectionManager connectionManager;
	private final HttpHost httpHost;

	public GnomadDataSourceHttp(
			DatabaseConnectService databaseConnectService,
			LiftoverConnector liftoverConnector,
			FastaSource fastaSource,
			AStorageConfigConnector aStorageConfigConnector
	) throws IOReactorException {
		this.liftoverConnector = liftoverConnector;
		this.fastaSource = fastaSource;

		aStorage = databaseConnectService.getAStorage(aStorageConfigConnector);

		requestConfig = RequestConfig.custom()
				.setConnectTimeout(5000)//Таймаут на подключение
				.setSocketTimeout(10 * 60 * 1000)//Таймаут между пакетами
				.setConnectionRequestTimeout(10 * 60 * 1000)//Таймаут на ответ
				.build();

		connectionManager = new PoolingNHttpClientConnectionManager(new DefaultConnectingIOReactor());
		connectionManager.setMaxTotal(100);
		connectionManager.setDefaultMaxPerRoute(100);

		httpHost = new HttpHost(aStorage.host, aStorage.port, "http");
	}

	@Override
	public List<DataResponse> getData(
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
				context,
				pos37,
				sequence.ref, sequence.alt,
				fromWhat, isSNV
		);

		if (records.isEmpty() && !isSNV) {
			pos37 = new Position(pos37.chromosome, pos37.value - 1);
			records = getRecord(
					context,
					pos37,
					sequence.ref, sequence.alt,
					fromWhat, isSNV
			);
		}

		List<DataResponse> dataResponses = new ArrayList<>();
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
			AnfisaExecuteContext context,
			Position pos37,
			String ref,
			String alt,
			String fromWhat,
			boolean isSNV
	) {
		List<JSONObject> jRecords = getData(context, pos37);
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
	private List<DataResponse> tryFindRefertData(
			AnfisaExecuteContext context, Variant variant, String fromWhat
	) {
		Assembly assembly = context.anfisaInput.mCase.assembly;
		if (assembly != Assembly.GRCh38) throw new IllegalArgumentException();
		Chromosome chromosome = variant.chromosome;

		Sequence sequence38 = fastaSource.getSequence(
				Assembly.GRCh38,
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
		if (sequence19Start == null || sequence19End == null || sequence19Start.value > sequence19End.value ) {
			return Collections.emptyList();
		}

		Sequence sequence19 = fastaSource.getSequence(
				Assembly.GRCh37,
				Interval.of(chromosome, sequence19Start.value, sequence19End.value)
		);

		//Проверяем, что при наложеная мутация на ref (hg38) мы получим ref (hg19) - обязательное услови
		if (!sequence19.value.equals(mergeSequence)) {
			return Collections.emptyList();
		}

		for (int pos = sequence19Start.value; pos <= sequence19End.value; pos++) {
			Position iPosition = new Position(chromosome, pos);
			List<JSONObject> jRecords = getRecord(
					context,
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


	private List<JSONObject> getData(AnfisaExecuteContext context, Position pos37) {
		JSONObject sourceAStorageHttp = context.sourceAStorageHttp;
		Assembly assembly = context.anfisaInput.mCase.assembly;
		Number sourcePos37;
		switch (assembly) {
			case GRCh37:
				sourcePos37 = sourceAStorageHttp.getAsNumber("pos");
				break;
			case GRCh38:
				sourcePos37 = sourceAStorageHttp.getAsNumber("hg19");
				break;
			default:
				throw new RuntimeException("Unknown assembly: " + assembly);
		}

		JSONArray jRecords;
		if (sourcePos37 != null && sourcePos37.intValue() == pos37.value) {
			jRecords = (JSONArray) sourceAStorageHttp.get("gnomAD");
		} else {
			JSONObject response = request(
					String.format("http://%s:%s/get?array=hg19&loc=%s:%s", aStorage.host, aStorage.port, pos37.chromosome.getChar(), pos37.value)
			);
			jRecords = (JSONArray) response.get("gnomAD");
		}
		if (jRecords == null) {
			return null;
		}
		return jRecords.stream()
				.map(o -> (JSONObject) o)
				.collect(Collectors.toList());

	}

	private JSONObject request(String url) {
		CompletableFuture<JSONObject> future = new CompletableFuture<>();
		try {
			CloseableHttpAsyncClient httpclient = HttpAsyncClients.custom()
					.setDefaultRequestConfig(requestConfig)
					.build();
			httpclient.start();

			HttpPost httpPostRequest = new HttpPost(new URI(url));

			httpclient.execute(httpHost, httpPostRequest, new FutureCallback<HttpResponse>() {
				@Override
				public void completed(HttpResponse response) {
					try {
						HttpEntity entity = response.getEntity();
						String entityBody = EntityUtils.toString(entity);

						Object rawResponse;
						try {
							rawResponse = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(entityBody);
						} catch (Exception e) {
							throw ExceptionBuilder.buildExternalServiceException(new RuntimeException("Exception parse response external service, response: " + entityBody));
						}
						if (rawResponse instanceof JSONObject) {
							future.complete((JSONObject) rawResponse);
						} else {
							throw ExceptionBuilder.buildExternalServiceException(
									new RuntimeException("Exception external service(AStorage), response: " + entityBody),
									"AStorage", "Response: " + entityBody
							);
						}
					} catch (Throwable ex) {
						future.completeExceptionally(ex);
					}

					try {
						httpclient.close();
					} catch (IOException ignore) {
						log.error("Exception close connect");
					}
				}

				@Override
				public void failed(Exception ex) {
					future.completeExceptionally(ex);
					try {
						httpclient.close();
					} catch (IOException ignore) {
						log.error("Exception close connect");
					}
				}

				@Override
				public void cancelled() {
					future.cancel(true);
					try {
						httpclient.close();
					} catch (IOException ignore) {
						log.error("Exception close connect");
					}
				}
			});
		} catch (Throwable t) {
			log.error("Exception close connect", t);
			future.completeExceptionally(t);
		}

		try {
			return future.get();
		} catch (InterruptedException e) {
			throw new RuntimeException();
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	@Override
	public List<SourceMetadata> getSourceMetadata() {
		return Collections.emptyList();
	}

	@Override
	public void close() {

	}

	private static DataResponse build(Position pos37, JSONObject record) {
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

		return new DataResponse(columns);
	}

	private static void addGroup(Map<String, Object> columns, JSONObject record, String group) {
		JSONObject jGroup = (JSONObject) record.get(group);
		if (jGroup == null) return;
		columns.put("AC_" + group, jGroup.get("AC"));
		columns.put("AF_" + group, jGroup.get("AF"));
		columns.put("AN_" + group, jGroup.get("AN"));
	}

	private static DataResponse buildRevert(Position pos37, JSONObject record) {
		Map<String, Object> columns = new HashMap<>();

		columns.put("CHROM", pos37.chromosome.getChar());
		columns.put("POS", pos37.value);
		columns.put("REF", record.get("REF"));
		columns.put("ALT", record.get("ALT"));

		columns.put("AN", record.get("AN"));
		columns.put("AC", record.getAsNumber("AN").longValue() - record.getAsNumber("AC").longValue());
		columns.put("AF", 1 - record.getAsNumber("AF").doubleValue());

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

		return new DataResponse(columns);
	}

	private static void addGroupRevert(Map<String, Object> columns, JSONObject record, String group) {
		JSONObject jGroup = (JSONObject) record.get(group);
		if (jGroup == null) return;
		columns.put("AN_" + group, jGroup.get("AN"));
		columns.put("AC_" + group, jGroup.getAsNumber("AN").longValue() - jGroup.getAsNumber("AC").longValue());
		columns.put("AF_" + group, 1 - record.getAsNumber("AF").doubleValue());
	}

}
