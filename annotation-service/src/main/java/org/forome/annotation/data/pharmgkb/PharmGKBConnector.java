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

package org.forome.annotation.data.pharmgkb;

import org.forome.annotation.config.connector.PharmGKBConfigConnector;
import org.forome.annotation.data.DatabaseConnector;
import org.forome.annotation.data.anfisa.struct.AnfisaResultView;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.struct.SourceMetadata;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PharmGKBConnector implements AutoCloseable {

	private final DatabaseConnector databaseConnector;

	public PharmGKBConnector(
			DatabaseConnectService databaseConnectService,
			PharmGKBConfigConnector pharmGKBConfigConnector
	) throws Exception {
		this.databaseConnector = new DatabaseConnector(databaseConnectService, pharmGKBConfigConnector);
	}

	public List<SourceMetadata> getSourceMetadata(){
		return databaseConnector.getSourceMetadata();
	}

	public List<AnfisaResultView.Pharmacogenomics.Item> getNotes(String variantId) {
		String sql = String.format(
				"select AssocKind, Note from %s.NOTES where Variant = '%s'",
				databaseConnector.getDatabase(),
				variantId
		);

		List<AnfisaResultView.Pharmacogenomics.Item> items = new ArrayList<>();
		try (Connection connection = databaseConnector.createConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						AnfisaResultView.Pharmacogenomics.Item item = new AnfisaResultView.Pharmacogenomics.Item(
								resultSet.getString("AssocKind"),
								resultSet.getString("Note")
						);
						items.add(item);
					}
				}
			}
		} catch (SQLException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex);
		}
		return items;
	}

	public List<AnfisaResultView.Pharmacogenomics.Item> getPmids(String variantId) {
		String sql = String.format(
				"select AssocKind, PMID from %s.PMIDS where Variant = '%s'",
				databaseConnector.getDatabase(),
				variantId
		);

		List<AnfisaResultView.Pharmacogenomics.Item> items = new ArrayList<>();
		try (Connection connection = databaseConnector.createConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						AnfisaResultView.Pharmacogenomics.Item item = new AnfisaResultView.Pharmacogenomics.Item(
								resultSet.getString("AssocKind"),
								resultSet.getString("PMID")
						);
						items.add(item);
					}
				}
			}
		} catch (SQLException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex);
		}
		return items;
	}

	public List<AnfisaResultView.Pharmacogenomics.Item> getDiseases(String variantId) {
		String sql = String.format(
				"select AssocKind, DisTitle from %s.DISEASES where Variant = '%s'",
				databaseConnector.getDatabase(),
				variantId
		);

		List<AnfisaResultView.Pharmacogenomics.Item> items = new ArrayList<>();
		try (Connection connection = databaseConnector.createConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						AnfisaResultView.Pharmacogenomics.Item item = new AnfisaResultView.Pharmacogenomics.Item(
								resultSet.getString("AssocKind"),
								resultSet.getString("DisTitle")
						);
						items.add(item);
					}
				}
			}
		} catch (SQLException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex);
		}
		return items;
	}

	public List<AnfisaResultView.Pharmacogenomics.Item> getChemicals(String variantId) {
		String sql = String.format(
				"select AssocKind, ChTitle from %s.CHEMICALS where Variant = '%s'",
				databaseConnector.getDatabase(),
				variantId
		);

		List<AnfisaResultView.Pharmacogenomics.Item> items = new ArrayList<>();
		try (Connection connection = databaseConnector.createConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						AnfisaResultView.Pharmacogenomics.Item item = new AnfisaResultView.Pharmacogenomics.Item(
								resultSet.getString("AssocKind"),
								resultSet.getString("ChTitle")
						);
						items.add(item);
					}
				}
			}
		} catch (SQLException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex);
		}
		return items;
	}

	@Override
	public void close() {
		this.databaseConnector.close();
	}

}
