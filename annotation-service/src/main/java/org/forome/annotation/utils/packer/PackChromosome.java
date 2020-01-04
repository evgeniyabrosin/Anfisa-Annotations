/*
 *  Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association
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

package org.forome.annotation.utils.packer;

import org.forome.annotation.struct.Chromosome;

public class PackChromosome {

	private static final byte BYTE_CHR_X = 24;

	private static final byte BYTE_CHR_Y = 25;

	public static Chromosome fromByte(byte value) {
		if (value == BYTE_CHR_X) {
			return Chromosome.CHR_X;
		} else if (value == BYTE_CHR_Y) {
			return Chromosome.CHR_Y;
		} else {
			/**
			for (Chromosome chromosome: Chromosome.values()) {
				if (value == Byte.parseByte(chromosome.getChar())) {
					return chromosome;
				}
			}
			 */
		}
		throw new IllegalArgumentException();
	}

	public static byte toByte(Chromosome value) {
		if (value == Chromosome.CHR_X) {
			return BYTE_CHR_X;
		} else if (value == Chromosome.CHR_Y) {
			return BYTE_CHR_Y;
		} else {
			return Byte.parseByte(value.getChar());
		}
	}
}
