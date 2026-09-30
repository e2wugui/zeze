package Zeze.Transaction;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Transaction.Collections.LogSortedMap2;
import Zeze.Transaction.Collections.PMap2;
import Zeze.Transaction.Collections.PSortedMap2;
import Zeze.Transaction.Logs.LogLong;
import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestMapBeanLogRoundTrip {
	static {
		Log.register(LogLong::new);
	}

	private record Fixture(Map<String, BValue> map, LogBean log, Consumer<Log> apply) {
	}

	private static Fixture fixture(boolean sorted, String key) {
		if (sorted) {
			var map = new PSortedMap2<String, BValue>(String.class, BValue.class);
			map.put(key, new BValue());
			return new Fixture(map, map.createLogBean(), map::followerApply);
		}
		var map = new PMap2<String, BValue>(String.class, BValue.class);
		map.put(key, new BValue());
		return new Fixture(map, map.createLogBean(), map::followerApply);
	}

	@SuppressWarnings("unchecked")
	private static Set<LogBean> changed(LogBean log) {
		return log instanceof LogMap2<?, ?> ? ((LogMap2<String, BValue>)log).getChanged()
				: ((LogSortedMap2<String, BValue>)log).getChanged();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, LogBean> index(LogBean log) {
		return log instanceof LogMap2<?, ?> ? ((LogMap2<String, BValue>)log).getChangedWithKey()
				: ((LogSortedMap2<String, BValue>)log).getChangedWithKey();
	}

	private static void change(Fixture fixture, String key, long value) {
		var bean = fixture.map().get(key);
		bean.setLong2(value);
		changed(fixture.log()).add(beanLog(bean, value));
	}

	private static LogBean beanLog(BValue bean, long value) {
		var beanLog = new LogBean(null, 0, bean);
		var fieldLog = new LogLong(2);
		fieldLog.value = value;
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
		for (boolean sorted : new boolean[] {false, true}) {
			var source = fixture(sorted, "k");
			change(source, "k", 7);
			var decoded = fixture(sorted, "k").log();
			decoded.decode(encode(source.log()));
			assertTrue(changed(decoded).isEmpty());
			var follower = fixture(sorted, "k");
			var oldBean = follower.map().get("k");
			follower.log().decode(encode(decoded));
			follower.apply().accept(follower.log());
			assertSame(oldBean, follower.map().get("k"));
			assertEquals(7, oldBean.getLong2());

			((LogLong)index(decoded).get("k").getVariables().get(2)).value = 11;
			follower.log().decode(encode(decoded));
			follower.apply().accept(follower.log());
			assertEquals(11, oldBean.getLong2());
			index(decoded).clear();
			follower.log().decode(encode(decoded));
			assertTrue(index(follower.log()).isEmpty());
			follower.apply().accept(follower.log());
			assertEquals(11, oldBean.getLong2());
		}
	}

	@Test
	public void reuseDecodeReplacesThePreviousIndexAndSourceChanges() {
		for (boolean sorted : new boolean[] {false, true}) {
			var first = fixture(sorted, "first");
			change(first, "first", 3);
			var second = fixture(sorted, "second");
			change(second, "second", 9);
			var reused = fixture(sorted, "stale");
			change(reused, "stale", 99);
			reused.log().decode(encode(first.log()));
			reused.log().decode(encode(second.log()));
			assertTrue(changed(reused.log()).isEmpty());
			assertEquals(Set.of("second"), index(reused.log()).keySet());
			var follower = fixture(sorted, "second");
			follower.log().decode(encode(reused.log()));
			follower.apply().accept(follower.log());
			assertEquals(9, follower.map().get("second").getLong2());
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void liveSourceReencodingStillFiltersReplacedBeans() {
		for (boolean sorted : new boolean[] {false, true}) {
			var source = fixture(sorted, "k");
			change(source, "k", 7);
			encode(source.log());
			assertEquals(1, index(source.log()).size());
			var replacement = new BValue();
			replacement.setLong2(13);
			if (sorted)
				((LogSortedMap2<String, BValue>)source.log()).put("k", replacement);
			else
				((LogMap2<String, BValue>)source.log()).put("k", replacement);
			var follower = fixture(sorted, "k");
			follower.log().decode(encode(source.log()));
			assertTrue(index(follower.log()).isEmpty());
			follower.apply().accept(follower.log());
			assertEquals(13, follower.map().get("k").getLong2());
		}
	}










}
