package Zeze.Net;

import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestCodecInputStreamReads {
	@Test
	void zeroLengthReadReturnsZeroAtEndOfInput() {
		var input = new DecompressMppcZstd.CodecInputStream();
		assertEquals(0, input.read(new byte[1], 1, 0));
		assertEquals(-1, input.read(new byte[1], 0, 1));
	}

	@Test
	void validatesTheWholeRequestBeforeClampingToAvailableBytes() {
		var input = new DecompressMppcZstd.CodecInputStream();
		var bytes = new byte[2];
		assertThrows(IndexOutOfBoundsException.class, () -> input.read(bytes, 0, -1));
		assertThrows(IndexOutOfBoundsException.class, () -> input.read(bytes, -1, 0));
		assertThrows(NullPointerException.class, () -> input.read(null, 0, 0));
		input.write(9);
		assertThrows(IndexOutOfBoundsException.class, () -> input.read(bytes, 0, 3));
		assertEquals(1, input.available(), "拒绝非法读取不能消费缓冲前缀");
		assertEquals(1, input.read(bytes, 1, 1));
		assertEquals(9, bytes[1]);
		assertEquals(-1, input.read());
	}
}
