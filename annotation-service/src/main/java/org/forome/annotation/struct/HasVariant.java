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

package org.forome.annotation.struct;

/*
REF - референс
ALT - альтернативный аллель
ALTk - альтернативный аллель не относящийся к обрабатываемому single варианту

Например:
chr1:100818464 T>C,A
В первом single вариант: REF - T, ALT - C, ALTk - A
Во втором single вариант: REF - T, ALT - A, ALTk - C
*/
public enum HasVariant {

	//Какая то совсем странная ситуация
	MIXED(0),

	REF_REF(0),

	ALTki_ALTkj(0),

	// REF/ALT vs ALT/REF
	REF_ALT(1),

	//ALT/ALTk vs ALTk/ALT
	ALT_ALTki(2);

	private final int outValue;

	HasVariant(int outValue) {
		this.outValue = outValue;
	}

	public int getOutValue() {
		return outValue;
	}
}
