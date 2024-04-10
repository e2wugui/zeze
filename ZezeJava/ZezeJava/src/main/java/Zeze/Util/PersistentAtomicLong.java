package Zeze.Util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import org.jetbrains.annotations.NotNull;

public class PersistentAtomicLong {
	private final AtomicLong currentId = new AtomicLong();
	private volatile long allocated;

	private final @NotNull String name;
	private final @NotNull String fileName;
	private final int allocateSize;

	private static final ConcurrentHashMap<String, PersistentAtomicLong> pals = new ConcurrentHashMap<>();

	/**
	 * 【小优化】请保存返回值，重复使用。
	 *
	 * @param ProgramInstanceName 程序实例名字。
	 *                            当进程只有一份实例时，可以直接使用程序名。
	 *                            有多个实例时，需要另一个Id区分，这里不能使用进程id（pid），需要稳定的。
	 *                            对于网络程序，可以使用"进程名+Main.Acceptor.Name"
	 */
	public static @NotNull PersistentAtomicLong getOrAdd(@NotNull String ProgramInstanceName, final int allocateSize) {
		var name = ProgramInstanceName.replace(':', '.');
		// 这样写，不小心重名也能工作。
		return pals.computeIfAbsent(name, (k) -> new PersistentAtomicLong(k, allocateSize));
	}

	public static @NotNull PersistentAtomicLong getOrAdd(@NotNull String ProgramInstanceName) {
		return getOrAdd(ProgramInstanceName, 5000);
	}

	private PersistentAtomicLong(@NotNull String ProgramInstanceName, int allocateSize) {
		if (allocateSize <= 0)
			throw new IllegalArgumentException();

		name = ProgramInstanceName;
		fileName = ProgramInstanceName + ".zeze.pal";
		this.allocateSize = allocateSize;

		try {
			var fs = open(fileName);
			fs.lock();
			try {
				var lock = fs.getChannel().lock();
				try {
					var line = fs.readLine();
					if (null != line) {
						allocated = Long.parseLong(line);
						currentId.set(allocated);
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
		for (; ; ) {
			long current = currentId.get();
			if (current >= allocated) {
				allocate();
				continue;
			}
			if (currentId.compareAndSet(current, current + 1))
				return current + 1;
		}
	}

	public static class FileWithLock extends RandomAccessFile {
		public final ReentrantLock thisLock = new ReentrantLock();
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

	private void allocate() {
		try {
			// 应该尽量减少allocate的次数，所以这里文件就不保持打开了。
			var fs = open(fileName);
			fs.lock();
			try {
				var lock = fs.getChannel().lock();
				try {
					if (currentId.get() < allocated)
						return; // has allocated. concurrent.
					fs.seek(0);
					var line = fs.readLine();
					var last = (null == line || line.isEmpty()) ? 0L : Long.parseLong(line);
					var newLast = last + allocateSize;
					var reset = newLast < 0;
					if (reset)
						newLast = allocateSize;
					fs.setLength(0);
					fs.write(String.valueOf(newLast).getBytes(StandardCharsets.UTF_8));
					allocated = newLast; // first
					if (reset)
						currentId.set(0); // second
				} finally {
					lock.release();
				}
			} finally {
				fs.unlock();
			}
		} catch (Exception e) {
			Task.forceThrow(e);
		}
	}
}
