package Zeze.Transaction;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import Zeze.Serialize.SQLStatement;
import Zeze.Transaction.Collections.CollOne;
import Zeze.Transaction.Collections.LogOne;
import Zeze.Transaction.Collections.LogOneMeta;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Fast
public class TestCollOneSqlTransactionView {
	public static final class SqlBean extends EmptyBean {
		private int number;

		@Override
		public void encodeSQLStatement(ArrayList<String> parents, SQLStatement st) {
			st.appendInt("number", number);
		}

		@Override
		public void decodeResultSet(ArrayList<String> parents, ResultSet rs) throws SQLException {
			number = rs.getInt("number");
		}
	}

	@Test
	public void sqlMappingUsesReplacementInCurrentSavepoint() throws SQLException {
		var parent = new EmptyBean();
		var committed = new SqlBean();
		committed.number = 11;
		var replacement = new SqlBean();
		replacement.number = 22;
		var field = new CollOne<>(committed, LogOneMeta.get(SqlBean.class));
		field.initRootInfo(new Record.RootInfo(null, new TableKey(1, 1)), parent);
		var transaction = Transaction.create(new Locks());
		try {
			transaction.begin();
			transaction.putLog(new LogOne<>(parent, field.variableId(), field, replacement));
			var sql = new SQLStatement();
			field.encodeSQLStatement(new ArrayList<>(), sql);
			assertEquals("number=22", sql.getSql().toString());

			try (var rows = RowSetProvider.newFactory().createCachedRowSet()) {
				var metadata = new RowSetMetaDataImpl();
				metadata.setColumnCount(1);
				metadata.setColumnName(1, "number");
				metadata.setColumnType(1, Types.INTEGER);
				rows.setMetaData(metadata);
				rows.moveToInsertRow();
				rows.updateInt(1, 33);
				rows.insertRow();
				rows.moveToCurrentRow();
				rows.beforeFirst();
				rows.next();
				field.decodeResultSet(new ArrayList<>(), rows);
			}
			assertEquals(33, replacement.number);
			transaction.rollback();
			sql.clear();
			field.encodeSQLStatement(new ArrayList<>(), sql);
			assertEquals("number=11", sql.getSql().toString());
		} finally {
			Transaction.destroy();
		}
	}
}
