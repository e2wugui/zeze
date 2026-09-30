package Zeze.Util;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import harness.Fast;
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
}
