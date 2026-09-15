package Zeze.History;

import org.jetbrains.annotations.NotNull;

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
}
