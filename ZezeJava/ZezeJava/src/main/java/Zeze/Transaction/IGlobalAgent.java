package Zeze.Transaction;

import Zeze.Net.Binary;
import Zeze.Util.Id128;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IGlobalAgent {
	record AcquireResult(long resultCode, int resultState, @Nullable Id128 reducedTid) {
			private static final @NotNull AcquireResult @NotNull [] successResults = new AcquireResult[4];

			static {
				for (int i = 0; i < successResults.length; i++)
					successResults[i] = new AcquireResult(0, i, null);
			}

			public static @NotNull AcquireResult getSuccessResult(int state) {
				return successResults[state];
			}

	}

	@Nullable AcquireResult acquire(@NotNull Binary gkey, int state, boolean fresh, boolean noWait);

	int getGlobalCacheManagerHashIndex(@NotNull Binary gkey);

	@NotNull GlobalAgentBase getAgent(int index);

	int getAgentCount();

	// 停机拆除：尽力语义（实现内逐agent失败记日志继续）、可重入。不挂Closeable/AutoCloseable：
	// 生命周期与Application同体，拆除是stop序列一环而非词法作用域退出（Service同形态）；
	// Releaser不属网络资源，关库前另有界等待（见awaitReleaser）。
	void stop() throws Exception;

	// FND10 txn-01：停机关库前有界等待活跃Releaser——遍历内部GlobalAgentBase逐个有界等待，
	// 等待逻辑见GlobalAgentBase.awaitReleaser。
	default void awaitReleaser(long timeoutMillis) {
		for (var i = 0; i < getAgentCount(); ++i)
			getAgent(i).awaitReleaser(timeoutMillis);
	}
}
