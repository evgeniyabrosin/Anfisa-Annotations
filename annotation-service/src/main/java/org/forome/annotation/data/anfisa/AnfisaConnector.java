/*
 Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association

 Developed by Vladimir Ulitin and Michael Bouzinier

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

	 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package org.forome.annotation.data.anfisa;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import htsjdk.variant.variantcontext.CommonInfo;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.forome.annotation.data.anfisa.struct.*;
import org.forome.annotation.data.clinvar.ClinvarConnector;
import org.forome.annotation.data.clinvar.struct.ClinvarResult;
import org.forome.annotation.data.clinvar.struct.ClinvarVariantSummary;
import org.forome.annotation.data.conservation.ConservationData;
import org.forome.annotation.data.conservation.struct.Conservation;
import org.forome.annotation.data.gnomad.GnomadConnector;
import org.forome.annotation.data.gnomad.struct.GnomadResult;
import org.forome.annotation.data.gtex.GTEXConnector;
import org.forome.annotation.data.gtex.struct.Tissue;
import org.forome.annotation.data.gtf.GTFConnector;
import org.forome.annotation.data.hgmd.HgmdConnector;
import org.forome.annotation.data.liftover.LiftoverConnector;
import org.forome.annotation.data.pharmgkb.PharmGKBConnector;
import org.forome.annotation.data.spliceai.SpliceAIConnector;
import org.forome.annotation.data.spliceai.struct.SpliceAIResult;
import org.forome.annotation.exception.AnnotatorException;
import org.forome.annotation.struct.Allele;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.Interval;
import org.forome.annotation.struct.mcase.Cohort;
import org.forome.annotation.struct.mcase.MCase;
import org.forome.annotation.struct.mcase.Sample;
import org.forome.annotation.struct.mcase.Sex;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.struct.variant.VariantType;
import org.forome.annotation.struct.variant.cnv.VariantCNV;
import org.forome.annotation.struct.variant.custom.VariantCustom;
import org.forome.annotation.struct.variant.vcf.VariantVCF;
import org.forome.annotation.struct.variant.vep.VariantVep;
import org.forome.annotation.utils.AppVersion;
import org.forome.annotation.utils.MathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnfisaConnector implements AutoCloseable {

	private final static Logger log = LoggerFactory.getLogger(AnfisaConnector.class);

	private static final Map<String, String> trustedSubmitters = new HashMap<String, String>() {{
		put("lmm", "Laboratory for Molecular Medicine,Partners HealthCare Personalized Medicine");
		put("gene_dx", "GeneDx");
	}};

	private static final Map<String, String> proteins_3_to_1 = new HashMap<String, String>() {{
		put("Ala", "A");
		put("Arg", "R");
		put("Asn", "N");
		put("Asp", "D");
		put("Cys", "C");
		put("Gln", "Q");
		put("Glu", "E");
		put("Gly", "G");
		put("His", "H");
		put("Ile", "I");
		put("Leu", "L");
		put("Lys", "K");
		put("Met", "M");
		put("Phe", "F");
		put("Pro", "P");
		put("Ser", "S");
		put("Thr", "T");
		put("Trp", "W");
		put("Tyr", "Y");
		put("Val", "V");

	}};

	public final GnomadConnector gnomadConnector;
	public final SpliceAIConnector spliceAIConnector;
	public final ConservationData conservationData;
	public final HgmdConnector hgmdConnector;
	public final ClinvarConnector clinvarConnector;
	private final LiftoverConnector liftoverConnector;

	private final GtfAnfisaBuilder gtfAnfisaBuilder;
	public final GTEXConnector gtexConnector;
	public final PharmGKBConnector pharmGKBConnector;

	public AnfisaConnector(
			GnomadConnector gnomadConnector,
			SpliceAIConnector spliceAIConnector,
			ConservationData conservationConnector,
			HgmdConnector hgmdConnector,
			ClinvarConnector clinvarConnector,
			LiftoverConnector liftoverConnector,
			GTFConnector gtfConnector,
			GTEXConnector gtexConnector,
			PharmGKBConnector pharmGKBConnector
	) {
		this.gnomadConnector = gnomadConnector;
		this.spliceAIConnector = spliceAIConnector;
		this.conservationData = conservationConnector;
		this.hgmdConnector = hgmdConnector;
		this.clinvarConnector = clinvarConnector;
		this.liftoverConnector = liftoverConnector;
		this.gtexConnector = gtexConnector;
		this.pharmGKBConnector = pharmGKBConnector;

		this.gtfAnfisaBuilder = new GtfAnfisaBuilder(gtfConnector);
	}

	public AnfisaResult build(
			AnfisaInput anfisaInput,
			Variant variant
	) {
		JSONObject vepJson = (variant instanceof VariantVep) ? ((VariantVep) variant).getVepJson() : null;

		Record record = new Record();
		AnfisaExecuteContext context = new AnfisaExecuteContext(
				anfisaInput, variant, vepJson
		);

		AnfisaResultFilters filters = new AnfisaResultFilters();
		AnfisaResultData data = new AnfisaResultData();
		AnfisaResultView view = new AnfisaResultView();

		data.version = AppVersion.getVersionFormat();

		callGnomAD(context, variant, anfisaInput.mCase, filters);
		callSpliceai(data, filters, variant);
		callHgmd(record, context, filters, data);
		callClinvar(context, record, variant.chromosome.getChar(), filters, data, view, vepJson);
		GtfAnfisaResult gtfAnfisaResult = gtfAnfisaBuilder.build(variant);
		callQuality(filters, variant);

		Sample proband = anfisaInput.mCase.proband;
		if (proband != null) {
			String probandId = proband.id;
			String mother = anfisaInput.mCase.samples.get(probandId).mother;
			String father = anfisaInput.mCase.samples.get(probandId).father;
			data.zygosity = new HashMap<>();
			filters.altZygosity = new HashMap<>();
			for (Map.Entry<String, Sample> entry : anfisaInput.mCase.samples.entrySet()) {
				String name = entry.getValue().name;
				Sex sex = entry.getValue().sex;
				String label;
				if (entry.getKey().equals(probandId)) {
					label = String.format("proband [%s]", name);
				} else if (entry.getKey().equals(mother)) {
					label = String.format("mother [%s]", name);
				} else if (entry.getKey().equals(father)) {
					label = String.format("father [%s]", name);
				} else {
					label = entry.getKey();
				}

				org.forome.annotation.struct.variant.Genotype genotype = variant.getGenotype(entry.getValue().id);
				if (genotype != null) {
					int zyg = genotype.hasVariant();
					data.zygosity.put(entry.getKey(), zyg);
					int modified_zygosity = (!variant.chromosome.equals(Chromosome.CHR_X) || sex == Sex.FEMALE || (zyg == 0)) ? zyg : 2;
					filters.altZygosity.put(entry.getKey(), modified_zygosity);
					if (zyg > 0) {
						filters.has_variant.add(label);
					}
				}
			}
		}

		List<Object> d = getDistanceFromExon(gtfAnfisaResult, (VariantVep) variant, Kind.WORST);
		filters.distFromExon = d.stream()
				.filter(o -> (o instanceof Number))
				.map(o -> ((Number) o).longValue())
				.min(Long::compareTo).orElse(0L);

		data.assemblyName = vepJson.getAsString("assembly_name");
		data.end = variant.end;
		data.regulatoryFeatureConsequences = (JSONArray) vepJson.get("regulatory_feature_consequences");
		data.motifFeatureConsequences = (JSONArray) vepJson.get("motif_feature_consequences");
		data.intergenicConsequences = (JSONArray) vepJson.get("intergenic_consequences");
		data.start = variant.getStart();
		data.mostSevereConsequence = variant.getMostSevereConsequence();
		data.alleleString = getAlleleString(variant);
		data.seqRegionName = vepJson.getAsString("seq_region_name");
		data.colocatedVariants = (JSONArray) vepJson.get("colocated_variants");
		if (!(variant instanceof VariantCNV)) {
			data.input = vepJson.getAsString("input");
		}
		data.label = getLabel(context);
		data.transcriptConsequences = ((VariantVep) variant).getTranscriptConsequences();
		data.id = ((VariantVep) variant).getId();
		data.strand = (vepJson.containsKey("strand")) ? vepJson.getAsNumber("strand").longValue() : null;
		data.variantClass = variant.getVariantType();

		data.colorCode = getColorCode((VariantVep) variant, data, record, filters);

		if (gtfAnfisaResult.canonical != null) {
			data.distFromBoundaryCanonical = gtfAnfisaResult.canonical.distances;
			data.regionCanonical = gtfAnfisaResult.canonical.region;
		}
		if (gtfAnfisaResult.worst != null) {
			data.distFromBoundaryWorst = gtfAnfisaResult.worst.distances;
			data.regionWorst = gtfAnfisaResult.worst.region;
		}

		if (variant instanceof VariantCNV) {
			VariantCNV variantCNV = (VariantCNV) variant;
			data.cnvGT = variantCNV.genotypes.values().stream()
					.collect(Collectors.toMap(item -> item.sampleName, item -> item.gt));
			filters.cnvLO = view.bioinformatics.cnvLO =
					variantCNV.getGenotype(anfisaInput.mCase.proband.id).lo;
		}

		createGeneralTab(context, data, filters, view, variant, anfisaInput.mCase);
		createQualityTab(view, variant, anfisaInput.mCase);
		createGnomadTab(context, variant.chromosome.getChar(), variant, anfisaInput.mCase, view);
		createDatabasesTab((VariantVep) variant, record, data, view);
		createPredictionsTab((VariantVep) variant, vepJson, view);
		createBioinformaticsTab(gtfAnfisaResult, context, data, view);
		createPharmacogenomicsTab(view, filters, variant);
		countCohorts(view, filters, anfisaInput.mCase, variant);

		return new AnfisaResult(filters, data, view);
	}

	private void countCohorts(AnfisaResultView view, AnfisaResultFilters filters, MCase mCase, Variant variant) {
		for (Cohort cohort : mCase.cohorts) {
			float[] count = countCohort(variant, cohort.getSamples());
			view.cohorts.put(
					cohort.name,
					new HashMap<String, Float>() {{
						put("AF", count[0]);
						put("AF2", count[1]);
					}}
			);
		}

		float[] countAll = countCohort(variant, mCase.samples.values());
		view.cohorts.put(
				"ALL",
				new HashMap<String, Float>() {{
					put("AF", countAll[0]);
					put("AF2", countAll[1]);
				}}
		);

		//Находим такие когорты, для которых, этот вариант есть хотя бы у одного сэмпла из когорты.
		LinkedHashSet<Cohort> cohortHasVariant = new LinkedHashSet<>();
		for (Sample sample : mCase.samples.values()) {
			if (sample.cohort == null) continue;

			int zyg = variant.getGenotype(sample.id).hasVariant();
			if (zyg > 0) {
				cohortHasVariant.add(sample.cohort);
			}
		}
		filters.cohortHasVariant = cohortHasVariant.stream().map(cohort -> cohort.name).toArray(String[]::new);
	}

	/**
	 * AF: Доля samples имеющих данный вариант, то есть, количество samples в когорте, у которых вариант присутствует, поделенное на общее количество samples в этой когорте.
	 * AF2: Доля гомозигот, то есть количество samples в когорте, у которых вариант присутствует в гомозиготном виде (zyg == 2), поделенное на общее количество samples в этой когорте.
	 *
	 * @param variant
	 * @param samples
	 * @return
	 */
	private float[] countCohort(Variant variant, Collection<Sample> samples) {
		int countAf = 0;
		int countAf2 = 0;
		for (Sample sample : samples) {
			org.forome.annotation.struct.variant.Genotype genotype = variant.getGenotype(sample.id);
			if (genotype == null) continue;
			int zyg = genotype.hasVariant();
			if (zyg > 0) {
				countAf++;
			}
			if (zyg == 2) {
				countAf2++;
			}
		}
		return new float[]{
				(float) countAf / (float) samples.size(), (float) countAf2 / (float) samples.size()
		};
	}


	private static String getAlleleString(Variant variant) {
		return variant.getRef() + "/" + variant.getStrAlt();
	}

	private void callHgmd(Record record, AnfisaExecuteContext anfisaExecuteContext, AnfisaResultFilters filters, AnfisaResultData data) {
		Variant variant = anfisaExecuteContext.variant;
		List<String> accNums = hgmdConnector.getAccNum(variant.chromosome.getChar(), variant.getStart(), variant.end);
		if (accNums.size() > 0) {
			HgmdConnector.Data hgmdData = hgmdConnector.getDataForAccessionNumbers(accNums);
			record.hgmdData = hgmdData;

			data.hgmd = String.join(",", accNums);
			List<Long[]> hg38 = hgmdConnector.getHg38(accNums);

			data.hgmdHg38 = hg38.stream().map(longs -> String.format("%s-%s", longs[0], longs[1])).collect(Collectors.joining(", "));
			List<String> tags = hgmdData.hgmdPmidRows.stream().map(hgmdPmidRow -> hgmdPmidRow.tag).collect(Collectors.toList());
			filters.hgmdBenign = (tags.size() == 0);
		}
	}

	private void callClinvar(AnfisaExecuteContext context, Record record, String _chromosome, AnfisaResultFilters filters, AnfisaResultData data, AnfisaResultView view, JSONObject json) {
		Variant variant = context.variant;
		Chromosome chromosome = variant.chromosome;

		List<ClinvarResult> clinvarResults;
		if (isSnv(variant)) {
			clinvarResults = clinvarConnector.getData(_chromosome, variant.getStart(), variant.end, variant.getStrAlt());
		} else {
			clinvarResults = clinvarConnector.getExpandedData(variant);
		}
		record.clinvarResults = clinvarResults;
		if (!clinvarResults.isEmpty()) {

			String[] variants = clinvarResults.stream().map(clinvarResult -> {
				return String.format("%s %s>%s",
						vstr(Interval.of(chromosome, clinvarResult.start, clinvarResult.end)),
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
					for (Map.Entry<String, String> entry : clinvarResult.submitters.entrySet()) {
						put(encodeToAscii(entry.getKey()), entry.getValue());
					}
				}
			}};

			view.databases.clinVar = clinvarResults.stream()
					.map(clinvarResult -> clinvarResult.variationID)
					.map(it -> String.format("https://www.ncbi.nlm.nih.gov/clinvar/variation/%s/", it))
					.toArray(String[]::new);
			data.clinvarVariants = variants;
			view.databases.clinVarSubmitters = data.clinvarSubmitters.entrySet().stream().map(entry -> {
				return String.format("%s: %s", encodeToAscii(entry.getKey()), entry.getValue());
			}).sorted().toArray(String[]::new);
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

		ClinvarVariantSummary clinvarVariantSummary = clinvarConnector.getDataVariantSummary(chromosome, variant.getStart(), variant.end);
		if (clinvarVariantSummary != null) {
			view.databases.clinvarReviewStatus = clinvarVariantSummary.reviewStatus.text;
			filters.clinvarReviewStatus = clinvarVariantSummary.reviewStatus;
			view.databases.numClinvarSubmitters = clinvarVariantSummary.numberSubmitters;
			filters.numClinvarSubmitters = clinvarVariantSummary.numberSubmitters;
			view.databases.clinvarAcmgGuidelines = filters.clinvarAcmgGuidelines =
					clinvarVariantSummary.guidelineTypes.stream()
							.map(guideline -> guideline.getStrValue()).toArray(String[]::new);
		}
	}

	private static void callQuality(AnfisaResultFilters filters, Variant variant) {
		if (variant instanceof VariantVCF) {
			VariantContext variantContext = ((VariantVCF) variant).maVariantVCF.variantContext;

			if (variantContext.isFiltered()) {
				filters.filters = new ArrayList<>(variantContext.getFilters());
			} else {
				filters.filters = Lists.newArrayList("PASS");
			}
		}
	}

	private void callGnomAD(AnfisaExecuteContext context, Variant variant, MCase samples, AnfisaResultFilters filters) {
		Double af = null;
		Double _af = null;
		Double emAf = null;
		Double emAfPb = null;
		Double gmAf = null;
		Double gmAfPb = null;
		Double _afPb = null;

		GnomadResult.Popmax popmax = null;
		GnomadResult.Popmax rawPopmax = null;

		Long hom = null;
		Long hem = null;

		GnomadResult gnomadResult = getGnomadResult(variant);
		if (gnomadResult == null) {
			return;
		}
		if (gnomadResult.exomes != null) {
			af = gnomadResult.exomes.af;
			emAf = Math.min((emAf != null && emAf != 0.0d) ? emAf : af, af);
			if (isProbandHasAllele(variant, samples)) {
				emAfPb = Math.min((emAfPb != null) ? emAfPb : af, af);
			}
		}
		if (gnomadResult.genomes != null) {
			af = gnomadResult.genomes.af;
			gmAf = Math.min((gmAf != null) ? gmAf : af, af);
			if (isProbandHasAllele(variant, samples)) {
				gmAfPb = Math.min((gmAfPb != null) ? gmAfPb : af, af);
			}
		}

		af = gnomadResult.overall.af;
		if (isProbandHasAllele(variant, samples)) {
			_afPb = Math.min((_afPb != null) ? _afPb : af, af);
		}

		if (hom == null || hom < gnomadResult.overall.hom) {
			hom = gnomadResult.overall.hom;
		}
		if (hem == null || hem < gnomadResult.overall.hem) {
			hem = gnomadResult.overall.hem;
		}

		if (_af == null || af < _af) {
			_af = af;
			popmax = gnomadResult.popmax;
			rawPopmax = gnomadResult.rawPopmax;
		}
		context.gnomadAfFam = _af;

		filters.gnomadAfFam = _af;
		filters.gnomadAfPb = _afPb;

		filters.gnomadDbExomesAf = emAf;
		filters.gnomadDbGenomesAf = gmAf;

		filters.gnomadPopmax = popmax;
		filters.gnomadRawPopmax = rawPopmax;

		filters.gnomadHom = hom;
		filters.gnomadHem = hem;
	}

	private GnomadResult getGnomadResult(Variant variant) {
		try {
			return gnomadConnector.request(
					variant.chromosome,
					Math.min(variant.getStart(), variant.end),
					variant.getRef(), variant.getStrAlt()
			).get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof AnnotatorException) {
				throw (AnnotatorException) cause;
			} else {
				throw new RuntimeException(cause);
			}
		}
	}

	private void callSpliceai(AnfisaResultData data, AnfisaResultFilters filters, Variant variant) {
		SpliceAIResult spliceAIResult = spliceAIConnector.getAll(
				variant.chromosome.getChar(),
				lowest_coord(variant),
				variant.getRef(),
				variant.getAlt()
		);
		data.spliceAI = spliceAIResult.dict_sql;
		filters.spliceAltering = spliceAIResult.cases;
		filters.spliceAiDsmax = spliceAIResult.max_ds;
	}

	private void createGeneralTab(AnfisaExecuteContext context, AnfisaResultData data, AnfisaResultFilters filters, AnfisaResultView view, Variant variant, MCase samples) {
		view.general.genes = getGenes((VariantVep) variant).stream().toArray(String[]::new);
		view.general.hg19 = str(context);
		view.general.hg38 = getStrHg38Coordinates(context);

		//Особенность связанна с удобством визуального отображением
		if (!isSnv(variant)) {
			view.general.ref = variant.getRef();
			view.general.alt = variant.getStrAlt();
		}

		List<String>[] cPosTpl = getPosTpl((VariantVep) variant, "c");
		view.general.cposWorst = cPosTpl[0];
		view.general.cposCanonical = cPosTpl[1];
		view.general.cposOther = cPosTpl[2];

		List<String>[] pPosTpl = getPosTpl((VariantVep) variant, "p");
		view.general.pposWorst = pPosTpl[0];
		view.general.pposCanonical = pPosTpl[1];
		view.general.pposOther = pPosTpl[2];

		Genotypes gGenotypes = getGenotypes(variant, samples);
		if (gGenotypes != null) {
			view.general.probandGenotype = gGenotypes.probandGenotype;
			view.general.maternalGenotype = gGenotypes.maternalGenotype;
			view.general.paternalGenotype = gGenotypes.paternalGenotype;
		}

		view.general.worstAnnotation = data.mostSevereConsequence;
		List<String> consequenceTerms = getFromCanonicalTranscript((VariantVep) variant, "consequence_terms");
		sortConsequences(consequenceTerms);

		if (variant instanceof VariantCNV) {
//			view.general.canonicalAnnotation = VariantCNV.COPY_NUMBER_VARIATION;
			view.general.canonicalAnnotation.add(VariantCNV.COPY_NUMBER_VARIATION);
		} else {
			view.general.canonicalAnnotation.addAll(consequenceTerms);
/*
			String canonicalAnnotation = getMostSevere(consequenceTerms);
			if (consequenceTerms.size() > 1) {
				String finalCanonicalAnnotation = canonicalAnnotation;
				List<String> otherTerms = consequenceTerms.stream()
						.filter(s -> !s.equals(finalCanonicalAnnotation))
						.collect(Collectors.toList());
				canonicalAnnotation = String.format("%s [%s]", canonicalAnnotation, String.join(", ", otherTerms));
			}
			view.general.canonicalAnnotation = canonicalAnnotation;
*/
		}

		view.general.spliceRegion = getFromTranscripts((VariantVep) variant, "spliceregion", "all");
		view.general.geneSplicer = getFromTranscripts((VariantVep) variant, "genesplicer", "all");

		List<JSONObject> transcripts = getMostSevereTranscripts((VariantVep) variant);
		view.general.refseqTranscriptWorst = getFromTranscripts(transcripts, "transcript_id", "RefSeq");
		view.general.ensemblTranscriptsWorst = getFromTranscripts(transcripts, "transcript_id", "Ensembl");

		transcripts = getCanonicalTranscripts((VariantVep) variant);
		if (view.general.canonicalAnnotation.size() > 1) {
			view.general.refseqTranscriptCanonical = getCsqFromTranscripts(transcripts, "RefSeq");
			view.general.ensemblTranscriptsCanonical = getCsqFromTranscripts(transcripts, "Ensembl");
		} else {
			view.general.refseqTranscriptCanonical =
					getFromTranscripts(transcripts, "transcript_id", "RefSeq");
			view.general.ensemblTranscriptsCanonical =
					getFromTranscripts(transcripts, "transcript_id", "Ensembl");
		}

		view.general.variantExonWorst = getFromWorstTranscript((VariantVep) variant, "exon");
		view.general.variantIntronWorst = getFromWorstTranscript((VariantVep) variant, "intron");
		view.general.variantExonCanonical = getFromCanonicalTranscript((VariantVep) variant, "exon");
		view.general.variantIntronCanonical = getFromCanonicalTranscript((VariantVep) variant, "intron");

		String[] intronOrExonCanonical = getIntronOrExon((VariantVep) variant, Kind.CANONICAL);
		data.variantExonIntronCanonical = intronOrExonCanonical[0];
		data.totalExonIntronCanonical = intronOrExonCanonical[1];

		String[] intronOrExonWorst = getIntronOrExon((VariantVep) variant, Kind.WORST);
		data.variantExonIntronWorst = intronOrExonWorst[0];
		data.totalExonIntronWorst = intronOrExonWorst[1];

		if (filters.spliceAiDsmax != null) {
			if (filters.spliceAiDsmax >= SpliceAIConnector.MAX_DS_UNLIKELY) {
				view.general.spliceAltering = Optional.ofNullable(getSpliceAltering(filters));
			}
		} else {
			view.general.spliceAltering = Optional.empty();
		}

		//Собираем на какие органы может максимально повлияет этот вариант
		List<Tissue> tissues = getTissues(getGenes((VariantVep) variant));
		view.general.mostlyExpressed = tissues.stream()
				.map(tissue -> tissue.toJSON()).collect(Collectors.toList());
		filters.topTissue = tissues.stream().map(tissue -> tissue.name).findFirst().orElse(null);
		filters.topTissues = tissues.stream().map(tissue -> tissue.name).distinct().toArray(String[]::new);
	}

	/**
	 * Собираем на какие органы может максимально повлияет этот вариант, в выборку попадают:
	 * 1) TOP3 по полю: expression
	 * 2) И tissue для которых RelExp больше 0.8
	 *
	 * @param genes
	 * @return
	 */
	public List<Tissue> getTissues(List<String> genes) {
		List<Tissue> fullTissues = new ArrayList<>();
		for (String gene : genes) {
			fullTissues.addAll(gtexConnector.getTissues(gene));
		}
		//Добавляем top3 по полю expression
		LinkedHashSet<Tissue> tissues = fullTissues.stream()
				.sorted((o1, o2) -> Float.compare(o2.expression, o1.expression))
				.limit(3).collect(Collectors.toCollection(LinkedHashSet::new));
		//Ищем у кого RelExp больше 0.8
		tissues.addAll(
				fullTissues.stream()
						.filter(tissue -> tissue.relExp >= 0.8f)
						.sorted((o1, o2) -> Float.compare(o2.relExp, o1.relExp))
						.collect(Collectors.toList())
		);
		return new ArrayList<>(tissues);
	}

	private static String getSpliceAltering(AnfisaResultFilters filters) {
		return filters.spliceAltering;
	}

	private static String[] getIntronOrExon(VariantVep variantVep, Kind kind) {
		List<String> introns = getFromTranscripts(variantVep, "intron", kind.value);
		List<String> exons = getFromTranscripts(variantVep, "exon", kind.value);
		List<String> e = (exons.size() > 0) ? exons : introns;
		if (e.size() == 0) {
			return new String[]{ null, null };
		}

		List<String> index = new ArrayList<>();
		List<String> total = new ArrayList<>();
		for (String x : e) {
			if (x == null) continue;
			String[] split = x.split("/");
			index.add(split[0]);
			if (split.length > 1) {
				total.add(split[1]);
			}
		}

		return new String[]{
				String.join(",", index),
				String.join(",", total)
		};
	}

	private static Double getVariantMQ(CommonInfo commonInfo) {
		Object oMQAttribute = commonInfo.getAttribute("MQ");
		if ("nan".equals(oMQAttribute)) {//В кейсе ipm0001 встретилась такая ситуация
			oMQAttribute = null;
		}
		return MathUtils.toDouble(oMQAttribute);
	}

	private void createQualityTab(AnfisaResultView view, Variant variant, MCase samples) {
		if (!(variant instanceof VariantVCF)) {
			return;
		}
		VariantContext variantContext = ((VariantVCF) variant).maVariantVCF.variantContext;

		CommonInfo commonInfo = variantContext.getCommonInfo();
		if (commonInfo == null) return;

		JSONObject q_all = new JSONObject();
		q_all.put("title", "All");
		q_all.put("strand_odds_ratio", MathUtils.toDouble(commonInfo.getAttribute("SOR")));
		q_all.put("mq", getVariantMQ(commonInfo));

		q_all.put("variant_call_quality", variantContext.getPhredScaledQual());
		q_all.put("qd", MathUtils.toDouble(commonInfo.getAttribute("QD")));
		q_all.put("fs", MathUtils.toDouble(commonInfo.getAttribute("FS")));
		q_all.put("ft", (commonInfo.isFiltered())
				? commonInfo.getFilters().stream().collect(Collectors.toList())
				: Lists.newArrayList("PASS")
		);
		view.qualitySamples.add(q_all);

		Sample proband = samples.proband;
		if (proband == null) return;
		String probandId = proband.id;

		String mother = samples.samples.get(probandId).mother;
		String father = samples.samples.get(probandId).father;
		List<htsjdk.variant.variantcontext.Allele> alleles = variantContext.getAlleles();
		for (Sample sample : getSortedSamples(samples)) {
			String s = sample.id;
			JSONObject q_s = new JSONObject();
			if (s.equals(probandId)) {
				q_s.put("title", String.format("Proband: %s", sample.name));
			} else if (s.equals(mother)) {
				q_s.put("title", String.format("Mother: %s", sample.name));
			} else if (s.equals(father)) {
				q_s.put("title", String.format("Father: %s", sample.name));
			} else {
				q_s.put("title", sample.name);
			}

			Genotype oGenotype = variantContext.getGenotype(sample.id);
			String type = oGenotype.getType().name();
			String gt = oGenotype.getGenotypeString(false);
			if (oGenotype.hasAD()) {
				int[] ad = oGenotype.getAD();
				List<String> adList = new ArrayList<>();
				for (int i = 0; i < ad.length; i++) {
					if (ad[i] > 0) {
						adList.add(alleles.get(i).getBaseString() + ":" + ad[i]);
					}
				}
				q_s.put("allelic_depth", String.join(",", adList));
			} else {
				q_s.put("allelic_depth", oGenotype.getAnyAttribute("AD"));
			}
			q_s.put("read_depth", oGenotype.getAnyAttribute("DP"));
			q_s.put("genotype_quality", variant.getGenotype(sample).getGQ());
			q_s.put("genotype", type + ":" + gt);
			view.qualitySamples.add(q_s);
		}
	}

	/**
	 * Return sort samples
	 * 1) Proband, if exists
	 * 2) Mother, if exists
	 * 3) Father, if exists
	 * 4) ...
	 */
	private static List<Sample> getSortedSamples(MCase samples) {
		List<Sample> result = new ArrayList<>();
		List<Sample> pool = new ArrayList<>(samples.samples.values());

		Sample proband = samples.proband;
		if (proband != null) {
			result.add(proband);
			pool.remove(proband);
		}

		String motherId = proband.mother;
		if (!"0".equals(motherId)) {
			Sample mother = samples.samples.get(motherId);
			result.add(mother);
			pool.remove(mother);
		}

		String fatherId = proband.father;
		if (!"0".equals(fatherId)) {
			Sample father = samples.samples.get(fatherId);
			result.add(father);
			pool.remove(father);
		}

		result.addAll(pool);

		return result;
	}

	private void createGnomadTab(AnfisaExecuteContext context, String chromosome, Variant variant, MCase samples, AnfisaResultView view) {
		Double gnomadAf = context.gnomadAfFam;
		if (gnomadAf != null && Math.abs(gnomadAf) > 0.000001D) {
			AnfisaResultView.GnomAD gnomAD = new AnfisaResultView.GnomAD();

			gnomAD.allele = variant.getStrAlt();

			gnomAD.pli = getPLIByAllele((VariantVep) variant);
			gnomAD.proband = (isProbandHasAllele(variant, samples)) ? "Yes" : "No";

			GnomadResult gnomadResult = getGnomadResult(variant);
			if (gnomadResult != null) {
				if (gnomadResult.exomes != null) {
					gnomAD.exomeAn = gnomadResult.exomes.an;
					gnomAD.exomeAf = gnomadResult.exomes.af;
				}
				if (gnomadResult.genomes != null) {
					gnomAD.genomeAn = gnomadResult.genomes.an;
					gnomAD.genomeAf = gnomadResult.genomes.af;
				}

				if (gnomadResult.overall != null) {
					gnomAD.af = gnomadResult.overall.af;
					gnomAD.hom = gnomadResult.overall.hom;
					gnomAD.hem = gnomadResult.overall.hem;
				}
				if (gnomadResult.popmax != null) {
					gnomAD.popmax = String.format(Locale.ENGLISH, "%s: %.5f [%s]",
							gnomadResult.popmax.group.name(), gnomadResult.popmax.af, gnomadResult.popmax.an
					);
				}
				if (gnomadResult.rawPopmax != null) {
					gnomAD.rawPopmax = String.format(Locale.ENGLISH, "%s: %.5f [%s]",
							gnomadResult.rawPopmax.group.name(), gnomadResult.rawPopmax.af, gnomadResult.rawPopmax.an
					);
				}

				gnomAD.url = gnomadResult.urls.stream().map(url -> url.toString()).toArray(String[]::new);
			}

			view.gnomAD = gnomAD;
		} else {
			int p1 = lowest_coord(variant) - 2;
			int p2 = highest_coord(variant) + 1;

			AnfisaResultView.GnomAD gnomAD = new AnfisaResultView.GnomAD();
			gnomAD.url = new String[]{
					String.format("https://gnomad.broadinstitute.org/region/%s-%s-%s", chromosome, p1, p2)
			};
			view.gnomAD = gnomAD;
		}
	}

	private static int lowest_coord(Variant variant) {
		return Math.min(variant.getStart(), variant.end);
	}

	private static int highest_coord(Variant variant) {
		return Math.max(variant.getStart(), variant.end);
	}

	private static List<Double> getPLIByAllele(VariantVep variantVep) {
		List<JSONObject> transcripts = getTranscripts(variantVep, "protein_coding");
		String key = "exacpli";
		List<Double> list = unique(
				transcripts.stream()
						.filter(jsonObject -> jsonObject.containsKey(key))
						.filter(jsonObject -> variantVep.getStrAlt().equals(jsonObject.get("variant_allele")))
						.map(jsonObject -> jsonObject.getAsNumber(key).doubleValue())
						.collect(Collectors.toList())
		);
		if (list.size() > 0) {
			return list;
		}
		return null;
	}

	private static Set<String> getHGMDTags(Record record) {
		if (record.hgmdData == null)
			return new HashSet<>();
		return record.hgmdData.hgmdPmidRows.stream().map(hgmdPmidRow -> hgmdPmidRow.tag).collect(Collectors.toSet());
	}

	private void createDatabasesTab(VariantVep variantVep, Record record, AnfisaResultData data, AnfisaResultView view) {
		if (data.hgmd != null) {
			view.databases.hgmd = data.hgmd;
			view.databases.hgmdHg38 = data.hgmdHg38;

			view.databases.hgmdTags = getHGMDTags(record).toArray(new String[0]);
			if (view.databases.hgmdTags.length == 0) view.databases.hgmdTags = null;

			view.databases.hgmdPhenotypes = record.hgmdData.phenotypes.toArray(new String[record.hgmdData.phenotypes.size()]);
			if (view.databases.hgmdPhenotypes.length == 0) view.databases.hgmdPhenotypes = null;

			view.databases.references.addAll(
					record.hgmdData.hgmdPmidRows.stream()
							.map(hgmdPmidRow -> hgmdPmidRow.pmid)
							.collect(Collectors.toList())
			);

			data.hgmdPmids = record.hgmdData.hgmdPmidRows.stream()
					.map(hgmdPmidRow -> hgmdPmidRow.pmid).toArray(String[]::new);
			if (data.hgmdPmids.length == 0) data.hgmdPmids = null;
		} else {
			view.databases.hgmd = "Not Present";
		}

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
	}

	private void createPredictionsTab(VariantVep variantVep, JSONObject vepJson, AnfisaResultView view) {
		view.predictions.lofScore = getFromTranscripts(variantVep, "loftool", "all")
				.stream().map(s -> Double.parseDouble(s)).sorted(Comparator.reverseOrder())
				.collect(Collectors.toList());
		view.predictions.lofScoreCanonical = getFromCanonicalTranscript(variantVep, "loftool")
				.stream().map(s -> Double.parseDouble(s)).sorted(Comparator.reverseOrder())
				.collect(Collectors.toList());

		view.predictions.maxEntScan = getMaxEnt(variantVep);

		view.predictions.polyphen = getFromTranscriptsList(variantVep, "polyphen_prediction").stream().toArray(String[]::new);
		view.predictions.polyphen2Hvar = getFromTranscriptsList(variantVep, "Polyphen2_HVAR_pred".toLowerCase()).stream().collect(Collectors.toList());
		view.predictions.polyphen2Hdiv = getFromTranscriptsList(variantVep, "Polyphen2_HDIV_pred".toLowerCase()).stream().collect(Collectors.toList());
		view.predictions.polyphen2HvarScore = getFromTranscriptsList(variantVep, "Polyphen2_HVAR_score".toLowerCase()).stream()
				.collect(Collectors.toList());
		view.predictions.polyphen2HdivScore = getFromTranscriptsList(variantVep, "Polyphen2_HDIV_score".toLowerCase()).stream()
				.collect(Collectors.toList());
		view.predictions.sift = getFromTranscriptsList(variantVep, "sift_prediction").stream().toArray(String[]::new);
		view.predictions.siftVEP = getFromTranscriptsList(variantVep, "sift_pred").stream().toArray(String[]::new);
		view.predictions.siftScore = getFromTranscriptsList(variantVep, "sift_score").stream().toArray(String[]::new);
		view.predictions.revel = getFromTranscriptsList(variantVep, "revel_score").stream().map(s -> Double.parseDouble(s))
				.collect(Collectors.toList());
		view.predictions.mutationTaster = getFromTranscriptsList(variantVep, "mutationtaster_pred").stream()
				.flatMap(s -> Arrays.stream(s.split(",")))
				.map(s -> s.trim())
				.filter(s -> !s.isEmpty())
				.distinct().toArray(String[]::new);
		view.predictions.fathmm = getFromTranscriptsList(variantVep, "fathmm_pred").stream()
				.flatMap(s -> Arrays.stream(s.split(",")))
				.map(s -> s.trim())
				.filter(s -> !s.isEmpty())
				.distinct().toArray(String[]::new);
		view.predictions.caddPhred = getFromTranscriptsList(variantVep, "cadd_phred").stream().map(s -> Double.parseDouble(s))
				.collect(Collectors.toList());
		view.predictions.caddRaw = getFromTranscriptsList(variantVep, "cadd_raw").stream().map(s -> Double.parseDouble(s))
				.collect(Collectors.toList());
		view.predictions.mutationAssessor = getFromTranscriptsList(variantVep, "mutationassessor_pred").stream().toArray(String[]::new);
	}

	private static List<String> getMaxEnt(VariantVep variantVep) {
		List<JSONObject> transcripts = getTranscripts(variantVep, "protein_coding");
		Set<String> x = new HashSet<>();
		for (JSONObject transcript : transcripts) {
			Number m1 = transcript.getAsNumber("maxentscan_ref");
			Number m2 = transcript.getAsNumber("maxentscan_alt");
			Number m3 = transcript.getAsNumber("maxentscan_diff");
			if (m1 != null && m2 != null && m3 != null) {
				String v = String.format("%s=%s-%s", m3, m1, m2);
				x.add(v);
			}
		}
		if (!x.isEmpty()) {
			return new ArrayList<String>(x);
		} else {
			return null;
		}
	}

	private void createBioinformaticsTab(GtfAnfisaResult gtfAnfisaResult, AnfisaExecuteContext anfisaExecuteContext, AnfisaResultData data, AnfisaResultView view) {
		AnfisaInput anfisaInput = anfisaExecuteContext.anfisaInput;
		Variant variant = anfisaExecuteContext.variant;

		view.bioinformatics.inheritedFrom = inherited_from(variant, anfisaInput.mCase);
		view.bioinformatics.distFromExonCanonical = getDistanceFromExon(gtfAnfisaResult, (VariantVep) variant, Kind.CANONICAL);
		view.bioinformatics.distFromExonWorst = getDistanceFromExon(gtfAnfisaResult, (VariantVep) variant, Kind.WORST);
		view.bioinformatics.conservation = buildConservation(anfisaExecuteContext);
		view.bioinformatics.speciesWithVariant = "";
		view.bioinformatics.speciesWithOthers = "";
		view.bioinformatics.maxEntScan = getMaxEnt((VariantVep) variant);
		view.bioinformatics.nnSplice = "";
		view.bioinformatics.humanSplicingFinder = "";
		view.bioinformatics.otherGenes = getOtherGenes((VariantVep) variant);
		view.bioinformatics.calledBy = getCallers(variant, anfisaInput.mCase).stream().toArray(String[]::new);
		view.bioinformatics.callerData = getCallersData(variant);
		view.bioinformatics.spliceAi = list_dsmax(data);

		String[] splice_ai_keys = new String[]{ "AG", "AL", "DG", "DL" };
		if (!data.spliceAI.isEmpty()) {
			for (Map.Entry<String, SpliceAIResult.DictSql> entry : data.spliceAI.entrySet()) {
				for (String s : splice_ai_keys) {
					String key = String.format("DS_%s", s);
					float score = entry.getValue().getValue(key).floatValue();
					if (score > 0.0f) {
						String key2 = String.format("DP_%s", s);
						int position = entry.getValue().getValue(key2).intValue();
						String sPosition = String.valueOf(position);
						if (position > 0) {
							sPosition = "+" + sPosition;
						}
						view.bioinformatics.getSpliceAiValues(s).add(
								String.format("%s: %s[%s]", entry.getKey(), String.format(Locale.ENGLISH, "%.4f", score), sPosition)
						);
					}
				}
			}
		}
	}

	private void createPharmacogenomicsTab(AnfisaResultView view, AnfisaResultFilters filters, Variant variant) {
		if (!(variant instanceof VariantVCF)) {
			return;
		}
		String variantId = ((VariantVCF) variant).maVariantVCF.variantContext.getID();
		if (variantId == null || ".".equals(variantId)) {
			return;
		}

		view.pharmacogenomics.notes = pharmGKBConnector.getNotes(variantId);
		List<AnfisaResultView.Pharmacogenomics.Item> pmids = pharmGKBConnector.getPmids(variantId);
		view.pharmacogenomics.pmids = pmids;
		List<AnfisaResultView.Pharmacogenomics.Item> diseases = pharmGKBConnector.getDiseases(variantId);
		view.pharmacogenomics.diseases = diseases;
		List<AnfisaResultView.Pharmacogenomics.Item> chemicals = pharmGKBConnector.getChemicals(variantId);
		view.pharmacogenomics.chemicals = chemicals;

		//Add filters
		filters.pharmacogenomicsDiseases = diseases.stream().map(item -> item.value).distinct().toArray(String[]::new);
		filters.pharmacogenomicsChemicals = chemicals.stream().map(item -> item.value).distinct().toArray(String[]::new);

		//Добавляем в общий список pmids
		view.databases.references.addAll(
				pmids.stream().map(item -> item.value).collect(Collectors.toList())
		);
	}

	public Conservation buildConservation(AnfisaExecuteContext context) {
		Variant variant = context.variant;
		String ref = variant.getRef();
		String alt = variant.getStrAlt();
		Interval hgmdHG19 = getHg19Coordinates(context);
		Interval hgmdHG38 = getHg38Coordinates(context);
		if (hgmdHG38 == null) {
			return null;
		}
		return conservationData.getConservation(hgmdHG19, hgmdHG38, ref, alt);
	}

	private static Map<String, Float> list_dsmax(AnfisaResultData data) {
		Map<String, Float> result = new HashMap<>();
		if (data.spliceAI.isEmpty()) {
			return result;
		}
		result.put(
				"DS_AG",
				data.spliceAI.values().stream().map(dictSql -> dictSql.ds_ag).max(Float::compareTo).get()
		);
		result.put(
				"DS_AL",
				data.spliceAI.values().stream().map(dictSql -> dictSql.ds_al).max(Float::compareTo).get()
		);
		result.put(
				"DS_DG",
				data.spliceAI.values().stream().map(dictSql -> dictSql.ds_dg).max(Float::compareTo).get()
		);
		result.put(
				"DS_DL",
				data.spliceAI.values().stream().map(dictSql -> dictSql.ds_dl).max(Float::compareTo).get()
		);
		return result;
	}

	private static String[] getOtherGenes(VariantVep variantVep) {
		Set<String> genes = new HashSet<>(getGenes(variantVep));
		Set<String> allGenes = new HashSet<>(getFromTranscriptsByBiotype(variantVep, "gene_symbol", "all"));

		Set<String> result = new HashSet<>();
		result.addAll(allGenes);
		result.removeAll(genes);
		return result.toArray(new String[result.size()]);
	}

	private String getStrHg38Coordinates(AnfisaExecuteContext context) {
		Chromosome chromosome = context.variant.chromosome;
		Interval positionHg38 = getHg38Coordinates(context);

		if (positionHg38 == null) {
			return String.format("%s:None", chromosome.toString());
		} else if (positionHg38.isSingle()) {
			return String.format("%s:%s", chromosome.toString(), positionHg38.start);
		} else {
			return String.format("%s:%s-%s",
					chromosome.toString(), positionHg38.start, positionHg38.end
			);
		}
	}

	private Interval getHg38Coordinates(AnfisaExecuteContext context) {
		Chromosome chromosome = context.variant.chromosome;
		return liftoverConnector.toHG38(
				Interval.of(
						chromosome,
						context.variant.getStart(),
						context.variant.end
				)
		);
	}

	public ColorCode.Code getColorCode(VariantVep variantVep, AnfisaResultData data, Record record, AnfisaResultFilters filters) {
		String csq = data.mostSevereConsequence;
		Consequences.Severity msq = Consequences.severity(csq);

		ColorCode.Shape shape;
		if (msq == Consequences.Severity.DAMAGING)
			shape = ColorCode.Shape.CROSS;
		else
			shape = ColorCode.Shape.CIRCLE;

		Set<String> hgmdTags = getHGMDTags(record);
		hgmdTags.retainAll(ImmutableSet.of("DM", "DM?"));
		boolean hgmdDamaging = !hgmdTags.isEmpty();

		ColorCode.Color color;
		if (data.clinvarSignificance != null) {
			boolean benign = filters.clinvarTrustedBenign.orElse(filters.clinvarBenign);
			if (benign) {
				if (shape == ColorCode.Shape.CROSS || hgmdDamaging) {
					color = ColorCode.Color.YELLOW;
				} else {
					color = ColorCode.Color.GREEN;
				}
				return ColorCode.code(shape, color);
			}

			for (String s : data.clinvarSignificance) {
				s = s.toLowerCase();
				if (s.contains("pathogenic") && !s.contains("conflict")) {
					color = ColorCode.Color.RED;
				} else if (s.contains("conflict") || s.contains("uncertain")) {
					color = ColorCode.Color.YELLOW;
				} else {
					continue;
				}

				return ColorCode.code(shape, color);
			}
		}
		if (hgmdDamaging) {
			return ColorCode.code(shape, ColorCode.Color.RED);
		}

		//Применяется модельные in-silico предсказание
		int minValue = 100;
		int maxValue = 0;
		for (String tool : ColorCode.allInSilicoTools()) {
			List<String> rawValues = getFromTranscriptsList(variantVep, tool);
			for (String rawValue : rawValues) {
				int value = ColorCode.inSilicoPrediction(tool, rawValue);
				if (value == 0)
					continue;
				if (value > maxValue)
					maxValue = value;
				if (value < minValue)
					minValue = value;
			}
		}

		if (minValue > maxValue) {
			if (shape == ColorCode.Shape.CROSS) {
				return ColorCode.code(shape, ColorCode.Color.YELLOW);
			}
			return null;
		}
		if (minValue >= 30) {
			color = ColorCode.Color.RED;
		} else if (maxValue <= 10) {
			color = ColorCode.Color.GREEN;
		} else if (minValue < 100) {
			color = ColorCode.Color.YELLOW;
		} else if (shape == ColorCode.Shape.CROSS) {
			return ColorCode.code(shape, ColorCode.Color.YELLOW);
		} else {
			return null;
		}

		return ColorCode.code(shape, color);
	}

	private static boolean isProbandHasAllele(Variant variant, MCase mCase) {
		if (mCase == null || mCase.proband == null) {
			return false;
		}

		List<Allele> probandAlleles = variant.getGenotype(mCase.proband).getAllele();
		if (probandAlleles == null) {
			return false;
		}

		Set<String> set = probandAlleles.stream().map(allele -> allele.getBaseString()).collect(Collectors.toSet());
		return set.contains(variant.getStrAlt());
	}

	private static List<Object> getDistanceFromExon(GtfAnfisaResult gtfAnfisaResult, VariantVep variantVep, Kind kind) {
		GtfAnfisaResult.RegionAndBoundary region = gtfAnfisaResult.getRegion(kind);
		if (region != null) {
			return unique(
					region.distances.stream().map(distance -> distance.dist).collect(Collectors.toList())
			);
		}

		return unique(
				getHgvsList(variantVep, "c", kind.value).stream()
						.map(hgvcs -> getDistanceHgvsc(hgvcs))
						.collect(Collectors.toList()),
				"Exonic"
		);
	}

	private static List<Character> hgvs_signs = Lists.newArrayList('-', '+', '*');

	private static Long getDistanceHgvsc(String hgvcs) {
		String[] sChunks = hgvcs.split(":");
		String coord = null;
		for (int i = 1; i < sChunks.length; i++) {
			String chunk = sChunks[i];
			char ch = chunk.charAt(0);
			if (ch == 'c' || ch == 'p') {
				coord = chunk;
				break;
			}
		}
		if (coord == null) {
			return null;
		}
		String[] xx = coord.split("\\.");
		Integer d = null;
		try {
			for (String x : xx[1].split("_")) {
				Integer sign = null;
				Integer p1 = null;
				Integer p2 = null;
				while (!Character.isDigit(x.charAt(0)) && !hgvs_signs.contains(x.charAt(0))) {
					x = x.substring(1);
				}
				int end = x.length();
				for (int i = 0; i < end; i++) {
					char c = x.charAt(i);
					if (Character.isDigit(c)) {
						continue;
					}
					if (hgvs_signs.contains(c)) {
						int p0 = (sign == null) ? 0 : sign + 1;
						p1 = (i > p0) ? Integer.parseInt(x.substring(p0, i)) : 0;
						sign = i;
					}
					if (Character.isAlphabetic(c)) {
						end = i;
						break;
					}
				}
				if (p1 != null && sign != null) {
					p2 = Integer.parseInt(x.substring(sign + 1, end));
				}
				if (p2 != null) {
					if (d == null || d > p2) {
						d = p2;
					}
				}
			}
			return (d != null) ? d.longValue() : null;
		} catch (Exception e) {
			log.error("Exception ", e);
			return null;
		}
	}

	public String getLabel(AnfisaExecuteContext context) {
		List<String> genes = getGenes((VariantVep) context.variant);
		String gene;
		if (genes.size() == 0) {
			gene = "None";
		} else if (genes.size() < 3) {
			gene = String.join(",", genes);
		} else {
			gene = "...";
		}

		String vstr = str(context);

		return String.format("[%s] %s", gene, vstr);
	}

	private static List<String> getGenes(VariantVep variantVep) {
		return getFromTranscriptsList(variantVep, "gene_symbol");
	}

	protected static List<JSONObject> getMostSevereTranscripts(VariantVep variantVep) {
		String msq = variantVep.getMostSevereConsequence();
		return getTranscripts(variantVep, "protein_coding").stream()
				.filter(jsonObject -> {
					JSONArray consequenceTerms = (JSONArray) jsonObject.get("consequence_terms");
					return (consequenceTerms != null && consequenceTerms.contains(msq));
				})
				.collect(Collectors.toList());
	}

	protected static List<JSONObject> getCanonicalTranscripts(VariantVep variantVep) {
		return getTranscripts(variantVep, "protein_coding").stream()
				.filter(jsonObject -> jsonObject.containsKey("canonical"))
				.collect(Collectors.toList());
	}


	private static List<String> getFromTranscriptsList(VariantVep variantVep, String key) {
		return getFromTranscriptsByBiotype(variantVep, key, "protein_coding");
	}

	private static List<JSONObject> getTranscripts(VariantVep variantVep, String biotype) {
		List<JSONObject> result = new ArrayList<>();
		JSONArray jTranscriptConsequences = variantVep.getTranscriptConsequences();
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

	private static List<String> getFromTranscriptsByBiotype(VariantVep variantVep, String key, String biotype) {
		List<String> result = new ArrayList<>();

		for (JSONObject item : getTranscripts(variantVep, biotype)) {
			Object oValue = item.get(key);
			if (oValue == null) continue;
			if (oValue instanceof JSONArray) {
				for (Object io : (JSONArray) oValue) {
					String i = String.valueOf(io);
					if (!result.contains(i)) {
						result.add(i);
					} else {
						//TODO Ulitin V. Необходимо выяснить, нужна ли такая особенность пострения уникального списка
						//Дело в том, что в python реализации косвенно получался такой результат,
						//непонятно, это сделано специально или нет, если не важна сортировака, то заменить на обычный Set
						result.remove(i);
						result.add(0, i);
					}
				}
			} else {
				String value = String.valueOf(oValue);
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
		}
		return result;
	}

	private static List<String> getFromWorstTranscript(VariantVep variantVep, String key) {
		if ("exon".equals(key) && variantVep instanceof VariantCNV) {
			return ((VariantCNV) variantVep).exonNums;
		} else {
			return unique(
					getMostSevereTranscripts(variantVep).stream()
							.filter(jsonObject -> jsonObject.containsKey(key))
							.map(jsonObject -> jsonObject.getAsString(key))
							.collect(Collectors.toList())
			);
		}
	}

	private static List<String> getFromCanonicalTranscript(VariantVep variantVep, String key) {
		if ("exon".equals(key) && variantVep instanceof VariantCNV) {
			return ((VariantCNV) variantVep).exonNums;
		} else {
			return unique(
					getCanonicalTranscripts(variantVep).stream()
							.filter(jsonObject -> jsonObject.containsKey(key))
							.flatMap(jsonObject -> {
								Object value = jsonObject.get(key);
								if (value instanceof String) {
									return Stream.of((String) value);
								} else if (value instanceof Number) {
									return Stream.of(String.valueOf((Number) value));
								} else {
									return ((JSONArray) value)
											.stream()
											.map(o -> (String) o);
								}
							})
							.collect(Collectors.toList())
			);
		}
	}

	private static List<String> getFromTranscripts(List<JSONObject> transcripts, String key, String source) {
		return unique(
				transcripts.stream()
						.filter(jsonObject -> source.equals(jsonObject.getAsString("source")))
						.map(jsonObject -> jsonObject.getAsString(key))
						.collect(Collectors.toList())
		);
	}

	private static List<String> getCsqFromTranscripts(List<JSONObject> transcripts, String source) {
		return unique(
				transcripts.stream()
						.filter(jsonObject -> source.equals(jsonObject.getAsString("source")))
						.map(jsonObject -> String.format("%s: %s", jsonObject.getAsString("transcript_id"),
								jsonObject.getAsString("consequence_terms")))
						.collect(Collectors.toList())
		);
	}

	private static List<String> getFromTranscripts(VariantVep variantVep, String key, String type) {
		if ("all".equals(type)) {
			return getFromTranscriptsList(variantVep, key);
		} else if ("canonical".equals(type)) {
			return getFromCanonicalTranscript(variantVep, key);
		} else if ("worst".equals(type)) {
			return getFromWorstTranscript(variantVep, key);
		} else {
			throw new RuntimeException("Unknown type: " + type);
		}
	}

	private static List<String> getHgvsList(VariantVep variantVep, String type, String kind) {
		if ("c".equals(type)) {
			return getFromTranscripts(variantVep, "hgvsc", kind);
		} else if ("p".equals(type)) {
			return getFromTranscripts(variantVep, "hgvsp", kind);
		} else {
			List<String> result = new ArrayList<>();
			result.addAll(getFromTranscripts(variantVep, "hgvsc", kind));
			result.addAll(getFromTranscripts(variantVep, "hgvsp", kind));
			return result;
		}
	}

	private static String hgvcsPos(String str, String type, boolean withPattern) {
		String pattern = String.format(":%s.", type);
		if (!str.contains(pattern)) {
			return null;
		}
		String x = str.split(pattern)[1];
		if ("p".equals(type)) {
			x = convert_p(x);
		}

		if (withPattern) {
			return String.format("%s%s", pattern.substring(1), x);
		} else {
			return x;
		}
	}

	private static String convert_p(String x) {
		List<Character> protein1 = new ArrayList<>();
		List<Character> pos = new ArrayList<>();
		List<Character> protein2 = new ArrayList<>();
		int state = 0;
		for (char c : x.toCharArray()) {
			if (state == 0) {
				if (Character.isLetter(c)) {
					protein1.add(c);
					continue;
				}
				state = 2;
			}
			if (state == 2) {
				if (Character.isDigit(c)) {
					pos.add(c);
					continue;
				}
				state = 3;
			}
			if (state == 3) {
				protein2.add(c);
			} else {
				break;
			}
		}
		String p1 = protein1.stream().map(c -> c.toString()).collect(Collectors.joining());
		String p2 = protein2.stream().map(c -> c.toString()).collect(Collectors.joining());
		String rpos = pos.stream().map(c -> c.toString()).collect(Collectors.joining());
		String rprotein1 = proteins_3_to_1.getOrDefault(p1, p1);
		String rprotein2 = proteins_3_to_1.getOrDefault(p2, p2);
		return String.format("%s%s%s", rprotein1, rpos, rprotein2);
	}

	private static List<String> getPos(VariantVep variantVep, String type, String kind) {
		List<String> hgvsList = getHgvsList(variantVep, type, kind);
		List<String> poss = hgvsList.stream()
				.map(hgvcs -> hgvcsPos(hgvcs, type, true))
				.collect(Collectors.toList());
		return unique(poss);
	}

	private static List<String>[] getPosTpl(VariantVep variantVep, String type) {
		if (variantVep instanceof VariantCNV) {
			return new List[]{
					Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
			};
		}

		Set<String> ss = new HashSet<>();

		List<String> c_worst = getPos(variantVep, type, "worst");
		ss.addAll(c_worst);

		List<String> c_canonical = getPos(variantVep, type, "canonical");
		ss.addAll(c_canonical);

		List<String> c_other = getPos(variantVep, type, "all");
		if (c_other.isEmpty()) {
			c_other = Collections.emptyList();
		} else {
			ss.removeAll(c_other);
			c_other = (ss.isEmpty()) ? null : new ArrayList<>(ss);
		}

		return new List[]{
				c_worst, c_canonical, c_other
		};
	}

	public String str(AnfisaExecuteContext context) {
		Variant variant = context.variant;
		String str = getStrHg19Coordinates(context);
		if (isSnv(variant)) {
			return String.format("%s %s>%s",
					str,
					variant.getRef(),
					variant.getStrAlt()
			);
		} else {
			VariantType typeVariable = variant.getVariantType();
			return String.format("%s %s", str, (typeVariable != null) ? typeVariable.toJSON() : "None");
		}
	}

	public Interval getHg19Coordinates(AnfisaExecuteContext context) {
		return Interval.of(
				context.variant.chromosome,
				context.variant.getStart(),
				context.variant.end
		);
	}

	public String getStrHg19Coordinates(AnfisaExecuteContext context) {
		Interval hg19Coordinates = getHg19Coordinates(context);
		return vstr(
				hg19Coordinates
		);
	}

	public String vstr(Interval hg19Coordinates) {
		if (hg19Coordinates.isSingle()) {
			return String.format("%s:%s", hg19Coordinates.chromosome.getChromosome(), hg19Coordinates.start);
		} else {
			return String.format("%s:%s-%s", hg19Coordinates.chromosome.getChromosome(), hg19Coordinates.start, hg19Coordinates.end);
		}
	}

	public boolean isSnv(Variant variant) {
		return variant.getVariantType() == VariantType.SNV;
	}

	private static String encodeToAscii(String s) {
		String regex = "[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+";
		try {
			return new String(s.replaceAll(regex, "").getBytes("ascii"), "ascii");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static Genotypes getGenotypes(Variant variant, MCase mCase) {
		Sample proband = mCase.proband;
		if (proband == null) {
			return null;
		}
		
		String empty = "Can not be determined";
		String probandId = proband.id;
		org.forome.annotation.struct.variant.Genotype gProband = variant.getGenotype(probandId);
		String probandGenotype;
		if (gProband != null && gProband.getGenotypeString() != null) {
			probandGenotype = gProband.getGenotypeString();
		} else {
			probandGenotype = empty;
		}

		String mother = mCase.samples.get(probandId).mother;
		if ("0".equals(mother)) {
			mother = null;
		}
		String maternalGenotype = (mother != null) ? variant.getGenotype(mother).getGenotypeString() : null;
		if (mother != null && maternalGenotype == null) {
			maternalGenotype = empty;
		}

		String father = mCase.samples.get(probandId).father;
		if ("0".equals(father)) {
			father = null;
		}
		String paternalGenotype = (father != null) ? variant.getGenotype(father).getGenotypeString() : null;
		if (father != null && paternalGenotype == null) {
			paternalGenotype = empty;
		}

		String finalProbandGenotype = probandGenotype;
		String finalMaternalGenotype = maternalGenotype;
		String finalPaternalGenotype = paternalGenotype;
		List<String> otherGenotypes = mCase.samples.keySet().stream()
				.map(iSample -> variant.getGenotype(iSample))
				.filter(genotype -> genotype != null)
				.map(genotype -> genotype.getGenotypeString())
				.filter(gtBases -> gtBases != null)
				.filter(gtBases -> !gtBases.equals(finalProbandGenotype))
				.filter(gtBases -> !gtBases.equals(finalMaternalGenotype))
				.filter(gtBases -> !gtBases.equals(finalPaternalGenotype))
				.distinct()
				.collect(Collectors.toList());

		return new Genotypes(probandGenotype, maternalGenotype, paternalGenotype, otherGenotypes);
	}

	private static String getMostSevere(List<String> consequenceTerms) {
		for (String item : AnfisaVariant.CONSEQUENCES) {
			if (consequenceTerms.contains(item)) {
				return item;
			}
		}
		return null;
	}

	private static void sortConsequences(List<String> consequenceTerms) {
		consequenceTerms.sort((o1, o2) -> {
			int i1 = AnfisaVariant.CONSEQUENCES.indexOf(o1);
			int i2 = AnfisaVariant.CONSEQUENCES.indexOf(o2);
			return i1 - i2;
		});
	}

	private static LinkedHashSet<String> getRawCallers(VariantContext variantContext) {
		LinkedHashSet<String> callers = new LinkedHashSet<>();
		CommonInfo commonInfo = variantContext.getCommonInfo();
		for (String caller : AnfisaVariant.CALLERS) {
			if (commonInfo.hasAttribute(caller)) {
				if (AnfisaVariant.BGM_BAYES_DE_NOVO.equals(caller) &&
						Double.parseDouble(commonInfo.getAttribute(caller).toString()) < 0
				) {
					//Отрицательное число, означает, что при работе коллера произошла ошибка…
					callers.add(AnfisaVariant.BGM_BAYES_DE_NOVO_S1);
					continue;
				}
				callers.add(caller);
			}
		}
		return callers;
	}


	private static LinkedHashSet<String> getCallers(Variant variant, MCase samples) {
		if (samples == null || variant == null) {
			return new LinkedHashSet();
		}
		String ref = variant.getRef();
		String alt = variant.getStrAlt();

		LinkedHashSet<String> callers;
		if (variant instanceof VariantVCF) {
			VariantContext variantContext = ((VariantVCF) variant).maVariantVCF.variantContext;
			callers = getRawCallers(variantContext);
		} else if (variant instanceof VariantCNV) {
			callers = new LinkedHashSet();
			callers.add("CNV");
		} else if (variant instanceof VariantCustom) {
			callers = new LinkedHashSet();
		} else {
			throw new RuntimeException("Not support variant: " + variant);
		}

		Genotypes genotypes = getGenotypes(variant, samples);
		if (genotypes == null) {
			return callers;
		}

		String probandGenotype = genotypes.probandGenotype;
		String maternalGenotype = genotypes.maternalGenotype;
		String paternalGenotype = genotypes.paternalGenotype;
		if (probandGenotype == null || maternalGenotype == null || paternalGenotype == null) {
			return callers;
		}

		Set<String> p_set = Arrays.stream(probandGenotype.split("/")).collect(Collectors.toSet());
		Set<String> m_set = Arrays.stream(maternalGenotype.split("/")).collect(Collectors.toSet());
		Set<String> f_set = Arrays.stream(paternalGenotype.split("/")).collect(Collectors.toSet());

		if (p_set.contains(alt) && !m_set.contains(alt) && !f_set.contains(alt)) {
			callers.add("GATK_DE_NOVO");
		}

		if (p_set.size() == 1 && p_set.contains(alt)) {
			if (m_set.size() == 2 && f_set.size() == 2 && m_set.contains(alt) && f_set.contains(alt)) {
				callers.add("GATK_HOMO_REC");
			}
		}

		if (p_set.size() == 1 && p_set.contains(ref)) {
			if (m_set.size() == 2 && f_set.size() == 2 && m_set.contains(ref) && f_set.contains(ref)) {
				callers.add("GATK_HOMOZYGOUS");
			}
		}

		if (callers.isEmpty()) {
			String inheritance = inherited_from(variant, samples);
			if ("De-Novo".equals(inheritance)) {
				throw new RuntimeException("Inconsistent inheritance");
			}
			if (!"Inconclusive".equals(inheritance)) {
				callers.add(String.format("INHERITED_FROM: %s", inheritance));
			}
		}

		return callers;
	}

	private static Map<String, Serializable> getCallersData(Variant variant) {
		if (variant == null || !(variant instanceof VariantVCF)) {
			return Collections.emptyMap();
		}
		VariantContext variantContext = ((VariantVCF) variant).maVariantVCF.variantContext;

		CommonInfo commonInfo = variantContext.getCommonInfo();

		Map<String, Serializable> result = new HashMap<>();
		LinkedHashSet<String> callers = getRawCallers(variantContext);
		for (String c : callers) {
			if (commonInfo.hasAttribute(c)) {
				Object value = commonInfo.getAttribute(c);
				if (value instanceof Boolean) {
					result.put(c, (Boolean) value);
				} else if (value instanceof String) {
					//В первой реализации было приведение к Long, но как оказалось,
					//в файле bgm9001_wgs_xbrowse.vep.vep.json есть значения вещественного типа
					result.put(c, Double.parseDouble((String) value));
				} else {
					throw new RuntimeException("Not support!");
				}
			}
		}
		return result;
	}

	private static String inherited_from(Variant variant, MCase mCase) {
		if (mCase == null || mCase.proband == null) {
			return null;
		}

		/**
		 * Ситуация когда ни один аллель генотипа не относится к иследуемому варианту,
		 * например ситуация, когда мы разрезали мультиалельный вариант
		 */
		List<Allele> probandAlleles = variant.getGenotype(mCase.proband).getAllele();
		if (probandAlleles != null) {
			if (!probandAlleles.contains(variant.getRefAllele()) && !probandAlleles.contains(variant.getAlt())) {
				return "Inconclusive";
			}
		}

		Genotypes genotypes = getGenotypes(variant, mCase);
		if (genotypes == null) {
			return null;
		}
		String probandGenotype = genotypes.probandGenotype;
		String maternalGenotype = genotypes.maternalGenotype;
		String paternalGenotype = genotypes.paternalGenotype;

		if (variant.chromosome.equals(Chromosome.CHR_X) && mCase.proband.sex == Sex.MALE) {
			if (probandGenotype.equals(maternalGenotype)) {
				return "Mother";
			} else {
				return "Inconclusive";
			}
		}

		if (maternalGenotype != null && maternalGenotype.equals(paternalGenotype)) {
			if (probandGenotype.equals(maternalGenotype)) {
				return "Both parents";
			} else {
				return "Possible De-Novo";
			}
		}

		if (probandGenotype != null && probandGenotype.equals(maternalGenotype)) {
			return "Mother";
		}
		if (probandGenotype != null && probandGenotype.equals(paternalGenotype)) {
			return "Father";
		}
		return "Inconclusive";
	}

	private static <T> List<T> unique(List<T> lst) {
		return unique(lst, null);
	}

	/**
	 * TODO Ulitin V. Необходимо выяснить, нужна ли такая особенность пострения уникального списка
	 * Дело в том, что в python реализации метод основан на set - в итоге проблема с сохранением порядка,
	 * непонятно, это сделано специально или нет, если не важна сортировака,
	 * то необходимо избавится от этого метода и перейти на Set
	 *
	 * @param lst
	 * @return
	 */
	private static <T> List<T> unique(List<T> lst, T replace_None) {
		if (lst == null) {
			return lst;
		}
		List<T> result = new ArrayList<>();
		for (T item : lst) {
			if (!result.contains(item)) {
				result.add(item);
			} else {
				result.remove(item);
				result.add(0, item);
			}
		}
		if (replace_None != null) {
			if (result.contains(null)) {
				result.remove(null);
				if (!result.contains(replace_None)) {
					result.add(replace_None);
				}
			}
		}
		return result;
	}

	@Override
	public void close() {

	}
}
