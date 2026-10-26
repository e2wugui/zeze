package Zeze.Netty;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/** Keeps UTF-16 characters intact across template writes, including flush boundaries. */
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
		// Publish complete characters promptly; flush retains an incomplete surrogate pair.
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
