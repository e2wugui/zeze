package Zeze.Transaction.Collections;

import Zeze.Transaction.Bean;
import Zeze.Transaction.TransactionLog;
import Zeze.Transaction.ChangeCaptureMode;

/** 事务集合容器（PList/PMap/PSet 等）的公共基类。 */
public abstract class Collection extends Bean {

	// 限制未验证输入引起的初始预分配，同时避免 HashMap 负载因子容量计算溢出。
	static int initialDecodeCapacity(int count) {
		return Math.min(count, 0x10000);
	}

	/** 仅在事务日志缓存未命中时调用；嵌套保存点沿用该采集模式。 */
	public abstract TransactionLog createLog(ChangeCaptureMode mode);

	@Override
	public long changeLogKey() {
		var owner = parent();
		return owner != null ? owner.objectId() + variableId() : 0;
	}
}
