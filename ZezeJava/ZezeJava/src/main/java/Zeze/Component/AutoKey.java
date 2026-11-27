package Zeze.Component;

import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongUnaryOperator;
import Zeze.Application;
import Zeze.Builtin.AutoKey.BSeedKey;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.OutLong;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import Zeze.Util.TimeAdaptedFund;
import org.jspecify.annotations.Nullable;

/**
 * 全局唯一ID发号器：serverId前缀+seed变长大端编码，按号段批量发号。
 */
public class AutoKey extends ReentrantLock {
	public static class Module extends AbstractAutoKey {
		private final ConcurrentHashMap<String, AutoKey> map = new ConcurrentHashMap<>();
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
		public AutoKey getOrAdd(String name) {
			return map.computeIfAbsent(name, name2 -> new AutoKey(this, name2));
		}
	}

	private final Module module;
	private final String name;
	private volatile @Nullable Range range;

	private final TimeAdaptedFund fund = TimeAdaptedFund.getDefaultFund();

	private AutoKey(Module module, String name) {
		this.module = module;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public int getAllocateCount() {
		return fund.get();
	}

	public long next() {
		return nextId();
	}

	public long nextId() {
		var bb = nextByteBuffer(); // seed受maxSeedForServerId预算约束（nextSeed源头夹取），编码总长≤8字节且不越符号位
		return ByteBuffer.ToLongBE(bb.Bytes, 0, bb.WriteIndex); // 这里用BE(大端)是为了保证返回值一定为正,且保证ID值随seed的增长而增长
	}

	public byte [] nextBytes() {
		return nextByteBuffer().Bytes; // nextByteBuffer的Bytes一定是正好大小的数组
	}

	public Binary nextBinary() {
		return new Binary(nextByteBuffer());
	}

	/**
	 * @return base64编码的ID
	 */
	public String nextString() {
		return Base64.getEncoder().encodeToString(nextBytes());
	}

	public ByteBuffer nextByteBuffer() {
		int serverId = module.zeze.getConfig().getServerId();
		if (serverId < 0) // serverId不应该<0,因为会导致nextId返回负值
			throw new IllegalStateException("AutoKey.nextByteBuffer: serverId(" + serverId + ") < 0");
		var seed = nextSeed();
		int size = ByteBuffer.WriteULongSize(seed);
		if (serverId > 0)
			size += ByteBuffer.WriteUIntSize(serverId);
		var bb = ByteBuffer.Allocate(size);
		writeId(bb, serverId, seed);
		return bb;
	}

	private static void writeId(ByteBuffer bb, int serverId, long seed) {
		if (serverId > 0) // 如果serverId==0,写1个字节0不会影响ToLongBE的结果,但会多占1个字节,所以只在serverId>0时写ByteBuffer
			bb.WriteUInt(serverId);
		bb.WriteULong(seed);
	}

	// 从AutoKey.next|nextId()得到的ID中提取出serverId. 暂不支持serverId=0的情况
	public static int getServerIdFromId(long id) {
		if (id == 0) // 0是"未赋值"最常见值：8字节全零会让下面的跳0循环越过缓冲区
			throw new IllegalArgumentException("AutoKey.getServerIdFromId: id must not be 0");
		var bb = ByteBuffer.Allocate(8);
		bb.WriteLong8BE(id);
		while (bb.Bytes[bb.ReadIndex] == 0) // 跳过前面的0值字节
			bb.ReadIndex++;
		int serverId = bb.ReadUInt();
		bb.SkipULong();
		if (bb.ReadIndex != bb.WriteIndex) // 检查是否合法生成的ID
			throw new IllegalArgumentException("AutoKey.getServerIdFromId: not a valid AutoKey id");
		return serverId;
	}

	/**
	 * 设置最小的ID值, 使下次nextId()的结果不小于此值
	 */
	public boolean setMinId(long minId) {
		int serverId = module.zeze.getConfig().getServerId();
		if (serverId < 0) // serverId不应该<0,因为会导致nextId返回负值
			throw new IllegalStateException("AutoKey.setMinId: serverId(" + serverId + ") < 0");
		var bb = ByteBuffer.Allocate(8);
		long low = 1;
		long high = maxSeedForServerId();
		if (encodeId(bb, serverId, high) < minId)
			throw new IllegalStateException("AutoKey.setMinId: minId(" + minId
					+ ") is too large for serverId(" + serverId + ')');
		// 同一 serverId 的编码随 seed 单调递增，找首个达到目标的可发种子。
		while (low < high) {
			var mid = low + (high - low) / 2;
			if (encodeId(bb, serverId, mid) < minId)
				low = mid + 1;
			else
				high = mid;
		}
		return setSeed(low - 1); // 水位是已分配号段的末尾，下一号从水位 + 1 开始。
	}

	private static long encodeId(ByteBuffer bb, int serverId, long seed) {
		bb.WriteIndex = 0;
		writeId(bb, serverId, seed);
		return ByteBuffer.ToLongBE(bb.Bytes, 0, bb.WriteIndex);
	}

	/**
	 * 设置当前 serverId 的种子，新种子必须比当前值大。
	 * 成功返回后，此后发起的 nextId() 保证不小于新水位；与本次调用并发的 nextId() 可能仍取到旧段号。
	 *
	 * @param seed 新种子
	 * @return 是否设置成功
	 * @throws IllegalStateException seed 超出当前 serverId 的编码上限
	 */
	public boolean setSeed(long seed) {
		if (seed > maxSeedForServerId())
			throw new IllegalStateException("AutoKey.setSeed: seed(" + seed + ") too large for serverId("
					+ module.zeze.getConfig().getServerId() + ')');
		return raiseWatermark("AutoKey.setSeed", current -> seed > current ? seed : -1);
	}

	/**
	 * 增加当前 serverId 的种子；若溢出或越过编码上限则返回 false。
	 *
	 * @param delta 增量
	 * @return 是否增加成功
	 */
	public boolean increaseSeed(long delta) {
		if (delta <= 0)
			return false;
		var maxSeed = maxSeedForServerId();
		return raiseWatermark("AutoKey.increaseSeed", current -> {
			var newSeed = current + delta;
			return newSeed > 0 && newSeed <= maxSeed ? newSeed : -1;
		});
	}

	/**
	 * 返回当前 serverId 的种子。
	 *
	 * @return 当前种子
	 */
	public long getSeed() {
		long ret;
		try {
			var result = new OutLong();
			ret = TaskSpec.ofProcedure(module.zeze.newProcedure(() -> {
				var seedKey = new BSeedKey(module.zeze.getConfig().getServerId(), name);
				var bAutoKey = module._tAutoKeys.getOrAdd(seedKey);
				result.value = bAutoKey.getNextId();
				return 0;
			}, "AutoKey.getSeed")).dispatchMode(DispatchMode.Critical).submitNow().get();
			if (ret == Procedure.Success)
				return result.value;
		} catch (InterruptedException | ExecutionException e) {
			throw Task.forceThrow(e);
		}
		throw new IllegalStateException("AutoKey.getSeed failed: " + ret);
	}

	// 持实例锁抬高表水位，且先失效本地号段再提交事务（允许空洞，不允许重号）；仅确认事务失败时恢复旧段
	private boolean raiseWatermark(String action, LongUnaryOperator raiser) {
		boolean interrupted = false;
		lock();
		try {
			var oldRange = range;
			range = null; // 失效发布先于提交
			try {
				var future = TaskSpec.ofProcedure(module.zeze.newProcedure(() -> {
					var seedKey = new BSeedKey(module.zeze.getConfig().getServerId(), name);
					var bAutoKey = module._tAutoKeys.getOrAdd(seedKey);
					var newSeed = raiser.applyAsLong(bAutoKey.getNextId()); // 负数表示不抬高（失败）
					if (newSeed < 0)
						return Procedure.LogicError;
					bAutoKey.setNextId(newSeed);
					return 0;
				}, action)).dispatchMode(DispatchMode.Critical).submitNow();
				long result;
				while (true) {
					try {
						result = future.get();
						break;
					} catch (InterruptedException e) {
						// 等待中断不会取消写库任务，须持锁等其结束后再决定恢复或重批
						interrupted = true;
					}
				}
				var success = result == Procedure.Success;
				if (!success)
					range = oldRange;
				return success;
			} catch (ExecutionException e) {
				throw Task.forceThrow(e);
			}
		} finally {
			unlock();
			if (interrupted)
				Thread.currentThread().interrupt();
		}
	}

	// 当前 serverId 下大端拼接编码（总长≤8字节且保证非负）允许的最大 seed
	private long maxSeedForServerId() {
		int serverId = module.zeze.getConfig().getServerId();
		int n = 8 - (serverId > 0 ? ByteBuffer.WriteUIntSize(serverId) : 0);
		if (serverId == 0 || serverId >= 0x80)
			n--;
		return (1L << (7 * n)) - 1;
	}

	private long nextSeed() {
		var maxSeed = maxSeedForServerId();
		while (true) {
			var localRange = range;
			if (localRange != null) {
				var next = localRange.tryNextId();
				// 复核号段仍有效，防止与 raiseWatermark 失效并发返回旧段号
				//noinspection NumberEquality
				if (next != 0 && range == localRange)
					return next;
			}

			lock();
			try {
				//noinspection NumberEquality
				if (range != localRange)
					continue;
				long ret;
				try {
					var newRange = new OutObject<Range>();
					var exhausted = new OutObject<Boolean>();
					ret = TaskSpec.ofProcedure(module.zeze.newProcedure(() -> {
						Transaction.whileCommit(fund::next);

						var seedKey = new BSeedKey(module.zeze.getConfig().getServerId(), name);
						var key = module._tAutoKeys.getOrAdd(seedKey);
						var start = key.getNextId();
						if (start < 0 || start >= maxSeed) {
							exhausted.value = true;
							return Procedure.LogicError;
						}
						// 段尾限制在编码上限内，同时避免 start + count 溢出
						var end = Math.min(start + fund.get(), maxSeed);
						key.setNextId(end);
						newRange.value = new Range(start, end);
						return 0;
					}, "AutoKey.allocateSeeds")).dispatchMode(DispatchMode.Critical).submitNow().get();
					if (ret == Procedure.Success) {
						range = newRange.value;
						continue;
					}
					if (exhausted.value != null)
						throw new IllegalStateException("AutoKey.nextSeed exhausted: serverId="
								+ module.zeze.getConfig().getServerId() + ", name=" + name
								+ ", maxSeed=" + maxSeed);
				} catch (InterruptedException | ExecutionException e) {
					throw Task.forceThrow(e);
				}
				throw new IllegalStateException("AutoKey.nextSeed failed: " + ret);
			} finally {
				unlock();
			}
		}
	}

	private static final class Range extends AtomicLong {
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
