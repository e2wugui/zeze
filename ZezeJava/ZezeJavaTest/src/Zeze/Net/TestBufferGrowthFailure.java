package Zeze.Net;

import java.nio.ByteBuffer;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestBufferGrowthFailure {
	@Test
	void rejectedCapacityKeepsTheOriginalWriteStateAndMark() {
		var buffer = ByteBuffer.allocate(16);
		buffer.putInt(0x12345678).mark().putInt(0x23456789);
		var original = buffer.array().clone();
		assertThrows(IllegalArgumentException.class, () -> Helper.realloc(buffer, Helper.MAX_BUFFER_SIZE));
		assertEquals(8, buffer.position());
		assertEquals(16, buffer.limit());
		assertArrayEquals(original, buffer.array());
		buffer.reset();
		assertEquals(4, buffer.position());
	}

	@Test
	void successfulGrowthKeepsWrittenBytesAndTheNoGrowthPathReusesTheBuffer() {
		var buffer = ByteBuffer.allocate(16);
		buffer.putLong(0x123456789abcdef0L);
		assertSame(buffer, Helper.realloc(buffer, 8));
		var grown = Helper.realloc(buffer, 16);
		assertEquals(8, grown.position());
		assertEquals(0x123456789abcdef0L, grown.getLong(0));
		assertEquals(24, grown.remaining());
		grown.putLong(0x23456789abcdef01L);
		assertEquals(0x23456789abcdef01L, grown.getLong(8));
	}
}
