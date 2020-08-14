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

package org.forome.annotation.utils.vcf.merge.struct;

import java.util.Objects;

public class Patient {

	public final String patientId;
	public final String patientCode;

	public final String targetPatientId;

	public Patient(String patientId, String patientCode, String targetPatientId) {
		this.patientId = patientId;
		this.patientCode = patientCode;
		this.targetPatientId = targetPatientId;
	}

	public String getSourcePatientId(){
		return patientId + "_" + patientCode;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Patient patient = (Patient) o;
		return Objects.equals(patientId, patient.patientId) &&
				Objects.equals(patientCode, patient.patientCode) &&
				Objects.equals(targetPatientId, patient.targetPatientId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(patientId, patientCode, targetPatientId);
	}

	@Override
	public String toString() {
		return "Patient{" +
				"sourcePatientId='" + getSourcePatientId() + '\'' +
				'}';
	}
}
