package Zeze.Util;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

@Fast
@Timeout(10)
public class TestFewModifyBulkBoundaries {
	private static Collection<?> throwingMembership(boolean containsFirst) {
		return new AbstractCollection<>() {
			@Override
			public Iterator<Object> iterator() {
				return Collections.emptyIterator();
			}
			@Override
			public int size() {
				return 0;
			}
			@Override
			public boolean contains(Object item) {
				if (Integer.valueOf(2).equals(item))
					throw new IllegalStateException("membership failed after the first removal");
				return containsFirst;
			}
		};
	}

	@Test
	public void listPartialBulkFailurePublishesTheUpdatedSnapshot() {
		var removed = new FewModifyList<Integer>(List.of(1, 2, 3));
		assertEquals(List.of(1, 2, 3), removed.snapshot());
		assertThrows(IllegalStateException.class, () -> removed.removeAll(throwingMembership(true)));
		assertEquals(List.of(2, 3), removed.snapshot());
		var retained = new FewModifyList<Integer>(List.of(1, 2, 3));
		assertEquals(List.of(1, 2, 3), retained.snapshot());
		assertThrows(IllegalStateException.class, () -> retained.retainAll(throwingMembership(false)));
		assertEquals(List.of(2, 3), retained.snapshot());
	}

	@Test
	public void sortedMapPartialPutAllFailureInvalidatesTheOldSnapshot() {
		var target = new FewModifySortedMap<Integer, Integer>((a, b) -> {
			if (a == 3 || b == 3)
				throw new IllegalStateException("comparison failed");
			return Integer.compare(a, b);
		});
		target.put(1, 1);
		assertEquals(Map.of(1, 1), target.snapshot());
		var source = new LinkedHashMap<Integer, Integer>();
		source.put(2, 2);
		source.put(3, 3);
		assertThrows(IllegalStateException.class, () -> target.putAll(source));
		assertEquals(Map.of(1, 1, 2, 2), target.snapshot());
	}

	@Test
	public void mapReadsTheForeignInputBeforeTakingItsWriteLock() throws Exception {
		for (Map<Integer, Integer> target : List.<Map<Integer, Integer>>of(
				new FewModifyMap<>(), new FewModifySortedMap<>())) {
			target.put(1, 1); // read snapshot remains absent, so a different reader must acquire the lock.
			try (var reader = Executors.newSingleThreadExecutor()) {
				var source = new AbstractMap<Integer, Integer>() {
					@Override
					public boolean isEmpty() {
						return false;
					}
					@Override
					public int size() {
						return 2;
					}
					@Override
					public Set<Entry<Integer, Integer>> entrySet() {
						try {
							assertEquals(1, reader.submit(target::size).get(2, TimeUnit.SECONDS));
						} catch (Exception e) {
							throw new IllegalStateException("foreign input was read while blocking another reader", e);
						}
						return Map.of(2, 2, 3, 3).entrySet();
					}
				};
				target.putAll(source);
				assertEquals(Map.of(1, 1, 2, 2, 3, 3), target);
			}
		}
	}

	@Test
	public void listRemoveIfAndSequencedRemovalsWorkAtomically() throws Exception {
		var list = new FewModifyList<Integer>(List.of(1, 2, 3, 4, 5));
		assertEquals(List.of(1, 2, 3, 4, 5), list.snapshot());
		assertTrue(list.removeIf(v -> v % 2 == 0));
		assertEquals(List.of(1, 3, 5), list.snapshot());
		assertFalse(list.removeIf(v -> v > 10));

		// 谓词中途抛异常后快照不陈旧
		assertThrows(IllegalStateException.class, () -> list.removeIf(v -> {
			if (v == 3)
				throw new IllegalStateException("predicate boom");
			return v == 1;
		}));
		assertEquals(List.of(1, 3, 5), list.snapshot());

		assertEquals(1, list.removeFirst());
		assertEquals(5, list.removeLast());
		assertEquals(3, list.removeLast());
		assertThrows(NoSuchElementException.class, list::removeFirst);
		assertThrows(NoSuchElementException.class, list::removeLast);

		// 并发 add/removeLast 仅抛 NoSuchElementException，不抛 IndexOutOfBoundsException
		var concurrentList = new FewModifyList<Integer>();
		var stop = new AtomicBoolean(false);
		var unexpected = new AtomicReference<@Nullable Throwable>();
		try (var pool = Executors.newFixedThreadPool(2)) {
			var writer = pool.submit(() -> {
				for (int i = 0; i < 4000 && !stop.get(); i++)
					concurrentList.add(i);
				stop.set(true);
			});
			var remover = pool.submit(() -> {
				while (!stop.get()) {
					try {
						concurrentList.removeLast();
					} catch (NoSuchElementException ignored) {
						// empty race is valid
					} catch (Throwable t) {
						unexpected.compareAndSet(null, t);
						stop.set(true);
					}
				}
			});
			writer.get(5, TimeUnit.SECONDS);
			remover.get(5, TimeUnit.SECONDS);
		}
		assertNull(unexpected.get(), () -> "unexpected exception: " + unexpected.get());
	}

	@Test
	public void mapGetOrDefaultReadsSingleConsistentSnapshotUnderConcurrentPut() throws Exception {
		for (Map<String, String> map : List.<Map<String, String>>of(
				new FewModifyMap<>(), new FewModifySortedMap<>())) {
			var stop = new AtomicBoolean(false);
			var torn = new AtomicReference<@Nullable String>();
			try (var pool = Executors.newFixedThreadPool(2)) {
				var writer = pool.submit(() -> {
					for (int i = 0; i < 5000 && !stop.get(); i++) {
						map.put("k", "val");
						map.remove("k");
					}
					stop.set(true);
				});
				var reader = pool.submit(() -> {
					while (!stop.get()) {
						String v = map.getOrDefault("k", "default");
						if (!"val".equals(v) && !"default".equals(v)) {
							torn.compareAndSet(null, String.valueOf(v));
							stop.set(true);
						}
					}
				});
				writer.get(5, TimeUnit.SECONDS);
				reader.get(5, TimeUnit.SECONDS);
			}
			assertNull(torn.get(), () -> "getOrDefault observed torn value: " + torn.get());
		}
	}
}
