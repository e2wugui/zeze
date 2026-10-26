package Zeze.Serialize;

import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestNioByteBufferSliceState {

	@Test
	public void absoluteCopiesPreservePositionAndMark() {
		for (var source : sources()) {
			var buffer = NioByteBuffer.Wrap(source);
			assertArrayEquals(new byte[]{1, 2}, buffer.getBytes(0, 2));
			assertEquals(5, source.position());
			assertEquals(4, source.reset().position());
		}
	}

	@Test
	public void failedCopiesPreservePositionAndMark() {
		for (var source : sources()) {
			var buffer = NioByteBuffer.Wrap(source);
			assertThrows(RuntimeException.class, () -> buffer.getBytes(2, 10));
			assertEquals(5, source.position());
			assertEquals(4, source.reset().position());
		}
	}

	private static java.nio.ByteBuffer[] sources() {
		var heap = java.nio.ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
		var direct = java.nio.ByteBuffer.allocateDirect(8).put(heap.duplicate()).flip();
		var readOnly = heap.asReadOnlyBuffer();
		var sources = new java.nio.ByteBuffer[]{heap, direct, readOnly};
		for (var source : sources)
			source.position(4).mark().position(5);
		return sources;
	}
}
