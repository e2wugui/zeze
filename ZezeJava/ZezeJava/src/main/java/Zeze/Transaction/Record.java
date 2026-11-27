package Zeze.Transaction;

import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Util.Random;
import org.jspecify.annotations.Nullable;
import Zeze.Transaction.IGlobalAgent.AcquireResult;

/**
 * 表格记录：缓存中一条记录的运行时载体，持有软引用值、脏事实、时间戳、
 * 全局缓存状态（GCM）与所属关联记录集。
 */
public abstract class Record extends ReentrantLock {
	public record RootInfo(@Nullable Record record, TableKey tableKey) {
	}

	// 时戳生成器，运行时状态，需要持久化时，再考虑保存到数据库。
	// 0 保留给不存在记录的的时戳。
	private static final AtomicLong timestampGen = new AtomicLong(1);

	public static long getNextTimestamp() {
		return timestampGen.getAndIncrement();
	}

	// 脏标记与脏期间强引用合并为单一 volatile 引用，避免无锁清脏与读路径交错：
	// null=干净；Bean=脏值强引用（防 softValue 被 GC）；DIRTY_NULL=脏删除哨兵。
	private static final Object DIRTY_NULL = new Object();
	private volatile @Nullable Object dirtyRef;

	private volatile long timestamp; // 正值表示load方式加载/修改的自增值;负值表示上次用dirty方式读取时间戳的负值
	private volatile SoftReference<@Nullable Bean> softValue;
	private volatile RelativeRecordSet relativeRecordSet = new RelativeRecordSet();
	private volatile int state = GlobalCacheManagerConst.StateInvalid;

	// too many try
	private boolean fresh;
	private long acquireTime; // 存放过期时间

	public Record(@Nullable Bean value) {
		super(true);
		softValue = new SoftReference<>(value);
		// Timestamp = NextTimestamp; // Table.FindInCacheOrStorage 可能发生数据变化，这里初始化一次不够。
	}

	final void enterFairLock() {
		if (tryLock())
			return;
		var r = (Record1<?, ?>)this;
		try (var ignored = Profiler.begin("RecordWaitFairLock", r.getTable().getName(), r.getObjectKey())) {
			lock();
		}
	}

	final boolean tryEnterFairLock() {
		return tryLock();
	}

	final boolean tryEnterFairLockWhenIdle() {
		return !hasQueuedThreads() && tryLock();
	}

	final void exitFairLock() {
		unlock();
	}

	final boolean getDirty() {
		return dirtyRef != null;
	}

	/**
	 * 脏值快照（单次volatile读，与判脏自洽）：脏删除返回null（记录不存在语义），
	 * 干净返回null（调用方走storage）——两者由调用方结合getDirty()区分。
	 */
	final @Nullable Bean getDirtyValue() {
		var ref = dirtyRef;
		return ref == DIRTY_NULL ? null : (Bean)ref;
	}

	final void setDirty(boolean value) {
		if (!value) {
			dirtyRef = null; // 单次volatile写清脏：读侧要么看到脏快照要么看到干净，无中间态
			return;
		}
		var v = softValue.get(); // 脏数据在记录内保持一份强引用
		dirtyRef = v != null ? v : DIRTY_NULL; // 脏删除没有Bean，哨兵维持脏事实
	}

	final long getTimestamp() {
		return timestamp;
	}

	final void setTimestamp(long value) {
		timestamp = value;
	}

	final @Nullable Bean getSoftValue() {
		return softValue.get();
	}

	final void setSoftValue(@Nullable Bean value) {
		softValue = new SoftReference<>(value);
	}

	final RelativeRecordSet getRelativeRecordSet() {
		return relativeRecordSet;
	}

	final void setRelativeRecordSet(RelativeRecordSet value) {
		relativeRecordSet = value;
	}

	final int getState() {
		return state;
	}

	final void setState(int value) {
		state = value;
	}

	final boolean isFresh() {
		return fresh;
	}

	final boolean isFreshAcquire() {
		return fresh && System.currentTimeMillis() < acquireTime;
	}

	final void setNotFresh() {
		fresh = false;
	}

	final void setFreshAcquire() {
		acquireTime = System.currentTimeMillis() + 300 + Random.getInstance().nextInt(300);
		fresh = true;
	}

	public final RootInfo createRootInfoIfNeed(TableKey tkey) {
		var strongRef = getSoftValue();
		var cur = strongRef != null ? strongRef.rootInfo : null;
		return cur != null ? cur : new RootInfo(this, tkey);
	}

	public abstract Table getTable();

	public abstract Object getObjectKey();

	public abstract void setDirty();

	public abstract @Nullable AcquireResult acquire(int state, boolean fresh, boolean noWait);

	public abstract void commit(RecordAccessed accessed);
}
