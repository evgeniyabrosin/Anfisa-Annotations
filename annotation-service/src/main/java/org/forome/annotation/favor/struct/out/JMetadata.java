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

package org.forome.annotation.favor.struct.out;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class JMetadata {

    public JMetadata() {

    }

    public JSONObject toJSON() {
        JSONObject out = new JSONObject();
        out.put("modes", new JSONArray(){{
                add("hg38");
        }});
        out.put("data_schema", "FAVOR");
        out.put("record_type", "metadata");
        out.put("versions", new JSONObject(){{
            put("FAVOR", "v0.2");
            put("annotations", "0.6.0");
            put("annotations_build", "0.6.0.37");
        }});
        return out;
    }
}
