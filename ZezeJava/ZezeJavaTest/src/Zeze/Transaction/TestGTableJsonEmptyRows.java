package Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.GTable.GTable1;
import Zeze.Transaction.GTable.GTable2;
import Zeze.Util.Json;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestGTableJsonEmptyRows {
	public static class Value extends Bean {
		public int value;
		private transient Object key;

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteInt(value);
		}

		@Override
		public void decode(IByteBuffer bb) {
			value = bb.ReadInt();
		}

		@Override
		public Object mapKey() {
			return key;
		}

		@Override
		public void mapKey(Object key) {
			this.key = key;
		}
	}

	public static class ScalarTable {
		public final GTable1<Integer, Integer, Integer> table =
				new GTable1<>(Integer.class, Integer.class, Integer.class);
	}

	public static class BeanTable {
		public final GTable2<Integer, Integer, Value, Value> table =
				new GTable2<>(Integer.class, Integer.class, Value.class);
	}

	@Test
	public void scalarRowsContainOnlyCells() throws Exception {
		var empty = (ScalarTable)Json.parse("{table:{1:{}}}", new ScalarTable());
		assertTrue(empty.table.isEmpty());
		assertFalse(empty.table.containsRow(1));
		assertFalse(empty.table.cellSet().iterator().hasNext());
		var mixed = (ScalarTable)Json.parse("{table:{1:{},2:{3:4},5:{}}}", new ScalarTable());
		assertEquals(1, mixed.table.size());
		assertEquals(1, mixed.table.rowKeySet().size());
		assertEquals(1, mixed.table.cellSet().stream().count());
		assertEquals(4, mixed.table.get(2, 3));
	}

	@Test
	public void beanRowsContainOnlyCells() throws Exception {
		var empty = (BeanTable)Json.parse("{table:{1:{}}}", new BeanTable());
		assertTrue(empty.table.isEmpty());
		assertFalse(empty.table.containsRow(1));
		assertFalse(empty.table.cellSet().iterator().hasNext());
		var mixed = (BeanTable)Json.parse("{table:{1:{},2:{3:{value:4}},5:{}}}", new BeanTable());
		assertEquals(1, mixed.table.size());
		assertEquals(1, mixed.table.rowKeySet().size());
		assertEquals(1, mixed.table.cellSet().stream().count());
		assertEquals(4, mixed.table.get(2, 3).value);
	}
}
