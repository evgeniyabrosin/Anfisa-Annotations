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

package org.forome.annotation.annotator.struct;

import com.google.common.base.Strings;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLine;
import io.reactivex.Observable;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.forome.annotation.data.anfisa.AnfisaConnector;
import org.forome.annotation.processing.struct.ProcessingResult;
import org.forome.annotation.struct.SourceMetadata;
import org.forome.annotation.struct.mcase.Cohort;
import org.forome.annotation.struct.mcase.MCase;
import org.forome.annotation.struct.mcase.Sample;
import org.forome.annotation.struct.mcase.Sex;
import org.forome.annotation.utils.AppVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnnotatorResult {

	private final static Logger log = LoggerFactory.getLogger(AnnotatorResult.class);

	public static class Metadata {

		public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
				.withZone(ZoneId.systemDefault());

		public static class Versions {

			private static Pattern PATTERN_GATK_VERSION = Pattern.compile(
					"^<ID=(ApplyRecalibration|CombineVariants),Version=(.*?)[,](.*)$", Pattern.CASE_INSENSITIVE
			);

			public final Instant pipelineDate;
			public final String pipeline;
			public final String annotations;
			public final String annotationsBuild;
			public final String reference;

			public final List<SourceMetadata> metadataDatabases;

			private final String toolGatksCombineVariants;
			private final String toolGatksApplyRecalibration;
			private final String bcftoolsAnnotateVersion;

			public Versions(Path pathVepVcf, AnfisaConnector anfisaConnector) {
				annotations = AppVersion.getVersionFormat();
				annotationsBuild = AppVersion.getVersion();
				if (pathVepVcf != null) {
					VCFFileReader vcfFileReader = new VCFFileReader(pathVepVcf, false);
					VCFHeader vcfHeader = vcfFileReader.getFileHeader();

					VCFHeaderLine hlPipeline = vcfHeader.getOtherHeaderLine("source");
					pipeline = (hlPipeline != null) ? hlPipeline.getValue() : null;

					VCFHeaderLine hlReference = vcfHeader.getOtherHeaderLine("reference");
					reference = (hlReference != null) ? hlReference.getValue() : null;

					VCFHeaderLine hlPipelineDate = vcfHeader.getOtherHeaderLine("fileDate");
					if (hlPipelineDate != null) {
						try {
							pipelineDate = new SimpleDateFormat("yyyyMMdd").parse(hlPipelineDate.getValue()).toInstant();
						} catch (ParseException e) {
							throw new RuntimeException(e);
						}
					} else {
						pipelineDate = null;
					}

					VCFHeaderLine hlGatkCV = vcfHeader.getMetaDataLine("GATKCommandLine.CombineVariants");
					if (hlGatkCV != null) {
						Matcher matcher = PATTERN_GATK_VERSION.matcher(hlGatkCV.getValue());
						if (!matcher.matches()) {
							throw new RuntimeException("Not support format GATK version: " + hlGatkCV.getValue());
						}
						toolGatksCombineVariants = matcher.group(2);
					} else {
						toolGatksCombineVariants = null;
					}
					VCFHeaderLine hlGatkAR = vcfHeader.getMetaDataLine("GATKCommandLine.ApplyRecalibration");
					if (hlGatkAR != null) {
						Matcher matcher = PATTERN_GATK_VERSION.matcher(hlGatkAR.getValue());
						if (!matcher.matches()) {
							throw new RuntimeException("Not support format GATK version: " + hlGatkAR.getValue());
						}
						toolGatksApplyRecalibration = matcher.group(2);
					} else {
						toolGatksApplyRecalibration = null;
					}

					VCFHeaderLine hlBCFAnnotateVersion = vcfHeader.getMetaDataLine("bcftools_annotateVersion");
					if (hlBCFAnnotateVersion != null) {
						bcftoolsAnnotateVersion = hlBCFAnnotateVersion.getValue();
					} else {
						bcftoolsAnnotateVersion = null;
					}
				} else {
					pipeline = null;
					reference = null;
					pipelineDate = null;
					toolGatksApplyRecalibration = null;
					toolGatksCombineVariants = null;
					bcftoolsAnnotateVersion = null;
				}

				metadataDatabases = new ArrayList<>();
				metadataDatabases.addAll(anfisaConnector.clinvarConnector.getSourceMetadata());
				metadataDatabases.addAll(anfisaConnector.hgmdConnector.getSourceMetadata());
				metadataDatabases.addAll(anfisaConnector.spliceAIConnector.getSourceMetadata());
				metadataDatabases.addAll(anfisaConnector.conservationData.getSourceMetadata());
				metadataDatabases.addAll(anfisaConnector.gnomadConnector.getSourceMetadata());
				metadataDatabases.addAll(anfisaConnector.gtexConnector.getSourceMetadata());
				metadataDatabases.addAll(anfisaConnector.pharmGKBConnector.getSourceMetadata());
				metadataDatabases.sort(Comparator.comparing(o -> o.product));
			}

			private JSONObject toJSON() {
				JSONObject out = new JSONObject();
				if (pipelineDate != null) {
					out.put("pipeline_date", DATE_TIME_FORMATTER.format(pipelineDate));
				}
				out.put("annotations_date", DATE_TIME_FORMATTER.format(Instant.now()));
				out.put("pipeline", pipeline);
				out.put("annotations", annotations);
				out.put("annotations_build", annotationsBuild);
				out.put("reference", reference);
				for (SourceMetadata metadata : metadataDatabases) {
					StringBuilder value = new StringBuilder();
					if (metadata.version != null) {
						value.append(metadata.version);
						if (metadata.date != null) {
							value.append(" | ");
						}
					}
					if (metadata.date != null) {
						value.append(DATE_TIME_FORMATTER.format(metadata.date));
					}
					out.put(metadata.product, value.toString());
				}
				if (!Strings.isNullOrEmpty(toolGatksApplyRecalibration)) {
					out.put("gatks_apply_recalibration", toolGatksApplyRecalibration);
				}
				if (!Strings.isNullOrEmpty(toolGatksCombineVariants)) {
					out.put("gatks_combine_variants", toolGatksCombineVariants);
				}
				if (!Strings.isNullOrEmpty(bcftoolsAnnotateVersion)) {
					out.put("bcftools_annotate_version", bcftoolsAnnotateVersion);
				}
				return out;
			}
		}

		public final String recordType = "metadata";
		public final String caseSequence;
		public final MCase mCase;
		public final Versions versions;

		public Metadata(String caseSequence, Path pathVepVcf, MCase mCase, AnfisaConnector anfisaConnector) {
			this.caseSequence = caseSequence;
			this.mCase = mCase;
			this.versions = new Versions(pathVepVcf, anfisaConnector);
		}

		public static Metadata build(String caseSequence, Path pathVepVcf, MCase samples, AnfisaConnector anfisaConnector) {
			return new Metadata(caseSequence, pathVepVcf, samples, anfisaConnector);
		}

		public JSONObject toJSON() {
			JSONObject out = new JSONObject();
			out.put("case", caseSequence);
			out.put("record_type", recordType);
			out.put("versions", versions.toJSON());
			if (mCase.proband != null) {
				out.put("proband", mCase.proband.id);
			} else {
				out.put("proband", null);
			}
			out.put("samples", new JSONObject() {{
				for (Sample sample : mCase.samples.values()) {
					put(sample.name, build(sample));
				}
			}});
			out.put("cohorts", new JSONArray() {{
				for (Cohort cohort : mCase.cohorts) {
					add(new JSONObject() {{
						put("name", cohort.name);
						put("members", new JSONArray() {{
							for (Sample sample : cohort.getSamples()) {
								add(sample.name);
							}
						}});
					}});
				}
			}});
			return out;
		}

		public static JSONObject build(Sample sample) {
			JSONObject out = new JSONObject();
			out.put("affected", sample.affected);
			out.put("name", sample.name);
			out.put("family", sample.family);
			out.put("father", sample.father);
			if (sample.sex == Sex.UNKNOWN) {
				out.put("sex", 0);
			} else if (sample.sex == Sex.MALE) {
				out.put("sex", 1);
			} else if (sample.sex == Sex.FEMALE) {
				out.put("sex", 2);
			} else {
				throw new RuntimeException("Unknown sex: " + sample.sex);
			}
			out.put("mother", sample.mother);
			out.put("id", sample.id);
			return out;
		}
	}

	public final Metadata metadata;
	public final Observable<ProcessingResult> observableAnfisaResult;

	public AnnotatorResult(Metadata metadata, Observable<ProcessingResult> observableAnfisaResult) {
		this.metadata = metadata;
		this.observableAnfisaResult = observableAnfisaResult;
	}
}
