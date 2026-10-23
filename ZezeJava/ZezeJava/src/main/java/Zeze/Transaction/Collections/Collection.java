package Zeze.Transaction.Collections;

import Zeze.Transaction.Bean;
import Zeze.Transaction.TransactionLog;
import Zeze.Transaction.ChangeCaptureMode;

/** 事务集合容器（PList/PMap/PSet 等）的公共基类。 */
public abstract class Collection extends Bean {

	// 长度来自未验证的输入，只限制初始预分配；合法大集合仍随实际解码内容扩容。
	// 先钳位再计算 HashMap 的负载因子容量，避免容量乘加溢出。
	static int initialDecodeCapacity(int count) {
		return Math.min(count, 0x10000);
	}

	/** Called only on a transaction log cache miss; nested savepoints retain this capture mode. */
	public abstract TransactionLog createLog(ChangeCaptureMode mode);

	@Override
	public long changeLogKey() {
		var owner = parent();
		return owner != null ? owner.objectId() + variableId() : 0;
	}
}
