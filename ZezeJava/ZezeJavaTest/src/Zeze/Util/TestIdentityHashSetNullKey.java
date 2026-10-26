package Zeze.Util;

import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestIdentityHashSetNullKey {

	@SuppressWarnings("DataFlowIssue") // Null is not a representable set member.
	@Test
	public void rejectedNullInsertionKeepsTheSetUsable() {
		var set = new IdentityHashSet<Object>();
		assertThrows(NullPointerException.class, () -> set.add(null));
		assertTrue(set.isEmpty());
		var value = new Object();
		assertTrue(set.add(value));
		assertEquals(1, set.size());
		assertEquals(value, set.toArray()[0]);
		assertTrue(set.remove(value));
		assertTrue(set.isEmpty());
	}

	@Test
	public void nullQueriesDoNotMatchOrRemoveEmptyBuckets() {
		var set = new IdentityHashSet<Object>();
		assertFalse(set.contains(null));
		assertFalse(set.remove(null));
		assertEquals(0, set.size());
		var value = new Object();
		set.add(value);
		assertFalse(set.contains(null));
		assertFalse(set.remove(null));
		assertEquals(1, set.size());
		assertTrue(set.contains(value));
	}
}
