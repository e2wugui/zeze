package Zeze.Util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// 持久化原子递增 id 发生器：按块预分配水位，崩溃安全地落到定宽水位文件
public class PersistentAtomicLong {
	// 水位文件定宽：20字节（long最大19位数字+至少1个空格）。覆盖写长度恒定，
	// i-size不参与落盘，数据与元数据没有先后顺序窗口。
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
		// 这样写，不小心重名也能工作。
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
							return; // has allocated. concurrent. 其他线程分配的预算已足够覆盖本次count。
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
							// 旧格式（变长数字）一次性迁移：先在尾部追加空格扩展到定宽并force。
							// 空格会被读取时的trim去掉，数值不变，迁移过程中任何崩溃点
							// 解析出来的都仍是完整旧值（追加数字会把数值放大，不可行）。
							var pads = new byte[(int) (VALUE_WIDTH - fileLen)];
							Arrays.fill(pads, (byte) ' ');
							fs.seek(fileLen);
							fs.write(pads);
							channel.force(false);
						}
						fs.seek(0);
						fs.write(toFixedWidthBytes(newLast)); // 定宽覆盖写不改变文件长度，崩溃点只可能是完整旧值或完整新值
						channel.force(false);
							// 冻结旧号段后再发布新边界：旧快路径的在途CAS只能失败或已被计入current。
							// 同进程连续扩容可接上未消耗尾部；其他进程已分配的区间必须跳过。
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

	/**
	 * 读取水位，兼容两种格式：旧格式为变长十进制数字，新格式为右对齐、
	 * 空格补齐到VALUE_WIDTH字节的定宽数字。trim后统一解析；
	 * 空文件或纯空白（新文件首次定宽写未完成时的前缀状态）解析为0。
	 */
	private static long readWatermark(RandomAccessFile fs) throws IOException {
		fs.seek(0);
		var line = fs.readLine();
		if (line == null)
			return 0;
		line = line.trim();
		return line.isEmpty() ? 0 : Long.parseLong(line);
	}

	// 值>=0时数字最长19位，宽度20保证至少1个空格；宽度与VALUE_WIDTH由构造保证同步。
	private static byte[] toFixedWidthBytes(long value) {
		var digits = String.valueOf(value).getBytes(StandardCharsets.UTF_8);
		var bytes = new byte[VALUE_WIDTH];
		Arrays.fill(bytes, (byte) ' ');
		System.arraycopy(digits, 0, bytes, VALUE_WIDTH - digits.length, digits.length);
		return bytes;
	}
}
