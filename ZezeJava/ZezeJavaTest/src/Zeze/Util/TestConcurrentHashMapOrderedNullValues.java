package Zeze.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestConcurrentHashMapOrderedNullValues {

	@SuppressWarnings("DataFlowIssue") // Deliberately violate the non-null value contract.
	@Test
	public void nullInsertionsLeaveNoMapOrQueueEntry() {
		List<Consumer<ConcurrentHashMapOrdered<String, String>>> insertions = List.of(
				map -> map.put("new", null),
				map -> map.putIfAbsent("new", null));
		for (var insertion : insertions) {
			var map = new ConcurrentHashMapOrdered<String, String>();
			assertThrows(NullPointerException.class, () -> insertion.accept(map));
			assertEquals(0, map.size());
			assertTrue(map.isEmpty());
			assertFalse(map.iterator().hasNext());
			map.put("new", "value");
			assertOrderedValue(map);
		}
	}

	@SuppressWarnings("DataFlowIssue") // Invalid replacements must preserve the existing entry.
	@Test
	public void nullReplacementsKeepLookupAndIterationConsistent() {
		List<Consumer<ConcurrentHashMapOrdered<String, String>>> replacements = List.of(
				map -> map.put("new", null),
				map -> map.putIfAbsent("new", null),
				map -> map.replace("new", null),
				map -> map.replace("new", "value", null),
				map -> map.replace("missing", null),
				map -> map.replace("new", "different", null));
		for (var replacement : replacements) {
			var map = new ConcurrentHashMapOrdered<String, String>();
			map.put("new", "value");
			assertThrows(NullPointerException.class, () -> replacement.accept(map));
			assertOrderedValue(map);
		}
	}

	private static void assertOrderedValue(ConcurrentHashMapOrdered<String, String> map) {
		assertEquals(1, map.size());
		assertEquals("value", map.get("new"));
		var values = new ArrayList<String>();
		map.forEach(values::add);
		assertEquals(List.of("value"), values);
	}
}
