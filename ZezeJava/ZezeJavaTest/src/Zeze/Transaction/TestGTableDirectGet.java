package Zeze.Transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import Zeze.Transaction.GTable.GTable1;
import Zeze.Transaction.GTable.GTable2;
import Zeze.Transaction.GTable.Table;
import demo.ModuleGTable.Bean1;
import demo.ModuleGTable.Bean1ReadOnly;
import harness.Fast;
import org.junit.jupiter.api.Test;

/** Direct cell lookups preserve the tolerant query contract for both value families. */
@Fast
public class TestGTableDirectGet {
	private static void assertMissingQueries(Table<?, ?, ?> table) {
		assertNull(table.get(2, "column"), "missing row");
		assertNull(table.get(1, "missing"), "missing column of an existing row");
		assertNull(table.get(null, "column"));
		assertNull(table.get(1, null));
		assertNull(table.get(null, null));
		assertNull(table.get("wrong-row-type", "column"));
		assertNull(table.get(1, 11), "wrong column type must also be safe when the row exists");
		assertEquals(1, table.size(), "queries must not create rows or cells");
	}

	@Test
	public void scalarCellQueriesTolerateNullMissingAndWrongTypeKeys() {
		var table = new GTable1<Integer, String, Integer>(Integer.class, String.class, Integer.class);
		table.put(1, "column", 7);
		assertEquals(7, table.get(1, "column"));
		assertMissingQueries(table);
		assertEquals(7, table.get(1, "column"));
	}

	@Test
	public void beanCellQueriesPreserveIdentityAndTolerateInvalidKeys() {
		var table = new GTable2<Integer, String, Bean1, Bean1ReadOnly>(Integer.class, String.class, Bean1.class);
		var value = new Bean1(7);
		table.put(1, "column", value);
		assertSame(value, table.get(1, "column"));
		assertMissingQueries(table);
		assertSame(value, table.get(1, "column"));
	}
}
