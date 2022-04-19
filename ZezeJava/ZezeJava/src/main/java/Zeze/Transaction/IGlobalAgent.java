package Zeze.Transaction;

import java.io.Closeable;

public interface IGlobalAgent extends Closeable {
	class AcquireResult {
		public final long ResultCode;
		public final int ResultState;
		public final long ResultGlobalSerialId;

		public AcquireResult(long code, int state, long serial) {
			ResultCode = code;
			ResultState = state;
			ResultGlobalSerialId = serial;
		}
	}

	AcquireResult Acquire(Zeze.Beans.GlobalCacheManagerWithRaft.GlobalTableKey gkey, int state);

	int GetGlobalCacheManagerHashIndex(Zeze.Beans.GlobalCacheManagerWithRaft.GlobalTableKey gkey);
}
