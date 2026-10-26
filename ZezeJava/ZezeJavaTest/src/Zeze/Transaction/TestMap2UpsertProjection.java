package Zeze.Transaction;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Transaction.Collections.Map2DeltaTestSupport;
import Zeze.Transaction.Collections.PMap2;
import Zeze.Transaction.Collections.PSortedMap2;
import Zeze.Transaction.Logs.LogLong;
import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Test;

@Fast
public class TestMap2UpsertProjection {
	static {
		Zeze.Transaction.LogRegistry.register(LogLong.TYPE_ID, "LogLong", LogLong::decode);
	}

	private static BValue bean(long value) {
		var bean = new BValue();
		bean.setLong2(value);
		return bean;
	}

	private static LogBean fieldChange(BValue bean) {
		var log = new LogBean(0, bean);
		var field = Map2TestSupport.longLog(2, bean.getLong2());
		log.getVariablesOrNew().put(2, field);
		return log;
	}

	private static byte[] encode(Log log) {
		var out = ByteBuffer.Allocate();
		log.encode(out);
		return out.Copy();
	}

	@Test
	public void mapProjectionDeliversUpdatesOnceAndExcludesStaleAndRemovedValues() {
		var source = new PMap2<Integer, BValue>(Integer.class, BValue.class);
		var changed = bean(7);
		var oldReplacement = bean(2);
		var removed = bean(3);
		var replacement = bean(10);
		source.put(1, changed);
		source.put(2, oldReplacement);
		source.put(3, removed);
		var payload = Map2DeltaTestSupport.map(source, delta -> {
			delta.put(2, replacement);
			delta.remove(3);
		});
		payload.addChild(source, fieldChange(changed));
		payload.addChild(source, fieldChange(changed));
		payload.addChild(source, fieldChange(oldReplacement));
		payload.addChild(source, fieldChange(removed));
		var before = encode(payload);
		var values = new HashMap<Integer, BValue>();
		payload.forEachUpsert((key, value) -> assertNull(values.put(key, value), "each key must be delivered once"));
		assertEquals(Set.of(1, 2), values.keySet());
		assertSame(changed, values.get(1));
		assertSame(replacement, values.get(2));
		assertEquals(Set.of(3), payload.getRemoved());
		assertArrayEquals(before, encode(payload));
	}

	@Test
	public void sortedProjectionDeliversEachCompleteValueOnce() {
		var source = new PSortedMap2<Integer, BValue>(Integer.class, BValue.class);
		var changedValue = bean(7);
		source.put(1, changedValue);
		var replacement = bean(10);
		var payload = Map2DeltaTestSupport.sorted(source, delta -> delta.put(10, replacement));
		payload.addChild(source, fieldChange(changedValue));
		var before = encode(payload);
		var values = new HashMap<Integer, BValue>();
		payload.forEachUpsert((key, value) -> assertNull(values.put(key, value), "each key must be delivered once"));
		assertEquals(Set.of(1, 10), values.keySet());
		assertSame(changedValue, values.get(1));
		assertSame(replacement, values.get(10));
		assertArrayEquals(before, encode(payload));
		assertEquals(1, payload.getReplaced().size());
		assertEquals(1, payload.getChangedWithKey().size());
	}

	@Test
	public void sortedProjectionKeepsCompleteValuesAndContainerComparator() {
		var source = new PSortedMap2<Integer, BValue>(Integer.class, BValue.class);
		Map2DeltaTestSupport.initSortedMap(source,
				org.pcollections.TreePMap.empty(Comparator.reverseOrder()));
		var second = bean(2);
		var fourth = bean(4);
		source.put(2, second);
		source.put(4, fourth);
		source.put(6, bean(6));
		var payload = Map2DeltaTestSupport.sorted(source, delta -> {
			delta.put(1, bean(1));
			delta.put(3, bean(3));
			delta.put(5, bean(5));
			delta.remove(6);
		});
		payload.addChild(source, fieldChange(second));
		payload.addChild(source, fieldChange(fourth));
		var before = encode(payload);
		var values = new HashMap<Integer, Long>();
		payload.forEachUpsert((key, value) -> {
			assertNull(values.put(key, value.getLong2()), "each key must be delivered once");
		});
		assertEquals(Map.of(1, 1L, 2, 2L, 3, 3L, 4, 4L, 5, 5L), values);
		// 投影不保证联合顺序；各底层容器仍保留源比较器。
		assertEquals(List.of(5, 3, 1), new ArrayList<>(payload.getReplaced().keySet()));
		assertEquals(List.of(4, 2), new ArrayList<>(payload.getChangedWithKey().keySet()));
		assertEquals(Set.of(6), payload.getRemoved());
		assertArrayEquals(before, encode(payload));
	}

	@Test
	public void sortedProjectionKeepsComparatorKeyEquivalenceWhenFilteringUpdates() {
		var source = new PSortedMap2<String, BValue>(String.class, BValue.class);
		Map2DeltaTestSupport.initSortedMap(source,
				org.pcollections.TreePMap.empty(String.CASE_INSENSITIVE_ORDER));
		var oldReplacement = bean(1);
		var changed = bean(2);
		var removed = bean(3);
		var replacement = bean(10);
		source.put("Alpha", oldReplacement);
		source.put("Bravo", changed);
		source.put("Charlie", removed);
		var payload = Map2DeltaTestSupport.sorted(source, delta -> {
			delta.put("aLPHa", replacement);
			delta.remove("cHARLie");
		});
		payload.addChild(source, fieldChange(changed));
		payload.addChild(source, fieldChange(changed));
		payload.addChild(source, fieldChange(oldReplacement));
		payload.addChild(source, fieldChange(removed));
		var before = encode(payload);
		var values = new TreeMap<String, BValue>(String.CASE_INSENSITIVE_ORDER);
		payload.forEachUpsert((key, value) -> assertNull(values.put(key, value), "equivalent keys must be delivered once"));
		assertEquals(2, values.size());
		assertSame(replacement, values.get("ALPHA"));
		assertSame(changed, values.get("BRAVO"));
		assertTrue(payload.getRemoved().contains("CHARLIE"));
		assertEquals(1, payload.getChangedWithKey().size());
		assertArrayEquals(before, encode(payload));
	}

	@Test
	public void mutableSourceMapRejectsInvalidEditsBeforeAnyAction() {
		var source = new PMap2<Integer, BValue>(Integer.class, BValue.class);
		var changedValue = bean(7);
		source.put(2, changedValue);
		var payload = Map2DeltaTestSupport.map(source, delta -> delta.put(1, bean(10)));
		payload.addChild(source, fieldChange(changedValue));
		var actions = new AtomicInteger();
		payload.forEachUpsert((key, value) -> actions.incrementAndGet());
		assertEquals(2, actions.get());
		payload.getChangedWithKey().put(3, new LogBean(0, null));
		actions.set(0);
		assertThrows(IllegalStateException.class, () -> payload.forEachUpsert((key, value) -> actions.incrementAndGet()));
		assertEquals(0, actions.get());
	}

	@Test
	public void mutableSourceSortedMapRejectsInvalidEditsBeforeAnyAction() {
		var source = new PSortedMap2<Integer, BValue>(Integer.class, BValue.class);
		var changedValue = bean(7);
		source.put(2, changedValue);
		var payload = Map2DeltaTestSupport.sorted(source, delta -> delta.put(1, bean(10)));
		payload.addChild(source, fieldChange(changedValue));
		var actions = new AtomicInteger();
		payload.forEachUpsert((key, value) -> actions.incrementAndGet());
		assertEquals(2, actions.get());
		payload.getChangedWithKey().put(3, new LogBean(0, null));
		actions.set(0);
		assertThrows(IllegalStateException.class, () -> payload.forEachUpsert((key, value) -> actions.incrementAndGet()));
		assertEquals(0, actions.get());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void decodedMixedMapPayloadRejectsProjectionBeforeAnyAction() {
		var source = Map2TestSupport.wireLog(new PMap2<Integer, BValue>(Integer.class, BValue.class));
		source.getReplaced().put(1, bean(10));
		source.getChangedWithKey().put(2, fieldChange(bean(7)));
		var decoded = Zeze.Transaction.Map2TestSupport.decode(Map2TestSupport.wireLog(new PMap2<Integer, BValue>(Integer.class, BValue.class)), ByteBuffer.Wrap(encode(source)));
		var actions = new AtomicInteger();
		assertThrows(IllegalStateException.class, () -> decoded.forEachUpsert((key, value) -> actions.incrementAndGet()));
		assertEquals(0, actions.get(), "a complete replacement must not leak out before rejection of the decoded edit");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void decodedMixedSortedPayloadRejectsProjectionBeforeAnyAction() {
		var source = Map2TestSupport.wireLog(new PSortedMap2<Integer, BValue>(Integer.class, BValue.class));
		source.getReplaced().put(1, bean(10));
		source.getChangedWithKey().put(2, fieldChange(bean(7)));
		var decoded = Zeze.Transaction.Map2TestSupport.decode(Map2TestSupport.wireLog(new PSortedMap2<Integer, BValue>(Integer.class, BValue.class)), ByteBuffer.Wrap(encode(source)));
		var actions = new AtomicInteger();
		assertThrows(IllegalStateException.class, () -> decoded.forEachUpsert((key, value) -> actions.incrementAndGet()));
		assertEquals(0, actions.get());
	}
}
