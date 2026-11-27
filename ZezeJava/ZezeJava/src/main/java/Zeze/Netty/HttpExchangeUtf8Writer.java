package Zeze.Netty;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * 模板输出 UTF-8 编码基类：跨多次 write/flush 保持 UTF-16 代理对完整。
 */
abstract class HttpExchangeUtf8Writer extends Writer {
	protected boolean closed;
	private final OutputStreamWriter encoder = new OutputStreamWriter(new OutputStream() {
		@Override
		public void write(int b) {
			writeBytes(new byte[]{(byte)b}, 0, 1);
		}

		@Override
		public void write(byte[] bytes, int off, int len) {
			writeBytes(bytes, off, len);
		}
	}, HttpServer.defaultCharset);

	protected abstract void writeBytes(byte[] bytes, int off, int len);

	@Override
	public void write(char[] chars, int off, int len) throws IOException {
		if (closed)
			throw new IOException("Writer closed");
		encoder.write(chars, off, len);
		// 及时刷出已完整的字符，flush 会保留未成对的高代理等待下次写入。
		encoder.flush();
	}

	@Override
	public void flush() throws IOException {
		if (closed)
			throw new IOException("Writer closed");
		encoder.flush();
	}

	protected final void finishEncoding() throws IOException {
		encoder.close();
	}
}
