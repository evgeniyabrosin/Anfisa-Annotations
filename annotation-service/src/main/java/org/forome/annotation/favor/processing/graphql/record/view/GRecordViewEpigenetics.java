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

@GraphQLName("record_view_epigenetics")
public class GRecordViewEpigenetics {

	enum Mode { STATUS, VALUE}

	private final Mode mode;
	public final Row row;

	public GRecordViewEpigenetics(Mode mode, Row row) {
		this.mode=mode;
		this.row = row;
	}

	@GraphQLField
	@GraphQLName("APC_Epigenetics")
	public String getAPC_Epigenetics() {
		if (mode == Mode.STATUS) {
			return "-";
		} else if (mode == Mode.VALUE) {
			return GRecord.formatDouble2(row.getValue("APC.Epigenetics"));
		} else {
			throw new RuntimeException();
		}
	}

	@GraphQLField
	@GraphQLName("DNase")
	public String getDNase() {
		if (mode == Mode.STATUS) {
			return "Open";
		} else if (mode == Mode.VALUE) {
			return GRecord.formatDouble2(row.getValue("EncodeDNase.sum"));
		} else {
			throw new RuntimeException();
		}
	}

	@GraphQLField
	@GraphQLName("H3K27ac")
	public String getH3K27ac() {
		if (mode == Mode.STATUS) {
			return "Open";
		} else if (mode == Mode.VALUE) {
			return GRecord.formatDouble2(row.getValue("EncodeH3K27ac.sum"));
		} else {
			throw new RuntimeException();
		}
	}

	@GraphQLField
	@GraphQLName("H3K4me1")
	public String getH3K4me1() {
		if (mode == Mode.STATUS) {
			return "Open";
		} else if (mode == Mode.VALUE) {
			return GRecord.formatDouble2(row.getValue("EncodeH3K4me1.sum"));
		} else {
			throw new RuntimeException();
		}
	}

	@GraphQLField
	@GraphQLName("H3K4me2")
	public String getH3K4me2() {
		if (mode == Mode.STATUS) {
			return "Open";
		} else if (mode == Mode.VALUE) {
			return GRecord.formatDouble2(row.getValue("EncodeH3K4me2.sum"));
		} else {
			throw new RuntimeException();
		}
	}

	@GraphQLField
	@GraphQLName("H3K4me3")
	public String getH3K4me3() {
		if (mode == Mode.STATUS) {
			return "Open";
		} else if (mode == Mode.VALUE) {
			return GRecord.formatDouble2(row.getValue("EncodeH3K4me3.sum"));
		} else {
			throw new RuntimeException();
		}
	}

	@GraphQLField
	@GraphQLName("H3K9ac")
	public String getH3K9ac() {
		if (mode == Mode.STATUS) {
			return "Open";
		} else if (mode == Mode.VALUE) {
			return GRecord.formatDouble2(row.getValue("EncodeH3K9ac.sum"));
		} else {
			throw new RuntimeException();
		}
	}

	@GraphQLField
	@GraphQLName("H4K20me1")
	public String getH4K20me1() {
		if (mode == Mode.STATUS) {
			return "Open";
		} else if (mode == Mode.VALUE) {
			return GRecord.formatDouble2(row.getValue("EncodeH4K20me1.sum"));
		} else {
			throw new RuntimeException();
		}
	}

	@GraphQLField
	@GraphQLName("H2AFZ")
	public String getH2AFZ() {
		if (mode == Mode.STATUS) {
			return "Open";
		} else if (mode == Mode.VALUE) {
			return GRecord.formatDouble2(row.getValue("EncodeH2AFZ.sum"));
		} else {
			throw new RuntimeException();
		}
	}

	@GraphQLField
	@GraphQLName("H3K9me3")
	public String getH3K9me3() {
		if (mode == Mode.STATUS) {
			return "Closed";
		} else if (mode == Mode.VALUE) {
			return GRecord.formatDouble2(row.getValue("EncodeH3K9me3.sum"));
		} else {
			throw new RuntimeException();
		}
	}

	@GraphQLField
	@GraphQLName("H3K27me3")
	public String getH3K27me3() {
		if (mode == Mode.STATUS) {
			return "Closed";
		} else if (mode == Mode.VALUE) {
			return GRecord.formatDouble2(row.getValue("EncodeH3K27me3.sum"));
		} else {
			throw new RuntimeException();
		}
	}

	@GraphQLField
	@GraphQLName("H3K36me3")
	public String getH3K36me3() {
		if (mode == Mode.STATUS) {
			return "Transcription";
		} else if (mode == Mode.VALUE) {
			return GRecord.formatDouble2(row.getValue("EncodeH3K36me3.sum"));
		} else {
			throw new RuntimeException();
		}
	}

	@GraphQLField
	@GraphQLName("H3K79me2")
	public String getH3K79me2() {
		if (mode == Mode.STATUS) {
			return "Transcription";
		} else if (mode == Mode.VALUE) {
			return GRecord.formatDouble2(row.getValue("EncodeH3K79me2.sum"));
		} else {
			throw new RuntimeException();
		}
	}

	@GraphQLField
	@GraphQLName("totalRNA")
	public String gettotalRNA() {
		if (mode == Mode.STATUS) {
			return "Transcription";
		} else if (mode == Mode.VALUE) {
			return GRecord.formatDouble2(row.getValue("EncodetotalRNA.sum"));
		} else {
			throw new RuntimeException();
		}
	}
}
