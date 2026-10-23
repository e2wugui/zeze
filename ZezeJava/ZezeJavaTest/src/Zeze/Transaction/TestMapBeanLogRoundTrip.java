package Zeze.Transaction;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Transaction.Collections.LogList2;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Transaction.Collections.LogSortedMap2;
import Zeze.Transaction.Collections.PMap2;
import Zeze.Transaction.Collections.PList2;
import Zeze.Transaction.Collections.PSortedMap2;
import Zeze.Transaction.Logs.LogLong;
import Zeze.Transaction.Logs.LogLongValue;
import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestMapBeanLogRoundTrip {
	static {
		Zeze.Transaction.LogRegistry.register(new Zeze.Transaction.LogRegistry.Factory(LogLong.TYPE_ID, "LogLong", LogLong::decode));
	}

	private static final class Fixture {
		private final Map<String, BValue> map;
		private LogBean log;
		private final Consumer<Log> apply;

		Fixture(Map<String, BValue> map, LogBean log, Consumer<Log> apply) {
			this.map = map;
			this.log = log;
			this.apply = apply;
		}

		Map<String, BValue> map() {
			return map;
		}

		LogBean log() {
			return log;
		}

		Consumer<Log> apply() {
			return apply;
		}

		void decode(ByteBuffer input) {
			log = Map2TestSupport.decode(log, input);
		}
	}

	private static Fixture fixture(boolean sorted, String key) {
		if (sorted) {
			var map = new PSortedMap2<String, BValue>(String.class, BValue.class);
			map.put(key, new BValue());
			return new Fixture(map, Map2TestSupport.wireLog(map), map::followerApply);
		}
		var map = new PMap2<String, BValue>(String.class, BValue.class);
		map.put(key, new BValue());
		return new Fixture(map, Map2TestSupport.wireLog(map), map::followerApply);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, LogBean> index(LogBean log) {
		return log instanceof LogMap2<?, ?> ? ((LogMap2<String, BValue>)log).getChangedWithKey()
			: ((LogSortedMap2<String, BValue>)log).getChangedWithKey();
	}

	private static void change(Fixture fixture, String key, long value) {
		var bean = fixture.map().get(key);
		bean.setLong2(value);
		// This fixture already describes a canonical wire payload. Source identity filtering
		// is exercised through collect in TestStaleBeanChangedFilter.
		index(fixture.log()).put(key, beanLog(bean, value));
	}

	private static LogBean beanLog(BValue bean, long value) {
		var beanLog = new LogBean(0, bean);
		var fieldLog = Map2TestSupport.longLog(2, value);
		beanLog.getVariablesOrNew().put(2, fieldLog);
		return beanLog;
	}

	private static ByteBuffer encode(Log log) {
		var bb = ByteBuffer.Allocate();
		log.encode(bb);
		return ByteBuffer.Wrap(bb.Copy());
	}

	@Test
	public void decodedChangesSurviveReencodingAndRemainEditable() {
		for (boolean sorted : new boolean[]{false, true}) {
			var source = fixture(sorted, "k");
			change(source, "k", 7);
			var decoded = fixture(sorted, "k").log();
			decoded = Zeze.Transaction.Map2TestSupport.decode(decoded, encode(source.log()));
			var follower = fixture(sorted, "k");
			var oldBean = follower.map().get("k");
			follower.decode(encode(decoded));
			follower.apply().accept(follower.log());
			assertSame(oldBean, follower.map().get("k"));
			assertEquals(7, oldBean.getLong2());

			requireNonNull(((LogLongValue)requireNonNull(index(decoded).get("k").getVariables()).get(2))).value = 11;
			follower.decode(encode(decoded));
			follower.apply().accept(follower.log());
			assertEquals(11, oldBean.getLong2());
			index(decoded).clear();
			follower.decode(encode(decoded));
			assertTrue(index(follower.log()).isEmpty());
			follower.apply().accept(follower.log());
			assertEquals(11, oldBean.getLong2());
		}
	}

	@Test
	public void reuseDecodeReplacesThePreviousIndexAndSourceChanges() {
		for (boolean sorted : new boolean[]{false, true}) {
			var first = fixture(sorted, "first");
			change(first, "first", 3);
			var second = fixture(sorted, "second");
			change(second, "second", 9);
			var reused = fixture(sorted, "stale");
			change(reused, "stale", 99);
			reused.decode(encode(first.log()));
			reused.decode(encode(second.log()));
			assertEquals(Set.of("second"), index(reused.log()).keySet());
			var follower = fixture(sorted, "second");
			follower.decode(encode(reused.log()));
			follower.apply().accept(follower.log());
			assertEquals(9, follower.map().get("second").getLong2());
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void canonicalEncodingDoesNotReconsultTheSourceMap() {
		for (boolean sorted : new boolean[]{false, true}) {
			var source = fixture(sorted, "k");
			change(source, "k", 7);
			var before = encode(source.log()).Copy();
			assertEquals(1, index(source.log()).size());
			var replacement = new BValue();
			replacement.setLong2(13);
			source.map().put("k", replacement);
			assertArrayEquals(before, encode(source.log()).Copy(),
				"encoding a canonical payload must not rebuild its keyed changes from a later source value");
			var follower = fixture(sorted, "k");
			follower.decode(encode(source.log()));
			assertEquals(Set.of("k"), index(follower.log()).keySet());
			follower.apply().accept(follower.log());
			assertEquals(7, follower.map().get("k").getLong2());
		}
	}

	private static PList2<BValue> list() {
		var list = new PList2<BValue>(BValue.class);
		list.add(new BValue());
		list.add(new BValue());
		return list;
	}

	@SuppressWarnings("unchecked")
	private static LogList2<BValue> listLog(PList2<BValue> list) {
		Zeze.History.Helper.registerLogList2(BValue.class);
		return (LogList2<BValue>)list.createLogBean(null);
	}

	@Test
	public void decodedListChangesSurviveReencodingAndRemainEditable() {
		var source = list();
		var sourceLog = listLog(source);
		sourceLog.addChild(source, beanLog(source.get(0), 7));
		var decoded = listLog(list());
		decoded = Zeze.Transaction.Map2TestSupport.decode(decoded, encode(sourceLog));
		var follower = list();
		var oldBean = follower.get(0);
		var replay = listLog(follower);
		replay = Zeze.Transaction.Map2TestSupport.decode(replay, encode(decoded));
		follower.followerApply(replay);
		assertSame(oldBean, follower.get(0));
		assertEquals(7, oldBean.getLong2());
		var entry = decoded.getChanged().entrySet().iterator().next();
		requireNonNull(((LogLongValue)requireNonNull(entry.getKey().getVariables()).get(2))).value = 11;
		entry.getValue().value = 1;
		replay = Zeze.Transaction.Map2TestSupport.decode(replay, encode(decoded));
		follower.followerApply(replay);
		assertEquals(7, follower.get(0).getLong2());
		assertEquals(11, follower.get(1).getLong2());
		decoded.getChanged().clear();
		replay = Zeze.Transaction.Map2TestSupport.decode(replay, encode(decoded));
		assertTrue(replay.getChanged().isEmpty());
	}

	@Test
	public void reuseListDecodeReplacesChangesAndOperations() {
		var first = list();
		var firstLog = listLog(first);
		firstLog.addChild(first, beanLog(first.get(0), 3));
		var second = list();
		var secondLog = listLog(second);
		secondLog.addChild(second, beanLog(second.get(1), 9));
		var reused = listLog(list());
		reused.getOpLogs().addAll(Zeze.Transaction.Collections.List2TestSupport.addOperation(2, new BValue()));
		reused = Zeze.Transaction.Map2TestSupport.decode(reused, encode(firstLog));
		reused = Zeze.Transaction.Map2TestSupport.decode(reused, encode(secondLog));
		assertTrue(reused.getOpLogs().isEmpty());
		assertEquals(1, reused.getChanged().size());
		var follower = list();
		var replay = listLog(follower);
		replay = Zeze.Transaction.Map2TestSupport.decode(replay, encode(reused));
		follower.followerApply(replay);
		assertEquals(0, follower.get(0).getLong2());
		assertEquals(9, follower.get(1).getLong2());
		assertEquals(2, follower.size());
	}

	@Test
	public void canonicalListEncodingDoesNotReconsultTheSourceList() {
		var source = list();
		var sourceLog = listLog(source);
		sourceLog.addChild(source, beanLog(source.get(0), 7));
		encode(sourceLog);
		assertEquals(1, sourceLog.getChanged().size());
		var replacement = new BValue();
		replacement.setLong2(13);
		source.set(0, replacement);
		var follower = list();
		var replay = listLog(follower);
		replay = Zeze.Transaction.Map2TestSupport.decode(replay, encode(sourceLog));
		assertEquals(1, replay.getChanged().size());
		follower.followerApply(replay);
		assertEquals(7, follower.get(0).getLong2());
	}
}
