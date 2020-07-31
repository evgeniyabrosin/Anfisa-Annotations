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

package org.forome.annotation.data.anfisa;

import net.minidev.json.JSONObject;
import org.forome.annotation.data.anfisa.struct.AnfisaExecuteContext;
import org.forome.annotation.data.anfisa.struct.GtfAnfisaResult;
import org.forome.annotation.data.anfisa.struct.Kind;
import org.forome.annotation.data.gtf.GTFConnector;
import org.forome.annotation.data.gtf.mysql.struct.GTFRegion;
import org.forome.annotation.data.gtf.mysql.struct.GTFTranscriptRow;
import org.forome.annotation.struct.Assembly;
import org.forome.annotation.struct.Interval;
import org.forome.annotation.struct.Position;
import org.forome.annotation.struct.variant.Variant;
import org.forome.annotation.struct.variant.cnv.VariantCNV;
import org.forome.annotation.struct.variant.vep.VariantVep;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GtfAnfisaBuilder {

	private final AnfisaConnector anfisaConnector;
	private final GTFConnector gtfConnector;

	protected GtfAnfisaBuilder(AnfisaConnector anfisaConnector, GTFConnector gtfConnector) {
		this.anfisaConnector = anfisaConnector;
		this.gtfConnector = gtfConnector;
	}

	public GtfAnfisaResult build(Variant variant, AnfisaExecuteContext context) {
		if (variant instanceof VariantCNV) {
			Assembly assembly = context.anfisaInput.mCase.assembly;
			return buildCNV(assembly, (VariantCNV) variant);
		} else {
			return buildVep(context, (VariantVep) variant);
		}
	}

	/**
	 * Логика работы:
	 * В CNV-файл переходят только те мутации, которые задевают какой-то экзон, и дистанция получается всегда 0,
	 * но для надежности, мы проверяем, что cnv-варианта, не входит полностью в какойто экзон, алгоритм:
	 * 1) По пробегаем по каждому транскрипту
	 * 2) В нем пробегаем по каждому входящему в него экзому
	 * 3) Проверяем, что вариант не помещается ни в какой экзом, если помещается, то вычисляем минимальное расстояние,
	 * между краями экзона и краями cnv-варианта
	 *
	 * @param variant
	 * @return
	 */
	public GtfAnfisaResult buildCNV(Assembly assembly, VariantCNV variant) {
		return new GtfAnfisaResult(
				getRegionByCNV(assembly, variant, Kind.CANONICAL),
				getRegionByCNV(assembly, variant, Kind.WORST)
		);
	}

	private GtfAnfisaResult.RegionAndBoundary getRegionByCNV(Assembly assembly, VariantCNV variant, Kind kind) {
		List<JSONObject> vepTranscripts = getVepTranscripts(variant, kind);
		List<String> transcripts = vepTranscripts.stream()
				.filter(jsonObject -> "Ensembl".equals(jsonObject.getAsString("source")))
				.map(jsonObject -> jsonObject.getAsString("transcript_id"))
				.collect(Collectors.toList());
		if (transcripts.isEmpty()) {
			return null;
		}

		List<GtfAnfisaResult.RegionAndBoundary.DistanceFromBoundary> distances = new ArrayList<>();
		for (String transcript : transcripts) {
			List<GTFTranscriptRow> transcriptRows = gtfConnector.getTranscriptRows(assembly, transcript);

			GtfAnfisaResult.RegionAndBoundary.DistanceFromBoundary distance = null;
			for (int index = 0; index < transcriptRows.size(); index++) {
				GTFTranscriptRow transcriptRow = transcriptRows.get(index);
				if (transcriptRow.start <= variant.getStart() && variant.end <= transcriptRow.end) {
					int minDist = Math.min(variant.getStart() - transcriptRow.start, transcriptRow.end - variant.end);
					if (distance == null || distance.dist > minDist) {
						distance = new GtfAnfisaResult.RegionAndBoundary.DistanceFromBoundary(
								minDist, "exon", index, transcriptRows.size()
						);
					}
				}
			}
			if (distance != null) {
				distances.add(distance);
			}
		}
		return new GtfAnfisaResult.RegionAndBoundary(new String[]{ "exon" }, distances);
	}

	public GtfAnfisaResult buildVep(AnfisaExecuteContext context, VariantVep variant) {
		return new GtfAnfisaResult(
				getRegion(context, variant, Kind.CANONICAL),
				getRegion(context, variant, Kind.WORST)
		);
	}

	private GtfAnfisaResult.RegionAndBoundary getRegion(AnfisaExecuteContext context, VariantVep variant, Kind kind) {
		//TODO Ulitin V. Отличие от python-реализации
		//Дело в том, что в оригинальной версии используется set для позиции, но в коде ниже используется итерация этому
		//списку и в конечном итоге это вляет на значение поля region - судя по всему это потенциальный баг и
		//необходима консультация с Михаилом
		List<Position> positions = new ArrayList<>();
		positions.add(new Position(variant.chromosome, variant.getStart()));
		if (variant.getStart() != variant.end) {
			positions.add(new Position(variant.chromosome, variant.end));
		}

		List<JSONObject> vepTranscripts = getVepTranscripts(variant, kind);
		List<String> transcriptIds = vepTranscripts.stream()
				.filter(jsonObject -> "Ensembl".equals(jsonObject.getAsString("source")))
				.map(jsonObject -> jsonObject.getAsString("transcript_id"))
				.collect(Collectors.toList());
		if (transcriptIds.isEmpty()) {
			return null;
		}

		List<GtfAnfisaResult.RegionAndBoundary.DistanceFromBoundary> distances = new ArrayList<>();

		Interval interval = Interval.ofWithoutValidation(variant.chromosome, variant.getStart(), variant.end);
		for (String transcriptId : transcriptIds) {
			GtfAnfisaResult.RegionAndBoundary.DistanceFromBoundary distanceFromBoundary = getDistanceFromBoundary(
					context,
					transcriptId,
					interval
			);
			if (distanceFromBoundary != null) {
				distances.add(distanceFromBoundary);
			}
		}

		List<String> regions = distances.stream()
				.map(distance -> distance.region)
				.distinct().collect(Collectors.toList());
		if (context.getMaskedRegion(anfisaConnector, context)) {
			regions.add("masked_repeats");
		}

		regions = regions.stream().sorted().collect(Collectors.toList());

		return new GtfAnfisaResult.RegionAndBoundary(
				regions.toArray(new String[regions.size()]),
				distances
		);
	}

	public GtfAnfisaResult.RegionAndBoundary.DistanceFromBoundary getDistanceFromBoundary(
			AnfisaExecuteContext context,
			String transcriptId,
			Interval interval
	) {
		Assembly assembly = context.anfisaInput.mCase.assembly;

		List<Position> positions = new ArrayList<>();
		positions.add(new Position(interval.chromosome, interval.start));
		if (!interval.isSingle()) {
			positions.add(new Position(interval.chromosome, interval.end));
		}

		String region = null;
		Integer index = null;
		Integer n = null;
		Long dist = null;
		for (Position position : positions) {
			Object[] result = gtfConnector.lookup(context, assembly, position, transcriptId);
			if (result == null) {
				continue;
			}
			long d = (long) result[0];

			GTFRegion gtfRegion = (GTFRegion) result[1];
			region = gtfRegion.region;
			if (gtfRegion.indexRegion != null) {
				index = gtfRegion.indexRegion;
				n = (int) result[2];
			}

			if (dist == null || d < dist) {
				dist = d;
			}
		}
		if (dist != null) {
			return new GtfAnfisaResult.RegionAndBoundary.DistanceFromBoundary(
					dist, region, index, n
			);
		} else {
			return null;
		}
	}



	private List<JSONObject> getVepTranscripts(VariantVep variant, Kind kind) {
		if (kind == Kind.CANONICAL) {
			return AnfisaConnector.getCanonicalTranscripts(variant);
		} else if (kind == Kind.WORST) {
			return AnfisaConnector.getMostSevereTranscripts(variant);
		} else {
			throw new RuntimeException("Unknown kind: " + kind);
		}
	}
}
