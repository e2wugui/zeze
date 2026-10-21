package Zeze.Net;

import java.util.Random;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@Fast
public class TestMppcZstdMixedUpdates {
    private static byte[] payload() {
        var data = new byte[4096];
        new Random(42).nextBytes(data);
        return data;
    }

    private static byte[] compress(byte[] data) {
        var encoded = new BufferCodec();
        try (var compressor = new CompressMppcZstd(encoded, 8192, 3, 0)) {
            compressor.updateBlock(data, 0, data.length);
            compressor.flush();
        }
        return encoded.Copy();
    }

    @Test
    void singleBytePrefixThenArraySuffixPreservesOrder() {
        var data = payload();
        var wire = compress(data);
        for (int split = 1; split < 32; split++) {
            var decoded = new BufferCodec();
            try (var decoder = new DecompressMppcZstd(decoded, 1024, 1024)) {
                for (int i = 0; i < split; i++)
                    decoder.update(wire[i]);
                decoder.update(wire, split, wire.length - split);
                decoder.flush();
                assertArrayEquals(data, decoded.Copy(), "split=" + split);
            }
        }
    }

    @Test
    void alternatingSingleByteAndArrayChunksPreservesOrder() {
        var data = payload();
        var wire = compress(data);
        var decoded = new BufferCodec();
        try (var decoder = new DecompressMppcZstd(decoded, 1024, 1024)) {
            for (int i = 0; i < wire.length; ) {
                decoder.update(wire[i++]);
                int size = Math.min(13, wire.length - i);
                decoder.update(wire, i, size);
                i += size;
            }
            decoder.flush();
            assertArrayEquals(data, decoded.Copy());
        }
    }

    @Test
    void uniformInputModesRemainEquivalent() {
        var data = payload();
        var wire = compress(data);
        for (boolean single : new boolean[] {false, true}) {
            var decoded = new BufferCodec();
            try (var decoder = new DecompressMppcZstd(decoded, 1024, 1024)) {
                if (single) {
                    for (byte b : wire)
                        decoder.update(b);
                } else
                    decoder.update(wire, 0, wire.length);
                decoder.flush();
                assertArrayEquals(data, decoded.Copy());
            }
        }
    }
}
