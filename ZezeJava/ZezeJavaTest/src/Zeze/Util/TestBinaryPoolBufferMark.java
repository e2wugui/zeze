package Zeze.Util;

import java.nio.ByteBuffer;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@Fast
public class TestBinaryPoolBufferMark {
	@Test
	public void internPreservesCallerMarkBeforeAndAfterInsertion() {
		var pool = new BinaryPool();
		var bytes = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
		var buffer = ByteBuffer.wrap(bytes).asReadOnlyBuffer();
		buffer.position(4).mark().position(6);

		var inserted = pool.intern(buffer, 1, 14);
		assertEquals(6, buffer.position());
		assertEquals(4, buffer.reset().position());
		assertSame(inserted, pool.intern(bytes, 1, 14));
		assertSame(inserted, pool.intern(buffer, 1, 14));
		assertEquals(4, buffer.reset().position());
	}
}
