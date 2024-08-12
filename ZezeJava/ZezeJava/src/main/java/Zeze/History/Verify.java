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
import org.jetbrains.annotations.NotNull;

public class Verify {
	private static final @NotNull Logger logger = LogManager.getLogger(Verify.class);

	public static void run(Application zeze) throws Exception {
		var applyDb = new ApplyDatabaseMemory();
		var defaultDb = zeze.getDatabase("");
		var applyTables = new ConcurrentHashMap<Integer, ApplyTable<?, ?>>();
		zeze.checkpointRun(); // 【注意】如果存在多个app，需要所有app都checkpoint，这里只保证当前app提交。
		var counter = new AtomicLong();
		var total = new AtomicLong();
		var lastK = new OutObject<>(new Id128());
		zeze.getHistoryModule().getHistoryTable().walkDatabase((key, value) -> {
			if (lastK.value.compareTo(key) >= 0) {
				logger.error("out of Id128 order: {}, {}", lastK.value, key);
				assert false;
			}
			lastK.value = key;

			for (var r : value.getChanges().entrySet()) {
				var applyTable = applyTables.computeIfAbsent(r.getKey().getTableId(), __ -> {
					var tableName = TableKey.tables.get(r.getKey().getTableId());
					if (tableName == null)
						throw new RuntimeException("table id not found. id=" + r.getKey().getTableId());
					logger.info("history apply table {}", tableName);
					var originTable = defaultDb.getTable(tableName);
					if (originTable == null)
						throw new RuntimeException("table not found. name=" + tableName);
					return originTable.createApplyTable(applyDb);
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

	public static @NotNull String toString(@NotNull BLogChanges.Data b) {
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
					var lb = new LogBean(null, 0, null);
					lb.decode(bb);
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
