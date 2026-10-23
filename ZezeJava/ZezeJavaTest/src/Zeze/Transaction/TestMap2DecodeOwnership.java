package Zeze.Transaction;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;

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
public class TestMap2DecodeOwnership {
	static {
		Zeze.Transaction.LogRegistry.register(new Zeze.Transaction.LogRegistry.Factory(LogLong.TYPE_ID, "LogLong", LogLong::decode));
	}

	private static byte[] encode(Log log) {
		var buffer = ByteBuffer.Allocate();
		log.encode(buffer);
		return buffer.Copy();
	}

	private static BValue bean(long value) {
		var bean = new BValue();
		bean.setLong2(value);
		return bean;
	}

	@Test
	public void mapDecodeFactoryLeavesBorrowedTransactionContainersUntouched() {
		var map = new PMap2<String, BValue>(String.class, BValue.class);
		map.put("deleted", bean(3));
		var replacement = bean(7);
		var payload = Map2DeltaTestSupport.map(map, delta -> {
			delta.remove("deleted");
			delta.put("replacement", replacement);
		});
		var borrowedReplaced = payload.getReplaced();
		var borrowedRemoved = payload.getRemoved();
		var nextMap = Map2TestSupport.wireLog(map);
		nextMap.getReplaced().put("decoded", bean(11));
		var originalBytes = encode(payload);
		assertSeparateDecodePreservesSource(payload, encode(nextMap), originalBytes);
		assertSame(map, payload.getThis());
		assertSame(borrowedReplaced, payload.getReplaced());
		assertSame(borrowedRemoved, payload.getRemoved());
		assertEquals(Set.of("replacement"), borrowedReplaced.keySet());
		assertSame(replacement, borrowedReplaced.get("replacement"));
		assertEquals(Set.of("deleted"), borrowedRemoved);
		assertSame(replacement, map.get("replacement"));
	}

	@Test
	public void sortedMapDecodeFactoryLeavesBorrowedTransactionContainersUntouched() {
		var map = new PSortedMap2<String, BValue>(String.class, BValue.class);
		map.put("deleted", bean(3));
		var replacement = bean(7);
		var payload = Map2DeltaTestSupport.sorted(map, delta -> {
			delta.remove("deleted");
			delta.put("replacement", replacement);
		});
		var borrowedReplaced = payload.getReplaced();
		var borrowedRemoved = payload.getRemoved();
		var next = Map2TestSupport.wireLog(map);
		next.getReplaced().put("decoded", bean(11));
		var originalBytes = encode(payload);
		assertSeparateDecodePreservesSource(payload, encode(next), originalBytes);
		assertSame(map, payload.getThis());
		assertSame(borrowedReplaced, payload.getReplaced());
		assertSame(borrowedRemoved, payload.getRemoved());
		assertEquals(Set.of("replacement"), borrowedReplaced.keySet());
		assertSame(replacement, borrowedReplaced.get("replacement"));
		assertEquals(Set.of("deleted"), borrowedRemoved);
		assertSame(replacement, map.get("replacement"));
	}

	private static void assertSeparateDecodePreservesSource(LogBean source, byte[] incoming, byte[] originalBytes) {
		var decoded = Map2TestSupport.decode(source, ByteBuffer.Wrap(incoming));
		assertNull(decoded.getThis());
		org.junit.jupiter.api.Assertions.assertNotSame(source, decoded);
		for (byte[] bytes : new byte[][]{Arrays.copyOf(incoming, incoming.length - 1), new byte[0]}) {
			assertThrows(RuntimeException.class, () -> Map2TestSupport.decode(source, ByteBuffer.Wrap(bytes)));
			assertArrayEquals(originalBytes, encode(source));
		}
		assertArrayEquals(originalBytes, encode(source));
	}

	private static LogBean fieldChange(long value) {
		var beanLog = new LogBean(0, null);
		var fieldLog = Map2TestSupport.longLog(2, value);
		beanLog.getVariablesOrNew().put(2, fieldLog);
		return beanLog;
	}

	@Test
	public void successiveMapDecodesRemainIndependentOnFailure() {
		var map = new PMap2<String, BValue>(String.class, BValue.class);
		var first = Map2TestSupport.wireLog(map);
		first.getChangedWithKey().put("edit-first", fieldChange(7));
		first.getReplaced().put("first", bean(11));
		first.getRemoved().add("removed-first");
		var next = Map2TestSupport.wireLog(map);
		next.getChangedWithKey().put("edit-next", fieldChange(9));
		next.getReplaced().put("next", bean(13));
		next.getRemoved().add("removed-next");
		var target = Map2TestSupport.wireLog(map);
		target = Zeze.Transaction.Map2TestSupport.decode(target, ByteBuffer.Wrap(encode(first)));
		var previousReplaced = target.getReplaced();
		var previousRemoved = target.getRemoved();
		var previousChanged = target.getChangedWithKey();
		assertOwnedDecode(target, first, next);
		assertEquals(Set.of("first"), previousReplaced.keySet(), "repeated decode must rebind, not clear prior owned containers");
		assertEquals(Set.of("removed-first"), previousRemoved);
		assertEquals(Set.of("edit-first"), previousChanged.keySet());
	}

	@Test
	public void successiveSortedMapDecodesRemainIndependentOnFailure() {
		var map = new PSortedMap2<String, BValue>(String.class, BValue.class);
		var first = Map2TestSupport.wireLog(map);
		first.getChangedWithKey().put("edit-first", fieldChange(7));
		first.getReplaced().put("first", bean(11));
		first.getRemoved().add("removed-first");
		var next = Map2TestSupport.wireLog(map);
		next.getChangedWithKey().put("edit-next", fieldChange(9));
		next.getReplaced().put("next", bean(13));
		next.getRemoved().add("removed-next");
		var target = Map2TestSupport.wireLog(map);
		target = Zeze.Transaction.Map2TestSupport.decode(target, ByteBuffer.Wrap(encode(first)));
		var previousReplaced = target.getReplaced();
		var previousRemoved = target.getRemoved();
		var previousChanged = target.getChangedWithKey();
		assertOwnedDecode(target, first, next);
		assertEquals(Set.of("first"), previousReplaced.keySet());
		assertEquals(Set.of("removed-first"), previousRemoved);
		assertEquals(Set.of("edit-first"), previousChanged.keySet());
	}

	private static void assertOwnedDecode(LogBean target, LogBean first, LogBean next) {
		assertNull(target.getThis());
		byte[] firstBytes = encode(first);
		byte[] nextBytes = encode(next);
		target = Zeze.Transaction.Map2TestSupport.decode(target, ByteBuffer.Wrap(firstBytes));
		assertArrayEquals(firstBytes, encode(target));
		var partial = ByteBuffer.Wrap(Arrays.copyOf(nextBytes, nextBytes.length - 1));
		var previous = target;
		assertThrows(RuntimeException.class, () -> Map2TestSupport.decode(previous, partial));
		assertTrue(partial.getReadIndex() > 0, "the wire decoder must have parsed data before the truncation");
		assertArrayEquals(firstBytes, encode(target), "a late decoding failure must not publish any section");
		assertNull(target.getThis());
		target = Zeze.Transaction.Map2TestSupport.decode(target, ByteBuffer.Wrap(nextBytes));
		assertArrayEquals(nextBytes, encode(target));
		target = Zeze.Transaction.Map2TestSupport.decode(target, ByteBuffer.Wrap(nextBytes));
		assertArrayEquals(nextBytes, encode(target), "repeated factory decoding preserves the wire value");
		byte[] empty = {0, 0, 0};
		target = Zeze.Transaction.Map2TestSupport.decode(target, ByteBuffer.Wrap(empty));
		assertArrayEquals(empty, encode(target), "empty decode must replace all previous sections");
		assertArrayEquals(firstBytes, encode(first));
		assertArrayEquals(nextBytes, encode(next));
	}
}
