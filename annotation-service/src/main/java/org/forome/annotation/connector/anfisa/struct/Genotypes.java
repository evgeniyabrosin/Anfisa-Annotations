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

package org.forome.annotation.connector.anfisa.struct;

import java.util.Collections;
import java.util.List;

public class Genotypes {

	public final String probandGenotype;
	public final String maternalGenotype;
	public final String paternalGenotype;
	public final List<String> otherGenotypes;

	public Genotypes(String probandGenotype, String maternalGenotype, String paternalGenotype, List<String> otherGenotypes) {
		this.probandGenotype = probandGenotype;
		this.maternalGenotype = maternalGenotype;
		this.paternalGenotype = paternalGenotype;
		this.otherGenotypes = (otherGenotypes!=null)?Collections.unmodifiableList(otherGenotypes):null;
	}
}
