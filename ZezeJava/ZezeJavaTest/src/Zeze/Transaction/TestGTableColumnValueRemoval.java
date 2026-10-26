package Zeze.Transaction;

import java.util.List;
import Zeze.Transaction.GTable.GTable1;
import Zeze.Transaction.GTable.GTable2;
import Zeze.Transaction.GTable.Table;
import demo.ModuleGTable.Bean1;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestGTableColumnValueRemoval {
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
