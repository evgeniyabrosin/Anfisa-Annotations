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

package org.forome.annotation.favor.processing.graphql.record.filters;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.forome.annotation.data.hgmd.HgmdConnector;
import org.forome.annotation.favor.processing.graphql.record.GRecord;
import org.forome.annotation.favor.processing.struct.GContext;
import org.forome.annotation.favor.utils.struct.table.Row;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@GraphQLName("record_filters")
public class GRecordFilters {

	public final GContext gContext;
	public final Row row;

	private final String[] splitvariantFormat;

	public GRecordFilters(GContext gContext) {
		this.gContext = gContext;
		this.row = gContext.row;

		String variantFormat = row.getValue("variant_format");
		splitvariantFormat = variantFormat.split("-");
		if (splitvariantFormat.length != 4) throw new RuntimeException();
	}

	@GraphQLField
	@GraphQLName("_ord")
	public int getOrd() {
		return row.order;
	}

	@GraphQLField
	@GraphQLName("chromosome")
	public String getChromosome() {
		return splitvariantFormat[0];
	}

	@GraphQLField
	@GraphQLName("position")
	public int getStart() {
		return gContext.getVariant().getStart();
	}

	@GraphQLField
	@GraphQLName("ref")
	public String getRef() {
		return splitvariantFormat[2];
	}

	@GraphQLField
	@GraphQLName("alt")
	public String getAlt() {
		return splitvariantFormat[3];
	}

	@GraphQLField
	@GraphQLName("genes")
	public String[] getGenes() {
		return new String[]{ row.getValue("GeneName") };
	}

	@GraphQLField
	@GraphQLName("gnomad_total_af")
	public double getGnomadTotalAf() {
		String value = row.getValue("GNOMAD_total");
		if (value == null) return 0;
		return Double.parseDouble(value);
	}

	@GraphQLField
	@GraphQLName("gencode_category")
	public String[] getGencodeCategory() {
		String value = row.getValue("GENCODE.Category");
		if (value == null) return null;
		return Arrays.stream(value.split(";"))
				.map(s -> s.trim())
				.filter(s -> !s.isEmpty())
				.distinct()
				.toArray(String[]::new);
	}

	@GraphQLField
	@GraphQLName("gencode_exonic_category")
	public String getGencodeExonicCategory() {
		return row.getValue("GENCODE.EXONIC.Category");
	}

	@GraphQLField
	@GraphQLName("polyphen2_hdiv")
	public String getPolyphen2HDIV() {
		String value = row.getValue("Polyphen2_HDIV_pred");
		if (".".equals(value)) return null;
		return value;
	}

	@GraphQLField
	@GraphQLName("polyphen2_hvar")
	public String getPolyphen2HVAR() {
		String value = row.getValue("Polyphen2_HVAR_pred");
		if (".".equals(value)) return null;
		return value;
	}

	@GraphQLField
	@GraphQLName("polyphen_cat")
	public String getPolyPhenCat() {
		return row.getValue("PolyPhenCat");
	}

	@GraphQLField
	@GraphQLName("sift_cat")
	public String getSiftCat() {
		return row.getValue("SIFTcat");
	}

	@GraphQLField
	@GraphQLName("clinvar")
	public String[] getClinvar() {
		String value = row.getValue("clinvar");
		if (value == null) return null;
		return Arrays.stream(value.split("\\|"))
				.map(s -> s.trim())
				.filter(s -> !s.isEmpty())
				.distinct()
				.toArray(String[]::new);
	}

	@GraphQLField
	@GraphQLName("hgmd_benign")
	public boolean getHgmdBenign() {
		HgmdConnector.Data hgmdData = gContext.getHgmdData();
		List<String> tags = hgmdData.hgmdPmidRows.stream().map(hgmdPmidRow -> hgmdPmidRow.tag).collect(Collectors.toList());
		return (tags.size() == 0);
	}

	@GraphQLField
	@GraphQLName("hgmd_tags")
	public String[] getHgmdTags() {
		HgmdConnector.Data hgmdData = gContext.getHgmdData();
		return hgmdData.hgmdPmidRows.stream().map(hgmdPmidRow -> hgmdPmidRow.tag).distinct().toArray(String[]::new);
	}

	@GraphQLField
	@GraphQLName("top_med_qc_status")
	public String[] getTOPMedQCStatus() {
		String value = row.getValue("FilterStatus");
		return value.split(";");
	}

	@GraphQLField
	@GraphQLName("top_med_bravo_af")
	public Double getTOPMedBravoAf() {
		return GRecord.toDouble(row.getValue("Bravo_AF"));
	}

	@GraphQLField
	@GraphQLName("exac03")
	public Double getExAC03() {
		return GRecord.toDouble(row.getValue("ExAC03"));
	}

	@GraphQLField
	@GraphQLName("disruptive_missense")
	public String getDisruptiveMissense() {
		return row.getValue("lof.in.nonsynonymous");
	}

	@GraphQLField
	@GraphQLName("cage_promoter")
	public String getCagePromoter() {
		return row.getValue("CAGE.Promoter");
	}

	@GraphQLField
	@GraphQLName("cage_enhancer")
	public String getCageEnhancer() {
		return row.getValue("CAGE.Enhancer");
	}

	@GraphQLField
	@GraphQLName("gene_hancer")
	public String getGeneHancer() {
		return row.getValue("GeneHancer");
	}

	@GraphQLField
	@GraphQLName("super_enhancer")
	public String getSuperEnhancer() {
		return row.getValue("SuperEnhancer");
	}

	@GraphQLField
	@GraphQLName("bstatistics")
	public Double getBStatistics() {
		return GRecord.toDouble(row.getValue("bStatistics"));
	}

	@GraphQLField
	@GraphQLName("freq1000bp")
	public Double getFreq1000bp() {
		return GRecord.toDouble(row.getValue("Freq1000bp"));
	}

	@GraphQLField
	@GraphQLName("rare1000bp")
	public Double getRare1000bp() {
		return GRecord.toDouble(row.getValue("Rare1000bp"));
	}

	@GraphQLField
	@GraphQLName("gc")
	public Double getGC() {
		return GRecord.toDouble(row.getValue("GC"));
	}

	@GraphQLField
	@GraphQLName("cpg")
	public Double getCpG() {
		return GRecord.toDouble(row.getValue("CpG"));
	}
}
