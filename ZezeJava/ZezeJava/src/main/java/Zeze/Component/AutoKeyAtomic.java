package Zeze.Component;

import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Builtin.AutoKeyAtomic.BSeedKey;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Transaction;
import Zeze.Util.OutLong;
import Zeze.Util.OutObject;
import Zeze.Util.Task;

public class AutoKeyAtomic {
	public static class Module extends AbstractAutoKeyAtomic {
		private final ConcurrentHashMap<String, AutoKeyAtomic> map = new ConcurrentHashMap<>();
		public final Zeze.Application zeze;

		// 这个组件Zeze.Application会自动初始化，不需要应用初始化。
		public Module(Zeze.Application zeze) {
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
		public AutoKeyAtomic getOrAdd(String name) {
			return map.computeIfAbsent(name, name2 -> new AutoKeyAtomic(this, name2));
		}
	}

	private static final int ALLOCATE_COUNT_MIN = 64;
	private static final int ALLOCATE_COUNT_MAX = 1024 * 1024;

	private final Module module;
	private final String name;
	private volatile AutoKeyAtomic.Range range;

	private int allocateCount = ALLOCATE_COUNT_MIN;
	private long lastAllocateTime = System.currentTimeMillis();

	private AutoKeyAtomic(Module module, String name) {
		this.module = module;
		this.name = name;
	}

	public int getAllocateCount() {
		return allocateCount;
	}

	public long nextId() {
		var bb = nextByteBuffer();
		if (bb.Size() > 8)
			throw new IllegalStateException("out of range: serverId=" + module.zeze.getConfig().getServerId()
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
		var serverId = module.zeze.getConfig().getServerId();
		var bb = ByteBuffer.Allocate(8);
		if (serverId > 0) // 如果serverId==0,写1个字节0不会影响ToLongBE的结果,但会多占1个字节,所以只在serverId>0时写ByteBuffer
			bb.WriteInt(serverId);
		else if (serverId < 0) // serverId不应该<0,因为会导致nextId返回负值
			throw new IllegalStateException("serverId(" + serverId + ") < 0");
		bb.WriteLong(nextSeed());
		return bb;
	}

	/**
	 * 设置当前serverId的种子，新种子必须比当前值大。
	 * @param seed new seed.
	 * @return true if success.
	 */
	public boolean setSeed(long seed) {
		var result = new OutObject<>(false);
		long ret = 0;
		try {
			ret = Task.runUnsafe(module.zeze.newProcedure(() -> {
				var seedKey = new BSeedKey(module.zeze.getConfig().getServerId(), name);
				var bAutoKey = module._tAutoKeys.getOrAdd(seedKey);
				if (seed > bAutoKey.getNextId()) {
					bAutoKey.setNextId(seed);
					result.value = true;
				}
				return 0;
			}, "")).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
		if (ret == 0)
			return result.value;
		throw new RuntimeException("set seed error.");
	}

	/**
	 * 增加当前serverId的种子。只能增加，如果溢出，返回失败。
	 * @param delta delta
	 * @return true if success.
	 */
	public boolean increaseSeed(long delta) {
		if (delta <= 0)
			return false;
		var result = new OutObject<>(false);
		long ret = 0;
		try {
			ret = Task.runUnsafe(module.zeze.newProcedure(() -> {
				var seedKey = new BSeedKey(module.zeze.getConfig().getServerId(), name);
				var bAutoKey = module._tAutoKeys.getOrAdd(seedKey);
				var newSeed = bAutoKey.getNextId() + delta;
				if (newSeed > 0) {
					bAutoKey.setNextId(newSeed);
					result.value = true;
				}
				// 溢出
				return 0;
			}, "increase seed")).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
		if (ret == 0)
			return result.value;
		throw new RuntimeException("increase seed error.");
	}

	/**
	 * 返回当前serverId的种子。
	 * @return seed
	 */
	public long getSeed() {
		long ret = 0;
		var result = new OutLong();
		try {
			ret = Task.runUnsafe(module.zeze.newProcedure(() -> {
				var seedKey = new BSeedKey(module.zeze.getConfig().getServerId(), name);
				var bAutoKey = module._tAutoKeys.getOrAdd(seedKey);
				result.value = bAutoKey.getNextId();
				return 0;
			}, "get")).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
		if (ret == 0)
			return result.value;
		throw new RuntimeException("get seed error.");
	}

	private long nextSeed() {
		while (true) {
			if (null != range) {
				var next = range.tryNextId();
				if (next != 0)
					return next; // allocate in range success
			}

			synchronized (this) {
				long ret = 0;
				var newRange = new OutObject<Range>();
				try {
					ret = Task.runUnsafe(module.zeze.newProcedure(() -> {
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
						var key = module._tAutoKeys.getOrAdd(seedKey);
						var start = key.getNextId();
						var end = start + allocateCount; // allocateCount == 0 会死循环。
						key.setNextId(end);
						newRange.value = new Range(start, end);
						return 0;
					}, "allocate")).get();
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e);
				}
				if (0 == ret) {
					range = newRange.value;
					continue;
				}
				throw new RuntimeException("allocate range error.");
			}
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
}
