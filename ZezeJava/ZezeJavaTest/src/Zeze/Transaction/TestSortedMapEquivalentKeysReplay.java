package Zeze.Transaction;

import java.math.BigDecimal;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Transaction.Collections.LogSortedMap1;
import Zeze.Transaction.Collections.LogSortedMap2;
import Zeze.Transaction.Collections.PSortedMap1;
import Zeze.Transaction.Collections.PSortedMap2;
import Zeze.Transaction.Collections.SortedMap1Meta;
import Zeze.Transaction.Collections.SortedMap2Meta;
import Zeze.Transaction.Logs.LogLong;
import demo.Module1.BValue;
import demo.ModuleGTable.Bean1;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestSortedMapEquivalentKeysReplay {
	static {
		Log.register(LogLong::new);
	}

	private static PSortedMap2<String, BValue> newCaseInsensitiveBeanMap() {
		var map = new PSortedMap2<String, BValue>(String.class, BValue.class);
		var initial = new LogSortedMap2<String, BValue>(null, 1, map,
				org.pcollections.TreePMap.empty(String.CASE_INSENSITIVE_ORDER), map.getMeta());
		initial.commit();
		return map;
	}

	private static PSortedMap1<String, Integer> newCaseInsensitiveValueMap() {
		var map = new PSortedMap1<String, Integer>(String.class, Integer.class);
		var initial = new LogSortedMap1<String, Integer>(null, 1, map,
				org.pcollections.TreePMap.empty(String.CASE_INSENSITIVE_ORDER), map.getMeta());
		initial.commit();
		return map;
	}

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

	@Test
	public void beanChangesWithEquivalentDecimalKeysSurviveEncodingAndReplay() {
		var leader = new PSortedMap2<BigDecimal, BValue>(BigDecimal.class, BValue.class);
		var value = new BValue();
		leader.put(new BigDecimal("1.0"), value);
		var follower = new PSortedMap2<BigDecimal, BValue>(BigDecimal.class, BValue.class);
		follower.put(new BigDecimal("1.00"), new BValue());
		assertBeanChangeReplay(leader, follower, new BigDecimal("1.000"));
	}

	@Test
	public void beanChangesUseTheValuesCustomComparator() {
		var leader = newCaseInsensitiveBeanMap();
		leader.put("Alpha", new BValue());
		var follower = newCaseInsensitiveBeanMap();
		follower.put("ALPHA", new BValue());
		assertBeanChangeReplay(leader, follower, "alpha");
	}

	private static <K extends Comparable<K>> void assertBeanChangeReplay(
			PSortedMap2<K, BValue> leader, PSortedMap2<K, BValue> follower, K equivalentKey) {
		var value = leader.get(equivalentKey);
		value.setLong2(7);
		var change = new LogBean(null, 0, value);
		var field = new LogLong(2); // BValue.long2
		field.value = 7;
		change.getVariablesOrNew().put(2, field);
		@SuppressWarnings("unchecked")
		var log = (LogSortedMap2<K, BValue>)leader.createLogBean();
		log.getChanged().add(change);
		assertTrue(log.buildChangedWithKey());
		assertEquals(1, log.getChangedWithKey().size());
		assertSame(change, log.getChangedWithKey().get(equivalentKey),
				"原位修改索引须使用底层映射的键等价关系");

		var input = encode(log);
		@SuppressWarnings("unchecked")
		var decoded = (LogSortedMap2<K, BValue>)follower.createLogBean();
		decoded.decode(input);
		assertTrue(input.isEmpty());
		assertEquals(1, decoded.getChangedWithKey().size());
		assertEquals(7, ((LogLong)decoded.getChangedWithKey().get(equivalentKey)
				.getVariables().get(2)).value);
		var followerValue = follower.get(equivalentKey);
		follower.followerApply(decoded);
		assertSame(followerValue, follower.get(equivalentKey), "回放须原位编辑已有 Bean");
		assertEquals(value.getLong2(), followerValue.getLong2());
		assertEquals(1, follower.size());

		log.mergeChangedToReplaced();
		assertSame(value, log.getReplaced().get(equivalentKey));
	}



	@Test
	public void clearAndCopyPreserveCustomComparator() {
		var values = newCaseInsensitiveValueMap();
		values.put("Alpha", 1);
		var valueCopy = values.copy();
		assertSame(String.CASE_INSENSITIVE_ORDER, valueCopy.comparator());
		assertEquals(1, valueCopy.get("ALPHA"));
		@SuppressWarnings("unchecked")
		var log = (LogSortedMap1<String, Integer>)values.createLogBean();
		log.clear();
		assertSame(String.CASE_INSENSITIVE_ORDER, log.getValue().comparator());
		log.put("Beta", 2);
		log.put("BETA", 3);
		assertEquals(1, log.getValue().size());
		log.commit();
		@SuppressWarnings("unchecked")
		var decoded = (LogSortedMap1<String, Integer>)valueCopy.createLogBean();
		decoded.decode(encode(log));
		valueCopy.followerApply(decoded);
		assertEquals(values.get("beta"), valueCopy.get("beta"));
		values.clear();
		assertSame(String.CASE_INSENSITIVE_ORDER, values.comparator());
		values.put("Gamma", 4);
		values.put("GAMMA", 5);
		assertEquals(1, values.size());

		var beans = newCaseInsensitiveBeanMap();
		var value = new BValue();
		value.setLong2(7);
		beans.put("Alpha", value);
		var beanCopy = beans.copy();
		assertSame(String.CASE_INSENSITIVE_ORDER, beanCopy.comparator());
		assertEquals(7, beanCopy.get("ALPHA").getLong2());
		assertNotSame(value, beanCopy.get("alpha"), "Bean copy 仍须深拷贝");
		assertEquals("Alpha", beanCopy.get("alpha").mapKey());
		var follower = newCaseInsensitiveBeanMap();
		follower.put("ALPHA", new BValue());
		assertBeanChangeReplay(beanCopy, follower, "alpha");
		beans.clear();
		assertSame(String.CASE_INSENSITIVE_ORDER, beans.comparator());
		beans.put("Beta", new BValue());
		beans.put("BETA", new BValue());
		assertEquals(1, beans.size());
	}

	private static ByteBuffer encode(Log log) {
		var buffer = ByteBuffer.Allocate();
		log.encode(buffer);
		return ByteBuffer.Wrap(buffer.Copy());
	}
}
