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
import org.forome.annotation.favor.processing.graphql.record.GRecord;
import org.forome.annotation.favor.utils.struct.table.Row;

@GraphQLName("record_view_integrative_score")
public class GRecordViewIntegrativeScore {

	public final Row row;

	public GRecordViewIntegrativeScore(Row row) {
		this.row = row;
	}

	@GraphQLField
	@GraphQLName("LINSIGHT")
	public String getLINSIGHT() {
		return GRecord.formatDouble2(row.getValue("LINSIGHT"));
	}

	@GraphQLField
	@GraphQLName("FATHMM_XF_coding")
	public String getFATHMM_XF_coding() {
		return GRecord.formatDouble2(row.getValue("FATHMM.XF.coding"));
	}

	@GraphQLField
	@GraphQLName("FATHMM_XF_noncoding")
	public String getFATHMM_XF_noncoding() {
		return GRecord.formatDouble2(row.getValue("FATHMM.XF.noncoding"));
	}

	@GraphQLField
	@GraphQLName("CADD_RawScore")
	public String getCADD_RawScore() {
		return GRecord.formatDouble2(row.getValue("CADD.RawScore"));
	}

	@GraphQLField
	@GraphQLName("CADD_PHRED")
	public String getCADD_PHRED() {
		return GRecord.formatDouble2(row.getValue("CADD.PHRED"));
	}

	@GraphQLField
	@GraphQLName("APC_ProteinFunction")
	public String getAPC_ProteinFunction() {
		return GRecord.formatDouble2(row.getValue("APC.ProteinFunction"));
	}

	@GraphQLField
	@GraphQLName("APC_Conservation")
	public String getAPC_Conservation() {
		return GRecord.formatDouble2(row.getValue("APC.Conservation"));
	}

	@GraphQLField
	@GraphQLName("APC_Epigenetics")
	public String getAPC_Epigenetics() {
		return GRecord.formatDouble2(row.getValue("APC.Epigenetics"));
	}

	@GraphQLField
	@GraphQLName("APC_Local_Nucleotide_Diversity")
	public String getAPC_Local_Nucleotide_Diversity() {
		return GRecord.formatDouble2(row.getValue("APC.Local.Diversity"));
	}

	@GraphQLField
	@GraphQLName("APC_MutationDensity")
	public String getAPC_MutationDensity() {
		return GRecord.formatDouble2(row.getValue("APC.MutationDensity"));
	}

	@GraphQLField
	@GraphQLName("APC_TranscriptionFactor")
	public String getAPC_TranscriptionFactor() {
		return GRecord.formatDouble2(row.getValue("APC.TranscriptionFactor"));
	}

	@GraphQLField
	@GraphQLName("APC_MapAbility")
	public String getAPC_MapAbility() {
		return GRecord.formatDouble2(row.getValue("APC.MapAbility"));
	}

	@GraphQLField
	@GraphQLName("APC_Proximity_To_TSSTES")
	public String getAPC_Proximity_To_TSSTES() {
		return GRecord.formatDouble2(row.getValue("APC.DistanceTSSTES"));
	}

	@GraphQLField
	@GraphQLName("gc")
	public String getGC() {
		return GRecord.formatDouble2(row.getValue("GC"));
	}

	@GraphQLField
	@GraphQLName("cpg")
	public String getCpG() {
		return GRecord.formatDouble2(row.getValue("CpG"));
	}
}
