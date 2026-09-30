package Zeze.Raft.RocksRaft;

import java.math.BigDecimal;
import Zeze.Raft.RocksRaft.Log1.LogInt;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.pcollections.TreePMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestSortedMapEquivalentKeysReplay {
	public static final class BValue extends Bean {
		int value;
		private transient Object key;

		public BValue() {
		}

		BValue(int value) {
			this.value = value;
		}

		@Override
		public Object mapKey() {
			return key;
		}

		@Override
		public void mapKey(Object key) {
			this.key = key;
		}

		@Override
		protected void initChildrenRootInfo(Record.RootInfo root) {
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteInt(value);
		}

		@Override
		public void decode(IByteBuffer bb) {
			value = bb.ReadInt();
		}

		@Override
		public BValue copy() {
			return new BValue(value);
		}

		@Override
		public void followerApply(Log log) {
			var variables = ((LogBean)log).getVariables();
			if (variables != null && variables.get(1) instanceof LogInt changed)
				value = changed.value;
		}

		@Override
		public void leaderApplyNoRecursive(Log log) {
			if (log.getVariableId() == 1)
				value = ((LogInt)log).value;
		}
	}

	@Test
	public void scalarReinsertionWithEquivalentKeySurvivesReplay() {
		var leader = new CollSortedMap1<BigDecimal, Integer>(BigDecimal.class, Integer.class);
		var follower = new CollSortedMap1<BigDecimal, Integer>(BigDecimal.class, Integer.class);
		leader.put(decimal("1.0"), 10);
		follower.put(decimal("1.0"), 10);
		var log = new LogSortedMap1<BigDecimal, Integer>(BigDecimal.class, Integer.class);
		log.setValue(leader.map);
		log.remove(decimal("1.00"));
		log.put(decimal("1.000"), 20);
		assertTrue(log.getRemoved().isEmpty(), "put必须撤销等价键的remove");
		assertEquals(1, log.getPutted().size());
		leader.leaderApplyNoRecursive(log);
		var decoded = new LogSortedMap1<BigDecimal, Integer>(BigDecimal.class, Integer.class);
		var input = encode(log);
		decoded.decode(input);
		follower.followerApply(decoded);
		assertTrue(input.isEmpty());
		assertEquals(1, follower.size());
		assertEquals(leader.get(decimal("1")), follower.get(decimal("1")));
		assertEquals(20, follower.get(decimal("1")));
	}

	@Test
	public void nestedEquivalentReplacementAndClearRetainFinalDelta() {
		var log = new LogSortedMap1<BigDecimal, Integer>(BigDecimal.class, Integer.class);
		log.setBelong(new BValue());
		log.setValue(TreePMap.<BigDecimal, Integer>empty());
		log.put(decimal("1.0"), 10);
		var savepoint = new Savepoint();
		savepoint.putLog(log);
		@SuppressWarnings("unchecked")
		var nested = (LogSortedMap1<BigDecimal, Integer>)log.beginSavepoint();
		nested.remove(decimal("1.00"));
		nested.put(decimal("1.000"), 20);
		nested.endSavepoint(savepoint);
		assertEquals(1, log.getPutted().size(), "合并不能留下两个等价替换键");
		assertTrue(log.getRemoved().isEmpty());
		log.clear();
		assertTrue(log.getPutted().isEmpty(), "clear须撤销所有等价键的替换");
		log.put(decimal("1.0000"), 30);
		var decoded = new LogSortedMap1<BigDecimal, Integer>(BigDecimal.class, Integer.class);
		decoded.decode(encode(log));
		var follower = new CollSortedMap1<BigDecimal, Integer>(BigDecimal.class, Integer.class);
		follower.followerApply(decoded);
		assertEquals(1, follower.size());
		assertEquals(30, follower.get(decimal("1")));
	}











	private static BigDecimal decimal(String value) {
		return new BigDecimal(value);
	}

	private static ByteBuffer encode(Log log) {
		var buffer = ByteBuffer.Allocate();
		log.encode(buffer);
		return ByteBuffer.Wrap(buffer.Copy());
	}
}
