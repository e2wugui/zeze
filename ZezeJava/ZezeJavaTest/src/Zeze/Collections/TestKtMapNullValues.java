package Zeze.Collections;

import java.util.Collections;
import java.util.List;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.pcollections.Empty;
import static org.junit.jupiter.api.Assertions.*;

@Fast
@Isolated // Temporarily selects the shared kt/ktt bulk operation strategy.
public class TestKtMapNullValues {
	@Test
	void aNullValueStillCreatesAPresentMapping() {
		var reference = Empty.<String, @Nullable String>map().plus("key", null);
		assertTrue(reference.containsKey("key"), "pcollections supports a present null value");
		var map = KtMap.<String, @Nullable String>empty().plus("key", null);
		assertEquals(reference, map);
		assertTrue(map.containsKey("key"));
		assertEquals(1, map.size());
		assertSame(map, map.plus("key", null));
	}

	@Test
	void bulkInsertionDistinguishesNullFromAnAbsentKey() {
		boolean original = Persistents.BULK;
		try {
			for (boolean bulk : new boolean[]{false, true}) {
				Persistents.BULK = bulk;
				var values = Collections.<String, @Nullable String>singletonMap("key", null);
				var map = KtMap.<String, @Nullable String>empty().plusAll(values);
				assertEquals(Empty.<String, @Nullable String>map().plusAll(values), map, "bulk=" + bulk);
				assertTrue(map.containsKey("key"));
				assertSame(map, map.plusAll(values));
			}
		} finally {
			Persistents.BULK = original;
		}
	}

	@Test
	void bulkRemovalDetectsAChangedMappingWhoseOldValueIsNull() {
		boolean original = Persistents.BULK;
		try {
			for (boolean bulk : new boolean[]{false, true}) {
				Persistents.BULK = bulk;
				var map = KtMap.<String, @Nullable String>empty().plus("key", "value").plus("key", null);
				assertTrue(map.containsKey("key"));
				assertSame(map, map.minusAll(List.of("missing")));
				assertTrue(map.minusAll(List.of("key", "key")).isEmpty(), "bulk=" + bulk);
				assertTrue(map.containsKey("key"), "the original map remains immutable");
			}
		} finally {
			Persistents.BULK = original;
		}
	}
}
