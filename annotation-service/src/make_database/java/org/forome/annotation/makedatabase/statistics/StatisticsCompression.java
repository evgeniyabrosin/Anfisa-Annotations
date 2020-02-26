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

package org.forome.annotation.makedatabase.statistics;

import org.forome.annotation.service.database.batchrecord.compression.TypeCompression;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsCompression {

	private final Map<String, List<StatisticItem>> statistics;

	public StatisticsCompression() {
		this.statistics = new HashMap<>();
	}

	public synchronized void add(String key, byte[] bytes) {
		TypeCompression typeCompression = TypeCompression.get(bytes[0]);
		statistics.computeIfAbsent(key, s -> new ArrayList<>())
				.add(new StatisticItem(typeCompression, bytes.length));
	}

	public void println() {
		PrintStream out = System.out;

		for (String key : statistics.keySet()) {
			List<StatisticItem> items = statistics.get(key);

			out.println("********* " + key + " ***********");
			out.println("Колличество записей: " + items.size());
			out.println("Чистый объем данных: "
					+ (items.size() * 4 + items.stream().mapToLong(item -> item.size).sum())
					+ " bytes"
			);
			out.println();

			out.println("Использованные алгоритмы упаковки");
			Map<TypeCompression, List<StatisticItem>> compressions = new HashMap<>();
			for (StatisticItem statisticItem : items) {
				compressions
						.computeIfAbsent(statisticItem.typeCompression, typeCompression -> new ArrayList<>())
						.add(statisticItem);
			}
			for (TypeCompression type : compressions.keySet()) {
				List<StatisticItem> compressionItems = compressions.get(type);

				StringBuilder sBuilder = new StringBuilder();
				sBuilder.append(type.name()).append(": ");
				sBuilder.append("Использовано: ")
						.append(compressionItems.size())
						.append(" (")
						.append(
								String.format("%.2f", compressionItems.size() * 100.0d / (double) items.size())
						).append("%), ");
				sBuilder.append("Средний размер пакета: ").append(compressionItems.stream().mapToLong(item -> item.size).sum() / compressionItems.size()).append(" bytes");
				out.println(sBuilder.toString());
			}
		}
	}
}
