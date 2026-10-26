package Zeze.Net;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Fast
public class TestBinarySliceBounds {
	@Test
	public void indexingCannotReadBeforeOrAfterTheSlice() {
		var binary = new Binary(new byte[]{10, 20, 30, 40}, 1, 2);
		Assertions.assertEquals(20, binary.get(0));
		Assertions.assertEquals(30, binary.get(1));
		Assertions.assertAll(
				() -> Assertions.assertThrows(IndexOutOfBoundsException.class, () -> binary.get(-1)),
				() -> Assertions.assertThrows(IndexOutOfBoundsException.class, () -> binary.get(2)),
				() -> Assertions.assertThrows(IndexOutOfBoundsException.class, () -> binary.get(Integer.MAX_VALUE)));
	}

	@Test
	public void emptyAndPrefixViewsCannotExposeBackingBytes() {
		var bytes = new byte[]{10, 20, 30};
		var empty = new Binary(bytes, 1, 0);
		var prefix = new Binary(bytes, 1);
		Assertions.assertAll(
				() -> Assertions.assertThrows(IndexOutOfBoundsException.class, () -> empty.get(0)),
				() -> Assertions.assertThrows(IndexOutOfBoundsException.class, () -> empty.get(-1)),
				() -> Assertions.assertThrows(IndexOutOfBoundsException.class, () -> prefix.get(1)));
	}
}
