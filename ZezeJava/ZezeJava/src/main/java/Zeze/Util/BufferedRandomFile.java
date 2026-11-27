package Zeze.Util;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import org.jspecify.annotations.Nullable;

/**
 * 带读缓冲的随机只读文件封装：支持 seek/position/readLine，持自身锁保证并发安全。
 */
public final class BufferedRandomFile extends ReentrantLock implements Closeable {
	private final FileChannel channel;
	private final ByteBuffer buffer;
	private long pos = 0;
	private final Charset charset;

	public BufferedRandomFile(File file, String charsetName) throws IOException {
		this(file, Str.lookupCharset(charsetName));
	}

	public BufferedRandomFile(File file, Charset charset) throws IOException {
		this.charset = charset;
		// NIO FileChannel 在 Windows 下默认共享删除权限，避免阻塞日志轮转；
		// 将 NoSuchFileException 转为 FileNotFoundException 以保持调用方异常契约。
		try {
			channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
		} catch (NoSuchFileException e) {
			throw new FileNotFoundException(e.getMessage());
		}
		buffer = ByteBuffer.allocate(16 * 1024);
		buffer.flip();
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
			channel.position(offset);
			buffer.clear();
			buffer.flip();
			pos = offset;
		} finally {
			unlock();
		}
	}

	/**
	 * 按字节读取。
	 *
	 * @return 实际读取字节数，到达文件尾返回 -1
	 */
	public int read(byte [] buf, int off, int len) throws IOException {
		Objects.checkFromIndexSize(off, len, buf.length);
		lock();
		try {
			var n = 0;
			while (n < len && fillBuffer()) {
				var copy = Math.min(buffer.remaining(), len - n);
				buffer.get(buf, off + n, copy);
				n += copy;
				pos += copy;
			}
			return n == 0 && len > 0 ? -1 : n;
		} finally {
			unlock();
		}
	}

	/** 缓冲耗尽时从文件填充；返回 false 表示到达文件尾。 */
	private boolean fillBuffer() throws IOException {
		if (buffer.hasRemaining())
			return true;
		buffer.clear();
		try {
			return channel.read(buffer) > 0;
		} catch (IOException e) {
			buffer.clear();
			throw e;
		} finally {
			buffer.flip();
		}
	}

	private int peek() throws IOException {
		if (!fillBuffer())
			return -1;
		return buffer.array()[buffer.position()] & 0xff;
	}

	private int read() throws IOException {
		if (!fillBuffer())
			return -1;
		pos++;
		// 无符号：-1 唯一表示 eof，数据字节 0xff 不能与哨兵混淆
		return buffer.get() & 0xff;
	}

	public @Nullable String readLine() throws IOException {
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
						read();
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
		// 持锁关闭，避免与并发读交错导致 ClosedChannelException。
		lock();
		try {
			channel.close();
		} finally {
			buffer.clear();
			buffer.flip();
			unlock();
		}
	}
}
