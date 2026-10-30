package Zeze.Collections;

import java.util.Collections;
import java.util.List;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.pcollections.Empty;
import org.pcollections.PMap;
import static org.junit.jupiter.api.Assertions.*;

@Fast
@Isolated // Temporarily selects the shared kt/ktt bulk operation strategy.
public class TestKtMapNullValues {
	private static List<PMap<String, @Nullable String>> emptyMaps() {
		return List.of(KtMap.empty(), MyKtMap.empty());
	}

	@ParameterizedTest
	@MethodSource("emptyMaps")
	void aNullValueStillCreatesAPresentMapping(PMap<String, @Nullable String> empty) {
		var reference = Empty.<String, @Nullable String>map().plus("key", null);
		assertTrue(reference.containsKey("key"), "pcollections supports a present null value");
		var map = empty.plus("key", null);
		assertEquals(reference, map);
		assertTrue(map.containsKey("key"));
		assertEquals(1, map.size());
		assertSame(map, map.plus("key", null));
	}

	@ParameterizedTest
	@MethodSource("emptyMaps")
	void bulkInsertionDistinguishesNullFromAnAbsentKey(PMap<String, @Nullable String> empty) {
		boolean original = Persistents.BULK;
		try {
			for (boolean bulk : new boolean[]{false, true}) {
				Persistents.BULK = bulk;
				var values = Collections.<String, @Nullable String>singletonMap("key", null);
				var map = empty.plusAll(values);
				assertEquals(Empty.<String, @Nullable String>map().plusAll(values), map, "bulk=" + bulk);
				assertTrue(map.containsKey("key"));
				assertSame(map, map.plusAll(values));
			}
		} finally {
			Persistents.BULK = original;
		}
	}

	@ParameterizedTest
	@MethodSource("emptyMaps")
	void bulkRemovalDetectsAChangedMappingWhoseOldValueIsNull(PMap<String, @Nullable String> empty) {
		boolean original = Persistents.BULK;
		try {
			for (boolean bulk : new boolean[]{false, true}) {
				Persistents.BULK = bulk;
				var map = empty.plus("key", "value").plus("key", null);
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
