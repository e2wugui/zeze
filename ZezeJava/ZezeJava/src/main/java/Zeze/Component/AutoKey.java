package Zeze.Component;

import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Builtin.AutoKey.BSeedKey;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Log;
import Zeze.Transaction.Savepoint;
import Zeze.Transaction.Transaction;

public final class AutoKey {
	public static class Module extends AbstractAutoKey {
		private final ConcurrentHashMap<String, AutoKey> map = new ConcurrentHashMap<>();
		public final Zeze.Application Zeze;

		// 这个组件Zeze.Application会自动初始化，不需要应用初始化。
		public Module(Zeze.Application zeze) {
			Zeze = zeze;
			RegisterZezeTables(zeze);
		}

		@Override
		public void UnRegister() {
			UnRegisterZezeTables(Zeze);
		}

		/**
		 * 这个返回值，可以在自己模块内保存下来，效率高一些。
		 */
		public AutoKey getOrAdd(String name) {
			return map.computeIfAbsent(name, name2 -> new AutoKey(this, name2));
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

	@SuppressWarnings("deprecation")
	private AutoKey(Module module, String name) {
		this.module = module;
		this.name = name;
		// 详细参考Bean的Log的用法。这里只有一个variable。
		logKey = Zeze.Transaction.Bean.nextObjectId();
	}

	public int getAllocateCount() {
		return allocateCount;
	}

	public long nextId() {
		var bb = nextByteBuffer();
		if (bb.Size() > 8)
			throw new IllegalStateException("out of range: serverId=" + module.Zeze.getConfig().getServerId()
					+ ", nextId=" + bb);
		return ByteBuffer.ToLongBE(bb.Bytes, bb.ReadIndex, bb.Size()); // 这里用BE(大端)是为了保证返回值一定为正
	}

	public byte[] nextBytes() {
		return nextByteBuffer().Copy();
	}

	public Binary nextBinary() {
		return new Binary(nextByteBuffer());
	}

	public String nextString() {
		return Base64.getEncoder().encodeToString(nextBytes());
	}

	public ByteBuffer nextByteBuffer() {
		var serverId = module.Zeze.getConfig().getServerId();
		var bb = ByteBuffer.Allocate(8);
		if (serverId > 0) // 如果serverId==0,写1个字节0不会影响ToLongBE的结果,但会多占1个字节,所以只在serverId>0时写ByteBuffer
			bb.WriteInt(serverId);
		else if (serverId < 0) // serverId不应该<0,因为会导致nextId返回负值
			throw new IllegalStateException("serverId(" + serverId + ") < 0");
		bb.WriteLong(nextSeed());
		return bb;
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

		var seedKey = new BSeedKey(module.Zeze.getConfig().getServerId(), name);
		var txn = Transaction.getCurrent();
		assert txn != null;
		var log = (RangeLog)txn.GetLog(logKey);
		while (true) {
			if (null == log) {
				// allocate: 多线程，多事务，多服务器（缓存同步）由zeze保证。
				var key = module._tAutoKeys.getOrAdd(seedKey);
				var start = key.getNextId();
				var end = start + allocateCount; // allocateCount == 0 会死循环。
				key.setNextId(end);
				// create log，本事务可见，
				log = new RangeLog(new Range(start, end));
				txn.PutLog(log);
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

	private class RangeLog extends Zeze.Transaction.Log {
		private final Range range;

		public RangeLog(Range range) {
			super(0); // null: 特殊日志，不关联Bean。
			this.range = range;
		}

		@Override
		public void commit() {
			// 这里直接修改拥有者的引用，开放出去，以后其他事务就能看到新的Range了。
			// 并发：多线程实际上由 _autokeys 表的锁来达到互斥，commit的时候，是互斥锁。
			AutoKey.this.range = range;
		}

		@Override
		public long getLogKey() {
			return AutoKey.this.logKey;
		}

		@Override
		public void endSavepoint(Savepoint currentSp) {
			currentSp.putLog(this);
		}

		@Override
		public Log beginSavepoint() {
			return this;
		}

		@Override
		public void encode(ByteBuffer bb) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void decode(ByteBuffer bb) {
			throw new UnsupportedOperationException();
		}
	}
}
