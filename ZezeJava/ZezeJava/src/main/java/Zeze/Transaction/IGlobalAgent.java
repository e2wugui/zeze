package Zeze.Transaction;

import java.io.Closeable;
import Zeze.Net.Binary;

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

	AcquireResult Acquire(Binary gkey, int state, boolean fresh);
	int GetGlobalCacheManagerHashIndex(Binary gkey);
}
