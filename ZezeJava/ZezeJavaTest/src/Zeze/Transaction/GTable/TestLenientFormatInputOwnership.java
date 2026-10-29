package Zeze.Transaction.GTable;

import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@Fast
public class TestLenientFormatInputOwnership {

	@Test
	public void formatsCovariantArrayWithoutChangingItsContents() {
		Integer[] arguments = {12, 34};

		assertEquals("12 + 34", Utils.lenientFormat("%s + %s", (Object[])arguments));
		assertArrayEquals(new Integer[]{12, 34}, arguments);
	}

	@Test
	public void preservesCallerObjectsAndNulls() {
		var value = new StringBuilder("value");
		Object[] arguments = {value, null};

		assertEquals("value [null]", Utils.lenientFormat("%s", arguments));
		assertSame(value, arguments[0]);
		assertNull(arguments[1]);
	}

	@Test
	public void retainsNullVarargsFormatting() {
		assertEquals("(Object[])null", Utils.lenientFormat("%s", (Object[])null));
	}
}
