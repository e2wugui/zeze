package Zeze.Transaction;

import java.util.Set;
import Zeze.Transaction.GTable.GTable1;
import Zeze.Transaction.GTable.GTable2;
import demo.ModuleGTable.Bean1;
import demo.ModuleGTable.Bean1ReadOnly;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestGTableRecreatedRowView {

	@Test
	public void cachedRowReadsTheRecreatedRow() {
		var table = new GTable1<Integer, Integer, Integer>(Integer.class, Integer.class, Integer.class);
		table.put(1, 1, 10);
		var row = table.row(1);
		assertEquals(10, row.get(1));
		table.rowMap().remove(1);
		table.put(1, 2, 20);
		table.put(1, 3, 30);

		assertNull(row.get(1));
		assertEquals(20, row.get(2));
		assertFalse(row.containsKey(1));
		assertTrue(row.containsKey(2));
		assertEquals(Set.of(2, 3), row.keySet());
		assertEquals(2, row.size());
	}

	@Test
	public void cachedRowWritesIntoTheRecreatedRow() {
		var table = new GTable1<Integer, Integer, Integer>(Integer.class, Integer.class, Integer.class);
		table.put(1, 1, 10);
		var row = table.row(1);
		assertEquals(10, row.get(1));
		table.rowMap().remove(1);
		table.put(1, 2, 20);

		assertNull(row.put(3, 30));
		assertEquals(30, table.get(1, 3));
		assertEquals(20, table.get(1, 2));
		assertNull(table.get(1, 1));
	}

	@Test
	public void cachedRowRemovesFromTheRecreatedRow() {
		var table = new GTable1<Integer, Integer, Integer>(Integer.class, Integer.class, Integer.class);
		table.put(1, 1, 10);
		var row = table.row(1);
		assertEquals(10, row.get(1));
		table.rowMap().remove(1);
		table.put(1, 1, 20);

		assertEquals(20, row.remove(1));
		assertFalse(table.containsRow(1));
		assertTrue(row.isEmpty());
	}

	@Test
	public void cachedRowClearsTheRecreatedRow() {
		var table = new GTable1<Integer, Integer, Integer>(Integer.class, Integer.class, Integer.class);
		table.put(1, 1, 10);
		var row = table.row(1);
		assertEquals(10, row.get(1));
		table.clear();
		table.put(1, 2, 20);

		row.clear();
		assertFalse(table.containsRow(1));
		assertTrue(row.isEmpty());
	}

	@Test
	public void cachedBeanRowWritesIntoTheRecreatedRow() {
		var table = new GTable2<Integer, Integer, Bean1, Bean1ReadOnly>(Integer.class, Integer.class, Bean1.class);
		var original = new Bean1();
		table.put(1, 1, original);
		var row = table.row(1);
		assertSame(original, row.get(1));
		table.rowMap().remove(1);
		var recreated = new Bean1();
		recreated.setIntVar(20);
		table.put(1, 2, recreated);
		var added = new Bean1();
		added.setIntVar(30);

		assertNull(row.put(3, added));
		assertSame(added, table.get(1, 3));
		assertSame(recreated, table.get(1, 2));
		assertNull(table.get(1, 1));
	}
}
