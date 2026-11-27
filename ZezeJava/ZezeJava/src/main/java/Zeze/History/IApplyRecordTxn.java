package Zeze.History;

import Zeze.Net.Binary;

/**
 * 单条 tHistory 记录级的原子应用事务：记录内所有 put/remove 随 {@link #commit()} 一次性生效，
 * 任一条目失败时由 {@link #rollback()} 整体撤销以保证重试幂等；单线程驱动，commit/rollback/close 幂等。
 */
public interface IApplyRecordTxn extends AutoCloseable {
	/**
	 * 事务内put：暂存待提交（内存实现）或写入记录级底层事务（Zeze实现），不即时生效。
	 */
	void put(String tableName, Binary key, Binary value) throws Exception;

	/**
	 * 事务内remove：同 {@link #put}，提交时才生效。
	 */
	void remove(String tableName, Binary key) throws Exception;

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
