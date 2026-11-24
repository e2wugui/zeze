package Zeze.Net;

import java.util.Arrays;
import java.util.Random;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@Fast
public class TestMppcZstdSegmentedFlush {
	@Test
	void flushFinishesTupleBeforeEnteringBlockMode() {
		byte[] prefix = {2, 3, 2, 1, 0, 2, 0, 2, 0};
		var block = new byte[140];
		new Random(240).nextBytes(block);
		var expected = Arrays.copyOf(prefix, prefix.length + block.length);
		System.arraycopy(block, 0, expected, prefix.length, block.length);

		var encoded = new BufferCodec();
		try (var compressor = new CompressMppcZstd(encoded, 8192, 3, 0)) {
			compressor.update(prefix);
			compressor.updateBlock(block, 0, block.length);
			compressor.flush();
		}

		var wire = encoded.Copy();
		for (int chunkSize = 1; chunkSize <= 8; chunkSize++) {
			var decoded = new BufferCodec();
			try (var decoder = new DecompressMppcZstd(decoded, 1024, 1024)) {
				for (int offset = 0; offset < wire.length; offset += chunkSize) {
					decoder.update(wire, offset, Math.min(chunkSize, wire.length - offset));
					decoder.flush();
				}
			}
			assertArrayEquals(expected, decoded.Copy(), "chunkSize=" + chunkSize);
		}
	}
}
