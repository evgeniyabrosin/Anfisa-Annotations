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

package org.forome.annotation.data.anfisa.struct;

import org.forome.annotation.data.clinvar.struct.ClinvarResult;
import org.forome.annotation.data.hgmd.HgmdConnector;

import java.util.List;

public class Record {

	public List<ClinvarResult> clinvarResults;
	public HgmdConnector.Data hgmdData;

}
