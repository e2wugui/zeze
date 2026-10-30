package Zeze.Net;

import Zeze.Util.ZstdFactory;

/**
 * Zstandard 流式解压 codec。
 */
public final class DecompressZstd implements Codec {
	public static final int DEFAULT_SRC_BUF_SIZE = 1024;

	private final Codec sink;
	private final ZstdFactory.ZstdDecompressStream ds;
	private final byte [] srcBuf;
	private int srcBufLen;

	public DecompressZstd(Codec sink) {
		this.sink = sink;
		srcBuf = new byte[DEFAULT_SRC_BUF_SIZE];
		ds = ZstdFactory.newDecompressStream();
	}

	public DecompressZstd(Codec sink, int srcBufSize, int dstBufSize) {
		this.sink = sink;
		srcBuf = new byte[Math.max(srcBufSize, 1)];
		ds = ZstdFactory.newDecompressStream(dstBufSize);
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
	public void update(byte [] data, int off, int len) throws CodecException {
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
		sink.close();
	}
}
