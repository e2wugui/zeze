package Zeze.Net;

import java.io.Closeable;
import java.io.InputStream;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.ZstdFactory;
import Zeze.Util.ZstdFactory.ZstdDecompressStream;
import org.jetbrains.annotations.NotNull;

public final class DecompressMppcZstd extends Decompress implements Closeable {
	public static final class CodecInputStream extends InputStream {
		private final @NotNull ByteBuffer buffer = ByteBuffer.Allocate(0);

		@Override
		public int available() {
			return buffer.size();
		}

		@Override
		public int read() {
			return buffer.isEmpty() ? -1 : buffer.ReadByte() & 0xff;
		}

		@Override
		public int read(byte @NotNull [] b, int off, int len) {
			int size = buffer.size();
			if (size <= 0)
				return -1;
			if (len > size)
				len = size;
			System.arraycopy(buffer.Bytes, buffer.ReadIndex, b, off, len);
			buffer.ReadIndex += len;
			return len;
		}

		private void shrink() {
			int size = buffer.size();
			System.arraycopy(buffer.Bytes, buffer.ReadIndex, buffer.Bytes, 0, size);
			buffer.ReadIndex = 0;
			buffer.WriteIndex = size;
		}

		public void write(int b) {
			if (buffer.ReadIndex >= 0x8000)
				shrink();
			buffer.WriteByte(b);
		}

		public void write(byte @NotNull [] b, int off, int len) {
			if (buffer.ReadIndex >= 0x8000)
				shrink();
			buffer.Append(b, off, len);
		}
	}

	private final @NotNull ZstdDecompressStream ds;
	private final byte @NotNull [] srcBuf;
	private int srcBufLen;
	private int blockState = -1; // -1:no block; -2:read block; >=0:read blockSize
	private int blockSize;

	public DecompressMppcZstd(@NotNull Codec sink, int srcBufSize, int dstBufSize) {
		super(sink);
		ds = ZstdFactory.newDecompressStream(dstBufSize);
		srcBuf = new byte[srcBufSize];
	}

	@Override
	public void update(byte c) throws CodecException {
		if (blockState == -1) {
			super.update(c);
			if (off == 0x1fff + 320) { // enter block mode
				off = -1;
				int r = rem;
				int p = pos;
				rem = 0;
				pos = 0;
				blockState = 0;
				for (p &= ~7; (p -= 8) >= 0; )
					update((byte)(r >> p));
			}
		} else if (blockState == -2) {
			srcBuf[srcBufLen++] = c;
			if (srcBufLen == srcBuf.length) {
				ds.decompress(srcBuf, 0, srcBufLen, sink);
				srcBufLen = 0;
			}
			if (--blockSize == 0)
				blockState = 0;
		} else {
			if (c < 0) {
				blockSize += (c & 0x7f) << blockState;
				blockState += 7;
				if (blockState >= 31)
					throw new CodecException("blockSize overflow");
			} else {
				blockSize += c << blockState;
				blockState = blockSize == 0 ? -1 : -2;
			}
			if (blockSize < 0)
				throw new CodecException("blockSize overflow");
		}
	}

	@Override
	public void update(byte @NotNull [] data, int pos, int len) throws CodecException {
		for (int end = pos + len; pos < end; ) {
			if (blockState == -1) {
				super.update(data[pos++]);
				if (off == 0x1fff + 320) { // enter block mode
					off = -1;
					int r = rem;
					int p = pos;
					rem = 0;
					pos = 0;
					blockState = 0;
					for (p &= ~7; (p -= 8) >= 0; )
						update((byte)(r >> p));
					if (srcBufLen > 0) {
						ds.decompress(srcBuf, 0, srcBufLen, sink);
						srcBufLen = 0;
					}
				}
			} else if (blockState == -2) {
				len = Math.min(blockSize, end - pos);
				if ((blockSize -= len) == 0)
					blockState = 0;
				ds.decompress(data, pos, pos + len, sink);
				pos += len;
			} else if (blockState >= 0) {
				int c = data[pos++];
				if (c < 0) {
					blockSize += (c & 0x7f) << blockState;
					blockState += 7;
					if (blockState >= 31)
						throw new CodecException("blockSize overflow");
				} else {
					blockSize += c << blockState;
					blockState = blockSize == 0 ? -1 : -2;
				}
				if (blockSize < 0)
					throw new CodecException("blockSize overflow");
			}
		}
	}

	@Override
	public void flush() throws CodecException {
		if (blockState == -1)
			super.flush();
		else {
			if (srcBufLen > 0) {
				ds.decompress(srcBuf, 0, srcBufLen, sink);
				srcBufLen = 0;
			}
			sink.flush();
		}
	}

	@Override
	public void close() {
		ds.close();
	}
}
