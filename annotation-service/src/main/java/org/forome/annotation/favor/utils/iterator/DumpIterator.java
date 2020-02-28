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

package org.forome.annotation.favor.utils.iterator;

import org.forome.annotation.favor.utils.struct.table.Field;
import org.forome.annotation.favor.utils.struct.table.Row;
import org.forome.annotation.favor.utils.struct.table.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DumpIterator implements Iterator<Row>, AutoCloseable {

	private final static Logger log = LoggerFactory.getLogger(DumpIterator.class);

	private static Pattern PATTERN_COPY_DATA = Pattern.compile(
			"^COPY (.*) \\((.*)\\) (.*)$"
	);

	private final BufferedReader bufferedReader;

	private Table currentTable;

	private Row nextValue;

	private int count;

	public DumpIterator(BufferedReader bufferedReader) {
		this.bufferedReader = bufferedReader;

		nextValue = readNextValue();
	}

	@Override
	public boolean hasNext() {
		return (nextValue != null);
	}

	@Override
	public Row next() {
		if (nextValue == null) {
			throw new NoSuchElementException();
		}
		Row value = nextValue;
		nextValue = readNextValue();
		return value;
	}

	private Row readNextValue() {
		String line;
		try {
			while (true) {
				line = bufferedReader.readLine();
				if (line == null) return null;

				Matcher matcherCopyData = PATTERN_COPY_DATA.matcher(line);
				if (matcherCopyData.matches()) {
					currentTable = nextTable(matcherCopyData, line);
					line = bufferedReader.readLine();
					if (line == null) return null;

					count = 0;
				}

				if (currentTable != null) {
					List<String> values = Arrays.stream(line.split("\t"))
							.map(s -> "\\N".equals(s) ? null : s)
							.collect(Collectors.toList());

					if (currentTable.fields.size() != values.size()) {
						log.error("Fail!!! Not correct dump: " + line);
						return null;
					}

					return new Row(count++, currentTable, values, line);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Table nextTable(Matcher matcherCopyData, String rawLine) {
		String tableName = matcherCopyData.group(1);
		if (tableName.startsWith("public.")) {
			tableName = tableName.substring("public.".length());
		}

		List<Field> fields = Arrays.stream(matcherCopyData.group(2).split(","))
				.map(s -> s.trim())
				.map(s -> s.replaceAll("\"", ""))
				.map(s -> new Field(s))
				.collect(Collectors.toList());


		return new Table(tableName, fields, rawLine);
	}

	@Override
	public void close() throws Exception {
		bufferedReader.close();
	}

}

