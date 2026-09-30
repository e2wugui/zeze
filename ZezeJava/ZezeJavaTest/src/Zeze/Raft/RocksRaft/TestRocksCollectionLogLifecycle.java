package Zeze.Raft.RocksRaft;

import Zeze.Raft.RocksRaft.Log1.LogInt;
import Zeze.Raft.RocksRaft.TestSortedMapEquivalentKeysReplay.BValue;
import Zeze.Serialize.ByteBuffer;
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



	@Test
	public void mapDecodeIntoLiveObjectRetainsWireChangedOnReencode() {
		Log.register(LogInt::new);
		var leader = new CollMap2<Integer, BValue>(Integer.class, BValue.class);
		var bean = new BValue(10);
		leader.put(1, bean);
		var source = new LogMap2<Integer, BValue>(Integer.class, BValue.class);
		source.setValue(leader.map);
		source.getChanged().add(change(bean, 42));
		var reused = new LogMap2<Integer, BValue>(Integer.class, BValue.class);
		reused.setValue(leader.map);
		reused.getChanged().add(change(bean, 99));
		reused.decode(encode(source));
		var replay = new LogMap2<Integer, BValue>(Integer.class, BValue.class);
		replay.decode(encode(reused));
		assertEquals(1, replay.getChangedWithKey().size());
		var follower = new CollMap2<Integer, BValue>(Integer.class, BValue.class);
		follower.put(1, new BValue(10));
		follower.followerApply(replay);
		assertEquals(42, follower.get(1).value, "wire中的42不能被旧source中的99替换");
	}

	@Test
	public void sortedMapDecodeIntoLiveObjectRetainsWireChangedOnReencode() {
		Log.register(LogInt::new);
		var leader = new CollSortedMap2<Integer, BValue>(Integer.class, BValue.class);
		var bean = new BValue(10);
		leader.put(1, bean);
		var source = new LogSortedMap2<Integer, BValue>(Integer.class, BValue.class);
		source.setValue(leader.map);
		source.getChanged().add(change(bean, 42));
		var reused = new LogSortedMap2<Integer, BValue>(Integer.class, BValue.class);
		reused.setValue(leader.map);
		reused.getChanged().add(change(bean, 99));
		reused.decode(encode(source));
		var replay = new LogSortedMap2<Integer, BValue>(Integer.class, BValue.class);
		replay.decode(encode(reused));
		assertEquals(1, replay.getChangedWithKey().size());
		var follower = new CollSortedMap2<Integer, BValue>(Integer.class, BValue.class);
		follower.put(1, new BValue(10));
		follower.followerApply(replay);
		assertEquals(42, follower.get(1).value);
	}

	private static LogBean change(BValue bean, int value) {
		var log = new LogBean();
		log.setThis(bean);
		log.getVariablesOrNew().put(1, new LogInt(bean, 1, value));
		return log;
	}

	private static ByteBuffer encode(Log log) {
		var buffer = ByteBuffer.Allocate();
		log.encode(buffer);
		return ByteBuffer.Wrap(buffer.Copy());
	}
}
