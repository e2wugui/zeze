package Zeze.Net;

import java.io.Closeable;
import Zeze.Util.ZstdFactory;
import Zeze.Util.ZstdFactory.ZstdCompressStream;
import org.jetbrains.annotations.NotNull;

public final class CompressZstd implements Codec, Closeable {
	public static final int DEFAULT_SRC_BUF_SIZE = 1024;

	private final @NotNull Codec sink;
	private final @NotNull ZstdCompressStream cs;
	private final byte @NotNull [] srcBuf;
	private int srcBufLen;

	public CompressZstd(@NotNull Codec sink) {
		this.sink = sink;
		cs = ZstdFactory.newCompressStream();
		srcBuf = new byte[DEFAULT_SRC_BUF_SIZE];
	}

	public CompressZstd(@NotNull Codec sink, int srcBufSize, int dstBufSize, int compressLevel, int windowLog) {
		this.sink = sink;
		cs = ZstdFactory.newCompressStream(dstBufSize, compressLevel, windowLog);
		srcBuf = new byte[Math.max(srcBufSize, 1)];
	}

	@Override
	public void update(byte c) throws CodecException {
		srcBuf[srcBufLen++] = c;
		if (srcBufLen == srcBuf.length) {
			cs.compress(srcBuf, 0, srcBufLen, sink);
			srcBufLen = 0;
		}
	}

	@Override
	public void update(byte @NotNull [] data, int off, int len) throws CodecException {
		if (srcBufLen > 0) {
			int n = Math.min(srcBuf.length - srcBufLen, len);
			System.arraycopy(data, off, srcBuf, srcBufLen, n);
			if ((srcBufLen += n) >= srcBuf.length) {
				cs.compress(srcBuf, 0, srcBufLen, sink);
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
			cs.compress(data, off, off + len, sink);
	}

	@Override
	public void flush() throws CodecException {
		if (srcBufLen > 0) {
			cs.compress(srcBuf, 0, srcBufLen, sink);
			srcBufLen = 0;
		}
		cs.flush(sink);
	}

	@Override
	public void close() {
		cs.close();
	}
}
