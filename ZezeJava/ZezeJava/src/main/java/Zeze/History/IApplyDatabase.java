package Zeze.History;

import Zeze.Util.Id128;
import org.jspecify.annotations.Nullable;

/**
 * 回放库后端接口：打开回放表、开启“单条tHistory记录”级原子事务、保存/恢复回放游标。
 */
public interface IApplyDatabase {
	IApplyTable open(String tableName);

	/**
	 * 开启单条 tHistory 记录级的原子事务；单线程驱动，不支持嵌套。
	 */
	IApplyRecordTxn beginRecordTxn();

	/**
	 * 恢复上次随记录数据原子提交的回放游标；内存后端默认返回 null。
	 */
	default @Nullable Id128 loadCursor() {
		return null;
	}

	/**
	 * 在当前记录级事务内保存回放游标，随记录数据原子提交；内存后端默认丢弃。
	 */
	default void saveCursor(Id128 key, IApplyRecordTxn txn) throws Exception {
	}
}
