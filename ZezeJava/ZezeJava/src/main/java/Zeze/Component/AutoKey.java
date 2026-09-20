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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutoKey extends ReentrantLock {
	public static class Module extends AbstractAutoKey {
		private final ConcurrentHashMap<String, AutoKey> map = new ConcurrentHashMap<>();
		public final @NotNull Application zeze;

		// 这个组件Zeze.Application会自动初始化，不需要应用初始化。
		public Module(@NotNull Application zeze) {
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
		public @NotNull AutoKey getOrAdd(@NotNull String name) {
			return map.computeIfAbsent(name, name2 -> new AutoKey(this, name2));
		}
	}

	private final @NotNull Module module;
	private final @NotNull String name;
	private volatile @Nullable Range range;

	private final TimeAdaptedFund fund = TimeAdaptedFund.getDefaultFund();

	private AutoKey(@NotNull Module module, @NotNull String name) {
		this.module = module;
		this.name = name;
	}

	public @NotNull String getName() {
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

	public byte @NotNull [] nextBytes() {
		return nextByteBuffer().Bytes; // nextByteBuffer的Bytes一定是正好大小的数组
	}

	public @NotNull Binary nextBinary() {
		return new Binary(nextByteBuffer());
	}

	/**
	 * @return base64编码的ID
	 */
	public @NotNull String nextString() {
		return Base64.getEncoder().encodeToString(nextBytes());
	}

	public @NotNull ByteBuffer nextByteBuffer() {
		int serverId = module.zeze.getConfig().getServerId();
		if (serverId < 0) // serverId不应该<0,因为会导致nextId返回负值
			throw new IllegalStateException("AutoKey.nextByteBuffer: serverId(" + serverId + ") < 0");
		var seed = nextSeed();
		int size = ByteBuffer.WriteULongSize(seed);
		if (serverId > 0)
			size += ByteBuffer.WriteUIntSize(serverId);
		var bb = ByteBuffer.Allocate(size);
		if (serverId > 0) // 如果serverId==0,写1个字节0不会影响ToLongBE的结果,但会多占1个字节,所以只在serverId>0时写ByteBuffer
			bb.WriteUInt(serverId);
		bb.WriteULong(seed);
		return bb;
	}

	// 从AutoKey.next|nextId()得到的ID中提取出serverId. 暂不支持serverId=0的情况
	public static int getServerIdFromId(long id) {
		if (id == 0) // 0是"未赋值"最常见值：8字节全零会让下面的跳0循环越过缓冲区（CP1-F4）
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
		if (serverId == 0)
			return setSeed(minId); // WriteULong(minId)再ToLongBE得到的值一定不小于minId,其实还能再选出符合条件的更小seed值,但serverId极少=0,所以不考虑那么多了
		var bb = ByteBuffer.Allocate(8);
		bb.WriteUInt(serverId);
		bb.WriteULong(0);
		long id = ByteBuffer.ToLongBE(bb.Bytes, 0, bb.WriteIndex);
		long seed = 0;
		while (id < minId) {
			if ((id & 0xff80_0000_0000_0000L) != 0) {
				throw new IllegalStateException("AutoKey.setMinId: minId(" + minId
						+ ") is too large for serverId(" + serverId + ')');
			}
			id <<= 8;
			seed = seed == 0 ? 0x80 : seed << 7; // 每多7位,WriteULong序列化就要多1字节
		}
		return setSeed(seed);
	}

	/**
	 * 设置当前serverId的种子，新种子必须比当前值大。
	 * 契约：成功返回后，此后发起的nextId()保证大于等于新水位；与本次调用并发执行的nextId()
	 * 可能取到旧段号（其消耗先于内部的失效发布），需要严格串行的场景应先静默发号再调用。
	 *
	 * @param seed new seed.
	 * @return true if success.
	 * @throws IllegalStateException seed 超出当前serverId的id编码上限（见maxSeedForServerId）。
	 */
	public boolean setSeed(long seed) {
		if (seed > maxSeedForServerId()) // 拒绝把水位投毒到无法编码的区间：之后批段只能永久异常
			throw new IllegalStateException("AutoKey.setSeed: seed(" + seed + ") too large for serverId("
					+ module.zeze.getConfig().getServerId() + ')');
		return raiseWatermark("AutoKey.setSeed", current -> seed > current ? seed : -1);
	}

	/**
	 * 增加当前serverId的种子。只能增加，如果溢出或越过编码上限，返回失败。
	 *
	 * @param delta delta
	 * @return true if success.
	 */
	public boolean increaseSeed(long delta) {
		if (delta <= 0)
			return false;
		var maxSeed = maxSeedForServerId();
		return raiseWatermark("AutoKey.increaseSeed", current -> {
			var newSeed = current + delta;
			return newSeed > 0 && newSeed <= maxSeed ? newSeed : -1; // 溢出（回绕为负）或越过编码上限：失败
		});
	}

	/**
	 * 返回当前serverId的种子。
	 *
	 * @return seed
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

	// 抬表水位（setSeed/increaseSeed）必须持AutoKey实例锁穿越提交点，且失效发布先于事务提交：
	// 若提交后再失效，存在“提交与失效之间”的窗口——快路径在缝里消耗旧段号并通过复核返回，
	// 合服时与存量id重号（FND4-45）。失效先于提交后：发布到提交期间慢路径等锁进不来，不会用
	// 旧水位重装段；快路径复核必失败（消耗作废成空洞，转慢路径）。失败/异常恢复现役段：水位
	// 未动，恢复即原状（窗口内被并发消耗过的号已成空洞，无重号）。
	private boolean raiseWatermark(String action, LongUnaryOperator raiser) {
		lock();
		try {
			var oldRange = range;
			range = null; // 失效发布先于提交
			try {
				var success = Procedure.Success == TaskSpec.ofProcedure(module.zeze.newProcedure(() -> {
					var seedKey = new BSeedKey(module.zeze.getConfig().getServerId(), name);
					var bAutoKey = module._tAutoKeys.getOrAdd(seedKey);
					var newSeed = raiser.applyAsLong(bAutoKey.getNextId()); // 负数=不抬（失败）
					if (newSeed < 0)
						return Procedure.LogicError;
					bAutoKey.setNextId(newSeed);
					return 0;
				}, action)).dispatchMode(DispatchMode.Critical).submitNow().get();
				if (!success)
					range = oldRange;
				return success;
			} catch (InterruptedException | ExecutionException e) {
				range = oldRange;
				throw Task.forceThrow(e);
			}
		} finally {
			unlock();
		}
	}

	// 当前serverId下id编码（nextByteBuffer：serverId前缀+seed变长大端拼装，总长≤8字节，每字节
	// 7位负载）可表达的seed上限。serverId==0或≥0x80时，8字节总长的首字节≥0x80，ToLongBE按
	// 有符号大端读会得到负数，违反nextId“返回值一定为正”的约定，故再保守少用1字节。
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
				// 复核号段仍有效再返回：失效可能发生在读取引用与消耗之间，此时旧段号作废成空洞，
				// 转慢路径按新表水位重新批段。
				//noinspection NumberEquality
				if (next != 0 && range == localRange)
					return next; // allocate in range success
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
						// 水位越过编码上限（真耗尽）或为负（表被旧版本越界setSeed投毒）：明确报错，
						// 不再批出无法编码的号段
						if (start < 0 || start >= maxSeed) {
							exhausted.value = true;
							return Procedure.LogicError;
						}
						// allocateCount == 0 会死循环；段尾夹在编码预算内，同时杜绝 start+count 的long溢出
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
