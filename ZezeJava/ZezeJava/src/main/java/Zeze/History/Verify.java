package Zeze.History;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import Zeze.Application;
import Zeze.Builtin.HistoryModule.BLogChanges;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.GenericBean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Transaction.TableKey;
import Zeze.Util.Id128;
import Zeze.Util.OutObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * tHistory 回放全量校验：独立重放历史到内存库，逐表比对业务库与回放副本。
 */
public class Verify {
	private static final Logger logger = LogManager.getLogger(Verify.class);

	/**
	 * 校验历史回放与源表一致。调用前须保证所有 app 已 checkpoint 且已停写，
	 * 仅校验 tHistory 中出现过且历史覆盖全生命周期的表。
	 */
	public static void run(Application zeze) throws Exception {
		var applyDb = new ApplyDatabaseMemory();
		var applyTables = new ConcurrentHashMap<Integer, ApplyTable<?, ?>>();
		// 仅触发当前 app 的 checkpoint；多 app 或未停写时 walkDatabase 与 cache+db 合并视图不一致会误报缺失。
		zeze.checkpointRun();
		var counter = new AtomicLong();
		var total = new AtomicLong();
		// 使用 null 哨兵而非零值 Id128，因 (0,0) 为合法首个 gid。
		var lastK = new OutObject<Id128>();
		HistoryModule.get(zeze).getHistoryTable().walkDatabase((key, value) -> {
			if (lastK.value != null && lastK.value.compareTo(key) >= 0) {
				logger.error("out of Id128 order: {}, {}", lastK.value, key);
				throw new RuntimeException("out of Id128 order: " + lastK.value + ", " + key);
			}
			lastK.value = key;

			for (var r : value.getChanges().entrySet()) {
				var applyTable = applyTables.computeIfAbsent(r.getKey().getTableId(), __ -> {
					var tableName = TableKey.tables.get(r.getKey().getTableId());
					if (tableName == null)
						throw new RuntimeException("table id not found. id=" + r.getKey().getTableId());
					logger.info("history apply table {}", tableName);
					// 通过 zeze.getTable 全库查找，兼容配置在非默认命名数据库中的业务表。
					var originTable = zeze.getTable(tableName);
					if (originTable == null)
						throw new RuntimeException("table not found. name=" + tableName);
					return ApplyTable.create(originTable, applyDb);
				});
				try {
					applyTable.apply(r.getKey(), r.getValue());
				} catch (Exception e) {
					throw new RuntimeException(String.format("apply(%d-%d:%s) exception", key.getHigh(), key.getLow(),
						applyTable.getOriginTable().decodeKey(ByteBuffer.Wrap(r.getKey().getKeyEncoded()))), e);
				}
			}
			var process = counter.incrementAndGet();
			if (process >= 50000) {
				logger.info("history applying ................. {}", total.addAndGet(process));
				counter.set(0);
			}
			return true;
		});
		var process = counter.incrementAndGet();
		logger.info("history apply end! +++++++++++++++++ {}", total.addAndGet(process));
		for (var applyTable : applyTables.values())
			applyTable.verifyAndClear();
		logger.info("history verify success!!!!!!!!!!!!!!!!!!!!!!");
	}

	public static String toString(BLogChanges.Data b) {
		var sb = new StringBuilder();
		sb.append("{\n");
		sb.append("  GlobalSerialId: ").append(b.getGlobalSerialId()).append('\n');
		sb.append("  ProtocolClassName: ").append(b.getProtocolClassName()).append('\n');
		sb.append("  Timestamp: ").append(b.getTimestamp()).append('\n');
		sb.append("  GlobalSerialId: ").append(b.getGlobalSerialId()).append('\n');
		sb.append("  Changes: {\n");
		for (var e : b.getChanges().entrySet()) {
			sb.append("    {").append(e.getKey().getTableId()).append(',').append(e.getKey().getKeyEncoded())
				.append("}: ");
			var bb = ByteBuffer.Wrap(e.getValue());
			switch (bb.ReadUInt()) {
			case Changes.Record.Remove:
				sb.append("remove }\n");
				break;
			case Changes.Record.Put:
				sb.append("put:\n");
				new GenericBean().decode(bb).buildString(sb);
				sb.append('\n');
				break;
			case Changes.Record.Edit:
				sb.append("edit: [\n");
				for (int i = 0, n = bb.ReadUInt(); i < n; i++) {
					var lb = LogBean.decode(0, bb);
					sb.append(lb);
				}
				sb.append("      ]\n");
				break;
			}
		}
		sb.append("  }\n");
		sb.append("}\n");
		return sb.toString();
	}
}
