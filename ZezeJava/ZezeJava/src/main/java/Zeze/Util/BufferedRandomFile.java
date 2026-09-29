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
	private final FileChannel channel;
	private final ByteBuffer buffer;
	private long pos = 0;
	private final Charset charset;

	public BufferedRandomFile(File file, String charsetName) throws IOException {
		this(file, Str.lookupCharset(charsetName));
	}

	public BufferedRandomFile(File file, Charset charset) throws IOException {
		this.charset = charset;
		// log4j-02（FND26）：NIO FileChannel 默认全共享打开（Windows 含 FILE_SHARE_DELETE），
		// 查询/buildIndex 的分钟级读持有窗口内写方对 active 的 rename 型轮转不被阻断——
		// RandomAccessFile 在 Windows 不带 FILE_SHARE_DELETE，持句柄期间写方轮转恒败
		// （log4j2 报错回退、active 无界增长）。Linux 语义不变（O_RDONLY 下 rename/unlink 本合法）。
		// 文件不存在保持 FileNotFoundException 契约：FileChannel.open 抛的 NoSuchFileException
		// 不是 FileNotFoundException 子类，而调用方（Log4jFileSession 构造失败回收链、
		// manager 装载路径）按 FileNotFoundException 捕获轮转竞态的条目消失。
		try {
			channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
		} catch (NoSuchFileException e) {
			throw new FileNotFoundException(e.getMessage());
		}
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
			channel.position(offset);
			buffer.clear();
			buffer.flip(); // ready for read out
			pos = offset;
		} finally {
			unlock();
		}
	}

	/**
	 * 按字节读取。
	 *
	 * @param buf buf
	 * @param off off
	 * @param len len
	 * @return n read bytes, -1 means eof.
	 * @throws IOException exception
	 */
	public int read(byte @NotNull [] buf, int off, int len) throws IOException {
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
	 *
	 * @return true fill success; false eof
	 * @throws IOException exception
	 */
	private boolean fillBuffer() throws IOException {
		if (0 == buffer.remaining()) {
			buffer.clear();
			var rc = channel.read(buffer); // 位置读：从channel.position读并前移，与seek配对
			buffer.flip();
			return rc != -1;
		}
		return true;
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
		// 与其余公开方法一致持自身锁，保证持锁读（fillBuffer→channel.read）期间
		// 文件状态稳定——并发close会使持锁读抛ClosedChannelException且buffer未消费数据作废。
		lock();
		try {
			channel.close();
		} finally {
			unlock();
		}
	}
}
