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

package org.forome.annotation.service.database.struct.batch;

import com.google.common.annotations.VisibleForTesting;
import org.forome.annotation.struct.Interval;
import org.forome.annotation.struct.Position;
import org.forome.annotation.utils.bits.IntegerBits;
import org.forome.annotation.utils.bits.ShortBits;

import java.util.Arrays;

public class BatchRecordConservation {

	public static final short SHORT_NULL_VALUE = Short.MIN_VALUE;
	public static final int INT_NULL_VALUE = Integer.MIN_VALUE;

	//Порядок сохраняемых значений и их размер в байтах
	private static final int[] FIELD_BYTE_SIZE = new int[]{
			2,//priPhCons
			2,//mamPhCons
			2,//verPhCons
			2,//priPhyloP
			2,//mamPhyloP
			2,//verPhyloP
			4,//gerpRS
			4,//gerpRSpval
			2,//gerpN
			4//gerpS
	};

	private static final int[] BYTE_OFFSETS = new int[]{
			0,
			Arrays.stream(FIELD_BYTE_SIZE).limit(1).sum(),
			Arrays.stream(FIELD_BYTE_SIZE).limit(2).sum(),
			Arrays.stream(FIELD_BYTE_SIZE).limit(3).sum(),
			Arrays.stream(FIELD_BYTE_SIZE).limit(4).sum(),
			Arrays.stream(FIELD_BYTE_SIZE).limit(5).sum(),
			Arrays.stream(FIELD_BYTE_SIZE).limit(6).sum(),
			Arrays.stream(FIELD_BYTE_SIZE).limit(7).sum(),
			Arrays.stream(FIELD_BYTE_SIZE).limit(8).sum(),
			Arrays.stream(FIELD_BYTE_SIZE).limit(9).sum(),
	};

	public static final int BYTE_SIZE_RECORD = Arrays.stream(BatchRecordConservation.FIELD_BYTE_SIZE).sum();

	private final Interval interval;
	private final byte[] bytes;
	private final int offsetBytes;

	@VisibleForTesting
	public BatchRecordConservation(Interval interval, byte[] bytes, int offsetBytes) {
		this.interval = interval;
		this.bytes = bytes;
		this.offsetBytes = offsetBytes;
	}

	public Float getPriPhCons(Position position) {
		int ioffset = getIOffset(position, 0);
		return getFloatByShort(ioffset);
	}

	public Float getMamPhCons(Position position) {
		int ioffset = getIOffset(position, 1);
		return getFloatByShort(ioffset);
	}

	public Float getVerPhCons(Position position) {
		int ioffset = getIOffset(position, 2);
		return getFloatByShort(ioffset);
	}

	public Float getPriPhyloP(Position position) {
		int ioffset = getIOffset(position, 3);
		return getFloatByShort(ioffset);
	}

	public Float getMamPhyloP(Position position) {
		int ioffset = getIOffset(position, 4);
		return getFloatByShort(ioffset);
	}

	public Float getVerPhyloP(Position position) {
		int ioffset = getIOffset(position, 5);
		return getFloatByShort(ioffset);
	}

	public Float getGerpRS(Position position) {
		int ioffset = getIOffset(position, 6);
		return getFloatByInteger(ioffset);
	}

	public Float getGerpRSpval(Position position) {
		int ioffset = getIOffset(position, 7);
		return getFloatByInteger(ioffset);
	}

	public Float getGerpN(Position position) {
		int ioffset = getIOffset(position, 8);
		return getFloatByShort(ioffset);
	}

	public Float getGerpS(Position position) {
		int ioffset = getIOffset(position, 9);
		return getFloatByInteger(ioffset);
	}

	private int getIOffset(Position position, int index) {
		return offsetBytes + (position.value - interval.start) * BYTE_SIZE_RECORD + BYTE_OFFSETS[index];
	}

	private Float getFloatByShort(int ioffset) {
		short sValue = ShortBits.fromByteArray(bytes, ioffset);
		if (sValue == SHORT_NULL_VALUE) return null;
		return (float) sValue / 1000.0f;
	}

	private Float getFloatByInteger(int ioffset) {
		int iValue = IntegerBits.fromByteArray(bytes, ioffset);
		if (iValue == INT_NULL_VALUE) return null;
		return (float) iValue / 1000.0f;
	}

	protected int getLengthBytes() {
		return (2 + 2) * (interval.end - interval.start + 1);
	}
}
