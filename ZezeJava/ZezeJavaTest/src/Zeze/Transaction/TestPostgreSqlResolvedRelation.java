package Zeze.Transaction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfSystemProperty(named = "zeze.test.pg.url", matches = ".+")
public class TestPostgreSqlResolvedRelation {
	private static List<String> query(Connection connection, String sql, String name) throws Exception {
		try (var statement = connection.prepareStatement(sql)) {
			statement.setString(1, name);
			try (var result = statement.executeQuery()) {
				var values = new ArrayList<String>();
				while (result.next())
					values.add(result.getString(1));
				return values;
			}
		}
	}

	@Test
	void metadataResolvesTheSameRelationAsDdl() throws Exception {
		var suffix = UUID.randomUUID().toString().replace("-", "");
		var first = "scope_a_" + suffix;
		var second = "scope_b_" + suffix;
		try (var connection = DriverManager.getConnection(System.getProperty("zeze.test.pg.url"));
			 var statement = connection.createStatement()) {
			connection.setAutoCommit(false); // The complete fixture is rolled back, including both schemas.
			try {
				statement.execute("CREATE SCHEMA " + first);
				statement.execute("CREATE SCHEMA " + second);
				statement.execute("SET LOCAL search_path TO " + first + ", " + second);
				statement.execute("CREATE TABLE " + second + ".target (remote_key int CONSTRAINT remote_pk PRIMARY KEY)");
				assertEquals(List.of("remote_key"), query(connection, DatabasePostgreSQL.SQL_PHYSICAL_COLUMNS, "target"));
				assertEquals(List.of("remote_pk"), query(connection, DatabasePostgreSQL.SQL_PK_CONSTRAINT, "target"));
				statement.execute("ALTER TABLE target ADD COLUMN applied int");
				assertEquals(List.of("remote_key", "applied"), query(connection,
						DatabasePostgreSQL.SQL_PHYSICAL_COLUMNS, "target"));
				statement.execute("CREATE TABLE " + first + ".target (local_key int CONSTRAINT local_pk PRIMARY KEY)");
				assertEquals(List.of("local_key"), query(connection, DatabasePostgreSQL.SQL_PHYSICAL_COLUMNS, "target"));
				assertEquals(List.of("local_pk"), query(connection, DatabasePostgreSQL.SQL_PK_CONSTRAINT, "target"));
				assertEquals(List.of("remote_pk"), query(connection, DatabasePostgreSQL.SQL_PK_CONSTRAINT,
						second + ".target"));
				statement.execute("CREATE TABLE " + second + ".\"MixedCase\" (quoted_key int CONSTRAINT quoted_pk PRIMARY KEY)");
				assertEquals(List.of("quoted_key"), query(connection, DatabasePostgreSQL.SQL_PHYSICAL_COLUMNS,
						second + ".\"MixedCase\""));
				assertEquals(List.of("quoted_pk"), query(connection, DatabasePostgreSQL.SQL_PK_CONSTRAINT,
						"\"MixedCase\""));
			} finally {
				connection.rollback();
			}
		}
	}
}
