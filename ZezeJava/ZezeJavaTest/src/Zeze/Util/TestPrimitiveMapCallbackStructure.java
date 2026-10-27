package Zeze.Util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ConcurrentModificationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import harness.Fast;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@Fast
public class TestPrimitiveMapCallbackStructure {
	private interface MapAccess {
		String get(int key);
		boolean containsKey(int key);
		int size();
		void put(int key, String value);
		void putIfAbsent(int key, String value);
		void putAllZero();
		void remove(int key);
		void remove(int key, String value);
		void clear();
		void clear(int maxCapacity);
		void ensureCapacity(int capacity);
		void shrink(int capacity);
		String compute(int key, BiFunction<Integer, String, String> op);
		String computeIfAbsent(int key, IntFunction<String> op);
		void foreachUpdate(BiFunction<Integer, String, String> op);
	}

	enum Kind {
		CHAR, INT, LONG;

		MapAccess create() {
			return switch (this) {
			case CHAR -> new MapAccess() {
				private final CharHashMap<String> map = new CharHashMap<>(32);
				public String get(int key) { return map.get((char)key); }
				public boolean containsKey(int key) { return map.containsKey((char)key); }
				public int size() { return map.size(); }
				public void put(int key, String value) { map.put((char)key, value); }
				public void putIfAbsent(int key, String value) { map.putIfAbsent((char)key, value); }
				public void putAllZero() { var values = new CharHashMap<String>(); values.put((char)0, "zero"); map.putAll(values); }
				public void remove(int key) { map.remove((char)key); }
				public void remove(int key, String value) { map.remove((char)key, value); }
				public void clear() { map.clear(); }
				public void clear(int capacity) { map.clear(capacity); }
				public void ensureCapacity(int capacity) { map.ensureCapacity(capacity); }
				public void shrink(int capacity) { map.shrink(capacity); }
				public String compute(int key, BiFunction<Integer, String, String> op) { return map.compute((char)key, (k, v) -> op.apply((int)k, v)); }
				public String computeIfAbsent(int key, IntFunction<String> op) { return map.computeIfAbsent((char)key, op); }
				public void foreachUpdate(BiFunction<Integer, String, String> op) { map.foreachUpdate((k, v) -> op.apply((int)k, v)); }
			};
			case INT -> new MapAccess() {
				private final IntHashMap<String> map = new IntHashMap<>(32);
				public String get(int key) { return map.get(key); }
				public boolean containsKey(int key) { return map.containsKey(key); }
				public int size() { return map.size(); }
				public void put(int key, String value) { map.put(key, value); }
				public void putIfAbsent(int key, String value) { map.putIfAbsent(key, value); }
				public void putAllZero() { var values = new IntHashMap<String>(); values.put(0, "zero"); map.putAll(values); }
				public void remove(int key) { map.remove(key); }
				public void remove(int key, String value) { map.remove(key, value); }
				public void clear() { map.clear(); }
				public void clear(int capacity) { map.clear(capacity); }
				public void ensureCapacity(int capacity) { map.ensureCapacity(capacity); }
				public void shrink(int capacity) { map.shrink(capacity); }
				public String compute(int key, BiFunction<Integer, String, String> op) { return map.compute(key, op::apply); }
				public String computeIfAbsent(int key, IntFunction<String> op) { return map.computeIfAbsent(key, op); }
				public void foreachUpdate(BiFunction<Integer, String, String> op) { map.foreachUpdate(op::apply); }
			};
			case LONG -> new MapAccess() {
				private final LongHashMap<String> map = new LongHashMap<>(32);
				public String get(int key) { return map.get(key); }
				public boolean containsKey(int key) { return map.containsKey(key); }
				public int size() { return map.size(); }
				public void put(int key, String value) { map.put(key, value); }
				public void putIfAbsent(int key, String value) { map.putIfAbsent(key, value); }
				public void putAllZero() { var values = new LongHashMap<String>(); values.put(0, "zero"); map.putAll(values); }
				public void remove(int key) { map.remove(key); }
				public void remove(int key, String value) { map.remove(key, value); }
				public void clear() { map.clear(); }
				public void clear(int capacity) { map.clear(capacity); }
				public void ensureCapacity(int capacity) { map.ensureCapacity(capacity); }
				public void shrink(int capacity) { map.shrink(capacity); }
				public String compute(int key, BiFunction<Integer, String, String> op) { return map.compute(key, (k, v) -> op.apply((int)k, v)); }
				public String computeIfAbsent(int key, IntFunction<String> op) { return map.computeIfAbsent(key, k -> op.apply((int)k)); }
				public void foreachUpdate(BiFunction<Integer, String, String> op) { map.foreachUpdate((k, v) -> op.apply((int)k, v)); }
			};
			};
		}
	}

	@ParameterizedTest
	@EnumSource(Kind.class)
	public void computeRejectsCapacityChangeInEverySlotState(Kind kind) {
		for (int key : new int[]{0, 7}) {
			for (String oldValue : new String[]{null, "old"}) {
				var map = kind.create();
				map.put(key, oldValue);
				assertThrows(ConcurrentModificationException.class, () -> map.compute(key, (k, v) -> {
					map.ensureCapacity(256);
					return "outer";
				}));
				assertEquals(oldValue, map.get(key));
				assertEquals(1, map.size());
			}
			var absent = kind.create();
			assertThrows(ConcurrentModificationException.class, () -> absent.compute(key, (k, v) -> {
				absent.ensureCapacity(256);
				return "outer";
			}));
			assertFalse(absent.containsKey(key));
			assertEquals(0, absent.size());
		}
	}

	@ParameterizedTest
	@EnumSource(Kind.class)
	public void computeIfAbsentRejectsCapacityChangeWithoutPublishingOuterValue(Kind kind) {
		for (int key : new int[]{0, 7}) {
			for (boolean present : new boolean[]{false, true}) {
				var map = kind.create();
				if (present)
					map.put(key, null);
				var calls = new AtomicInteger();
				assertThrows(ConcurrentModificationException.class, () -> map.computeIfAbsent(key, k -> {
					calls.incrementAndGet();
					map.ensureCapacity(256);
					return "outer";
				}));
				assertEquals(1, calls.get());
				assertEquals(present, map.containsKey(key));
				assertNull(map.get(key));
				assertEquals(present ? 1 : 0, map.size());
			}
		}
	}

	@ParameterizedTest
	@EnumSource(Kind.class)
	public void sameKeyRecursiveInsertKeepsInnerResultAndRejectsOuterResult(Kind kind) {
		for (int key : new int[]{0, 7}) {
			var map = kind.create();
			assertThrows(ConcurrentModificationException.class, () -> map.computeIfAbsent(key, k -> {
				map.compute(key, (innerKey, v) -> "inner");
				return "outer";
			}));
			assertEquals("inner", map.get(key));
			assertEquals(1, map.size());
		}
	}

	@ParameterizedTest
	@EnumSource(Kind.class)
	public void structuralMutationsCannotHideBehindUnchangedSizeOrTable(Kind kind) {
		for (int mutation = 0; mutation < 10; mutation++) {
			var map = kind.create();
			map.put(7, "old");
			final int operation = mutation;
			assertThrows(ConcurrentModificationException.class, () -> map.compute(7, (k, v) -> {
				switch (operation) {
				case 0 -> map.put(8, "inserted");
				case 1 -> { map.remove(7); map.put(8, "replacement"); }
				case 2 -> map.putIfAbsent(0, "zero");
				case 3 -> map.putAllZero();
				case 4 -> map.clear();
				case 5 -> map.clear(0);
				case 6 -> map.shrink(0);
				case 7 -> map.remove(7, "old");
				case 8 -> map.computeIfAbsent(0, ignored -> "zero");
				case 9 -> map.foreachUpdate((innerKey, old) -> null);
				default -> throw new AssertionError();
				}
				return "outer";
			}), "mutation " + mutation);
			assertNotEquals("outer", map.get(7));
		}
	}

	@ParameterizedTest
	@EnumSource(Kind.class)
	public void foreachUpdateRejectsReentrantLayoutBeforeWritingOrDeleting(Kind kind) {
		for (int key : new int[]{0, 7}) {
			var map = kind.create();
			map.put(key, "old");
			assertThrows(ConcurrentModificationException.class, () -> map.foreachUpdate((k, v) -> {
				map.ensureCapacity(256);
				return null;
			}));
			assertEquals("old", map.get(key));
			assertEquals(1, map.size());
		}
	}

	@ParameterizedTest
	@EnumSource(Kind.class)
	public void readOnlyRecursionAndExistingValueReplacementRemainValid(Kind kind) {
		var map = kind.create();
		map.put(7, "old");
		map.put(8, "other");
		assertEquals("outer", map.compute(7, (k, v) -> {
			assertEquals("old", map.get(7));
			map.put(8, "replaced");
			map.ensureCapacity(2);
			return "outer";
		}));
		assertEquals("replaced", map.get(8));
		map.foreachUpdate((k, v) -> null);
		assertEquals(0, map.size());
	}
}
