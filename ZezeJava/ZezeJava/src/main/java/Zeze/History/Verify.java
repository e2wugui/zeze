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

/**
 * tHistory 回放全量校验：独立重放历史到内存库，逐表比对业务库与回放副本。
 */
public class Verify {
	private static final @NotNull Logger logger = LogManager.getLogger(Verify.class);

	/**
	 * 校验历史回放与源表一致。前置：所有app已checkpoint且当前app已停写（如先
	 * WaitAllRunningTasksAndClear 静默），否则并发提交窗口会误报 record miss——详见方法内注释。
	 * <p>
	 * 覆盖边界：只校验 tHistory 记录中出现过的表（applyTables 按记录内的 tableId 建立）——
	 * 从未产生过 history 记录的表静默不校验；且要求被校验表的 history 覆盖其全生命周期：
	 * history 启用前已有存量数据、启用后又有写入的表，verifyAndClear 的合并视图能看到存量
	 * 记录而回放副本无对应 Put，将确定性误报 record miss（规避法见 Simulate 的 clearTables）。
	 */
	public static void run(Application zeze) throws Exception {
		var applyDb = new ApplyDatabaseMemory();
		var applyTables = new ConcurrentHashMap<Integer, ApplyTable<?, ?>>();
		// 【注意】如果存在多个app，需要所有app都checkpoint，这里只保证当前app提交。
		// 且当前app必须已停写（如先 WaitAllRunningTasksAndClear 静默，见 Simulate 用法）：
		// checkpoint 后新提交的记录对 tHistory 的 walkDatabase 不可见（Table 模式下最多延迟
		// 一个 period 才 flush），而 verifyAndClear 的 originTable.walk 走 cache+db 合并视图
		// 可见，将误报 record miss。
		zeze.checkpointRun();
		var counter = new AtomicLong();
		var total = new AtomicLong();
		// null哨兵：(0,0)是合法的首个gid（Tid128Cache对齐TidCache先返后增后，SM从零起grant的段首号
		// 会真实发放），不能再用零值Id128充当"无前驱"，否则全新部署的首条记录即被误判乱序。
		var lastK = new OutObject<Id128>();
		zeze.getHistoryModule().getHistoryTable().walkDatabase((key, value) -> {
			if (lastK.value != null && lastK.value.compareTo(key) >= 0) {
				logger.error("out of Id128 order: {}, {}", lastK.value, key);
				// Id128全序是回放确定性前提（游标空洞检测依赖），前提破坏时校验结果不可信，必须中断。
				throw new RuntimeException("out of Id128 order: " + lastK.value + ", " + key);
			}
			lastK.value = key;

			for (var r : value.getChanges().entrySet()) {
				var applyTable = applyTables.computeIfAbsent(r.getKey().getTableId(), __ -> {
					var tableName = TableKey.tables.get(r.getKey().getTableId());
					if (tableName == null)
						throw new RuntimeException("table id not found. id=" + r.getKey().getTableId());
					logger.info("history apply table {}", tableName);
					// 与ApplyHelper一致用全库查表（zeze.getTable）：业务表配置在命名数据库
					// （<DatabaseConf Name="xxx">）时，getDatabase("").getTable只查默认库
					// 必然返回null，Verify全量校验确定性中断。
					var originTable = zeze.getTable(tableName);
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
