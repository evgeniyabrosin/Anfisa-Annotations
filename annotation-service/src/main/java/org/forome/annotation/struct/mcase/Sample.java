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

package org.forome.annotation.struct.mcase;

public class Sample {

	public final String id;
	public final String name;
	public final String family;
	public final String father;
	public final String mother;
	public final int sex;
	public final boolean affected;

	public final Cohort cohort;

	public Sample(String id, String name, String family, String father, String mother, int sex, boolean affected, Cohort cohort) {
		this.id = id;
		this.name = name;
		this.family = family;
		this.father = father;
		this.mother = mother;
		this.sex = sex;
		this.affected = affected;

		this.cohort = cohort;
		if (cohort != null) {
			cohort.register(this);
		}
	}
}
