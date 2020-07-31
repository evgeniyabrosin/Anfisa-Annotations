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

package org.forome.annotation.data.astorage.struct;

import net.minidev.json.JSONObject;
import org.forome.annotation.struct.Assembly;

public class AStorageSource {

	public final Assembly assembly;
	public final JSONObject data;

	public AStorageSource(Assembly assembly, JSONObject data) {
		this.assembly = assembly;
		this.data = data;
	}

	public Integer getStart(Assembly assembly) {
		switch (assembly) {
			case GRCh37:
				return getStart37();
			case GRCh38:
				return getStart38();
			default:
				throw new RuntimeException("Unknown assembly: " + assembly);
		}
	}

	public Integer getEnd(Assembly assembly) {
		switch (assembly) {
			case GRCh37:
				return getEnd37();
			case GRCh38:
				return getEnd38();
			default:
				throw new RuntimeException("Unknown assembly: " + assembly);
		}
	}

	public Integer getStart37() {
		Number sourcePos37;
		switch (assembly) {
			case GRCh37:
				sourcePos37 = data.getAsNumber("pos");
				break;
			case GRCh38:
				sourcePos37 = data.getAsNumber("hg19");
				break;
			default:
				throw new RuntimeException("Unknown assembly: " + assembly);
		}
		if (sourcePos37 != null) {
			return sourcePos37.intValue();
		} else {
			return null;
		}
	}

	public Integer getEnd37() {
		Number sourcePos37;
		switch (assembly) {
			case GRCh37:
				sourcePos37 = data.getAsNumber("last");
				break;
			case GRCh38:
				sourcePos37 = data.getAsNumber("hg19-last");
				break;
			default:
				throw new RuntimeException("Unknown assembly: " + assembly);
		}
		if (sourcePos37 != null) {
			return sourcePos37.intValue();
		} else {
			return null;
		}
	}

	public Integer getStart38() {
		Number sourcePos38;
		switch (this.assembly) {
			case GRCh37:
				sourcePos38 = data.getAsNumber("hg38");
				break;
			case GRCh38:
				sourcePos38 = data.getAsNumber("pos");
				break;
			default:
				throw new RuntimeException("Unknown assembly: " + assembly);
		}
		if (sourcePos38 != null) {
			return sourcePos38.intValue();
		} else {
			return null;
		}
	}

	public Integer getEnd38() {
		Number sourcePos38;
		switch (assembly) {
			case GRCh37:
				sourcePos38 = data.getAsNumber("hg38-last");
				break;
			case GRCh38:
				sourcePos38 = data.getAsNumber("last");
				break;
			default:
				throw new RuntimeException("Unknown assembly: " + assembly);
		}
		if (sourcePos38 != null) {
			return sourcePos38.intValue();
		} else {
			return null;
		}
	}
}
