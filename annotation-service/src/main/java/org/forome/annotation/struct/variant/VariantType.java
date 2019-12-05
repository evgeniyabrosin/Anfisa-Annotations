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

package org.forome.annotation.struct.variant;

public enum VariantType {

	SNV("SNV"),

	INDEL("indel"),

	SEQUENCE_ALTERATION("sequence_alteration"),

	/** Deletion relative to the reference */
	DEL("deletion"),

	/** Insertion of novel sequence relative to the reference */
	INS("insertion"),

	SUBSTITUTION("substitution"),

	/** Copy number variable region */
	CNV("CNV: deletion");

	private final String jsonValue;

	VariantType(String jsonValue) {
		this.jsonValue = jsonValue;
	}

	public String toJSON(){
		return jsonValue;
	}

	public static VariantType findByName(String value) {
		for (VariantType item: VariantType.values()) {
			if (item.toJSON().equals(value)) {
				return item;
			}
		}
		throw new RuntimeException("Unknown type: " + value);
	}
}
