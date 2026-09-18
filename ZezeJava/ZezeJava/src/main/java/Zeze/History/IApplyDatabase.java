package Zeze.History;

import Zeze.Util.Id128;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IApplyDatabase {
	@NotNull IApplyTable open(@NotNull String tableName);

	/**
	 * 开启“单条tHistory记录”级别的原子应用单元（记录级事务）。
	 * <p>
	 * commit/rollback之前，通过本库{@link IApplyTable}执行的put/remove都计入该事务，
	 * 不即时落库；全部entry成功commit一次性生效，任一异常rollback整条丢弃。
	 * 由ApplyHelper.apply在锁内单线程驱动，不支持嵌套开启。
	 */
	@NotNull IApplyRecordTxn beginRecordTxn();

	/**
	 * FND8-28：恢复回放游标（ApplyHelper构造时调用一次）。
	 * 持久化后端必须返回上次{@link #saveCursor}随记录数据同事务提交的游标，
	 * 否则重启后游标归零、apply从tHistory表头整段重放到已有状态上
	 * （Edit类日志非幂等：PList2的OP_ADD按索引插入，重放产生重复元素）。
	 * default覆盖内存后端：重启即空，null游标一致。
	 */
	default @Nullable Id128 loadCursor() {
		return null;
	}

	/**
	 * FND8-28：保存回放游标——必须在当前记录级事务内与记录数据同原子单元写入
	 * （commit成功才生效）；default覆盖内存后端：丢弃。
	 */
	default void saveCursor(@NotNull Id128 key, @NotNull IApplyRecordTxn txn) throws Exception {
	}
}
