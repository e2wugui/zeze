package Zeze.Net;

import java.io.Closeable;
import java.io.InputStream;
import java.util.Objects;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.ZstdFactory;
import Zeze.Util.ZstdFactory.ZstdDecompressStream;

/**
 * MPPC 与 zstd 混合解压 codec：解码 MPPC 流并展开其中的 zstd 压缩块。
 */
public final class DecompressMppcZstd extends Decompress implements Closeable {
	public static final class CodecInputStream extends InputStream {
		private final ByteBuffer buffer = ByteBuffer.Allocate(0);

		@Override
		public int available() {
			return buffer.size();
		}

		@Override
		public int read() {
			return buffer.isEmpty() ? -1 : buffer.ReadByte() & 0xff;
		}

		@Override
		public int read(byte [] b, int off, int len) {
			Objects.checkFromIndexSize(off, len, b.length);
			if (len == 0)
				return 0;
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

		public void write(byte [] b, int off, int len) {
			if (buffer.ReadIndex >= 0x8000)
				shrink();
			buffer.Append(b, off, len);
		}
	}

	private final ZstdDecompressStream ds;
	private final byte [] srcBuf;
	private int srcBufLen;
	private int blockState = -1; // -1:no block; -2:read block; >=0:read blockSize
	private int blockSize;

	public DecompressMppcZstd(Codec sink, int srcBufSize, int dstBufSize) {
		super(sink);
		srcBuf = new byte[Math.max(srcBufSize, 1)];
		ds = ZstdFactory.newDecompressStream(dstBufSize);
	}

	@Override
	void onLongOffset() {
		if (off != 0x1fff + 320)
			return;
		// Consume the escape at the token boundary, including when flush finishes a preceding MPPC tuple.
		off = -1;
		int buffered = rem;
		int wholeBits = pos & ~7;
		rem = 0;
		pos = 0;
		blockState = 0;
		for (int shift = wholeBits - 8; shift >= 0; shift -= 8)
			update((byte)(buffered >> shift));
	}

	@Override
	public void update(byte c) throws CodecException {
		if (blockState == -1) {
			super.update(c);
		} else if (blockState == -2) {
			srcBuf[srcBufLen++] = c;
			if (srcBufLen == srcBuf.length)
				flushBufferedBlockData();
			if (--blockSize == 0) {
				blockState = 0;
				// 块数据结束时srcBuf可能仍有未喂给zstd的残留（数组版路径块数据直达ds，无此问题）：
				// 不在这里排空，后续回到常规MPPC段的输出会越过zstd流顺序，且流结束时（blockState==-1
				// 走super.flush）残留彻底丢失。zstd流式解压接受任意部分输入，此处排空安全。
				flushBufferedBlockData();
			}
		} else {
			if (c < 0) {
				if (blockState >= 28)
					throw new CodecException("blockSize overflow");
				blockSize += (c & 0x7f) << blockState;
				blockState += 7;
			} else {
				if (blockState == 28 && c > 7)
					throw new CodecException("blockSize overflow");
				blockSize += c << blockState;
				blockState = blockSize == 0 ? -1 : -2;
			}
		}
	}

	@Override
	public void update(byte [] data, int pos, int len) throws CodecException {
		for (int end = pos + len; pos < end; ) {
			if (blockState != -2) {
				update(data[pos++]);
			} else {
				// 单字节入口留下的前缀必须先送入解压器，数组后缀不能越过它。
				flushBufferedBlockData();
				len = Math.min(blockSize, end - pos);
				if ((blockSize -= len) == 0)
					blockState = 0;
				ds.decompress(data, pos, pos + len, sink);
				pos += len;
			}
		}
	}

	private void flushBufferedBlockData() throws CodecException {
		if (srcBufLen > 0) {
			ds.decompress(srcBuf, 0, srcBufLen, sink);
			srcBufLen = 0;
		}
	}

	@Override
	public void flush() throws CodecException {
		if (blockState == -1)
			super.flush();
		else {
			flushBufferedBlockData();
			sink.flush();
		}
	}

	@Override
	public void close() {
		ds.close();
		super.close();
	}
}
