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

@GraphQLName("record_view_protein_function")
public class GRecordViewProteinFunction {

	public final Row row;

	public GRecordViewProteinFunction(Row row) {
		this.row = row;
	}

	@GraphQLField
	@GraphQLName("APC_ProteinFunction")
	public String getAPC_ProteinFunction() {
		return GRecord.formatDouble2(row.getValue("APC.ProteinFunction"));
	}

	@GraphQLField
	@GraphQLName("PolyPhenCat")
	public String getPolyPhenCat() {
		return row.getValue("PolyPhenCat");
	}

	@GraphQLField
	@GraphQLName("PolyPhenVal")
	public String getPolyPhenVal() {
		return row.getValue("PolyPhenVal");
	}

	@GraphQLField
	@GraphQLName("polyphen2_hdiv")
	public String getPolyphen2Hdiv() {
		return row.getValue("Polyphen2_HDIV_pred");
	}

	@GraphQLField
	@GraphQLName("polyphen2_hvar")
	public String getPolyphen2Hvar() {
		return row.getValue("Polyphen2_HVAR_pred");
	}

	@GraphQLField
	@GraphQLName("polyphen2_hdiv_score")
	public String getPolyphen2HdivScore() {
		return row.getValue("Polyphen2_HDIV_score");
	}

	@GraphQLField
	@GraphQLName("polyphen2_hvar_score")
	public String getPolyphen2HvarScore() {
		return row.getValue("Polyphen2_HVAR_score");
	}

	@GraphQLField
	@GraphQLName("Grantham")
	public String getGrantham() {
		return row.getValue("Grantham");
	}

	@GraphQLField
	@GraphQLName("MutationTaster")
	public String getMutationTaster() {
		return row.getValue("MutationTaster_score");
	}

	@GraphQLField
	@GraphQLName("MutationAssessor")
	public String getMutationAssessor() {
		return row.getValue("MutationAssessor_score");
	}

	@GraphQLField
	@GraphQLName("SIFTcat")
	public String getSIFTcat() {
		return row.getValue("SIFTcat");
	}

	@GraphQLField
	@GraphQLName("SIFTval")
	public String getSIFTval() {
		return row.getValue("SIFTval");
	}


}
