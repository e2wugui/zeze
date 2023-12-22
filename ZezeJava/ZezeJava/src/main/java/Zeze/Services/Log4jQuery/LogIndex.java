package Zeze.Services.Log4jQuery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 日志按时间顺序的索引。
 * 用来根据时间快速定位到日志数据文件。
 * 每个索引记录固定长度=time(8bytes)+offset(8bytes)。
 *
 * 【扩展】如果索引记录可变长并可以自定义，这个类用途会更加广泛。
 * 变长的实现方式：1. 限制最长记录长度，按最长存储（变成定长）；2. 记录边界可识别（如文本加回车）。
 * 扩展需要实现的话，在新的类中实现，这里仅仅实现Log4jQuery需要的特性。
 */
public class LogIndex {
	public static class Record {
		public final long time;
		public final long offset;

		public Record(long time, long offset) {
			this.time = time;
			this.offset = offset;
		}

		public static Record of(long time, long offset) {
			return new Record(time, offset);
		}
	}

	public final static int eIndexRecordSize = 16;

	private final File file;
	private MappedByteBuffer mmap;
	private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
	private long beginTime;
	private long endTime;

	public LogIndex(File file) throws Exception {
		// 修正由于文件系统刷新不是原子导致的索引记录可能不完整的问题。
		try (var channel = new FileOutputStream(file, true).getChannel()) {
			var fileSize = channel.size();
			if ((fileSize & (eIndexRecordSize - 1)) != 0)
				channel.truncate(fileSize / eIndexRecordSize * eIndexRecordSize);
		}
		this.file = file;
		mmap(0);

		// initialize beginTime & endTime
		if (mmap.limit() >= eIndexRecordSize) {
			this.beginTime = mmap.getLong(0);
			this.endTime = mmap.getLong(mmap.limit() - eIndexRecordSize);
		} else {
			// empty index file
			this.beginTime = Long.MAX_VALUE;
			this.endTime = 0;
		}
	}

	public boolean inTimeRange(long time) {
		return time >= beginTime && time <= endTime;
	}

	public long getBeginTime() {
		return beginTime;
	}

	public long getEndTime() {
		return endTime;
	}

	private int mmap(int newAllocateSize) throws IOException {
		try (var channel = new RandomAccessFile(file, "rw").getChannel()) {
			var currentSize = channel.size();
			mmap = channel.map(FileChannel.MapMode.READ_WRITE, 0, currentSize + newAllocateSize);
			return (int)currentSize;
		}
	}

	public void addIndex(long time, long offset) throws IOException {
		addIndex(List.of(Record.of(time, offset)));
	}

	public void addIndex(List<Record> rs) throws IOException {
		if (rs.isEmpty())
			return;

		rwLock.writeLock().lock();
		try {
			var newSize = rs.size() * eIndexRecordSize;
			var position = mmap(newSize);
			mmap.position(position);
			for (var r : rs) {
				mmap.putLong(r.time);
				mmap.putLong(r.offset);
			}

			// new beginTime & endTime
			var first = rs.get(0);
			var last = rs.get(rs.size() - 1);
			if (first.time < beginTime)
				beginTime = first.time;
			if (last.time > endTime)
				endTime = last.time;
		} finally {
			rwLock.writeLock().unlock();
		}
	}

	/**
	 * std::lower_bound 定义。返回指定key为上限的索引。即在>=key范围内找最小的key的索引
	 *
	 * @param time time
	 * @return index.offset 不存在时返回-1。
	 */
	public long lowerBound(long time) {
		rwLock.readLock().lock();
		try {
			var size = mmap.limit() / eIndexRecordSize;
			var idx = lowerBoundIndex(time, size);
			if (idx > size)
				return -1;
			return mmap.getLong(idx * eIndexRecordSize + 8);
		} finally {
			rwLock.readLock().unlock();
		}
	}

	/**
	 * std::lower_bound 定义。返回指定key为上限的索引。即在>=key范围内找最小的key的索引
	 *
	 * @param key key
	 * @return index locate，不存在时返回lastIndex+1。
	 */
	private int lowerBoundIndex(long key, int limit) {
		var first = 0;
		var count = limit;
		while (count > 0) {
			var it = first;
			var step = count >> 1;
			it += step;
			if (mmap.getLong(it * eIndexRecordSize) < key) {
				first = it + 1;
				count -= step + 1;
			} else
				count = step;
		}
		return first;
	}

	/**
	 * std::upper_bound 定义。返回指定key为下限的索引。即在>key范围内找最小的key的索引
	 *
	 * @param time time
	 * @return index.offset 不存在时返回-1。
	 */
	public long upperBound(long time) {
		rwLock.readLock().lock();
		try {
			var size = mmap.limit() / eIndexRecordSize;
			var idx = upperBoundIndex(time, size);
			if (idx > size)
				return -1;
			return mmap.getLong(idx * eIndexRecordSize + 8);
		} finally {
			rwLock.readLock().unlock();
		}
	}

	/**
	 * std::upper_bound 定义。返回指定key为下限的索引。即在>key范围内找最小的key的索引
	 *
	 * @param key key
	 * @return index locate，不存在时返回lastIndex+1
	 */
	private int upperBoundIndex(long key, int limit) {
		var first = 0;
		var count = limit;
		while (count > 0) {
			var it = first;
			var step = count >> 1;
			it += step;
			if (mmap.getLong(it * eIndexRecordSize) <= key) {
				first = it + 1;
				count -= step + 1;
			} else
				count = step;
		}
		return first;
	}
}
