package Zeze.Util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** 持久化原子递增 ID 生成器：按块预分配水位并定宽落盘以保证崩溃安全。 */
public class PersistentAtomicLong {
	// 水位文件定宽 20 字节（long 最长 19 位 + 至少 1 空格），覆盖写不改变文件长度
	private static final int VALUE_WIDTH = 20;

	private volatile Range range;

	private static final class Range extends AtomicLong {
		private final long end;

		private Range(long start, long end) {
			super(start);
			this.end = end;
		}

		private long tryNext(int count) {
			for (; ; ) {
				var current = get();
				if (end - current < count)
					return 0;
				var next = current + count;
				if (compareAndSet(current, next))
					return next;
			}
		}
	}

	private final String name;
	private final String fileName;
	private final TimeAdaptedFund fund = TimeAdaptedFund.getDefaultFund();

	private static final ConcurrentHashMap<String, PersistentAtomicLong> pals = new ConcurrentHashMap<>();

	/**
	 * 【小优化】请保存返回值，重复使用。
	 *
	 * @param ProgramInstanceName 程序实例名字。
	 *                            当进程只有一份实例时，可以直接使用程序名。
	 *                            有多个实例时，需要另一个Id区分，这里不能使用进程id（pid），需要稳定的。
	 *                            对于网络程序，可以使用"进程名+Main.Acceptor.Name"
	 */
	public static PersistentAtomicLong getOrAdd(String ProgramInstanceName) {
		var name = ProgramInstanceName.replace(':', '.');
		return pals.computeIfAbsent(name, PersistentAtomicLong::new);
	}

	private PersistentAtomicLong(String ProgramInstanceName) {
		name = ProgramInstanceName;
		fileName = ProgramInstanceName + ".zeze.pal";

		try {
			var fs = open(fileName);
			fs.lock();
			try {
				var lock = fs.getChannel().lock();
				try {
					var last = readWatermark(fs);
						range = new Range(last, last);
					// 初始化的时候不allocate，如果程序启动，没有分配就退出，保持原来的值。
				} finally {
					lock.release();
				}
			} finally {
				fs.unlock();
			}
		} catch (IOException e) {
			throw Task.forceThrow(e);
		}
	}

	public String getName() {
		return name;
	}

	public long next() {
		return next(1);
	}

	public long next(int count) {
		if (count < 1)
			throw new IllegalArgumentException("count < 1");

		for (; ; ) {
			var next = range.tryNext(count);
			if (next != 0)
				return next;
			allocate(count);
		}
	}

	public static class FileWithLock extends RandomAccessFile {
		public final FastLock thisLock = new FastLock();

		public FileWithLock(String name, String mode) throws FileNotFoundException {
			super(name, mode);
		}

		public void lock() {
			thisLock.lock();
		}

		public void unlock() {
			thisLock.unlock();
		}
	}

	private static final ConcurrentHashMap<String, FileWithLock> allocFiles = new ConcurrentHashMap<>();

	private static FileWithLock open(String fileName) {
		return allocFiles.computeIfAbsent(fileName, k -> {
			try {
				return new FileWithLock(k, "rw");
			} catch (FileNotFoundException e) {
				throw Task.forceThrow(e);
			}
		});
	}

	private void allocate(int count) {
		try {
			for (; ; ) {
				var fs = open(fileName);
				fs.lock(); // 文件锁线程不安全，所以本进程需要保护一次。
				try {
					var channel = fs.getChannel();
					if (!channel.isOpen()) {
						allocFiles.remove(fileName, fs);
						continue;
					}
					try (var ignored = channel.lock()) {
							var oldRange = range;
							if (oldRange.end - oldRange.get() >= count)
							return; // 其他线程并发分配的区间已足够覆盖本次 count
						var last = readWatermark(fs);
							long allocateSize = fund.next();
						if (allocateSize < count)
							allocateSize += count;
						var newLast = last + allocateSize;
						var reset = newLast < 0;
						if (reset)
							newLast = allocateSize;
						var fileLen = fs.length();
						if (fileLen != 0 && fileLen < VALUE_WIDTH) {
							// 兼容旧变长格式：先在尾部补空格扩展至定宽并 force，确保迁移中崩溃仍可完整读出旧值
							var pads = new byte[(int) (VALUE_WIDTH - fileLen)];
							Arrays.fill(pads, (byte) ' ');
							fs.seek(fileLen);
							fs.write(pads);
							channel.force(false);
						}
						fs.seek(0);
						fs.write(toFixedWidthBytes(newLast)); // 定宽覆盖写不改变文件长度，崩溃安全
						channel.force(false);
							// 冻结旧号段后再发布新边界；同进程续接未消耗尾部，跨进程则跳过已被占用的区间
							var current = oldRange.getAndSet(oldRange.end);
							var start = reset ? 0 : last == oldRange.end ? current : last;
							range = new Range(start, newLast);
					}
				} finally {
					fs.unlock();
				}
				break;
			}
		} catch (IOException e) {
			throw Task.forceThrow(e);
		}
	}

	/** 读取水位（兼容旧变长格式与定宽右对齐格式）；空文件或纯空白返回 0。 */
	private static long readWatermark(RandomAccessFile fs) throws IOException {
		fs.seek(0);
		var line = fs.readLine();
		if (line == null)
			return 0;
		line = line.trim();
		return line.isEmpty() ? 0 : Long.parseLong(line);
	}

	private static byte[] toFixedWidthBytes(long value) {
		var digits = String.valueOf(value).getBytes(StandardCharsets.UTF_8);
		var bytes = new byte[VALUE_WIDTH];
		Arrays.fill(bytes, (byte) ' ');
		System.arraycopy(digits, 0, bytes, VALUE_WIDTH - digits.length, digits.length);
		return bytes;
	}
}
