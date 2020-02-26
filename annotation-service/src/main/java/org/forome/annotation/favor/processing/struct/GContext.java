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

package org.forome.annotation.favor.processing.struct;

import org.forome.annotation.data.hgmd.HgmdConnector;
import org.forome.annotation.favor.utils.struct.table.Row;
import org.forome.annotation.struct.Allele;
import org.forome.annotation.struct.Chromosome;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.struct.variant.custom.VariantCustom;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GContext {

	public final HgmdConnector hgmdConnector;

	public final Row row;

	private Variant _lazyVariant;
	private List<String> _lazyHgmdAccNums;
	private String _lazyHgmdAccNumHg38;
	private HgmdConnector.Data _lazyHgmdData;

	public GContext(HgmdConnector hgmdConnector, Row row) {
		this.hgmdConnector = hgmdConnector;

		this.row = row;
	}

	public Variant getVariant() {
		if (_lazyVariant == null) {
			String variantFormat = row.getValue("variant_format");
			String[] splitvariantFormat = variantFormat.split("-");
			if (splitvariantFormat.length != 4) throw new RuntimeException();

			int position = Double.valueOf(splitvariantFormat[1]).intValue();

			_lazyVariant = new VariantCustom(
					Chromosome.of(splitvariantFormat[0]),
					position,
					position,
					new Allele(splitvariantFormat[3])
			);
		}
		return _lazyVariant;
	}

	public List<String> getHgmdAccNums() {
		if (_lazyHgmdAccNums == null) {
			Variant variant = getVariant();
			_lazyHgmdAccNums = hgmdConnector.getAccNum(variant.chromosome.getChar(), variant.getStart(), variant.end);
		}
		return _lazyHgmdAccNums;
	}

	public String getHgmdAccNumHg38() {
		if (_lazyHgmdAccNumHg38 == null) {
			List<String> accNums = getHgmdAccNums();
			if (!accNums.isEmpty()) {
				List<Long[]> hg38 = hgmdConnector.getHg38(accNums);
				_lazyHgmdAccNumHg38 = hg38.stream().map(longs -> String.format("%s-%s", longs[0], longs[1])).collect(Collectors.joining(", "));
			} else {
				_lazyHgmdAccNumHg38 = "";
			}
		}
		return _lazyHgmdAccNumHg38;
	}


	public HgmdConnector.Data getHgmdData() {
		if (_lazyHgmdData == null) {
			List<String> accNums = getHgmdAccNums();
			if (!accNums.isEmpty()) {
				_lazyHgmdData = hgmdConnector.getDataForAccessionNumbers(accNums);
			} else {
				_lazyHgmdData = new HgmdConnector.Data(Collections.emptyList(), Collections.emptyList());
			}
		}
		return _lazyHgmdData;
	}

}
