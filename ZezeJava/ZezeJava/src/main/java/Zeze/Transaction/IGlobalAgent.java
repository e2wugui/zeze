package Zeze.Transaction;

import java.io.Closeable;
import Zeze.Net.Binary;
import Zeze.Util.Id128;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IGlobalAgent extends Closeable {
	class AcquireResult {
		private static final @NotNull AcquireResult @NotNull [] successResults = new AcquireResult[4];

		static {
			for (int i = 0; i < successResults.length; i++)
				successResults[i] = new AcquireResult(0, i, null);
		}

		public static @NotNull AcquireResult getSuccessResult(int state) {
			return successResults[state];
		}

		public final long resultCode;
		public final int resultState;
		public final @Nullable Id128 reducedTid;

		public AcquireResult(long code, int state, @Nullable Id128 reducedTid) {
			resultCode = code;
			resultState = state;
			this.reducedTid = reducedTid;
		}
	}

	@Nullable AcquireResult acquire(@NotNull Binary gkey, int state, boolean fresh, boolean noWait);

	int getGlobalCacheManagerHashIndex(@NotNull Binary gkey);

	@NotNull GlobalAgentBase getAgent(int index);

	int getAgentCount();

	// FND10 txn-01：停机关库前有界等待活跃Releaser——遍历内部GlobalAgentBase逐个有界等待，
	// 等待逻辑见GlobalAgentBase.awaitReleaser。
	default void awaitReleaser(long timeoutMillis) {
		for (var i = 0; i < getAgentCount(); ++i)
			getAgent(i).awaitReleaser(timeoutMillis);
	}
}
