package Zeze.History;

import java.util.concurrent.ConcurrentHashMap;

import Zeze.Builtin.HistoryModule.BLogChanges;
import Zeze.Builtin.HistoryModule.BTableKey;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Database;
import Zeze.Transaction.IHistory;
import Zeze.Transaction.Table;
import Zeze.Util.Id128;
import org.jspecify.annotations.Nullable;

/**
 * 一个事务关联集合（rrs）的tHistory变更缓冲，两段流水线：
 * logChanges（原始对象）--encode0--&gt; encoded（编码字节）--writeOnly--&gt; tHistory库事务。
 * 不变量：构造完成后，容器只在 rrs 锁内合并、编码和核销；条目
 * 离开 logChanges 前必已进 encoded；encoded 只能由 onPersisted 清空——它在数据库事务全部
 * 提交成功后由 Checkpoint.flush 调用，失败回滚后容器保留，重试按系列号幂等重写。
 * mergeFrom 用于事务路径的合并，只改接收者；snapshot 为 FlushSet 创建独立容器，
 * 后续成员合并到该快照。快照的写入/核销集合恒等于本轮成员集合，失败轮不污染成员。
 * <p>
 * 表和 {@link PendingGidLedger} 在创建时绑定，快照保留绑定；合并必须属于同一实例和表，
 * 防止跨 Application 写库或核销数值重叠的 gid。
 */
public class History implements IHistory.PendingHistory {
	private final Table table;
	private final PendingGidLedger ledger;

	// 每个待刷缓冲创建时已有一条历史，快照也复制完整容器，无须再维护惰性字段状态。
	private final ConcurrentHashMap<Id128, BLogChanges.Data> logChanges = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Id128, Binary> encoded = new ConcurrentHashMap<>();

	public History(Table table, PendingGidLedger ledger, BLogChanges.Data firstData) {
		this(table, ledger);
		logChanges.put(firstData.getGlobalSerialId(), firstData);
	}

	private History(Table table, PendingGidLedger ledger) {
		this.table = table;
		this.ledger = ledger;
	}

	@Override
	public Table getTable() {
		return table;
	}

	@Override
	public void encode0() {
		// 锁内。只编码不清空：条目离开 logChanges 前必已进 encoded，但 encoded 的清理
		// 由 onPersisted 绑定数据库事务提交结果——flush 失败回滚后容器保留，重试幂等重写。
		logChanges.forEach((gid, change) -> {
			// 相同系列号内容不变，重试复用已编码字节。
			encoded.computeIfAbsent(change.getGlobalSerialId(), __ -> {
				var bb = ByteBuffer.Allocate();
				change.encode(bb);
				return new Binary(bb);
			});
		});
	}

	/** 把 encoded 写入tHistory事务。只写不清空，清理由 onPersisted 绑定提交结果。 */
	@Override
	public void writeOnly(Database.Table table, Database.Transaction txn) {
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
	@Override
	public void onPersisted() {
		for (var k : encoded.keySet())
			ledger.retire(k);
		for (var k : logChanges.keySet())
			ledger.retire(k);
		logChanges.clear();
		encoded.clear();
	}

	@Override
	public void mergeFrom(IHistory.PendingHistory from) {
		if (!(from instanceof History other) || table != other.table || ledger != other.ledger)
			throw new IllegalArgumentException("history buffers must belong to the same Application and table");
		if (other == this)
			return;
		other.encoded.forEach(encoded::putIfAbsent);
		other.logChanges.forEach((gid, change) -> logChanges.putIfAbsent(change.getGlobalSerialId(), change));
	}

	@Override
	public History snapshot() {
		var copy = new History(table, ledger);
		copy.mergeFrom(this);
		return copy;
	}

	/** 用已解析的 gid 编码日志变更。gid 必须在日志应用前解析并登记对账账本（调用方
	 * HistoryModule.prepareCommit：取号失败=数据未应用即干净失败；成功即入账，此后任何失败
	 * 都留下账本痕迹），tHistory 行随数据库事务提交成功由 onPersisted 核销。 */
	@SuppressWarnings("ConstantConditions")
	public static BLogChanges.Data buildLogChanges(Id128 globalSerialId,
												   Changes changes,
												   @Nullable String protocolClassName,
												   @Nullable Binary protocolArgument) {
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
