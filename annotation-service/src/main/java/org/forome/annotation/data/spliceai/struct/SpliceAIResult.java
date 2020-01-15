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

package org.forome.annotation.data.spliceai.struct;

import java.util.Map;

public class SpliceAIResult {

	public static class DictSql {

		public final int dp_ag;
		public final int dp_al;
		public final int dp_dg;
		public final int dp_dl;
		public final float ds_ag;
		public final float ds_al;
		public final float ds_dg;
		public final float ds_dl;

		public DictSql(
				int dp_ag, int dp_al, int dp_dg, int dp_dl,
				float ds_ag, float ds_al, float ds_dg, float ds_dl
		) {
			this.dp_ag = dp_ag;
			this.dp_al = dp_al;
			this.dp_dg = dp_dg;
			this.dp_dl = dp_dl;
			this.ds_ag = ds_ag;
			this.ds_al = ds_al;
			this.ds_dg = ds_dg;
			this.ds_dl = ds_dl;
		}

		public Number getValue(String key) {
			switch (key) {
				case "DP_AG":
					return dp_ag;
				case "DP_AL":
					return dp_al;
				case "DP_DG":
					return dp_dg;
				case "DP_DL":
					return dp_dl;
				case "DS_AG":
					return ds_ag;
				case "DS_AL":
					return ds_al;
				case "DS_DG":
					return ds_dg;
				case "DS_DL":
					return ds_dl;
				default:
					throw new RuntimeException("Unknown key");
			}
		}
	}

	public final String cases;
	public final Float max_ds;

	public final Map<String, DictSql> dict_sql;

	public SpliceAIResult(String cases, Float max_ds, Map<String, DictSql> dict_sql) {
		this.cases = cases;
		this.max_ds = max_ds;
		this.dict_sql = dict_sql;
	}

}
