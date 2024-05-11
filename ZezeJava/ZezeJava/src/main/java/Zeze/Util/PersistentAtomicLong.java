package Zeze.Util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.NotNull;

public class PersistentAtomicLong {
	private final AtomicLong currentId = new AtomicLong();
	private volatile long allocatedEnd;

	private final @NotNull String name;
	private final @NotNull String fileName;
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
	public static @NotNull PersistentAtomicLong getOrAdd(@NotNull String ProgramInstanceName) {
		var name = ProgramInstanceName.replace(':', '.');
		// 这样写，不小心重名也能工作。
		return pals.computeIfAbsent(name, PersistentAtomicLong::new);
	}

	private PersistentAtomicLong(@NotNull String ProgramInstanceName) {
		name = ProgramInstanceName;
		fileName = ProgramInstanceName + ".zeze.pal";

		try {
			var fs = open(fileName);
			fs.lock();
			try {
				var lock = fs.getChannel().lock();
				try {
					var line = fs.readLine();
					if (null != line) {
						allocatedEnd = Long.parseLong(line);
						currentId.set(allocatedEnd);
					}
					// 初始化的时候不allocate，如果程序启动，没有分配就退出，保持原来的值。
				} finally {
					lock.release();
				}
			} finally {
				fs.unlock();
			}
		} catch (IOException e) {
			Task.forceThrow(e);
		}
	}

	public @NotNull String getName() {
		return name;
	}

	public long next() {
		return next(1);
		/* 旧的分配一个的代码。比较确认完成以后删除。
		for (; ; ) {
			var current = currentId.get();
			if (current >= allocatedEnd) {
				allocate();
				continue;
			}
			if (currentId.compareAndSet(current, current + 1))
				return current + 1;
		}
		*/
	}

	public long next(int count) {
		if (count < 1)
			throw new IllegalArgumentException("count < 1");

		for (; ; ) {
			var current = currentId.get();
			if (current >= allocatedEnd) {
				allocate(count);
				continue;
			}
			var next = current + count;
			if (currentId.compareAndSet(current, next))
				return next;
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

	private static @NotNull FileWithLock open(@NotNull String fileName) {
		return allocFiles.computeIfAbsent(fileName, (k) -> {
			try {
				return new FileWithLock(k, "rw");
			} catch (FileNotFoundException e) {
				Task.forceThrow(e);
				return null; // never run here
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
						if (currentId.get() < allocatedEnd)
							return; // has allocated. concurrent.
						fs.seek(0);
						var line = fs.readLine();
						var last = (null == line || line.isEmpty()) ? 0L : Long.parseLong(line);
						var allocateSize = fund.next();
						if (allocateSize < count)
							allocateSize += count;
						var newLast = last + allocateSize;
						var reset = newLast < 0;
						if (reset)
							newLast = allocateSize;
						fs.setLength(0);
						fs.write(String.valueOf(newLast).getBytes(StandardCharsets.UTF_8));
						allocatedEnd = newLast; // first
						if (reset)
							currentId.set(0); // second
					}
				} finally {
					fs.unlock();
				}
				break;
			}
		} catch (IOException e) {
			Task.forceThrow(e);
		}
	}
}
