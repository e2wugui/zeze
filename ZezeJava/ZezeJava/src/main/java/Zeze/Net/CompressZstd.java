package Zeze.Net;

import java.io.Closeable;
import Zeze.Util.ZstdFactory;
import Zeze.Util.ZstdFactory.ZstdCompressStream;

public final class CompressZstd implements Codec, Closeable {
	public static final int DEFAULT_SRC_BUF_SIZE = 1024;

	private final Codec sink;
	private final ZstdCompressStream cs;
	private final byte[] srcBuf;
	private int srcBufLen;

	public CompressZstd(Codec sink) {
		this.sink = sink;
		cs = ZstdFactory.newCompressStream();
		srcBuf = new byte[DEFAULT_SRC_BUF_SIZE];
	}

	public CompressZstd(Codec sink, int srcBufSize, int dstBufSize, int compressLevel, int windowLog) {
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
	public void update(byte[] data, int off, int len) throws CodecException {
		if (srcBufLen > 0) {
			cs.compress(srcBuf, 0, srcBufLen, sink);
			srcBufLen = 0;
		}
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
