package Zeze.Net;

import java.io.Closeable;
import Zeze.Util.ZstdFactory;
import Zeze.Util.ZstdFactory.ZstdCompressStream;
import org.jetbrains.annotations.NotNull;

public final class CompressMppcZstd extends Compress implements Closeable {
	public static final class SinkWrapper implements Codec {
		private final @NotNull Codec sink;

		public SinkWrapper(@NotNull Codec sink) {
			this.sink = sink;
		}

		@Override
		public void update(byte b) {
			sink.update((byte)1);
			sink.update(b);
		}

		@Override
		public void update(byte @NotNull [] b, int off, int len) {
			if (len <= 0)
				return; // 0是flush标记（见flush），不能作为长度前缀写出
			int v = len; // varint编码用副本，写数据必须用原始len
			while (v >= 0x80) {
				sink.update((byte)(v | 0x80));
				v >>= 7;
			}
			sink.update((byte)v);
			sink.update(b, off, len);
		}

		@Override
		public void flush() {
			sink.update((byte)0);
			sink.flush();
		}
	}

	private final @NotNull SinkWrapper sinkWrapper;
	private final @NotNull ZstdCompressStream cs;
	private boolean blockMode;

	public CompressMppcZstd(@NotNull Codec sink, int dstBufSize, int compressLevel, int windowLog) {
		super(sink);
		sinkWrapper = new SinkWrapper(sink);
		cs = ZstdFactory.newCompressStream(dstBufSize, compressLevel, windowLog);
	}

	@Override
	public void update(byte c) throws CodecException {
		if (blockMode)
			flushBlock();
		super.update(c);
	}

	@Override
	public void update(byte @NotNull [] data, int off, int len) throws CodecException {
		if (blockMode)
			flushBlock();
		super.update(data, off, len);
	}

	@Override
	public void flush() throws CodecException {
		if (blockMode)
			flushBlock();
		else
			super.flush();
	}

	public void updateBlock(byte @NotNull [] data, int off, int len) {
		if (!blockMode) {
			blockMode = true;
			putBits(0xc000 + 0x1fff, 16); // block mode
			int pos = getPos();
			if (pos > 0)
				putBits(0, 8 - pos); // byte align
		}
		cs.compress(data, off, off + len, sinkWrapper);
	}

	public void flushBlock() {
		blockMode = false;
		cs.flush(sinkWrapper);
	}

	@Override
	public void close() {
		cs.close();
		super.close();
	}
}
