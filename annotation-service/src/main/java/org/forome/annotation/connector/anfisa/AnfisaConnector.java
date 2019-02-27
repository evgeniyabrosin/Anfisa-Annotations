package org.forome.annotation.connector.anfisa;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.forome.annotation.connector.anfisa.struct.*;
import org.forome.annotation.connector.beacon.BeaconConnector;
import org.forome.annotation.connector.gnomad.GnomadConnector;
import org.forome.annotation.connector.gnomad.struct.GnomadResult;
import org.forome.annotation.connector.hgmd.HgmdConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.forome.annotation.connector.clinvar.ClinvarConnector;
import org.forome.annotation.connector.clinvar.struct.ClinvarResult;
import org.forome.annotation.connector.liftover.LiftoverConnector;
import org.forome.annotation.controller.utils.RequestParser;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.exception.ServiceException;
import org.forome.annotation.struct.Variant;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class AnfisaConnector implements Closeable {

	private final static Logger log = LoggerFactory.getLogger(AnfisaConnector.class);

	private final Map<String, String> trustedSubmitters = new HashMap<String, String>() {{
		put("lmm", "Laboratory for Molecular Medicine,Partners HealthCare Personalized Medicine");
		put("gene_dx", "GeneDx");
	}};

	private final ImmutableList<String> csq_damaging = ImmutableList.of(
			"transcript_ablation", "splice_acceptor_variant", "splice_donor_variant", "stop_gained", "frameshift_variant",
			"stop_lost", "start_lost", "transcript_amplification", "inframe_insertion", "inframe_deletion"
	);

	private final ImmutableList<String> csq_missense = ImmutableList.of("missense_variant");

	private final GnomadConnector gnomadConnector;
	private final HgmdConnector hgmdConnector;
	private final ClinvarConnector clinvarConnector;
	private final LiftoverConnector liftoverConnector;

	private final AnfisaHttpClient anfisaHttpClient;

	public AnfisaConnector(
			GnomadConnector gnomadConnector,
			HgmdConnector hgmdConnector,
			ClinvarConnector clinvarConnector,
			LiftoverConnector liftoverConnector
	) throws IOException {
		this.gnomadConnector = gnomadConnector;
		this.hgmdConnector = hgmdConnector;
		this.clinvarConnector = clinvarConnector;
		this.liftoverConnector = liftoverConnector;
		this.anfisaHttpClient = new AnfisaHttpClient();
	}

	public CompletableFuture<List<AnfisaResult>> request(String chromosome, long start, long end, String alternative) {
		String region = String.format("%s:%s:%s", chromosome, start, end);
		String endpoint = String.format("/vep/human/region/%s/%s", region, alternative);

		return anfisaHttpClient.request(endpoint).thenApply(body -> {
			Object rawResponse;
			try {
				rawResponse = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(body);
			} catch (Exception e) {
				throw new RuntimeException("Error parse response, endpoint: " + endpoint + " response: '" + body + "'", e);
			}

			if (rawResponse instanceof JSONArray) {
				JSONArray response = (JSONArray) rawResponse;
				List<AnfisaResult> result = new ArrayList<>();
				for (Object item : response) {
					result.add(build(chromosome, start, end, (JSONObject) item));
				}
				return result;
			} else if (rawResponse instanceof JSONObject && ((JSONObject) rawResponse).containsKey("error")) {
				String error = ((JSONObject) rawResponse).getAsString("error");
				log.error("Exception", ExceptionBuilder.buildExternalServiceException(new RuntimeException(error)));
				return Collections.emptyList();
			} else {
				throw new RuntimeException("Unknown response, endpoint: " + endpoint + " response: '" + body + "'");
			}
		});
	}

	private AnfisaResult build(String chromosome, long start, long end, JSONObject response) {
		Record record = new Record();

		AnfisaResultFilters filters = new AnfisaResultFilters();
		AnfisaResultData data = new AnfisaResultData();
		AnfisaResultView view = new AnfisaResultView();

		callGnomAD(response, filters);
		callHgmd(record, chromosome, start, end, filters, data);
		callClinvar(record, chromosome, start, filters, data, view);
		callBeacon(response, data);


		filters.minGq = getMinGQ();
		filters.probandGq = getProbandGQ();
		filters.severity = getSeverity(response);
		filters.has_variant = new String[0];


		List<Integer> d = getDistanceFromExon(response, "worst");
		filters.distFromExon = (d.isEmpty()) ? 0 : Collections.min(d);


		data.assemblyName = response.getAsString("assembly_name");
		data.end = response.getAsNumber("end").longValue();
		data.regulatoryFeatureConsequences = (JSONArray) response.get("regulatory_feature_consequences");
		data.motifFeatureConsequences = (JSONArray) response.get("motif_feature_consequences");
		data.intergenicConsequences = (JSONArray) response.get("intergenic_consequences");
		data.start = response.getAsNumber("start").longValue();
		data.mostSevereConsequence = response.getAsString("most_severe_consequence");
		data.alleleString = response.getAsString("allele_string");
		data.seqRegionName = response.getAsString("seq_region_name");
		data.colocatedVariants = (JSONArray) response.get("colocated_variants");
		data.input = response.getAsString("input");
		data.label = getLabel(response);
		data.transcriptConsequences = (JSONArray) response.get("transcript_consequences");
		data.id = response.getAsString("id");
		data.strand = (response.containsKey("strand")) ? response.getAsNumber("strand").longValue() : null;

		data.colorCode = getColorCode(response, data);

		createGeneralTab(response, data, view);
		createGnomadTab(response, filters, data, view);
		createDatabasesTab(response, record, data, view);
		createPredictionsTab(response, view);
		createBioinformaticsTab(response, data, view);

		return new AnfisaResult(filters, data, view);
	}

	private void callHgmd(Record record, String chromosome, long start, long end, AnfisaResultFilters filters, AnfisaResultData data) {
		List<String> accNums = hgmdConnector.getAccNum(chromosome, start, end);
		if (accNums.size() > 0) {
			HgmdConnector.Data hgmdData = hgmdConnector.getDataForAccessionNumbers(accNums);
			record.hgmdData = hgmdData;

			data.hgmd = String.join(",", accNums);
			List<String[]> hg38 = hgmdConnector.getHg38(accNums);
			data.hgmdHg38 = hg38.stream().map(strings -> String.format("%s-%s", strings[0], strings[1])).collect(Collectors.joining(", "));
			List<String> tags = hgmdData.hgmdPmidRows.stream().map(hgmdPmidRow -> hgmdPmidRow.tag).collect(Collectors.toList());
			filters.hgmdBenign = (tags.size() == 0);
		}
	}

	private void callClinvar(Record record, String chromosome, long start, AnfisaResultFilters filters, AnfisaResultData data, AnfisaResultView view) {
		List<ClinvarResult> clinvarResults;
		if (isSnv()) {
			throw new RuntimeException("Not implemented");
		} else {
			clinvarResults = clinvarConnector.getExpandedData(chromosome, start);
		}
		record.clinvarResults = clinvarResults;
		if (clinvarResults.isEmpty()) return;

		String[] variants = clinvarResults.stream().map(clinvarResult -> {
			return String.format("%s %s>%s",
					vstr(chromosome, clinvarResult.start, clinvarResult.end),
					clinvarResult.referenceAllele, clinvarResult.alternateAllele
			);
		}).toArray(String[]::new);

		List<String> significance = new ArrayList<>();
		Map<String, String> submissions = new HashMap<>();
		for (ClinvarResult clinvarResult : clinvarResults) {
			significance.addAll(Arrays.asList(clinvarResult.clinicalSignificance.split("/")));
			submissions.putAll(clinvarResult.submitters);
		}

		List<String> idList = clinvarResults.stream().flatMap(it -> {
			return Lists.newArrayList(it.phenotypeIDs, it.otherIDs).stream();
		}).collect(Collectors.toList());
		for (String id : idList) {
			if (id.indexOf(":") != -1) {
				continue;
			}
			//TODO not implemented
		}

		data.clinVar = clinvarResults.stream()
				.map(clinvarResult -> clinvarResult.variationID)
				.map(it -> Long.parseLong(it))
				.toArray(Long[]::new);
		data.clinvarSubmitters = new HashMap<String, String>() {{
			for (ClinvarResult clinvarResult : clinvarResults) {
				putAll(clinvarResult.submitters);
			}
		}};

		view.databases.clinVar = clinvarResults.stream()
				.map(clinvarResult -> clinvarResult.variationID)
				.map(it -> String.format("https://www.ncbi.nlm.nih.gov/clinvar/variation/%s/", it))
				.toArray(String[]::new);
		data.clinvarVariants = variants;
		view.databases.clinVarSubmitters = data.clinvarSubmitters.entrySet().stream().map(entry -> {
			return String.format("%s: %s", encodeToAscii(entry.getKey()), entry.getValue());
		}).toArray(String[]::new);
		data.clinvarSignificance = significance.toArray(new String[significance.size()]);
		data.clinvarPhenotypes = clinvarResults.stream()
				.map(clinvarResult -> clinvarResult.phenotypeList)
				.toArray(String[]::new);

		filters.clinvarBenign = (significance.stream().filter(s -> (s.toLowerCase().indexOf("benign") == -1)).count() == 0);

		Boolean benign = null;
		for (String submitter : trustedSubmitters.keySet()) {
			String fullName = trustedSubmitters.get(submitter);
			if (submissions.containsKey(fullName)) {
				String prediction = submissions.get(fullName).toLowerCase();
				data.setField(submitter, prediction);
				if (!prediction.contains("benign")) {
					benign = false;
				} else if (benign == null) {
					benign = true;
				}
			}
		}
		filters.clinvarTrustedBenign = Optional.ofNullable(benign);
	}

	private void callBeacon(JSONObject response, AnfisaResultData data) {
		List<String> alts = getAlts(response);
		data.beaconUrls = alts.stream()
				.map(alt ->
						BeaconConnector.getUrl(
								RequestParser.toChromosome(getStrChromosome(response)),
								Math.min(response.getAsNumber("start").longValue(), response.getAsNumber("end").longValue()),
								getRef(response), alt
						)
				)
				.toArray(String[]::new);
	}

	private void callGnomAD(JSONObject response, AnfisaResultFilters filters) {
		Double af = null;
		Double _af = null;
		Double emAf = null;
		Double emAfPb = null;
		Double gmAf = null;
		Double gmAfPb = null;
		Double _afPb = null;

		String popmax = null;
		Double popmaxAf = null;
		Long popmaxAn = null;

		for (String alt : getAlts(response)) {
			GnomadResult gnomadResult = getGnomadResult(response, alt);
			if (gnomadResult == GnomadResult.EMPTY) {
				continue;
			}
			if (gnomadResult.exomes != null) {
				af = gnomadResult.exomes.af;
				emAf = Math.min((emAf != null) ? emAf : af, af);
				if (isProbandHasAllele(alt)) {
					emAfPb = Math.min((emAfPb != null) ? emAfPb : af, af);
				}
			}
			if (gnomadResult.genomes != null) {
				af = gnomadResult.genomes.af;
				gmAf = Math.min((gmAf != null) ? gmAf : af, af);
				if (isProbandHasAllele(alt)) {
					gmAfPb = Math.min((gmAfPb != null) ? gmAfPb : af, af);
				}
			}

			af = gnomadResult.overall.af;
			if (isProbandHasAllele(alt)) {
				_afPb = Math.min((_afPb != null) ? _afPb : af, af);
			}

			if (_af == null || af < _af) {
				_af = af;
				popmax = gnomadResult.popmax;
				popmaxAf = gnomadResult.popmaxAf;
				popmaxAn = gnomadResult.popmaxAn;
			}
		}

		filters.gnomadDbExomesAf = emAf;
		filters.gnomadDbGenomesAf = gmAf;
		filters.gnomadAfFam = _af;
		filters.gnomadAfPb = _afPb;

		filters.gnomadPopmax = popmax;
		filters.gnomadPopmaxAf = popmaxAf;
		filters.gnomadPopmaxAn = popmaxAn;
	}

	private GnomadResult getGnomadResult(JSONObject response, String alt) {
		try {
			return gnomadConnector.request(
					RequestParser.toChromosome(getStrChromosome(response)),
					Math.min(response.getAsNumber("start").longValue(), response.getAsNumber("end").longValue()),
					getRef(response), alt
			).get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof ServiceException) {
				throw (ServiceException) cause;
			} else {
				throw new RuntimeException(cause);
			}
		}
	}


	private void createGeneralTab(JSONObject response, AnfisaResultData data, AnfisaResultView view) {
		view.general.worstAnnotation = data.mostSevereConsequence;
		view.general.ref = getRef(response);
		view.general.alt = altString(response);
		view.general.hg38 = getHg38Coordinates(response);
		view.general.hg19 = str(response);
		view.general.genes = getGenes(response).stream().toArray(String[]::new);
	}

	private void createGnomadTab(JSONObject response, AnfisaResultFilters filters, AnfisaResultData data, AnfisaResultView view) {
		Double gnomadAf = getGnomadAf(filters);
		if (gnomadAf != null && Math.abs(gnomadAf) > 0.000001D) {
			for (String allele : getAlts(response)) {
				GnomadResult gnomadResult = getGnomadResult(response, allele);

				AnfisaResultView.GnomAD gnomAD = new AnfisaResultView.GnomAD();
				gnomAD.allele = allele;

				if (gnomadResult.exomes != null) {
					gnomAD.exomeAn = gnomadResult.exomes.an;
					gnomAD.exomeAf = gnomadResult.exomes.af;
				}
				if (gnomadResult.genomes != null) {
					gnomAD.genomeAn = gnomadResult.genomes.an;
					gnomAD.genomeAf = gnomadResult.genomes.af;
				}

				gnomAD.proband = (isProbandHasAllele(allele)) ? "Yes" : "No";
				gnomAD.af = gnomadResult.overall.af;
				gnomAD.popMax = String.format("%s: %s [%s]", gnomadResult.popmax, gnomadResult.popmaxAf, gnomadResult.popmaxAn);
				gnomAD.url = gnomadResult.urls.stream().map(url -> url.toString()).toArray(String[]::new);

				view.gnomAD.add(gnomAD);
			}
		}

	}

	private void createDatabasesTab(JSONObject response, Record record, AnfisaResultData data, AnfisaResultView view) {
		if (data.hgmd != null) {
			view.databases.hgmd = data.hgmd;
			view.databases.hgmdHg38 = data.hgmdHg38;

			view.databases.hgmdTags = record.hgmdData.hgmdPmidRows.stream().map(hgmdPmidRow -> hgmdPmidRow.tag).toArray(String[]::new);
			if (view.databases.hgmdTags.length == 0) view.databases.hgmdTags = null;

			view.databases.hgmdPhenotypes = record.hgmdData.phenotypes.toArray(new String[record.hgmdData.phenotypes.size()]);
			if (view.databases.hgmdPhenotypes.length == 0) view.databases.hgmdPhenotypes = null;

			view.databases.hgmdPmids = record.hgmdData.hgmdPmidRows.stream()
					.map(hgmdPmidRow -> hgmdPmidRow.pmid).map(pmid -> linkToPmid(pmid)).toArray(String[]::new);
			if (view.databases.hgmdPmids.length == 0) view.databases.hgmdPmids = null;

			data.hgmdPmids = record.hgmdData.hgmdPmidRows.stream()
					.map(hgmdPmidRow -> hgmdPmidRow.pmid).toArray(String[]::new);
			if (data.hgmdPmids.length == 0) data.hgmdPmids = null;
		} else {
			view.databases.hgmd = "Not Present";
		}
		view.databases.beaconUrl = data.beaconUrls;

		if (data.clinvarVariants != null) {
			view.databases.clinVarVariants = Arrays.stream(data.clinvarVariants).distinct().toArray(String[]::new);
		}
		if (data.clinvarSignificance != null) {
			view.databases.clinVarSignificance = Arrays.stream(data.clinvarSignificance).distinct().toArray(String[]::new);
		}
		if (data.clinvarPhenotypes != null) {
			view.databases.clinVarPhenotypes = Arrays.stream(data.clinvarPhenotypes).distinct().toArray(String[]::new);
		}
		for (String submitter : trustedSubmitters.keySet()) {
			view.databases.setField(String.format("%s_significance", submitter), data.getField(submitter));
		}
		view.databases.pubmedSearch = getTenwiseLink(response);
		view.databases.omim = getGenes(response).stream().map(gene ->
				String.format("https://omim.org/search/?search=approved_gene_symbol:%s&retrieve=geneMap", gene)
		).toArray(String[]::new);
		view.databases.geneCards = getGenes(response).stream().map(gene ->
				String.format("https://www.genecards.org/cgi-bin/carddisp.pl?gene=%s", gene)
		).toArray(String[]::new);
	}

	private void createPredictionsTab(JSONObject response, AnfisaResultView view) {
		view.predictions.polyphen = getFromTranscriptsList(response, "polyphen_prediction").stream().toArray(String[]::new);
		view.predictions.sift = getFromTranscriptsList(response, "sift_prediction").stream().toArray(String[]::new);
		view.predictions.siftScore = getFromTranscriptsList(response, "sift_score").stream()
				.map(s -> Double.parseDouble(s)).toArray(Double[]::new);
	}

	private void createBioinformaticsTab(JSONObject response, AnfisaResultData data, AnfisaResultView view) {
		view.bioinformatics.humanSplicingFinder = "";
		view.bioinformatics.nnSplice = "";
		view.bioinformatics.speciesWithVariant = "";
		view.bioinformatics.speciesWithOthers = "";
		view.bioinformatics.otherGenes = getOtherGenes(response);
	}

	private String[] getOtherGenes(JSONObject response) {
		Set<String> genes = new HashSet<>(getGenes(response));
		Set<String> allGenes = new HashSet<>(getFromTranscriptsByBiotype(response, "gene_symbol", "all"));

		Set<String> result = new HashSet<>();
		result.addAll(allGenes);
		result.removeAll(genes);
		return result.toArray(new String[result.size()]);
	}

	private String getHg38Coordinates(JSONObject response) {
		String chromosome = RequestParser.toChromosome(getStrChromosome(response));

		Integer hg38Start = liftoverConnector.hg38(
				chromosome,
				response.getAsNumber("start").longValue()
		);
		Integer hg38End = liftoverConnector.hg38(
				chromosome,
				response.getAsNumber("end").longValue()
		);
		if (Objects.equals(hg38Start, hg38End)) {
			return String.format("%s:%s", chromosome, (hg38Start != null) ? hg38Start : "None");
		} else {
			return String.format("%s:%s-%s", chromosome,
					(hg38Start != null) ? hg38Start : "None",
					(hg38End != null) ? hg38End : "None"
			);
		}
	}

	public String getColorCode(JSONObject response, AnfisaResultData data) {
		List<String> pp = getFromTranscriptsList(response, "polyphen_prediction");
		List<String> ss = getFromTranscriptsList(response, "sift_prediction");

		String best = null;
		String worst = null;
		for (String p : pp) {
			if (p.contains("benign")) {
				best = "B";
			} else if (p.contains("possibly_damaging")) {
				if (!"D".equals(worst)) {
					worst = "PD";
				}
			} else if (p.contains("damaging")) {
				worst = "D";
			}
		}
		for (String s : ss) {
			if (s.contains("tolerated")) {
				best = "B";
			}
			if (s.contains("deleterious")) {
				worst = "D";
			}
		}

		String code = null;
		if (!"B".equals(best) && "D".equals(worst)) {
			code = "red";
		} else if ("B".equals(best) && worst == null) {
			code = "green";
		} else if (best != null || worst != null) {
			code = "yellow";
		}
		if (code != null) return code;

		String csq = data.mostSevereConsequence;
		if (csq_damaging.contains(csq)) {
			code = "red-cross";
		} else if (csq_missense.contains(csq)) {
			code = "yellow-cross";
		}

		return code;
	}

	private String[] getTenwiseLink(JSONObject response) {
		List<String> hgncIds = getHgncIds(response);
		return hgncIds.stream().map(hgncId ->
				String.format("https://www.tenwiseapps.nl/publicdl/variant_report/HGNC_%s_variant_report.html", hgncId)
		).toArray(String[]::new);
	}

	public String altString(JSONObject response) {
		return String.join(",", getAlts(response));
	}

	private List<String> getAlts(JSONObject response) {
		return getAlts1(response);
	}

	private List<String> getAlts1(JSONObject response) {
		String[] ss = getAllele(response).split("/");
		List<String> result = new ArrayList<>();
		for (int i = 1; i < ss.length; i++) {
			result.add(ss[i]);
		}
		return result;
	}

	private String getAllele(JSONObject response) {
		return response.getAsString("allele_string");
	}

	private String getRef(JSONObject response) {
		return response.getAsString("allele_string").split("/")[0];
	}

	private String getStrChromosome(JSONObject response) {
		return response.getAsString("seq_region_name");
	}

	private boolean isProbandHasAllele(String alt) {
		//TODO Not implemented
		return false;
	}

	private Long getMinGQ() {
		//TODO Not implemented
		return null;
	}

	private Long getProbandGQ() {
		//TODO Not implemented
		return null;
	}

	private Long getSeverity(JSONObject response) {
		String csq = response.getAsString("most_severe_consequence");
		int n = Variant.SEVERITY.size();
		for (int s = 0; s < n; s++) {
			if (Variant.SEVERITY.get(s).contains(csq)) {
				return Long.valueOf(n - s - 1);
			}
		}
		return null;
	}

	private List<Integer> getDistanceFromExon(JSONObject response, String kind) {
		String key = String.format("dist_from_boundary_%s", kind);
		if (response.containsKey(key)) {
			throw new RuntimeException("Not implemented");
		}
		return new ArrayList<>();
	}

	public String getLabel(JSONObject response) {
		List<String> genes = getGenes(response);
		String gene;
		if (genes.size() == 0) {
			gene = "None";
		} else if (genes.size() < 3) {
			gene = String.join(",", genes);
		} else {
			gene = "...";
		}

		String vstr = str(response);

		return String.format("[%s] %s", gene, vstr);
	}

	public List<String> getGenes(JSONObject response) {
		return getFromTranscriptsList(response, "gene_symbol");
	}

	public List<String> getHgncIds(JSONObject response) {
		return getFromTranscriptsList(response, "hgnc_id");
	}

	public List<String> getFromTranscriptsList(JSONObject response, String key) {
		return getFromTranscriptsByBiotype(response, key, "protein_coding");
	}

	public List<String> getFromTranscriptsByBiotype(JSONObject response, String key, String biotype) {
		List<String> result = new ArrayList<>();
		for (JSONObject item : getTranscripts(response, biotype)) {
			String value = item.getAsString(key);
			if (value == null) continue;
			if (!result.contains(value)) {
				result.add(value);
			} else {
				//TODO Ulitin V. Необходимо выяснить, нужна ли такая особенность пострения уникального списка
				//Дело в том, что в python реализации косвенно получался такой результат,
				//непонятно, это сделано специально или нет, если не важна сортировака, то заменить на обычный Set
				result.remove(value);
				result.add(0, value);
			}
		}
		return result;
	}

	public List<JSONObject> getTranscripts(JSONObject response, String biotype) {
		List<JSONObject> result = new ArrayList<>();
		JSONArray jTranscriptConsequences = (JSONArray) response.get("transcript_consequences");
		if (jTranscriptConsequences != null) {
			for (Object oItem : jTranscriptConsequences) {
				JSONObject item = (JSONObject) oItem;
				if (biotype == null || biotype.toUpperCase().equals("ALL")) {
					result.add(item);
				} else if (item.get("biotype").equals(biotype)) {
					result.add(item);
				}
			}
		}
		return result;
	}

	public String str(JSONObject response) {
		String str = getHg19Coordinates(response);
		return String.format("%s None", str);
	}

	public String getHg19Coordinates(JSONObject response) {
		return vstr(
				RequestParser.toChromosome(getStrChromosome(response)),
				response.getAsNumber("start").longValue(),
				response.getAsNumber("end").longValue()
		);
	}

	public String vstr(String c, long s, long e) {
		if (s == e) {
			return String.format("%s:%s", c, s);
		} else {
			return String.format("%s:%s-%s", c, s, e);
		}
	}

	public boolean isSnv() {
		return false;
	}

	public Double getGnomadAf(AnfisaResultFilters filters) {
		return filters.gnomadAfFam;
	}

	public String linkToPmid(String pmid) {
		return String.format("https://www.ncbi.nlm.nih.gov/pubmed/%s", pmid);
	}

	private static String encodeToAscii(String s) {
		String regex = "[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+";
		try {
			return new String(s.replaceAll(regex, "").getBytes("ascii"), "ascii");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() {

	}
}
