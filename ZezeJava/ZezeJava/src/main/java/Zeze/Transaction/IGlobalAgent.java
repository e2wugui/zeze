package Zeze.Transaction;

import java.io.Closeable;
import Zeze.Net.Binary;

public interface IGlobalAgent extends Closeable {
	class AcquireResult {
		public final long ResultCode;
		public final int ResultState;

		public AcquireResult(long code, int state) {
			ResultCode = code;
			ResultState = state;
		}
	}

	AcquireResult Acquire(Binary gkey, int state, boolean fresh);
	int GetGlobalCacheManagerHashIndex(Binary gkey);
}
