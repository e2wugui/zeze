package Zeze.History;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.HistoryModule.BLogChanges;
import Zeze.Builtin.HistoryModule.BTableKey;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Database;
import Zeze.Util.Id128;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 一个事务关联集合（rrs）的tHistory变更缓冲，两段流水线：
 * logChanges（原始对象）--encode0--&gt; encoded（编码字节）--writeOnly--&gt; tHistory库事务。
 * 不变量：容器只被 owner rrs 的 addLogChanges/encode0/commitDone 变更（rrs 锁内）；条目
 * 离开 logChanges 前必已进 encoded；encoded 只能由 commitDone 清空——它在数据库事务全部
 * 提交成功后由 Checkpoint.flush 调用，失败回滚后容器保留，重试按系列号幂等重写。
 * merge 仅用于事务路径的所有权转移（from 即死，条目归幸存 rrs 所有）；combine 用于
 * FlushSet 的 flush 组装只读快照（绝不写参数容器、绝不共享 map 引用）——快照的写入/核销
 * 集合恒等于本轮成员集合，失败轮不污染成员自有容器。
 *
 * 已发 gid 的落库对账账本见 {@link PendingGidLedger}：随 register/commitDone 由调用方
 * 携带（Application 实例维度），本类不持有账本状态。
 */
public class History {

	// 为了节约内存，在确实需要的时候才分配。
	// 容器访问已全部在 rrs 锁内，并发Map非必需，改动无收益。
	private volatile @Nullable ConcurrentHashMap<Id128, BLogChanges.Data> logChanges;

	private final ConcurrentHashMap<Id128, Binary> encoded = new ConcurrentHashMap<>();

	public History(@NotNull BLogChanges.Data firstData) {
		addLogChanges(firstData);
	}

	/** 仅供 {@link #combine} 组装快照用：空容器起步，条目由收编填充。 */
	private History() {
	}

	public void addLogChanges(@NotNull BLogChanges.Data _logChanges) {
		var logChanges = this.logChanges;
		if (logChanges == null)
			this.logChanges = logChanges = new ConcurrentHashMap<>();
		logChanges.put(_logChanges.getGlobalSerialId(), _logChanges);
	}

	public void encode0() {
		// 锁内。只编码不清空：条目离开 logChanges 前必已进 encoded，但 encoded 的清理
		// 由 commitDone 绑定数据库事务提交结果——flush 失败回滚后容器保留，重试幂等重写。
		var changes = logChanges;
		if (changes != null) {
			changes.forEach((key, v) -> {
				// logChanges只要系列号一样，表示内容一样，所以，只要key存在，不需要再encode一次。
				encoded.computeIfAbsent(v.getGlobalSerialId(), __ -> {
					var bb = ByteBuffer.Allocate();
					v.encode(bb);
					return new Binary(bb);
				});
			});
		}
	}

	/** 把 encoded 写入tHistory事务。只写不清空，清理由 commitDone 绑定提交结果。 */
	public void writeOnly(@NotNull Database.Table table, @NotNull Database.Transaction txn) {
		// 但仅仅Checkpoint访问，不需要加锁。现实也在锁内。
		for (var e : encoded.entrySet()) {
			var key = ByteBuffer.Allocate();
			e.getKey().encode(key);
			var value = ByteBuffer.Wrap(e.getValue());
			table.replace(txn, key, value);
		}
	}

	/** 数据库事务全部提交成功后调用：tHistory 行已持久化，容器可以安全清空。
	 * 清空前按 gid 核销对账账本（Application 实例维度）：同一数值 gid 在不同 app 间互不干扰。 */
	public void commitDone(@NotNull PendingGidLedger ledger) {
		for (var k : encoded.keySet())
			ledger.retire(k);
		var changes = logChanges;
		if (changes != null) {
			for (var k : changes.keySet())
				ledger.retire(k);
			changes.clear();
		}
		encoded.clear();
	}

	public static void putLogChangesAll(@NotNull ConcurrentHashMap<Id128, BLogChanges.Data> to,
										@NotNull ConcurrentHashMap<Id128, BLogChanges.Data> other) {
		other.forEach((key, v) -> to.putIfAbsent(v.getGlobalSerialId(), v));
	}

	public static void putEncodedAll(@NotNull ConcurrentHashMap<Id128, Binary> to,
									 @NotNull ConcurrentHashMap<Id128, Binary> other) {
		other.forEach(to::putIfAbsent);
	}

	// merge 仅用于事务路径（RelativeRecordSet.merge）的所有权转移：from 的 rrs 即死
	// （mergeTo 指向幸存者），条目归幸存 rrs 所有，允许写入 to 的持久容器。
	// FlushSet 的组组装不得走这里——见 combine。
	public static @Nullable History merge(@Nullable History to, @Nullable History from) {
		// rrs 锁内
		if (to == null)
			return from; // still maybe null. 直接全部接管。

		// 合并encoded
		if (from != null)
			putEncodedAll(to.encoded, from.encoded);

		// 合并logChanges
		var toLogChanges = to.logChanges;
		if (toLogChanges == null) {
			// 拷贝收编，不共享 map 引用：别名会让幸存者 commitDone 的清空波及死者容器。
			var fromLogChanges = from == null ? null : from.logChanges; // still maybe null
			if (fromLogChanges != null) {
				to.logChanges = new ConcurrentHashMap<>();
				putLogChangesAll(to.logChanges, fromLogChanges);
			}
			return to;
		}

		if (from != null) {
			var fromLogChanges = from.logChanges;
			if (fromLogChanges != null)
				putLogChangesAll(toLogChanges, fromLogChanges);
		}
		return to;
	}

	/** FlushSet 组快照组装：把 a、b 的条目收编进全新容器返回快照，绝不写参数容器、
	 * 绝不共享 map 引用（putIfAbsent 语义与 merge 一致，gid 相同即同内容）。失败轮回滚
	 * 后成员 rrs 的自有容器原样保留，重试轮无论怎么重分组，每个快照的写入/核销集合都
	 * 恒等于本轮成员集合——不会带出幽灵 tHistory 行、不会跨组核销别人的 gid。 */
	public static @Nullable History combine(@Nullable History a, @Nullable History b) {
		// rrs 锁内（调用方 FlushSet.flush 持有全部成员锁）
		if (a == null && b == null)
			return null;
		var combined = new History();
		if (a != null)
			combined.absorb(a);
		if (b != null)
			combined.absorb(b);
		return combined;
	}

	/** 快照收编：把 other 的 encoded/logChanges 条目浅拷贝进本容器（putIfAbsent）。 */
	private void absorb(@NotNull History other) {
		putEncodedAll(encoded, other.encoded);
		var otherLogChanges = other.logChanges;
		if (otherLogChanges != null) {
			var logChanges = this.logChanges;
			if (logChanges == null)
				this.logChanges = logChanges = new ConcurrentHashMap<>();
			putLogChangesAll(logChanges, otherLogChanges);
		}
	}

	/** globalSerialId 必须在日志应用（finalCommit 的 commit.run）之前解析：取号失败时数据
	 * 未应用、事务干净失败，历史与数据同生共死（调用方 Transaction.finalCommit 的
	 * HistoryChangesCollector.beforeApply）。gid 在此消费即入对账账本（Application 实例
	 * 维度），随 tHistory 行提交成功由 commitDone 核销。 */
	public static @NotNull BLogChanges.Data buildLogChanges(@NotNull PendingGidLedger ledger,
															@NotNull Id128 globalSerialId,
															@NotNull Changes changes,
															@Nullable String protocolClassName,
															@Nullable Binary protocolArgument) {
		ledger.register(globalSerialId, System.nanoTime()); // 单调基入账，与 sweep 判龄同基
		var logChanges = new BLogChanges.Data();
		if (protocolClassName != null)
			logChanges.setProtocolClassName(protocolClassName);
		if (protocolArgument != null)
			logChanges.setProtocolArgument(protocolArgument);
		for (var e : changes.getRecords().entrySet()) {
			var value = e.getValue();
			var table = value.getTable();
			if (table != null && !table.isMemory()) { // 内存表的日志变更不需要持久化，直接忽略。
				var key = e.getKey();
				var tableKey = new BTableKey(key.getId(), new Binary(table.encodeKey(key.getKey())));
				var bbValue = ByteBuffer.Allocate();
				value.encode(bbValue);
				logChanges.getChanges().put(tableKey, new Binary(bbValue));
			}
		}
		logChanges.setTimestamp(System.currentTimeMillis());
		logChanges.setGlobalSerialId(globalSerialId);
		return logChanges;
	}
}
