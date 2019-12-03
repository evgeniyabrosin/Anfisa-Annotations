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

package org.forome.annotation.connector.anfisa.struct;

public class Consequences {

    public enum Severity {
        DAMAGING,
        MISSENSE,
        OTHER
    }

    public static Severity severity(String csq){
        if (AnfisaVariant.CSQ_DAMAGING.contains (csq)) {
            return Severity.DAMAGING;
        } else if (AnfisaVariant.CSQ_MISSENSE.contains (csq)){
            return Severity.MISSENSE;
        } else {
            return Severity.OTHER;
        }
    }
}
