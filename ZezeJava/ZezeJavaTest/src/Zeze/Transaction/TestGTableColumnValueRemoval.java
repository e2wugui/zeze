package Zeze.Transaction;

import java.util.AbstractList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import Zeze.Transaction.GTable.GTable1;
import Zeze.Transaction.GTable.GTable2;
import Zeze.Transaction.GTable.Table;
import demo.ModuleGTable.Bean1;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestGTableColumnValueRemoval {
	private static <T> List<T> candidate(T value) {
		return new AbstractList<>() {
			@Override
			public int size() {
				return 1;
			}

			@Override
			public T get(int index) {
				if (index != 0)
					throw new IndexOutOfBoundsException(index);
				return value;
			}

			@Override
			public boolean contains(Object object) {
				throw new AssertionError("small removals should iterate the candidates");
			}
		};
	}

	@Test
	void columnKeyAndEntryRemovalUseSmallCandidatesAndRemoveEmptyRows() {
		var table = new GTable1<>(Integer.class, Integer.class, String.class);
		table.put(1, 0, "first");
		table.put(1, 1, "keep");
		table.put(2, 0, "second");
		table.put(3, 0, "third");
		var column = table.column(0);
		assertFalse(column.entrySet().removeAll(List.of()));
		assertTrue(column.keySet().removeAll(candidate(1)));
		assertTrue(column.entrySet().removeAll(candidate(Map.entry(2, "second"))));
		assertTrue(column.entrySet().removeAll(Set.of(Map.entry(3, "third"), Map.entry(4, "missing"),
				Map.entry(5, "missing"))));
		assertTrue(column.isEmpty());
		assertEquals(Set.of(1), table.rowMap().keySet());
		assertEquals("keep", table.get(1, 1));
	}
	private static <V> void checkRemoval(Table<Integer, Integer, V> table, V value, V equalValue, V other) {
		table.put(0, 0, value);
		table.put(1, 0, equalValue);
		table.put(2, 0, value);
		table.put(0, 1, other);
		var values = table.column(0).values();
		assertTrue(values.remove(equalValue));
		assertEquals(2, values.size(), "单次 remove 只能删除一个相等值");
		assertEquals(3, table.size());
		assertSame(other, table.get(0, 1));
		for (var row : table.rowMap().values())
			assertFalse(row.isEmpty(), "删除最后一格须移除空行");
		assertTrue(values.removeAll(List.of(equalValue)), "removeAll 仍须删除全部匹配项");
		assertTrue(values.isEmpty());
		assertEquals(1, table.size());
		assertEquals(1, table.rowMap().size());
		assertFalse(values.remove(equalValue));
		assertFalse(values.remove(null));
	}

	@Test
	void columnViewsCanRemoveAllOfTheirOwnEntries() {
		var table = new GTable1<>(Integer.class, Integer.class, String.class);
		table.put(1, 0, "first");
		table.put(2, 0, "second");
		table.put(3, 1, "third");
		table.put(4, 1, "fourth");
		var keys = table.column(0).keySet();
		var entries = table.column(1).entrySet();
		assertTrue(keys.removeAll(keys));
		assertTrue(entries.removeAll(entries));
		assertTrue(table.rowMap().isEmpty());
	}

	@Test
	void scalarColumnRemovesOneEqualValue() {
		checkRemoval(new GTable1<>(Integer.class, Integer.class, String.class),
				new String("same"), new String("same"), "other");
	}

	@Test
	void beanColumnRemovesOneEqualValue() {
		var other = new Bean1();
		other.setIntVar(9);
		checkRemoval(new GTable2<Integer, Integer, Bean1, Object>(Integer.class, Integer.class, Bean1.class),
				new Bean1(), new Bean1(), other);
	}
}
