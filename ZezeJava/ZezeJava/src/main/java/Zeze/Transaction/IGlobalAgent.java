package Zeze.Transaction;

import java.io.Closeable;
import Zeze.Net.Binary;

public interface IGlobalAgent extends Closeable {
	class AcquireResult {
		private static final AcquireResult[] successResults = new AcquireResult[4];

		static {
			for (int i = 0; i < successResults.length; i++)
				successResults[i] = new AcquireResult(0, i);
		}

		public static AcquireResult getSuccessResult(int state) {
			return successResults[state];
		}

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
