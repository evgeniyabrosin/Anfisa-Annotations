package org.forome.annotation.connector.gnomad;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.forome.annotation.connector.DatabaseConnector;
import org.forome.annotation.exception.ExceptionBuilder;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class GnomadDataConnector {

	private static final Logger log = LoggerFactory.getLogger(GnomadDataConnector.class);

	public static final ImmutableList<String> ANCESTRIES = ImmutableList.of(
			"AFR",
			"AMR",
			"ASJ",
			"EAS",
			"FIN",
			"NFE",
			"OTH"
	);

	private static final ImmutableList<String> POP_GROUPS =
			new ImmutableList.Builder()
					.add("Male", "Female", "raw", "POPMAX")
					.addAll(ANCESTRIES)
					.build();

	private static final ImmutableList<String> KEY_COLUMNS = ImmutableList.of(
			"CHROM",
			"POS",
			"ID",
			"REF",
			"ALT"
	);

	private static final ImmutableList<String> AGGREGATE_DATA_COLUMNS = ImmutableList.of(
			"AN_Female + AN_Male as AN",
			"AC_Female + AC_Male as AC",
			"AN_POPMAX",
			"AC_POPMAX",
			"POPMAX"
	);

	private static final ImmutableList<String> DATA_PREFIXES = ImmutableList.of("AN", "AC");

	private static final ImmutableList<String> DATA_COLUMNS =
			DATA_PREFIXES.stream().flatMap(
					a -> POP_GROUPS.stream().map(b -> a + '_' + b)
			).collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));

	private static final String TABLE = "gnomad.VARIANTS";

	private static final ImmutableList<String> COLUMNS = new ImmutableList.Builder()
			.addAll(KEY_COLUMNS)
			.addAll(AGGREGATE_DATA_COLUMNS)
			.addAll(DATA_COLUMNS)
			.build();

	public class Result {

		public final Map<String, Object> columns;

		public Result(ResultSet resultSet) throws SQLException {
			Map<String, Object> modifiableColumns = new HashMap<>();
			ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
			for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
				String columnName = resultSetMetaData.getColumnName(i);
				modifiableColumns.put(columnName, resultSet.getObject(i));
			}
			this.columns = Collections.unmodifiableMap(modifiableColumns);
		}
	}

	private final DatabaseConnector databaseConnector;

	public GnomadDataConnector(DatabaseConnector databaseConnector) {
		this.databaseConnector = databaseConnector;
	}

	public List<Result> getData(
			String chromosome,
			long position,
			String ref,
			String alt,
			String fromWhat,
			boolean exact) throws Exception {

		String sql = String.format(
				"SELECT %s FROM %s WHERE CHROM = '%%s' and POS = %%s",
				String.join(", ", COLUMNS), TABLE
		);

		if (ref != null && alt != null) {
			if (exact) {
				sql += " and REF = '%s' and ALT = '%s'";
			} else {
				sql += " and REF LIKE '%%%s%%' and ALT LIKE '%%%s%%'";
			}
		}

		if (fromWhat != null) {
			List<String> q = Arrays.asList(fromWhat.toLowerCase().split(","));
			String s;
			if (q.size() == 1) {
				if (q.get(0).contains("exome")) {
					s = "e";
				} else if (q.get(0).contains("genome")) {
					s = "g";
				} else {
					s = q.get(0);
				}
			} else {
				throw new RuntimeException("Not support many s");
			}
			if (!(q.contains("e") && q.contains("g"))) {
				sql = String.format("%s and `SOURCE` = '%s'", sql, s);
			}
		}

		try (Connection connection = databaseConnector.createConnection()) {
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(String.format(sql, chromosome, position, ref, alt));
			boolean isEmptyResultSet = !resultSet.next();

			if (!exact && isEmptyResultSet && ref != null && alt != null) {
				if (ref.length() > alt.length()) {
					if (ref.contains(alt)) {
						int idx = ref.indexOf(alt);
						String newAlt;
						String newRef;
						if (idx == 0) {
							newAlt = String.valueOf(alt.charAt(0));
							newRef = String.valueOf(ref.charAt(0)) + ref.substring(alt.length());
						} else {
							newAlt = alt;
							newRef = ref;
						}

						resultSet.close();
						statement.close();

						statement = connection.createStatement();
						resultSet = statement.executeQuery(
								String.format(sql, chromosome, position + idx - 1, newRef, newAlt)
						);
					}
				} else if (ref.length() < alt.length()) {
					if (alt.contains(ref)) {
						int idx = alt.indexOf(ref);
						String newAlt;
						String newRef;
						if (idx == 0) {
							newAlt = String.valueOf(alt.charAt(0)) + alt.substring(ref.length());
							newRef = String.valueOf(ref.charAt(0));
						} else {
							newAlt = alt;
							newRef = ref;
						}

						resultSet.close();
						statement.close();

						statement = connection.createStatement();
						resultSet = statement.executeQuery(
								String.format(sql, chromosome, position + idx - 1, newRef, newAlt)
						);
					}
				}
			}

			List<Result> results = new ArrayList<>();
			resultSet.beforeFirst();
			if (!exact) {
				while (resultSet.next()) {
					if (Objects.equals(diff(ref, alt), diff(resultSet.getString(4), resultSet.getString(5)))) {
						results.add(new Result(resultSet));
					}
				}
			} else {
				while (resultSet.next()) {
					results.add(new Result(resultSet));
				}
			}

			resultSet.close();
			statement.close();

			return results;
		} catch (SQLException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex);
		}
	}

	@VisibleForTesting
	public static String diff(String s1, String s2) {
		if (s1.equals(s2)) {
			return "";
		} else if (s2.contains(s1)) {
			int idx = s2.indexOf(s1);
			return s2.substring(0, idx) + s2.substring(idx + s1.length());
		} else if (s1.contains(s2)) {
			return "-" + diff(s2, s1);
		} else {
			return null;
		}
	}
}
