package Zeze.Raft.RocksRaft;

import Zeze.Raft.RocksRaft.TestSortedMapEquivalentKeysReplay.BValue;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@Fast
public class TestRocksCollectionLogLifecycle {
	@Test
	public void beanCollectionCopiesFollowCurrentSavepointStructure() {
		var map = new CollMap2<Integer, BValue>(Integer.class, BValue.class);
		var list = new CollList2<BValue>(BValue.class);
		var parent = new BValue();
		var root = new Record.RootInfo(null, new TableKey("CopyViews", 1));
		map.variableId(1);
		list.variableId(2);
		map.initRootInfo(root, parent);
		list.initRootInfo(root, parent);
		var transaction = Transaction.create();
		transaction.begin();
		try {
			var mapValue = new BValue(10);
			var listValue = new BValue(20);
			map.put(1, mapValue);
			list.add(listValue);
			assertEquals(1, map.copy().size());
			assertSame(mapValue, map.copy().get(1), "复制保留已有Bean浅共享契约");
			assertEquals(1, list.copy().size());
			assertSame(listValue, list.copy().get(0));
			transaction.begin();
			map.remove(1);
			map.put(2, new BValue(30));
			list.clear();
			list.add(new BValue(40));
			assertEquals(30, map.copy().get(2).value);
			assertEquals(40, list.copy().get(0).value);
			transaction.rollback();
			assertEquals(1, map.copy().size());
			assertSame(mapValue, map.copy().get(1));
			assertEquals(1, list.copy().size());
			assertSame(listValue, list.copy().get(0));
		} finally {
			Transaction.destroy();
		}
	}










}
