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
 * 事务关联集合（rrs）的 tHistory 变更缓冲，按 logChanges → encode0 → encoded → writeOnly 两段流水线落库。
 * 合并、编码与核销均在 rrs 锁内执行，encoded 仅在数据库事务提交后由 {@link #onPersisted()} 清空并核销 {@link PendingGidLedger}。
 * 缓冲与所属表及账本绑定，{@link #snapshot()} 保留绑定以隔离失败轮次。
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

	/** 数据库事务全部提交成功后核销对账账本并清空缓冲。 */
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

	/** 使用已在应用数据前解析并入账的 gid 编码日志变更；落库成功后由 onPersisted 核销。 */
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
