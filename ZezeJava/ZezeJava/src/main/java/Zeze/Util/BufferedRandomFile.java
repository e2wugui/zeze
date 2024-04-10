package Zeze.Util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.locks.ReentrantLock;
import org.jetbrains.annotations.NotNull;

/**
 * 目标：
 * 1. 最终类，不接入java流接口。
 * 2. 支持seek
 * 3. 支持position
 * 4. 支持buffered
 */
public final class BufferedRandomFile extends ReentrantLock implements Closeable {
	private final RandomAccessFile randomAccessFile;
	private final ByteBuffer buffer;
	private long pos = 0;
	private final Charset charset;

	public BufferedRandomFile(File file, String charsetName) throws IOException {
		this(file, Str.lookupCharset(charsetName));
	}

	public BufferedRandomFile(File file, Charset charset) throws IOException {
		this.charset = charset;
		randomAccessFile = new RandomAccessFile(file, "r");
		buffer = ByteBuffer.allocate(16 * 1024);
		buffer.flip(); // ready for read out
	}

	public long getPosition() {
		lock();
		try {
			return pos;
		} finally {
			unlock();
		}
	}

	public void seek(long offset) throws IOException {
		lock();
		try {
			randomAccessFile.seek(offset);
			buffer.clear();
			buffer.flip(); // ready for read out
			pos = offset;
		} finally {
			unlock();
		}
	}

	/**
	 * 按字节读取。
	 * @param buf buf
	 * @param off off
	 * @param len len
	 * @return n read bytes, -1 means eof.
	 * @throws IOException exception
	 */
	public int read(@NotNull byte[] buf, int off, int len) throws IOException {
		lock();
		try {
			var n = 0;
			while (len > n) {
				var remaining = buffer.remaining();
				var copy = Math.min(remaining, len - n);
				System.arraycopy(buffer.array(), buffer.position(), buf, off, copy);
				buffer.position(buffer.position() + copy);
				n += copy;
				off += copy;
				if (!fillBuffer()) {
					if (n > 0) {
						pos += n;
						return n;
					}
					return -1; // eof
				}
			}
			pos += n;
			return n;
		} finally {
			unlock();
		}
	}

	/**
	 * 如果buffer没有数据，从文件中读取填入。
	 * @return true fill success; false eof
	 * @throws IOException exception
	 */
	private boolean fillBuffer() throws IOException {
		if (0 == buffer.remaining()) {
			buffer.clear();
			var rc = randomAccessFile.getChannel().read(buffer);
			buffer.flip();
			return rc != -1;
		}
		return true;
	}

	private int peek() throws IOException {
		if (!fillBuffer())
			return -1;
		return buffer.array()[buffer.position()];
	}

	private int read() throws IOException {
		if (!fillBuffer())
			return -1;
		pos ++;
		return buffer.get();
	}

	public String readLine() throws IOException {
		lock();
		try {
			var line = Zeze.Serialize.ByteBuffer.Allocate(4096);
			int c = -1;
			boolean eol = false;

			while (!eol) {
				switch (c = read()) {
				case -1:
				case '\n':
					eol = true;
					break;
				case '\r':
					eol = true;
					if (peek() == '\n') {
						read(); // 如果是换行，读走。
					}
					break;
				default:
					line.WriteByte(c);
					break;
				}
			}

			if ((c == -1) && line.isEmpty()) {
				return null;
			}
			return new String(line.Bytes, line.ReadIndex, line.size(), charset);
		} finally {
			unlock();
		}
	}

	@Override
	public void close() throws IOException {
		randomAccessFile.close();
	}
}
