package Zeze.Component;

import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Application;
import Zeze.Builtin.AutoKeyOld.BSeedKey;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Transaction.Transaction;

@Deprecated // 暂时保留
public final class AutoKeyOld {
	public static class Module extends AbstractAutoKeyOld {
		private final ConcurrentHashMap<String, AutoKeyOld> map = new ConcurrentHashMap<>();
		public final Application zeze;

		// 这个组件Zeze.Application会自动初始化，不需要应用初始化。
		public Module(Application zeze) {
			this.zeze = zeze;
			RegisterZezeTables(zeze);
		}

		@Override
		public void UnRegister() {
			UnRegisterZezeTables(zeze);
		}

		/**
		 * 这个返回值，可以在自己模块内保存下来，效率高一些。
		 */
		public AutoKeyOld getOrAdd(String name) {
			return map.computeIfAbsent(name, name2 -> new AutoKeyOld(this, name2));
		}
	}

	private static final int ALLOCATE_COUNT_MIN = 64;
	private static final int ALLOCATE_COUNT_MAX = 1024 * 1024;

	private final Module module;
	private final String name;
	private final long logKey;
	private volatile Range range;
	private int allocateCount = ALLOCATE_COUNT_MIN;
	private long lastAllocateTime = System.currentTimeMillis();

	private AutoKeyOld(Module module, String name) {
		this.module = module;
		this.name = name;
		// 详细参考Bean的Log的用法。这里只有一个variable。
		logKey = Bean.nextObjectId();
	}

	public int getAllocateCount() {
		return allocateCount;
	}

	public long nextId() {
		var bb = nextByteBuffer();
		if (bb.size() > 8)
			throw new IllegalStateException("out of range: serverId=" + module.zeze.getConfig().getServerId()
					+ ", nextId=" + bb);
		return ByteBuffer.ToLongBE(bb.Bytes, bb.ReadIndex, bb.size()); // 这里用BE(大端)是为了保证返回值一定为正
	}

	public byte[] nextBytes() {
		return nextByteBuffer().CopyIf();
	}

	public Binary nextBinary() {
		return new Binary(nextByteBuffer());
	}

	public String nextString() {
		return Base64.getEncoder().encodeToString(nextBytes());
	}

	public ByteBuffer nextByteBuffer() {
		var serverId = module.zeze.getConfig().getServerId();
		var bb = ByteBuffer.Allocate(8);
		if (serverId > 0) // 如果serverId==0,写1个字节0不会影响ToLongBE的结果,但会多占1个字节,所以只在serverId>0时写ByteBuffer
			bb.WriteUInt(serverId);
		else if (serverId < 0) // serverId不应该<0,因为会导致nextId返回负值
			throw new IllegalStateException("serverId(" + serverId + ") < 0");
		bb.WriteULong(nextSeed());
		return bb;
	}

	/**
	 * 设置当前serverId的种子，新种子必须比当前值大。
	 *
	 * @param seed new seed.
	 * @return true if success.
	 */
	public boolean setSeed(long seed) {
		var seedKey = new BSeedKey(module.zeze.getConfig().getServerId(), name);
		var bAutoKey = module._tAutoKeys.getOrAdd(seedKey);
		if (seed > bAutoKey.getNextId()) {
			bAutoKey.setNextId(seed);
			return true;
		}
		return false;
	}

	/**
	 * 增加当前serverId的种子。只能增加，如果溢出，返回失败。
	 *
	 * @param delta delta
	 * @return true if success.
	 */
	public boolean increaseSeed(long delta) {
		if (delta <= 0)
			return false;
		var seedKey = new BSeedKey(module.zeze.getConfig().getServerId(), name);
		var bAutoKey = module._tAutoKeys.getOrAdd(seedKey);
		var newSeed = bAutoKey.getNextId() + delta;
		if (newSeed > 0) {
			bAutoKey.setNextId(newSeed);
			return true;
		}
		// 溢出
		return false;
	}

	/**
	 * 返回当前serverId的种子。
	 *
	 * @return seed
	 */
	public long getSeed() {
		var seedKey = new BSeedKey(module.zeze.getConfig().getServerId(), name);
		var bAutoKey = module._tAutoKeys.getOrAdd(seedKey);
		return bAutoKey.getNextId();
	}

	private long nextSeed() {
		if (null != range) {
			var next = range.tryNextId();
			if (next != 0)
				return next; // allocate in range success
		}

		Transaction.whileCommit(() -> {
			// 不能在重做时重复计算，一次事务重新计算一次，下一次生效。
			// 这里可能有并发问题, 不过影响可以忽略
			var now = System.currentTimeMillis();
			var diff = now - lastAllocateTime;
			lastAllocateTime = now;
			long newCount = allocateCount;
			if (diff < 30 * 1000) // 30 seconds
				newCount <<= 1;
			else if (diff > 120 * 1000) // 120 seconds
				newCount >>= 1;
			else
				return;
			allocateCount = (int)Math.min(Math.max(newCount, ALLOCATE_COUNT_MIN), ALLOCATE_COUNT_MAX);
		});

		var seedKey = new BSeedKey(module.zeze.getConfig().getServerId(), name);
		var txn = Transaction.getCurrent();
		assert txn != null;
		var log = (RangeLog)txn.getLog(logKey);
		while (true) {
			if (null == log) {
				// allocate: 多线程，多事务，多服务器（缓存同步）由zeze保证。
				var key = module._tAutoKeys.getOrAdd(seedKey);
				var start = key.getNextId();
				var end = start + allocateCount; // allocateCount == 0 会死循环。
				key.setNextId(end);
				// create log，本事务可见，
				log = new RangeLog(new Range(start, end));
				txn.putLog(log);
			}
			var tryNext = log.range.tryNextId();
			if (tryNext != 0)
				return tryNext;

			// 事务内分配了超出Range范围的id，再次allocate。
			// 覆盖RangeLog是可以的。就像事务内多次改变变量。最后面的Log里面的数据是最新的。
			// 已分配的范围保存在_AutoKeys表内，事务内可以继续分配。
			log = null;
		}
	}

	private static class Range extends AtomicLong {
		private final long max;

		public long tryNextId() {
			var nextId = incrementAndGet(); // 可能会超过max,但通常不会超出很多,更不可能溢出long最大值
			return nextId <= max ? nextId : 0;
		}

		// 分配范围: [start+1,end]
		public Range(long start, long end) {
			super(start);
			max = end;
		}
	}

	private class RangeLog extends Log {
		private final Range range;

		public RangeLog(Range range) {
			this.range = range;
		}

		@Override
		public Category category() {
			return Category.eSpecial;
		}

		@Override
		public int getTypeId() {
			return 0; // null: 特殊日志，不关联Bean。
		}

		@Override
		public void commit() {
			// 这里直接修改拥有者的引用，开放出去，以后其他事务就能看到新的Range了。
			// 并发：多线程实际上由 _autokeys 表的锁来达到互斥，commit的时候，是互斥锁。
			AutoKeyOld.this.range = range;
		}

		@Override
		public long getLogKey() {
			return AutoKeyOld.this.logKey;
		}
	}
}
