package Zeze.Util;

import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestStableRandomFiniteBounds {
	@Test
	public void finiteBoundsAndHistoricalSequences() {
		var actual = new StableRandom(1);
		var reference = new StableRandom(1);
		var actual2 = new StableRandom2(2, 512, 4096, 131072, 8388608);
		var reference2 = new StableRandom2(2, 512, 4096, 131072, 8388608);
		for (int i = 0; i < 1000; i++) {
			float maxFloat = i % 2 == 0 ? Float.MAX_VALUE : 3.5f;
			double maxDouble = i % 2 == 0 ? Double.MAX_VALUE : 3.5;
			checkFloat(reference.nextInt(), maxFloat, actual.nextFloat(maxFloat));
			checkDouble(reference.nextLong(), maxDouble, actual.nextDouble(maxDouble));
			checkFloat(reference2.nextInt(), maxFloat, actual2.nextFloat(maxFloat));
			checkDouble(reference2.nextLong(), maxDouble, actual2.nextDouble(maxDouble));
			assertEquals(reference.next(), actual.next());
			assertEquals(reference2.next(), actual2.next());
		}
	}

	private static void checkFloat(int random, float max, float actual) {
		assertTrue(Float.isFinite(actual) && actual >= 0 && actual <= max);
		float historical = random * max * (1f / Integer.MAX_VALUE);
		if (Float.isFinite(historical))
			assertEquals(Float.floatToRawIntBits(historical), Float.floatToRawIntBits(actual));
	}

	private static void checkDouble(long random, double max, double actual) {
		assertTrue(Double.isFinite(actual) && actual >= 0 && actual <= max);
		double historical = random * max * (1.0 / Long.MAX_VALUE);
		if (Double.isFinite(historical))
			assertEquals(Double.doubleToRawLongBits(historical), Double.doubleToRawLongBits(actual));
	}
}
