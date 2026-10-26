package Zeze.Util;

import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestLockRegistryAllocation {
	private static final class Key implements Lockey<Key> {
		final int id;
		final boolean allocated;
		final boolean fail;

		Key(int id, boolean allocated, boolean fail) {
			this.id = id;
			this.allocated = allocated;
			this.fail = fail;
		}

		@Override
		public Key alloc() {
			if (fail)
				throw new IllegalStateException("allocation failed");
			return new Key(id, true, false);
		}

		@Override
		public int compareTo(Key other) {
			return Integer.compare(id, other.id);
		}

		@Override
		public int hashCode() {
			return id;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return other instanceof Key key && id == key.id;
		}
	}

	@Test
	public void allocatedObjectBecomesTheCanonicalRegistryValue() {
		var registry = new Locks<Key>(1);
		var allocated = registry.get(new Key(1, false, false));
		assertTrue(allocated.allocated);
		assertSame(allocated, registry.get(new Key(1, false, false)));
		assertSame(allocated, registry.get(allocated));
	}

	@Test
	public void failedAllocationDoesNotRegisterTheProbeKey() {
		var registry = new Locks<Key>(1);
		var failing = new Key(2, false, true);
		assertThrows(IllegalStateException.class, () -> registry.get(failing));
		assertFalse(registry.contains(failing));
		assertTrue(registry.get(new Key(2, false, false)).allocated);
	}
}
