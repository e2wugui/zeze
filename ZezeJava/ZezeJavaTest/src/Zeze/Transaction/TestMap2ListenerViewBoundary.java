package Zeze.Transaction;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Transaction.Collections.Map2DeltaTestSupport;
import Zeze.Transaction.Collections.Map2ListenerView;
import Zeze.Transaction.Collections.PMap2;
import Zeze.Transaction.Collections.PSortedMap2;
import Zeze.Transaction.Logs.LogLong;
import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Test;

@Fast
public class TestMap2ListenerViewBoundary {
	static {
		Zeze.Transaction.LogRegistry.register(new Zeze.Transaction.LogRegistry.Factory(LogLong.TYPE_ID, "LogLong", LogLong::decode));
	}

	private static BValue bean(long value) {
		var bean = new BValue();
		bean.setLong2(value);
		return bean;
	}

	private static LogBean fieldChange(BValue bean) {
		var log = new LogBean(0, bean);
		var field = Map2TestSupport.longLog(2, 7);
		log.getVariablesOrNew().put(2, field);
		return log;
	}

	private static byte[] encode(Log log) {
		var out = ByteBuffer.Allocate();
		log.encode(out);
		return out.Copy();
	}

	@Test
	public void sortedProjectionMergesStructuralAndFieldChangesInComparatorOrder() {
		var source = new PSortedMap2<Integer, BValue>(Integer.class, BValue.class);
		var changedValue = bean(7);
		source.put(1, changedValue);
		var payload = Map2DeltaTestSupport.sorted(source, delta -> delta.put(10, bean(10)));
		payload.addChild(source, fieldChange(changedValue));
		var before = encode(payload);
		var viewOrder = new ArrayList<Integer>();
		Map2ListenerView.of(payload).forEachUpsert((key, value) -> viewOrder.add(key));
		assertEquals(List.of(1, 10), viewOrder, "union traversal must preserve the map comparator across both sections");
		var directOrder = new ArrayList<Integer>();
		Map2ListenerView.forEachUpsert(payload, (key, value) -> directOrder.add(key));
		assertEquals(viewOrder, directOrder);
		assertArrayEquals(before, encode(payload));
		assertEquals(1, payload.getReplaced().size());
		assertEquals(1, payload.getChangedWithKey().size());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void decodedMixedMapPayloadRejectsProjectionBeforeAnyAction() {
		var source = Map2TestSupport.wireLog(new PMap2<Integer, BValue>(Integer.class, BValue.class));
		source.getReplaced().put(1, bean(10));
		source.getChangedWithKey().put(2, fieldChange(bean(7)));
		var decoded = Zeze.Transaction.Map2TestSupport.decode(Map2TestSupport.wireLog(new PMap2<Integer, BValue>(Integer.class, BValue.class)), ByteBuffer.Wrap(encode(source)));
		var actions = new AtomicInteger();
		assertThrows(IllegalStateException.class, () -> Map2ListenerView.of(decoded));
		assertThrows(IllegalStateException.class,
			() -> Map2ListenerView.forEachUpsert(decoded, (key, value) -> actions.incrementAndGet()));
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
		assertThrows(IllegalStateException.class, () -> Map2ListenerView.of(decoded));
		assertThrows(IllegalStateException.class,
			() -> Map2ListenerView.forEachUpsert(decoded, (key, value) -> actions.incrementAndGet()));
		assertEquals(0, actions.get());
	}
}
