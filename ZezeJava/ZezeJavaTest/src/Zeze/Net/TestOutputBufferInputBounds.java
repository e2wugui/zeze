package Zeze.Net;

import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestOutputBufferInputBounds {
	@Test
	void rejectsInvalidSlicesBeforeAllocatingOrAppending() throws Exception {
		checkInvalidSlice(false);
		checkInvalidSlice(true);
	}

	private static void checkInvalidSlice(boolean buffered) throws Exception {
		var allocator = new TestOutputBuffer.Alloc(2);
		try (var output = new OutputBuffer(allocator)) {
			if (buffered)
				output.put((byte)7);
			int previousAllocations = allocator.allocCount;
			int previousSize = output.size();
			assertThrows(IndexOutOfBoundsException.class, () -> output.put(new byte[]{1, 2, 3}, 1, 3));
			assertEquals(previousSize, output.size());
			assertEquals(previousAllocations, allocator.allocCount);
			assertThrows(IndexOutOfBoundsException.class, () -> output.update(new byte[1], 0, -1));
			assertThrows(NullPointerException.class, () -> output.put(null, 0, 1));
			assertEquals(previousSize, output.size());
			output.put((byte)9);
			var channel = new TestOutputBuffer.Channel();
			channel.setRemaining(10);
			while (output.size() > 0)
				assertTrue(output.writeTo(channel) > 0);
			assertArrayEquals(buffered ? new byte[]{7, 9} : new byte[]{9}, channel.getSink().Copy());
		}
		assertEquals(0, allocator.getInUseCount());
	}

	@Test
	void emptyUpdatesKeepTheirExistingNoOpContract() {
		var allocator = new TestOutputBuffer.Alloc(2);
		try (var output = new OutputBuffer(allocator)) {
			output.put(null, 0, 0);
			output.update(null, Integer.MAX_VALUE, 0);
			assertEquals(0, output.size());
			assertEquals(0, allocator.allocCount);
		}
	}
}
