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

package org.forome.annotation.data.pharmgkb.mysql;

import org.forome.annotation.config.connector.ForomeConfigConnector;
import org.forome.annotation.data.DatabaseConnector;
import org.forome.annotation.data.anfisa.struct.AnfisaResultView;
import org.forome.annotation.data.pharmgkb.PharmGKBConnector;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.struct.SourceMetadata;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PharmGKBConnectorMysql implements PharmGKBConnector, AutoCloseable {

	private final DatabaseConnector databaseConnector;

	public PharmGKBConnectorMysql(
			DatabaseConnectService databaseConnectService,
			ForomeConfigConnector foromeConfigConnector
	) throws Exception {
		this.databaseConnector = new DatabaseConnector(databaseConnectService, foromeConfigConnector);
	}

	@Override
	public List<SourceMetadata> getSourceMetadata(){
		return Collections.emptyList();
//		return databaseConnector.getSourceMetadata();
	}

	@Override
	public List<AnfisaResultView.Pharmacogenomics.Item> getNotes(String variantId) {
		String sql = String.format(
				"select AssocKind, Note from %s.PharmNOTES where Variant = '%s'",
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

	@Override
	public List<AnfisaResultView.Pharmacogenomics.Item> getPmids(String variantId) {
		String sql = String.format(
				"select AssocKind, PMID from %s.PharmPMIDS where Variant = '%s'",
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

	@Override
	public List<AnfisaResultView.Pharmacogenomics.Item> getDiseases(String variantId) {
		String sql = String.format(
				"select AssocKind, DisTitle from %s.PharmDISEASES where Variant = '%s'",
				databaseConnector.getDatabase(),
				variantId
		);

		List<AnfisaResultView.Pharmacogenomics.Item> items = new ArrayList<>();
		try (Connection connection = databaseConnector.createConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						String association = resultSet.getString("AssocKind");
						String values = resultSet.getString("DisTitle");
						for (String value: values.split(";")) {//Режем по символу ';'
							String tValue = value.trim();
							if (tValue.isEmpty()) continue;

							AnfisaResultView.Pharmacogenomics.Item item = new AnfisaResultView.Pharmacogenomics.Item(
									association, tValue
							);
							items.add(item);
						}
					}
				}
			}
		} catch (SQLException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex);
		}
		return items;
	}

	@Override
	public List<AnfisaResultView.Pharmacogenomics.Item> getChemicals(String variantId) {
		String sql = String.format(
				"select AssocKind, ChTitle from %s.PharmCHEMICALS where Variant = '%s'",
				databaseConnector.getDatabase(),
				variantId
		);

		List<AnfisaResultView.Pharmacogenomics.Item> items = new ArrayList<>();
		try (Connection connection = databaseConnector.createConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						String association = resultSet.getString("AssocKind");
						String values = resultSet.getString("ChTitle");
						for (String value: values.split(";")) {//Режем по символу ';'
							String tValue = value.trim();
							if (tValue.isEmpty()) continue;

							AnfisaResultView.Pharmacogenomics.Item item = new AnfisaResultView.Pharmacogenomics.Item(
									association, tValue
							);
							items.add(item);
						}
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
