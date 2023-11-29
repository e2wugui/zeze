package Zeze.Transaction;

import java.io.Closeable;
import Zeze.Net.Binary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IGlobalAgent extends Closeable {
	class AcquireResult {
		private static final @NotNull AcquireResult[] successResults = new AcquireResult[4];

		static {
			for (int i = 0; i < successResults.length; i++)
				successResults[i] = new AcquireResult(0, i);
		}

		public static @NotNull AcquireResult getSuccessResult(int state) {
			return successResults[state];
		}

		public final long resultCode;
		public final int resultState;

		public AcquireResult(long code, int state) {
			resultCode = code;
			resultState = state;
		}
	}

	@Nullable AcquireResult acquire(@NotNull Binary gkey, int state, boolean fresh, boolean noWait);

	int getGlobalCacheManagerHashIndex(@NotNull Binary gkey);
}
