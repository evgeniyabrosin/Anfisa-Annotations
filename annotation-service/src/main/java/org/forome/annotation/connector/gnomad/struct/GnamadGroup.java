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

package org.forome.annotation.connector.gnomad.struct;

import java.util.Arrays;

public enum GnamadGroup {

    AFR(Type.GENERAL),
    AMR(Type.GENERAL),
    EAS(Type.GENERAL),
    NFE(Type.GENERAL),
    SAS(Type.GENERAL),//South Asia

    ASJ(Type.THOROUGHBRED),
    FIN(Type.THOROUGHBRED),
    OTH(Type.THOROUGHBRED),
    RAW(Type.THOROUGHBRED);

    public enum Type {
        GENERAL,
        THOROUGHBRED
    }

    public final Type type;

    GnamadGroup(Type type) {
        this.type = type;
    }

    public static GnamadGroup[] getByType(Type type) {
        return Arrays.stream(GnamadGroup.values())
                .filter(gnamadGroup -> gnamadGroup.type == type)
                .toArray(value -> new GnamadGroup[value]);
    }
}
