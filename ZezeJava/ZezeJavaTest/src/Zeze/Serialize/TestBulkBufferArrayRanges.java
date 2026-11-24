package Zeze.Serialize;

import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestBulkBufferArrayRanges {
	@Test
	public void invalidWriteRangeLeavesBufferUnchanged() {
		var output = ByteBuffer.Allocate();
		output.WriteByte(42);
		assertThrows(IndexOutOfBoundsException.class, () -> output.WriteInt4s(new int[]{1}, 0, 2));
		assertThrows(IndexOutOfBoundsException.class, () -> output.WriteLong8s(new long[]{1}, 0, 2));
		assertThrows(IndexOutOfBoundsException.class, () -> output.WriteFloats(new float[]{1}, 0, 2));
		assertThrows(IndexOutOfBoundsException.class, () -> output.WriteDoubles(new double[]{1}, 0, 2));
		assertArrayEquals(new byte[]{42}, output.Copy());
	}

	@Test
	public void invalidReadRangeLeavesCursorAndDestinationUnchanged() {
		var bytes = new byte[16];
		for (IByteBuffer input : new IByteBuffer[]{ByteBuffer.Wrap(bytes), NioByteBuffer.Wrap(bytes)}) {
			var ints = new int[]{7};
			var longs = new long[]{7};
			var floats = new float[]{7};
			var doubles = new double[]{7};
			assertThrows(IndexOutOfBoundsException.class, () -> input.ReadInt4s(ints, 0, 2));
			assertThrows(IndexOutOfBoundsException.class, () -> input.ReadLong8s(longs, 0, 2));
			assertThrows(IndexOutOfBoundsException.class, () -> input.ReadFloats(floats, 0, 2));
			assertThrows(IndexOutOfBoundsException.class, () -> input.ReadDoubles(doubles, 0, 2));
			assertEquals(0, input.getReadIndex());
			assertEquals(7, ints[0]);
			assertEquals(7L, longs[0]);
			assertEquals(7f, floats[0]);
			assertEquals(7d, doubles[0]);
		}
	}
}
