package Zeze.Net;

import java.io.Closeable;
import Zeze.Util.ZstdFactory;
import org.jetbrains.annotations.NotNull;

public final class DecompressZstd implements Codec, Closeable {
	public static final int DEFAULT_SRC_BUF_SIZE = 1024;

	private final @NotNull Codec sink;
	private final @NotNull ZstdFactory.ZstdDecompressStream ds;
	private final byte @NotNull [] srcBuf;
	private int srcBufLen;

	public DecompressZstd(@NotNull Codec sink) {
		this.sink = sink;
		ds = ZstdFactory.newDecompressStream();
		srcBuf = new byte[DEFAULT_SRC_BUF_SIZE];
	}

	public DecompressZstd(@NotNull Codec sink, int srcBufSize, int dstBufSize) {
		this.sink = sink;
		ds = ZstdFactory.newDecompressStream(dstBufSize);
		srcBuf = new byte[Math.max(srcBufSize, 1)];
	}

	@Override
	public void update(byte c) throws CodecException {
		srcBuf[srcBufLen++] = c;
		if (srcBufLen == srcBuf.length) {
			ds.decompress(srcBuf, 0, srcBufLen, sink);
			srcBufLen = 0;
		}
	}

	@Override
	public void update(byte @NotNull [] data, int off, int len) throws CodecException {
		if (srcBufLen > 0) {
			int n = Math.min(srcBuf.length - srcBufLen, len);
			System.arraycopy(data, off, srcBuf, srcBufLen, n);
			if ((srcBufLen += n) >= srcBuf.length) {
				ds.decompress(srcBuf, 0, srcBufLen, sink);
				srcBufLen = 0;
			}
			if ((len -= n) <= 0)
				return;
			off += n;
		}
		if (len < srcBuf.length) {
			System.arraycopy(data, off, srcBuf, 0, len);
			srcBufLen = len;
		} else
			ds.decompress(data, off, off + len, sink);
	}

	@Override
	public void flush() throws CodecException {
		if (srcBufLen > 0) {
			ds.decompress(srcBuf, 0, srcBufLen, sink);
			srcBufLen = 0;
		}
		sink.flush();
	}

	@Override
	public void close() {
		ds.close();
	}
}
