package Zeze.Transaction;

import Zeze.Net.Binary;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Util.Id128;
import org.jspecify.annotations.Nullable;

/**
 * 全局缓存管理代理接口：封装对 GlobalCacheManager 集群的 acquire/reduce 权限协调，
 * 管理多 agent 并提供停机拆除与 Releaser 等待。
 */
public interface IGlobalAgent {
	record AcquireResult(long resultCode, int resultState, @Nullable Id128 reducedTid) {
			private static final AcquireResult [] successResults = new AcquireResult[4];

			static {
				for (int i = 0; i < successResults.length; i++)
					successResults[i] = new AcquireResult(0, i, null);
			}

			public static AcquireResult getSuccessResult(int state) {
				if (state < 0 || state >= successResults.length) // 越界state原为AIOOBE，此处给可诊断的参数错
					throw new IllegalArgumentException("state=" + state + " not in [0," + successResults.length + ")");
				return successResults[state];
			}

			/** 协商已完成。调用方还需核对返回的权限是否满足本次请求。 */
			public boolean isSuccess() {
				return resultCode == 0
						|| (resultState == GlobalCacheManagerConst.StateModify
						&& (resultCode == GlobalCacheManagerConst.AcquireShareAlreadyIsModify
						|| resultCode == GlobalCacheManagerConst.AcquireModifyAlreadyIsModify));
			}

	}

	/**
	 * 向 GCM 协商记录权限。
	 *
	 * <p>调用方通过 {@link AcquireResult#isSuccess()} 判断协商是否完成，并检查
	 * resultState 是否满足请求。已持有 Modify 权限的应答保留协议原码。
	 * {@link GlobalCacheManagerConst#AcquireShareFailed}/
	 * {@link GlobalCacheManagerConst#AcquireModifyFailed} 为协商失败（实现方转为
	 * throwAbort，不会以返回码形态到达调用方）；其余失败码保留原值供诊断。
	 * reducedTid 非空表示结果与该事务号的 reduce 相关，不适用为 null。
	 */
	@Nullable AcquireResult acquire(Binary gkey, int state, boolean fresh, boolean noWait);

	int getGlobalCacheManagerHashIndex(Binary gkey);

	GlobalAgentBase getAgent(int index);

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
