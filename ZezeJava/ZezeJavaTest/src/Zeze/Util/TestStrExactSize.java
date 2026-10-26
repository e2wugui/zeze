package Zeze.Util;

import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestStrExactSize {

	@Test
	public void decimalSizesKeepIntegerPrecisionBeforeTruncation() {
		assertEquals(9_007_199_254_740_993L, Str.parseLongSize("9007199254740993.0"));
		assertEquals(9_007_199_254_740_993L, Str.parseLongSize("8796093022208.0009765625K"));
		assertEquals(Long.MAX_VALUE - 1, Str.parseLongSize("9223372036854775806.9"));
		assertEquals(1, Str.parseLongSize("1.9999999999999999999999"));
		assertEquals(Long.MAX_VALUE, Str.parseLongSize("9223372036854775807.0"));
	}

	@Test
	public void sizesOutsideLongRangeAreRejectedBeforeConversion() {
		for (var size : new String[]{"9223372036854775808", "9223372036854775807.1", "8E"})
			assertThrows(IllegalStateException.class, () -> Str.parseLongSize(size), size);
	}

	@Test
	public void theWholeNumericPartMustBeValid() {
		for (var size : new String[]{"1..5", "0.5.1", "1.2.3K", "."})
			assertThrows(NumberFormatException.class, () -> Str.parseLongSize(size), size);
	}
}
