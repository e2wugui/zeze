package Zeze.Transaction;

import java.math.BigDecimal;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.LogSortedMap1;
import Zeze.Transaction.Collections.LogSortedMap2;
import Zeze.Transaction.Collections.PSortedMap1;
import Zeze.Transaction.Collections.PSortedMap2;
import Zeze.Transaction.Collections.SortedMap1Meta;
import Zeze.Transaction.Collections.SortedMap2Meta;
import demo.ModuleGTable.Bean1;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestSortedMapEquivalentKeysReplay {

	@Test
	public void removalWithEquivalentDecimalKeySurvivesReplay() {
		var leader = new PSortedMap1<BigDecimal, Integer>(BigDecimal.class, Integer.class);
		var meta = SortedMap1Meta.get(BigDecimal.class, Integer.class);
		var log = new LogSortedMap1<BigDecimal, Integer>(null, 1, leader, leader.getMap(), meta);
		log.put(new BigDecimal("1.0"), 10);
		assertEquals(10, log.remove(new BigDecimal("1.00")));
		log.commit();

		var input = encode(log);
		var decoded = new LogSortedMap1<BigDecimal, Integer>(null, 1, null, leader.getMap(), meta);
		decoded.decode(input);
		var follower = new PSortedMap1<BigDecimal, Integer>(BigDecimal.class, Integer.class);
		follower.followerApply(decoded);
		assertTrue(input.isEmpty());
		assertTrue(leader.isEmpty());
		assertTrue(follower.isEmpty(), "Replay must preserve deletion of the equivalent key");
	}

	@Test
	public void latestReplacementWithEquivalentDecimalKeySurvivesReplay() {
		var leader = new PSortedMap1<BigDecimal, Integer>(BigDecimal.class, Integer.class);
		var meta = SortedMap1Meta.get(BigDecimal.class, Integer.class);
		var log = new LogSortedMap1<BigDecimal, Integer>(null, 1, leader, leader.getMap(), meta);
		log.put(new BigDecimal("1.00"), 10);
		assertEquals(10, log.put(new BigDecimal("1.0"), 20));
		log.commit();

		var input = encode(log);
		var decoded = new LogSortedMap1<BigDecimal, Integer>(null, 1, null, leader.getMap(), meta);
		decoded.decode(input);
		var follower = new PSortedMap1<BigDecimal, Integer>(BigDecimal.class, Integer.class);
		follower.followerApply(decoded);
		assertTrue(input.isEmpty());
		assertEquals(1, follower.size());
		assertEquals(20, leader.get(new BigDecimal("1")));
		assertEquals(20, follower.get(new BigDecimal("1")), "Replay must retain the last replacement");
	}

	@Test
	public void reinsertionWithEquivalentDecimalKeySurvivesReplay() {
		var leader = new PSortedMap1<BigDecimal, Integer>(BigDecimal.class, Integer.class);
		leader.put(new BigDecimal("1.0"), 10);
		var follower = leader.copy();
		var meta = SortedMap1Meta.get(BigDecimal.class, Integer.class);
		var log = new LogSortedMap1<BigDecimal, Integer>(null, 1, leader, leader.getMap(), meta);
		assertEquals(10, log.remove(new BigDecimal("1.00")));
		log.put(new BigDecimal("1.000"), 20);
		log.commit();

		var input = encode(log);
		var decoded = new LogSortedMap1<BigDecimal, Integer>(null, 1, null, leader.getMap(), meta);
		decoded.decode(input);
		follower.followerApply(decoded);
		assertTrue(input.isEmpty());
		assertEquals(1, follower.size());
		assertEquals(20, follower.get(new BigDecimal("1")));
	}

	@Test
	public void nestedRemovalWithEquivalentDecimalKeySurvivesReplay() {
		var leader = new PSortedMap1<BigDecimal, Integer>(BigDecimal.class, Integer.class);
		var meta = SortedMap1Meta.get(BigDecimal.class, Integer.class);
		var log = new LogSortedMap1<BigDecimal, Integer>(new EmptyBean(), 1, leader, leader.getMap(), meta);
		log.put(new BigDecimal("1.0"), 10);
		var savepoint = new Savepoint();
		savepoint.putLog(log);
		@SuppressWarnings("unchecked")
		var nested = (LogSortedMap1<BigDecimal, Integer>)log.beginSavepoint();
		assertEquals(10, nested.remove(new BigDecimal("1.00")));
		nested.endSavepoint(savepoint);
		log.commit();

		var input = encode(log);
		var decoded = new LogSortedMap1<BigDecimal, Integer>(null, 1, null, leader.getMap(), meta);
		decoded.decode(input);
		var follower = new PSortedMap1<BigDecimal, Integer>(BigDecimal.class, Integer.class);
		follower.followerApply(decoded);
		assertTrue(input.isEmpty());
		assertTrue(leader.isEmpty());
		assertTrue(follower.isEmpty(), "Merged savepoint must preserve deletion of the equivalent key");
	}

	@Test
	public void beanRemovalWithEquivalentDecimalKeySurvivesReplay() {
		var leader = new PSortedMap2<BigDecimal, Bean1>(BigDecimal.class, Bean1.class);
		var meta = SortedMap2Meta.get(BigDecimal.class, Bean1.class);
		var log = new LogSortedMap2<BigDecimal, Bean1>(null, 1, leader, leader.getMap(), meta);
		var bean = new Bean1();
		bean.setIntVar(10);
		log.put(new BigDecimal("1.0"), bean);
		assertEquals(bean, log.remove(new BigDecimal("1.00")));
		log.commit();

		var input = encode(log);
		var decoded = new LogSortedMap2<BigDecimal, Bean1>(null, 1, null, leader.getMap(), meta);
		decoded.decode(input);
		var follower = new PSortedMap2<BigDecimal, Bean1>(BigDecimal.class, Bean1.class);
		follower.followerApply(decoded);
		assertTrue(input.isEmpty());
		assertTrue(leader.isEmpty());
		assertTrue(follower.isEmpty(), "Bean map replay must preserve deletion of the equivalent key");
	}

	private static ByteBuffer encode(Log log) {
		var buffer = ByteBuffer.Allocate();
		log.encode(buffer);
		return ByteBuffer.Wrap(buffer.Copy());
	}
}
