package Zeze.Transaction.Collections;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Transaction;

/** 事务集合容器（PList/PMap/PSet 等）的公共基类。 */
public abstract class Collection extends Bean {
	@Override
	public LogBean createChangeLog(Changes changes) {
		// 尚未拆分的 List/Set/One 等仍复用事务日志；已拆分容器覆写此入口。
		var transaction = Transaction.getCurrent();
		var parent = parent();
		var log = transaction != null && parent != null
				? transaction.getLog(parent.objectId() + variableId()) : null;
		return log != null ? (LogBean)log : createLogBean();
	}

	// 长度来自未验证的输入，只限制初始预分配；合法大集合仍随实际解码内容扩容。
	// 先钳位再计算 HashMap 的负载因子容量，避免容量乘加溢出。
	static int initialDecodeCapacity(int count) {
		return Math.min(count, 0x10000);
	}
}
