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

package org.forome.annotation.data.ref;

import org.forome.annotation.data.DatabaseConnector;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.utils.NucleotideUtils;
import org.forome.core.struct.Chromosome;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class RefDataConnector implements Closeable {

	private static final long GENE_BUCKET_SIZE = 1000000L;

	private final DatabaseConnector databaseConnector;

	public RefDataConnector(DatabaseConnector databaseConnector) {
		this.databaseConnector = databaseConnector;
	}

	public String getRef(Chromosome chromosome, int start, int end) {
		String sql = String.format(
				"SELECT Ref, Pos FROM util.hg19 WHERE Chrom = %s AND Pos between %s and %s ORDER BY Pos ASC",
				chromosome.getChar(), Math.min(start, end), Math.max(start, end)
		);

		StringBuilder ref = new StringBuilder();
		int lastPos = start - 1;
		try (Connection connection = databaseConnector.createConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						int pos = resultSet.getInt("Pos");
						//Валидируем порядок
						if (pos != ++lastPos) {
							throw new RuntimeException("Bad sequence, sql: " + sql);
						}
						String sNucleotide = resultSet.getString("Ref");
						if (sNucleotide.length() != 1) {
							throw new RuntimeException("Bad ref, sql: " + sql + ", ref: " + sNucleotide);
						}
						char nucleotide = sNucleotide.toUpperCase().charAt(0);
						if (!NucleotideUtils.validation(nucleotide)) {
							throw new RuntimeException("Not valid nucleotide, sql: " + sql + ", nucleotide: " + nucleotide);
						}

						ref.append(nucleotide);
					}
				}
			}
		} catch (SQLException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex);
		}
		if (ref.length() != Math.max(start, end) - Math.min(start, end) + 1) {
			throw new RuntimeException("Bad len ref, sql: " + sql + ", ref: " + ref);
		}
		return ref.toString();
	}

	@Override
	public void close() {
		databaseConnector.close();
	}
}
