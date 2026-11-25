package Zeze.Net;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void blockSizeVarintFifthByteOverflowThrowsCodecException() {
        for (byte fifthByte : new byte[] {0x08, 0x10, 0x11, 0x7f, (byte)0x80}) {
            var decoded = new BufferCodec();
            try (var decoder = new DecompressMppcZstd(decoded, 1024, 1024)) {
                byte[] wire = new byte[] {
                        (byte)0xdf, (byte)0xff, // MPPC block-mode escape (0xc000 + 0x1fff)
                        (byte)0x80, (byte)0x80, (byte)0x80, (byte)0x80, fifthByte
                };
                assertThrows(CodecException.class, () -> {
                    for (byte b : wire)
                        decoder.update(b);
                }, "fifthByte=0x" + Integer.toHexString(fifthByte & 0xff));
            }
        }
    }

    @Test
    void zeroSrcBufSizeClampedToOneDoesNotThrow() {
        var data = payload();
        var wire = compress(data);
        var decoded = new BufferCodec();
        try (var decoder = new DecompressMppcZstd(decoded, 0, 1024)) {
            for (byte b : wire)
                decoder.update(b);
            decoder.flush();
            assertArrayEquals(data, decoded.Copy());
        }
    }

    @Test
    void flushBlockOutsideBlockModeAndRepeatedFlushBlockAreIdempotent() {
        var prefix = "prefix-mppc-data-prefix-mppc-data-".getBytes(StandardCharsets.UTF_8);
        var block = payload();
        var suffix = "-suffix-mppc-data-suffix-mppc-data".getBytes(StandardCharsets.UTF_8);
        var encoded = new BufferCodec();
        try (var compressor = new CompressMppcZstd(encoded, 8192, 3, 0)) {
            compressor.flushBlock(); // 非块态首调
            compressor.update(prefix, 0, prefix.length);
            compressor.flushBlock(); // 普通MPPC态中途调用不得注入原始0x00字节
            compressor.updateBlock(block, 0, block.length);
            compressor.flushBlock();
            compressor.flushBlock(); // 块态结束后重复调用须幂等
            compressor.update(suffix, 0, suffix.length);
            compressor.flushBlock();
            compressor.flush();
        }
        var decoded = new BufferCodec();
        try (var decoder = new DecompressMppcZstd(decoded, 1024, 1024)) {
            var wire = encoded.Copy();
            decoder.update(wire, 0, wire.length);
            decoder.flush();
        }
        var expected = new byte[prefix.length + block.length + suffix.length];
        System.arraycopy(prefix, 0, expected, 0, prefix.length);
        System.arraycopy(block, 0, expected, prefix.length, block.length);
        System.arraycopy(suffix, 0, expected, prefix.length + block.length, suffix.length);
        assertArrayEquals(expected, decoded.Copy());
    }
}
