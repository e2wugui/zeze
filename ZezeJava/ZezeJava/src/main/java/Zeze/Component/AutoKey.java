package Zeze.Component;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.AutoKey.BSeedKey;
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
			return getOrAdd(name, DefaultAllocateCount);
		}
		public AutoKey getOrAdd(String name, int allocateCount) {
			if (allocateCount <= 0)
				throw new IllegalArgumentException("allocateCount <= 0");
			return map.computeIfAbsent(name, name2 -> new AutoKey(this, name2, allocateCount));
		}
	}

	private static final int DefaultAllocateCount = 500;

	private final Module module;
	private final String name;
	private volatile Range range;
	private final long logKey;
	private final int allocateCount;

	@SuppressWarnings("deprecation")
	private AutoKey(Module module, String name, int allocateCount) {
		this.module = module;
		this.name = name;
		this.allocateCount = allocateCount;
		// 详细参考Bean的Log的用法。这里只有一个variable。
		logKey = Zeze.Transaction.Bean.nextObjectId();
	}

	public long nextId() {
		var bytes = nextBytes();
		if (bytes.length > 8)
			throw new IllegalStateException("out of range");
		return ByteBuffer.ToLong(bytes, 0, bytes.length);
	}

	public byte[] nextBytes() {
		return nextByteBuffer().Copy();
	}

	public String nextString() {
		return Base64.getEncoder().encodeToString(nextBytes());
	}

	public ByteBuffer nextByteBuffer() {
		var bb = ByteBuffer.Allocate(16);
		bb.WriteInt(module.Zeze.getConfig().getServerId());
		bb.WriteLong(nextSeed());
		return bb;
	}

	private long nextSeed() {
		if (null != range) {
			var next = range.tryNextId();
			if (next != 0)
				return next; // allocate in range success
		}

		var seedKey = new BSeedKey(module.Zeze.getConfig().getServerId(), name);
		var txn = Transaction.getCurrent();
		assert txn != null;
		var log = (RangeLog)txn.GetLog(logKey);
		while (true) {
			if (null == log) {
				// allocate: 多线程，多事务，多服务器（缓存同步）由zeze保证。
				var key = module._tAutoKeys.getOrAdd(seedKey);
				var start = key.getNextId();
				var end = start + allocateCount; // AllocateCount == 0 会死循环。
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

	private static class Range {
		private static final VarHandle nextIdHandle;

		static {
			try {
				nextIdHandle = MethodHandles.lookup().findVarHandle(Range.class, "nextId", long.class);
			} catch (ReflectiveOperationException e) {
				throw new ExceptionInInitializerError(e);
			}
		}

		@SuppressWarnings("FieldMayBeFinal")
		private volatile long nextId;
		private final long max;

		public long tryNextId() {
			long lastId = (long)nextIdHandle.getAndAdd(this, 1L);
			return lastId < max ? lastId + 1 : 0;
		}

		public Range(long start, long end) {
			nextId = start;
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
		public void Commit() {
			// 这里直接修改拥有者的引用，开放出去，以后其他事务就能看到新的Range了。
			// 并发：多线程实际上由 _autokeys 表的锁来达到互斥，commit的时候，是互斥锁。
			AutoKey.this.range = range;
		}

		@Override
		public long getLogKey() {
			return AutoKey.this.logKey;
		}

		@Override
		public void EndSavepoint(Savepoint currentsp) {
			currentsp.PutLog(this);
		}

		@Override
		public Log BeginSavepoint() {
			return this;
		}

		@Override
		public void Encode(ByteBuffer bb) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void Decode(ByteBuffer bb) {
			throw new UnsupportedOperationException();
		}
	}
}
