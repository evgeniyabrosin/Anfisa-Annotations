package org.forome.annotation.connector.hgmd;

import org.forome.annotation.config.connector.HgmdConfigConnector;
import org.forome.annotation.connector.DatabaseConnector;
import org.forome.annotation.connector.hgmd.struct.HgmdPmidRow;
import org.forome.annotation.exception.ExceptionBuilder;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class HgmdConnector implements Closeable {

	private static final String SQL_ACC_NUM = "select acc_num from `hgmd_pro`.`hg19_coords` where chromosome = '%s' and coordSTART = %s and coordEND = %s";
	private static final String SQL_PMID = "SELECT distinct disease, PMID, Tag from `hgmd_pro`.`mutation` where acc_num = '%s'";
	private static final String SQL_PHEN = "SELECT distinct phenotype " +
			"FROM `hgmd_phenbase`.`hgmd_mutation` as m join `hgmd_phenbase`.`hgmd_phenotype` as p on p.phen_id = m.phen_id " +
			"WHERE acc_num = '%s'";
	private static final String SQL_HG38 = "SELECT coordSTART, coordEND FROM `hgmd_pro`.`hg38_coords` WHERE acc_num = '%s'";

	public class Data {
		public final List<HgmdPmidRow> hgmdPmidRows;
		public final List<String> phenotypes;

		public Data(List<HgmdPmidRow> hgmdPmidRows, List<String> phenotypes) {
			this.hgmdPmidRows = hgmdPmidRows;
			this.phenotypes = phenotypes;
		}
	}

	private final DatabaseConnector databaseConnector;

	public HgmdConnector(HgmdConfigConnector hgmdConfigConnector) throws Exception {
		this.databaseConnector = new DatabaseConnector(hgmdConfigConnector);
	}

	public List<String> getAccNum(String chromosome, long start, long end) {
		List<String> accNums = new ArrayList<>();
		try (Connection connection = databaseConnector.createConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(String.format(SQL_ACC_NUM, chromosome, start, end))) {
					while (resultSet.next()) {
						String accNum = resultSet.getString("acc_num");
						accNums.add(accNum);
					}
				}
			}
		} catch (SQLException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex);
		}
		return accNums;
	}

	public Data getDataForAccessionNumbers(List<String> accNums) {
		List<HgmdPmidRow> hgmdPmidRows = new ArrayList<>();
		List<String> phenotypes = new ArrayList<>();
		try (Connection connection = databaseConnector.createConnection()) {
			try (Statement statement = connection.createStatement()) {
				for (String accNum : accNums) {
					try (ResultSet resultSet = statement.executeQuery(String.format(SQL_PMID, accNum))) {
						while (resultSet.next()) {
							String disease = resultSet.getString("disease");
							String pMID = resultSet.getString("PMID");
							String tag = resultSet.getString("Tag");
							hgmdPmidRows.add(new HgmdPmidRow(
									disease, pMID, tag
							));
						}
					}
					try (ResultSet resultSet = statement.executeQuery(String.format(SQL_PHEN, accNum))) {
						while (resultSet.next()) {
							String phenotype = resultSet.getString("phenotype");
							phenotypes.add(phenotype);
						}
					}
				}
			}
		} catch (SQLException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex);
		}
		return new Data(hgmdPmidRows, phenotypes);
	}

	public List<String[]> getHg38(List<String> accNums) {
		List<String[]> hg38s = new ArrayList<>();
		try (Connection connection = databaseConnector.createConnection()) {
			try (Statement statement = connection.createStatement()) {
				for (String accNum : accNums) {
					try (ResultSet resultSet = statement.executeQuery(String.format(SQL_HG38, accNum))) {
						while (resultSet.next()) {
							String coordSTART = resultSet.getString("coordSTART");
							String coordEND = resultSet.getString("coordEND");
							hg38s.add(new String[]{coordSTART, coordEND});
						}
					}
				}

			}
		} catch (SQLException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex);
		}
		return hg38s;
	}

	@Override
	public void close() {
		databaseConnector.close();
	}
}
