package Zeze.Transaction;

import Zeze.Net.Binary;
import Zeze.Util.Id128;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 全局缓存管理代理接口：封装对 GlobalCacheManager 集群的 acquire/reduce 权限协调，
 * 管理多 agent 并提供停机拆除与 Releaser 等待。
 */
public interface IGlobalAgent {
	record AcquireResult(long resultCode, int resultState, @Nullable Id128 reducedTid) {
			private static final @NotNull AcquireResult @NotNull [] successResults = new AcquireResult[4];

			static {
				for (int i = 0; i < successResults.length; i++)
					successResults[i] = new AcquireResult(0, i, null);
			}

			public static @NotNull AcquireResult getSuccessResult(int state) {
				if (state < 0 || state >= successResults.length) // 越界state原为AIOOBE，此处给可诊断的参数错
					throw new IllegalArgumentException("state=" + state + " not in [0," + successResults.length + ")");
				return successResults[state];
			}

	}

	/**
	 * 向 GCM 协商记录权限。
	 *
	 * <p>结果码契约：resultCode==0 为成功，resultState 为协商后的状态
	 * （GlobalCacheManagerConst.StateInvalid/StateShare/StateModify）；
	 * {@link GlobalCacheManagerConst#AcquireShareFailed}/
	 * {@link GlobalCacheManagerConst#AcquireModifyFailed} 为协商失败（实现方转为
	 * throwAbort，不会以返回码形态到达调用方）；其余非零码为环境性失败。
	 * 注意：各实现（GlobalAgent 与 raft 版）对非零失败码的分类不完全一致，
	 * 调用方按"零成功、非零失败"处理，保留原始码记日志，勿按具体负值分支。
	 * reducedTid 非空表示结果与该事务号的 reduce 相关，不适用为 null。
	 */
	@Nullable AcquireResult acquire(@NotNull Binary gkey, int state, boolean fresh, boolean noWait);

	int getGlobalCacheManagerHashIndex(@NotNull Binary gkey);

	@NotNull GlobalAgentBase getAgent(int index);

	int getAgentCount();

	// 停机拆除：尽力语义（实现内逐agent失败记日志继续）、可重入。不挂Closeable/AutoCloseable：
	// 生命周期与Application同体，拆除是stop序列一环而非词法作用域退出（Service同形态）；
	// Releaser不属网络资源，关库前另有界等待（见awaitReleaser）。
	void stop() throws Exception;

	// 停机关库前有界等待活跃Releaser——遍历内部GlobalAgentBase逐个有界等待，
	// 等待逻辑见GlobalAgentBase.awaitReleaser。
	default void awaitReleaser(long timeoutMillis) {
		for (var i = 0; i < getAgentCount(); ++i)
			getAgent(i).awaitReleaser(timeoutMillis);
	}
}
