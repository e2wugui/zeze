package Zeze.History;

import Zeze.Net.Binary;
import org.jetbrains.annotations.NotNull;

/**
 * 单条tHistory记录级别的原子应用单元（记录级事务）。
 * <p>
 * ApplyHelper应用一条历史记录前 {@link IApplyDatabase#beginRecordTxn()} 开启本事务，
 * 记录内全部entry通过 {@link IApplyTable} 执行的put/remove都计入本事务（暂存或挂在
 * 同一底层事务上，不即时落库）；全部entry成功后 {@link #commit} 一次性生效；
 * 任一entry异常 {@link #rollback} 整条丢弃后原样重抛——保证重试整条记录时从干净
 * 状态完整重放，前缀entry不会被二次应用（Edit类非幂等日志如PList2的OP_ADD按索引
 * 插入，二次应用会产生重复元素）。
 * <p>
 * 事务由ApplyHelper.apply在其锁内单线程驱动，不支持嵌套开启；commit/rollback/close幂等。
 */
public interface IApplyRecordTxn extends AutoCloseable {
	/**
	 * 事务内put：暂存待提交（内存实现）或写入记录级底层事务（Zeze实现），不即时生效。
	 */
	void put(@NotNull String tableName, @NotNull Binary key, @NotNull Binary value) throws Exception;

	/**
	 * 事务内remove：同 {@link #put}，提交时才生效。
	 */
	void remove(@NotNull String tableName, @NotNull Binary key) throws Exception;

	/**
	 * 提交：记录内全部entry成功后一次性生效。
	 */
	void commit() throws Exception;

	/**
	 * 回滚：丢弃记录内全部未提交写入。幂等，不抛出（不得掩盖正在传播的原始异常）。
	 */
	void rollback();

	/**
	 * 幂等收尾：已commit/rollback则no-op，否则按回滚处理（供finally兜底Error穿透路径）。
	 * 实现保证不抛出。
	 */
	@Override
	void close();
}
