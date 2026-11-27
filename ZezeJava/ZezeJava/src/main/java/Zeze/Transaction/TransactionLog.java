package Zeze.Transaction;

import org.jspecify.annotations.Nullable;

/**
 * 保存点持有的本地事务日志接口：集合日志仅用于本地提交不直接上树，
 * 原子字段日志同时实现 Log 作为完整变更载荷，Changes 统一采集可消费载荷。
 */
public interface TransactionLog {
	@Nullable Bean getBelong();

	int getVariableId();

	default long getLogKey() {
		var owner = getBelong();
		if (owner == null)
			throw new IllegalStateException("decoded log has no transaction target");
		return owner.objectId() + getVariableId();
	}

	default TransactionLog beginSavepoint() {
		return this;
	}

	default void endSavepoint(Savepoint parent) {
		parent.putLog(this);
	}

	void commit();
}
