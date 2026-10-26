package Zeze.Util;

import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestHashLoadFactor {

	@Test
	public void undefinedLoadFactorCannotCreateAnAlwaysGrowingTable() {
		assertAll(
				() -> assertThrows(IllegalArgumentException.class, () -> new IntHashSet(0, Float.NaN)),
				() -> assertThrows(IllegalArgumentException.class, () -> new LongHashSet(0, Float.NaN)),
				() -> assertThrows(IllegalArgumentException.class, () -> new IdentityHashSet<>(0, Float.NaN)),
				() -> assertThrows(IllegalArgumentException.class, () -> new CharHashMap<>(0, Float.NaN)),
				() -> assertThrows(IllegalArgumentException.class, () -> new IntHashMap<>(0, Float.NaN)),
				() -> assertThrows(IllegalArgumentException.class, () -> new LongHashMap<>(0, Float.NaN)));
	}
}
