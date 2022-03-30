package Zeze.Util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class PersistentAtomicLong {
	private final AtomicLong currentId = new AtomicLong();
	private volatile long allocated;

	private final String Name;
	private final String FileName;
	private final int AllocateSize;

	private final static ConcurrentHashMap<String, PersistentAtomicLong> pals = new ConcurrentHashMap<>();

	/**
	 * 【小优化】请保存返回值，重复使用。
	 *
	 * @param ProgramInstanceName 程序实例名字。
	 *                            当进程只有一份实例时，可以直接使用程序名。
	 *                            有多个实例时，需要另一个Id区分，这里不能使用进程id（pid），需要稳定的。
	 *                            对于网络程序，可以使用"进程名+Main.Acceptor.Name"
	 */
	public static PersistentAtomicLong getOrAdd(final String ProgramInstanceName, final int allocateSize) {
		var name = ProgramInstanceName.replace(':', '.');
		// 这样写，不小心重名也能工作。
		return pals.computeIfAbsent(name, (k) -> new PersistentAtomicLong(k, allocateSize));
	}

	public static PersistentAtomicLong getOrAdd(final String ProgramInstanceName) {
		return getOrAdd(ProgramInstanceName, 5000);
	}

	private PersistentAtomicLong(String ProgramInstanceName, int allocateSize) {
		if (allocateSize <= 0)
			throw new IllegalArgumentException();

		Name = ProgramInstanceName;
		FileName = ProgramInstanceName + ".zeze.pal";
		AllocateSize = allocateSize;

		try {
			var fs = open(FileName);
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (fs) {
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
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public String getName() {
		return Name;
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

	private static final ConcurrentHashMap<String, RandomAccessFile> AllocFiles = new ConcurrentHashMap<>();

	private static RandomAccessFile open(String fileName) {
		return AllocFiles.computeIfAbsent(fileName, (k) -> {
			try {
				return new RandomAccessFile(fileName, "rw");
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private void allocate() {
		try {
			// 应该尽量减少allocate的次数，所以这里文件就不保持打开了。
			var fs = open(FileName);
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (fs) {
				var lock = fs.getChannel().lock();
				try {
					if (currentId.get() < allocated)
						return; // has allocated. concurrent.
					fs.seek(0);
					var line = fs.readLine();
					var last = (null == line || line.isEmpty()) ? 0L : Long.parseLong(line);
					var newLast = last + AllocateSize;
					var reset = newLast < 0;
					if (reset)
						newLast = AllocateSize;
					fs.setLength(0);
					fs.write(String.valueOf(newLast).getBytes(StandardCharsets.UTF_8));
					allocated = newLast; // first
					if (reset)
						currentId.set(0); // second
				} finally {
					lock.release();
				}
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
}
