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

package org.forome.annotation.service.database.batchrecord.compression.impl;

import org.forome.annotation.service.database.batchrecord.compression.AbstractCompression;
import org.forome.annotation.service.database.batchrecord.compression.exception.NotSupportCompression;

import java.util.List;

public class CompressionEmpty extends AbstractCompression {

	@Override
	public byte[] pack(Class[] types, List<Object[]> items) throws NotSupportCompression {
		for (Object[] values : items) {
			for (int i = 0; i < values.length; i++) {
				Object value = values[i];
				if (value != null) {
					throw new NotSupportCompression();
				}
			}
		}
		return new byte[0];
	}

	public Object[] unpackValues(Class[] types, byte[] bytes, int offsetBytes, int index){
		return new Object[types.length];
	}

	@Override
	public int unpackSize(Class[] types, int sizeInterval, byte[] bytes, int offsetBytes) {
		return 0;
	}
}
