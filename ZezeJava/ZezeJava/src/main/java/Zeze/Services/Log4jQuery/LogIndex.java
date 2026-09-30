package Zeze.Services.Log4jQuery;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * 日志按时间顺序的索引。
 * 用来根据时间快速定位到日志数据文件。
 * 每个索引记录固定长度=time(8bytes)+offset(8bytes)。
 * 不变量：记录数组按time非降序——构造器清理尾部崩溃残留、addIndex写边界维持，
 * lowerBound/upperBound的二分查找以它为前提。
 * <p>
 * 如果索引记录可变长并可以自定义，这个类用途会更加广泛。
 * 变长的实现方式：1. 限制最长记录长度，按最长存储（变成定长）；2. 记录边界可识别（如文本加回车）。
 * 扩展需要实现的话，在新的类中实现，这里仅仅实现Log4jQuery需要的特性。
 */
public class LogIndex {
	private static final @NotNull Logger logger = LogManager.getLogger(LogIndex.class);

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
		// 文件系统刷新非原子，打开时按有效记录清理尾部，恢复“记录数组非降序、endTime=末记录时间”
		// 的装载前提：
		// 1) 尾部不完整记录（size非16倍数）：截掉；
		// 2) 尾部连续零记录（time==0&&offset==0）：addIndex的mmap扩容先以写零扩展文件、记录
		//    putLong在其后，崩溃于两步之间留下整条零记录——16倍数尾巴截不掉，endTime被读为0，
		//    buildIndex从0全量重扫追加重复记录，有序不变量进一步破坏。真实记录time为epoch毫秒
		//    （>0），整条全零不与真实记录混淆。
		// 通道须rw：零记录扫描要read、清理要truncate——FileOutputStream的通道只写（read抛
		// NonReadableChannelException）；rw与mmap()的RandomAccessFile("rw")共享语义一致（无share-delete）。
		try (var raf = new RandomAccessFile(file, "rw")) {
			var channel = raf.getChannel();
			var records = (int)(channel.size() / eIndexRecordSize);
			var record = ByteBuffer.allocate(eIndexRecordSize);
			while (records > 0 && isZeroRecord(channel, record, records - 1))
				--records;
			if (channel.size() != (long)records * eIndexRecordSize)
				channel.truncate((long)records * eIndexRecordSize);
		}
		this.file = file;
		mmap(0);

		if (mmap.limit() >= eIndexRecordSize) {
			this.beginTime = mmap.getLong(0);
			this.endTime = mmap.getLong(mmap.limit() - eIndexRecordSize);
		} else {
			// empty index file
			this.beginTime = Long.MAX_VALUE;
			this.endTime = 0;
		}
	}

	// 尾部零记录判定（构造内使用）：按记录起点整读；文件比预期短（并发收缩等异常形态）按
	// 零记录处理交由truncate收敛，不使装载失败。
	private static boolean isZeroRecord(FileChannel channel, ByteBuffer record, int recordIndex) throws IOException {
		record.clear();
		var position = (long)recordIndex * eIndexRecordSize;
		while (record.hasRemaining()) {
			if (channel.read(record, position) < 0)
				return true;
		}
		return record.getLong(0) == 0 && record.getLong(8) == 0;
	}

	// 写侧（addIndex）持rwLock.writeLock更新，读侧getter须持同一rwLock.readLock：
	// 裸读与写无happens-before，长期可见陈旧值（seek跳条目/getIndexOffset回退offset 0全量重扫）。
	public long getBeginTime() {
		rwLock.readLock().lock();
		try {
			return beginTime;
		} finally {
			rwLock.readLock().unlock();
		}
	}

	/**
	 * 索引文件路径：current索引经硬链接打开时即链接路径——LogIndex的mmap增长
	 * （addIndex→mmap(newSize)）按此路径重开文件，链接是存活索引的增长通道。清理方据此识别
	 * "仍被存活条目持有的链接"，不得删除（Linux下删了增长即FNFE、Windows下mmap钉住删不掉）。
	 */
	public File getFile() {
		return file;
	}

	public long getEndTime() {
		rwLock.readLock().lock();
		try {
			return endTime;
		} finally {
			rwLock.readLock().unlock();
		}
	}

	/** 最后一个物理索引记录的偏移；空索引从文件头读，用于检查尚未采样的尾部。 */
	long lastOffset() {
		rwLock.readLock().lock();
		try {
			return mmap.limit() >= eIndexRecordSize ? mmap.getLong(mmap.limit() - 8) : 0;
		} finally {
			rwLock.readLock().unlock();
		}
	}

	private int mmap(int newAllocateSize) throws IOException {
		try (var raf = new RandomAccessFile(file, "rw"); var channel = raf.getChannel()) {
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
			// 写边界不变量：追加记录的time不小于既有endTime（记录数组非降序是lowerBound/upperBound
			// 二分查找的前提）。时钟回拨/滞后门限（loadIndex的lastIndexTime批间才推进）产生的乱序
			// 记录按当前endTime对齐丢弃并告警一次：被丢弃时间窗已由既有记录承载定位，只损失该窗的
			// 定位精度；拒绝整批或抛错中断装载会把乱序扩大为索引缺失。运行边界随批内保留记录推进，
			// 批内逆序对同样被吸收。
			var boundary = endTime;
			var kept = 0;
			for (var r : rs) {
				if (r.time >= boundary) {
					boundary = r.time;
					++kept;
				}
			}
			if (kept < rs.size())
				logger.warn("addIndex drop out-of-order records: dropped={}, kept={}, endTime={}",
						rs.size() - kept, kept, endTime);
			if (0 == kept)
				return;

			var newSize = kept * eIndexRecordSize;
			var position = mmap(newSize);
			mmap.position(position);
			var firstKeptTime = Long.MAX_VALUE;
			var lastKeptTime = 0L;
			boundary = endTime;
			for (var r : rs) {
				if (r.time < boundary)
					continue;
				boundary = r.time;
				mmap.putLong(r.time);
				mmap.putLong(r.offset);
				if (firstKeptTime == Long.MAX_VALUE)
					firstKeptTime = r.time;
				lastKeptTime = r.time;
			}
			if (firstKeptTime < beginTime)
				beginTime = firstKeptTime;
			if (lastKeptTime > endTime)
				endTime = lastKeptTime;
		} finally {
			rwLock.writeLock().unlock();
		}
	}

	/**
	 * 全量记录快照（持读锁）：轮转移交（Log4jFileManager.transferIndexToRotate）批量复制既有
	 * 记录到新实例用。记录量=索引条数（每10s一条，日常量级KB），复制成本低。
	 */
	public List<Record> snapshotRecords() {
		rwLock.readLock().lock();
		try {
			var size = mmap.limit() / eIndexRecordSize;
			var records = new ArrayList<Record>(size);
			for (var i = 0; i < size; ++i)
				records.add(Record.of(mmap.getLong(i * eIndexRecordSize), mmap.getLong(i * eIndexRecordSize + 8)));
			return records;
		} finally {
			rwLock.readLock().unlock();
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
			if (idx >= size)
				return -1;
			return mmap.getLong(idx * eIndexRecordSize + 8);
		} finally {
			rwLock.readLock().unlock();
		}
	}

	/**
	 * 前驱记录（floor）：最后一个 time&lt;=key 的记录的 offset。查询定位（seek）专用锚：
	 * 索引是采样而非完备集（10s节拍+装载批间基线推进都留未索引间隙），取lowerBound锚
	 * （首条&gt;=key的记录）会使前驱与锚之间未入索引的日志（时间可&gt;=key、物理位置在
	 * 锚之前）不被读到——窗口头部静默漏读。key早于首条记录/空索引返回-1（无前驱可用，
	 * 调用方回落文件头扫描）；key超出末端的前驱即末记录（尾窗续扫语义由此统一承载）。
	 *
	 * @param time time
	 * @return index.offset 不存在时返回-1。
	 */
	public long floorOffset(long time) {
		rwLock.readLock().lock();
		try {
			var size = mmap.limit() / eIndexRecordSize;
			var idx = lowerBoundIndex(time, size);
			if (idx < size && mmap.getLong(idx * eIndexRecordSize) == time)
				return mmap.getLong(idx * eIndexRecordSize + 8);
			if (idx > 0)
				return mmap.getLong((idx - 1) * eIndexRecordSize + 8);
			return -1;
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
	 * std::upper_bound 定义。返回指定key为下限的索引。即在＞key范围内找最小的key的索引
	 *
	 * @param time time
	 * @return index.offset 不存在时返回-1。
	 */
	public long upperBound(long time) {
		rwLock.readLock().lock();
		try {
			var size = mmap.limit() / eIndexRecordSize;
			var idx = upperBoundIndex(time, size);
			if (idx >= size) // 与lowerBound一致：upperBoundIndex返回size表示全部记录时间<=time，越界须判>=
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
