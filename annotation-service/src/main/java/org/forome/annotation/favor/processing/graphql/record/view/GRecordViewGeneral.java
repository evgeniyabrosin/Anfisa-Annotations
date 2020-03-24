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

package org.forome.annotation.favor.processing.graphql.record.view;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.forome.annotation.data.hgmd.HgmdConnector;
import org.forome.annotation.favor.processing.graphql.record.GRecord;
import org.forome.annotation.favor.processing.struct.GContext;
import org.forome.annotation.favor.utils.struct.table.Row;

import java.util.List;

@GraphQLName("record_view_general")
public class GRecordViewGeneral {

	public final GContext gContext;
	public final Row row;

	public GRecordViewGeneral(GContext gContext) {
		this.gContext = gContext;
		this.row = gContext.row;
	}

	@GraphQLField
	@GraphQLName("Variant")
	public String getVariant() {
		return row.getValue("variant_format");
	}

	@GraphQLField
	@GraphQLName("rsID")
	public String getRsID() {
		return row.getValue("rsID");
	}

	@GraphQLField
	@GraphQLName("TOPMed_Depth")
	public String getTOPMed_Depth() {
		return GRecord.formatDouble2(row.getValue("TOPMedDP"));
	}

	@GraphQLField
	@GraphQLName("TOPMed_QC_Status")
	public String getTOPMed_QC_Status() {
		return row.getValue("FilterStatus");
	}

	@GraphQLField
	@GraphQLName("TOPMed_Bravo_AF")
	public String getTOPMed_Bravo_AF() {
		return row.getValue("Bravo_AF");
	}

	@GraphQLField
	@GraphQLName("GNOMAD_Total_AF")
	public String getGNOMAD_Total_AF() {
		return row.getValue("GNOMAD_total");
	}

	@GraphQLField
	@GraphQLName("ALL_1000G_AF")
	public String getALL_1000G_AF() {
		return row.getValue("TG_ALL");
	}

	@GraphQLField
	@GraphQLName("genes")
	public String[] getGenes() {
		return gContext.getGenes();
	}

	@GraphQLField
	@GraphQLName("hgmd")
	public String getHgmd() {
		List<String> accNums = gContext.getHgmdAccNums();
		if (accNums.isEmpty()) return null;
		return String.join(",", accNums);
	}

	@GraphQLField
	@GraphQLName("hgmd_hg38")
	public String getHgmdHg38() {
		String hgmdAccNumHg38 = gContext.getHgmdAccNumHg38();
		if (hgmdAccNumHg38.isEmpty()) return null;
		return hgmdAccNumHg38;
	}

	@GraphQLField
	@GraphQLName("hgmd_tags")
	public String[] getHgmdTags() {
		HgmdConnector.Data hgmdData = gContext.getHgmdData();
		return hgmdData.hgmdPmidRows.stream().map(hgmdPmidRow -> hgmdPmidRow.tag).distinct().toArray(String[]::new);
	}

	@GraphQLField
	@GraphQLName("hgmd_phenotypes")
	public String[] getHgmdPhenotypes() {
		HgmdConnector.Data hgmdData = gContext.getHgmdData();
		return hgmdData.phenotypes.stream().toArray(String[]::new);
	}

	@GraphQLField
	@GraphQLName("hgmd_pmids")
	public String[] getHgmdPmids() {
		HgmdConnector.Data hgmdData = gContext.getHgmdData();
		return hgmdData.hgmdPmidRows.stream()
			.map(hgmdPmidRow -> hgmdPmidRow.pmid).toArray(String[]::new);
	}
}
