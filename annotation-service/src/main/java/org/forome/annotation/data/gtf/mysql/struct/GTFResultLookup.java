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

package org.forome.annotation.data.gtf.mysql.struct;

public class GTFResultLookup {

    public final String transcript;
    public final String gene;
    public final long position;
    public final String region;
    public final Integer index;

    public GTFResultLookup(String transcript, String gene, long position, String region, Integer index) {
        this.transcript = transcript;
        this.gene = gene;
        this.position = position;
        this.region = region;
        this.index = index;
    }
}
