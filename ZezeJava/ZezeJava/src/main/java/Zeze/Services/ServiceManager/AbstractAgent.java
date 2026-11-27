package Zeze.Services.ServiceManager;

import java.util.Objects;
import java.io.Closeable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Component.Threading;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action1;
import Zeze.Util.Action0;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.Map.Entry;
import Zeze.Services.ServiceManager.Id128UdpClient.FutureNode;

/**
 * 服务管理器客户端抽象基类：管理服务订阅状态与增量回调、断线重连重放、
 * Suspect/TakeoverDone 通知以及 AutoKey/TidCache/Tid128Cache 号段分配。
 */
public abstract class AbstractAgent extends ReentrantLock implements Closeable {
	static final Logger logger = LogManager.getLogger(AbstractAgent.class);

	// key 为 ServiceName；每个 Agent 对同一服务只保留一个订阅。
	protected final ConcurrentHashMap<String, Agent.SubscribeState> subscribeStates = new ConcurrentHashMap<>();

	protected @Nullable Config config;

	/**
	 * 订阅服务状态发生变化时回调。 如果需要处理这个事件，请在订阅前设置回调。
	 */
	protected @Nullable Action1<BEditService> onChanged;
	protected @Nullable Action1<BServerLoad> onSetServerLoad;

	// SM 定时发送 KeepAlive 并等待结果，应用可在此执行健康检测（如测试事务）。
	protected @Nullable Runnable onKeepAlive;
	// SM Suspect 提示回调，参数为疑似失效节点的 serverId。
	protected volatile @Nullable Action1<Integer> onSuspect;
	private volatile @Nullable TaskCompletionSource<TidCache> lastTidCacheFuture;
	// 按 globalName 缓存最近一次 tid128 分配 future，避免多名字号段混用。
	private final ConcurrentHashMap<String, Id128UdpClient.FutureNode> tid128CacheFutures = new ConcurrentHashMap<>();
	// 无参 getLastTid128CacheFuture() 的兼容镜像，仅供测试/诊断，正确性路径按 globalName 存取。
	private volatile @Nullable FutureNode lastTid128CacheFuture;
	protected @Nullable Id128UdpClient tid128UdpClient; // 子类初始化。

	protected final ConcurrentHashMap<String, AutoKey> autoKeys = new ConcurrentHashMap<>();

	public final ConcurrentHashMap<String, BServerLoad> loads = new ConcurrentHashMap<>();

	public ConcurrentHashMap<String, Agent.SubscribeState> getSubscribeStates() {
		return subscribeStates;
	}

	public Config getConfig() {
		return Objects.requireNonNull(config);
	}

	/**
	 * Takeover 全部 scope 搬运完成后向 SM 上报回执（fire-and-forget），供 SM 停止 version 梯队升档；
	 * 默认空实现，子类按对应协议发送。
	 */
	public void sendTakeoverDone(int deadServerId) {
	}

	public @Nullable Action1<BEditService> getOnChanged() {
		return onChanged;
	}

	public void setOnChanged(@Nullable Action1<BEditService> value) {
		onChanged = value;
	}

	public void setOnSetServerLoad(@Nullable Action1<BServerLoad> value) {
		onSetServerLoad = value;
	}

	public @Nullable Runnable getOnKeepAlive() {
		return onKeepAlive;
	}

	public void setOnKeepAlive(@Nullable Runnable value) {
		onKeepAlive = value;
	}

	public @Nullable Action1<Integer> getOnSuspect() {
		return onSuspect;
	}

	public void setOnSuspect(@Nullable Action1<Integer> value) {
		onSuspect = value;
	}

	/** 两种传输共用的版本防线；Suspect 只是提示，租约仍由 Takeover 裁决。 */
	protected final void notifySuspect(int serverId, long version) {
		var on = onSuspect;
		if (on == null)
			return;
		//noinspection DataFlowIssue
		if (config.getAppMainVersion() < version) {
			logger.debug("Suspect ignored: myVersion={} < deadVersion={} serverId={}",
					config.getAppMainVersion(), version, serverId);
			return;
		}
		try {
			on.run(serverId);
		} catch (Throwable e) {
			logger.error("Suspect serverId={}", serverId, e);
		}
	}

	/** 在传输应答及 onChanged 回调之前，按接收顺序应用增量并修正通知集合。 */
	protected final void applyServiceEdit(BEditService edit) {
		for (var it = edit.getRemove().iterator(); it.hasNext(); ) {
			var info = it.next();
			var state = subscribeStates.get(info.getServiceName());
			if (state == null || !state.onUnRegister(info))
				it.remove();
		}
		edit.getRemove().removeIf(removed -> edit.getAdd().stream().anyMatch(removed::fullEquals));
		for (var info : edit.getAdd()) {
			var state = subscribeStates.get(info.getServiceName());
			if (state == null)
				continue;
			var old = state.onRegister(info);
			if (old != null)
				edit.getRemove().add(old);
		}
	}

	/** 成功退订的本地提交点：在接收应答的串行回调内调用，先于完成调用者的等待。 */
	protected final void applyUnsubscribe(BUnSubscribeArgument arg) {
		for (var serviceName : arg.serviceNames)
			subscribeStates.remove(serviceName);
	}

	/** 注册确认后才重放订阅；同步/异步失败共用一条重试链，停机后拒绝迟到回调登记。 */
	protected final class ServiceReplay implements AutoCloseable {
		private final Action0 registerAction;
		private final String taskName;
		private final Level failureLevel;
		private @Nullable Future<?> retryTask; // 由 this 保护，绝不持锁等待 RPC
		private boolean closed; // 由 this 保护

		public ServiceReplay(Action0 registerAction, String taskName) {
			this(registerAction, taskName, Level.WARN);
		}

		public ServiceReplay(Action0 registerAction, String taskName, Level failureLevel) {
			this.registerAction = registerAction;
			this.taskName = taskName;
			this.failureLevel = failureLevel;
		}

		public void run() {
			try {
				registerAction.run();
			} catch (Throwable e) {
				logger.log(failureLevel, "{}: replay registers failed, schedule retry", taskName, e);
				scheduleRetry();
				return;
			}
			var subscriptions = new BSubscribeArgument();
			for (var state : subscribeStates.values())
				subscriptions.subs.add(state.getSubscribeInfo());
			if (subscriptions.subs.isEmpty())
				return;
			try {
				subscribeServicesAsync(subscriptions).whenComplete((__, e) -> {
					if (e != null)
						subscribeFailed(e);
				});
			} catch (Throwable e) {
				subscribeFailed(e);
			}
		}

		private void subscribeFailed(Throwable e) {
			logger.log(failureLevel, "{}: replay subscribes failed, schedule retry", taskName, e);
			scheduleRetry();
		}

		private synchronized void scheduleRetry() {
			if (closed || retryTask != null)
				return;
			retryTask = TaskSpec.ofAction(() -> {
				synchronized (this) {
					retryTask = null; // 重放再失败时可以立即登记下一次，不能在重放之后清除。
				}
				run();
			}).name(taskName).scheduleNow(5_000);
		}

		@Override
		public synchronized void close() {
			closed = true;
			if (retryTask != null) {
				retryTask.cancel(false);
				retryTask = null;
			}
		}
	}

	protected abstract void allocate(AutoKey autoKey, int pool);

	public @Nullable TaskCompletionSource<TidCache> getLastTidCacheFuture() {
		return lastTidCacheFuture;
	}

	public TaskCompletionSource<TidCache> allocateTidCacheFuture(String globalName) {
		var future = new TaskCompletionSource<TidCache>();
		lock();
		try {
			var lastFuture = lastTidCacheFuture;
			var allocateCount = TidCache.ALLOCATE_COUNT_MIN;
			if (lastFuture != null) {
				// 锁内仅非阻塞探测：完成回调在 IO 线程内联需获取本锁，等待未完成 future 会死锁；未完成或异常时回退默认档位。
				if (lastFuture.isCompletedExceptionally()) {
					logger.warn("allocateTidCacheFuture('{}'): last future failed, re-allocate with default count",
							globalName);
				} else {
					var last = lastFuture.getNow();
					if (last != null)
						allocateCount = last.allocateCount();
				}
			}
			var sent = allocateAsync(globalName, allocateCount, rpc -> {
				lock();
				try {
					if (rpc.getResultCode() == 0 || rpc.getResultCode() == Procedure.RaftApplied) {
						var newest = new TidCache(globalName, this, rpc.Result.getStartId(), rpc.Result.getCount());
						future.setResult(newest);
					} else {
						var exception = new Exception("AllocateId rc=" + IModule.getErrorCode(rpc.getResultCode()));
						future.setException(exception);
					}
				} finally {
					unlock();
				}
				return 0;
			});
			if (!sent) // 错误的请求将保持,使得所有事务都失败,本质上不允许出错.
				future.setException(new Exception("AllocatedId send fail."));

			lastTidCacheFuture = future;
		} finally {
			unlock();
		}
		return future;
	}

	public @Nullable FutureNode getLastTid128CacheFuture(String globalName) {
		return tid128CacheFutures.get(globalName);
	}

	/**
	 * @deprecated 单槽遗留查询：返回最近一次分配的future（无论名字），仅供测试/诊断；
	 *     正确性路径使用 {@link #getLastTid128CacheFuture(String)}。
	 */
	@Deprecated
	public @Nullable FutureNode getLastTid128CacheFuture() {
		return lastTid128CacheFuture;
	}

	/**
	 * 按 globalName 分配一段 Id128 并缓存，号段耗尽后由 {@link Tid128Cache#next()} 递归换段。
	 * @param globalName 全局 history 名称
	 * @param allocateCount 单次分配个数；&lt;=0 表示自适应，&gt;0 按指定数量分配
	 * @return 分配结果的 future
	 */
	public Id128UdpClient.FutureNode allocateTid128CacheFuture(String globalName, int allocateCount) {
		lock();
		try {
			var future = tid128CacheFutures.get(globalName);
			if (allocateCount <= 0) {
				allocateCount = Tid128Cache.ALLOCATE_COUNT_MIN;
				if (future != null) {
					// 锁内仅非阻塞探测，避免持锁等待网络 RTT/超时阻塞其他分配调用；未完成或异常时回退默认档位。
					if (future.isCompletedExceptionally()) {
						logger.warn("allocateTid128CacheFuture('{}'): last future failed, re-allocate with default count",
								globalName);
					} else {
						var last = future.getNow();
						if (last != null)
							allocateCount = last.allocateCount();
					}
				}
			}
			if (tid128UdpClient == null)
				throw new IllegalStateException("tid128UdpClient is not available (unsupported combination, " +
						"e.g. ServiceManager=raft): allocateTid128CacheFuture('" + globalName + "')");
			future = tid128UdpClient.allocateFuture(globalName, allocateCount);
			tid128CacheFutures.put(globalName, future);
			lastTid128CacheFuture = future;
			return future;
		} finally {
			unlock();
		}
	}

	/**
	 * 返回指定 globalName 可用的 tid128 分配 future；未分配或上次异常完成时重新分配替换。
	 */
	public Id128UdpClient.FutureNode getUsableTid128CacheFuture(String globalName) {
		var future = getLastTid128CacheFuture(globalName);
		if (future == null || future.isCompletedExceptionally())
			//noinspection DataFlowIssue
			return allocateTid128CacheFuture(globalName, config.getHistoryAllocCount());
		return future;
	}

	protected abstract boolean allocateAsync(String globalName, int allocCount,
											 ProtocolHandle<Rpc<BAllocateIdArgument, BAllocateIdResult>> callback);

	public abstract void start() throws Exception;

	public abstract void waitReady();

	private static final String SMCallbackOneByOneKey = "SMCallbackOneByOneKey";

	protected void triggerOnChanged(BEditService edit) {
		if (onChanged != null) {
			TaskSpec.ofAction(() -> {
				try {
					if (onChanged != null) {
						onChanged.run(edit);
					}
				} catch (Throwable e) {
					logger.error("", e);
				}
			}).executeOneByOne(SMCallbackOneByOneKey, Task.getOneByOne());
		}
	}

	/**
	 * 单个服务的本地订阅状态：缓存各版本地址列表与本地附着状态，供断线重连重放与变更查询。
	 */
	public static final class SubscribeState extends ReentrantLock {
		private volatile BSubscribeInfo subscribeInfo;
		private volatile BServiceInfosVersion serviceInfos = new BServiceInfosVersion();

		// 按 identity 保存本地就绪状态。
		private final ConcurrentHashMap<String, Object> localStates = new ConcurrentHashMap<>();
		private @Nullable Iterator<Map.Entry<String, Object>> localStatesIterator;

		public SubscribeState(BSubscribeInfo info) {
			subscribeInfo = info;
		}

		public @Nullable Entry<String, Object> getNextStateEntry() {
			lock();
			try {
				if (localStatesIterator == null || !localStatesIterator.hasNext())
					localStatesIterator = localStates.entrySet().iterator();
				return localStatesIterator.hasNext() ? localStatesIterator.next() : null;
			} finally {
				unlock();
			}
		}

		@Override
		public String toString() {
			return serviceInfos.toString();
		}

		public BSubscribeInfo getSubscribeInfo() {
			return subscribeInfo;
		}

		/** 同名重订阅时更新本地订阅信息，确保断线重连按新过滤版本重放。 */
		public void updateSubscribeInfo(BSubscribeInfo info) {
			lock();
			try {
				if (subscribeInfo != info)
					subscribeInfo = info;
			} finally {
				unlock();
			}
		}

		public String getServiceName() {
			return subscribeInfo.getServiceName();
		}

		/**
		 * @return 锁内复制版本桶和地址列表的只读快照；BServiceInfo 按只读对象共享。
		 */
		public BServiceInfosVersion getServiceInfosVersion() {
			lock();
			try {
				return new BServiceInfosVersion(serviceInfos);
			} finally {
				unlock();
			}
		}

		/**
		 * @return 只读地址列表（禁止修改）；持锁查询以避开底层表扩容并发。
		 */
		public @Nullable BServiceInfos getServiceInfos(long version) {
			lock();
			try {
				return serviceInfos.getInfos(version);
			} finally {
				unlock();
			}
		}

		/**
		 * @return 只读, 禁止修改
		 */
		public @Nullable BServiceInfos findNewestInfos() {
			lock();
			try {
				return serviceInfos.getNewestInfos(); // 按需迭代，与onRegister/onUnRegister的修改互斥
			} finally {
				unlock();
			}
		}

		public @Nullable BServiceInfo findServiceInfoByIdentity(String identity) {
			lock();
			try {
				for (var it = serviceInfos.getInfosIterator(); it.moveToNext(); ) {
					var info = it.value().findServiceInfoByIdentity(identity);
					if (info != null)
						return info;
				}
				return null;
			} finally {
				unlock();
			}
		}

		public @Nullable BServiceInfo findServiceInfoByServerId(int serverId) {
			return findServiceInfoByIdentity(String.valueOf(serverId));
		}

		// 以下方法不加锁

		public ConcurrentHashMap<String, Object> getLocalStates() {
			return localStates;
		}

		public void setIdentityLocalState(String identity, @Nullable Object state) {
			if (state == null)
				localStates.remove(identity);
			else
				localStates.put(identity, state);
		}

		/**
		 * @return 被替换的旧BServiceInfo; 无更新或无变化则返回null
		 */
		public @Nullable BServiceInfo onRegister(BServiceInfo info) {
			lock();
			try {
				// 以 name+id 为键，跨版本重注册时先移除其他版本桶中的旧记录，避免残留旧地址。
				BServiceInfo oldNotSame = null;
				for (var it = serviceInfos.getInfosIterator(); it.moveToNext(); ) {
					if (it.key() == info.getVersion())
						continue;
					var old = it.value().remove(info);
					if (old != null && !old.fullEquals(info) && oldNotSame == null)
						oldNotSame = old;
				}
				var versions = serviceInfos.getOrAddInfos(info.getVersion());
				var exist = versions.insert(info);
				serviceInfos.pruneEmptyVersions();
				if (oldNotSame == null && null != exist && !exist.fullEquals(info))
					oldNotSame = exist;
				return oldNotSame;
			} finally {
				unlock();
			}
		}

		public boolean onUnRegister(BServiceInfo info) {
			lock();
			try {
				// 以 name+id 为键跨全部版本桶移除，避免残留旧地址。
				var removed = false;
				for (var it = serviceInfos.getInfosIterator(); it.moveToNext(); ) {
					if (it.value().remove(info) != null)
						removed = true;
				}
				serviceInfos.pruneEmptyVersions();
				return removed;
			} finally {
				unlock();
			}
		}

		public void onFirstCommit(BServiceInfosVersion infos, BEditService edits) {
			lock();
			try {
				for (var it = serviceInfos.getInfosIterator(); it.moveToNext(); ) {
					var current = infos.getInfos(it.key());
					for (var old : it.value().getSortedIdentities()) {
						var replacement = current != null ? current.findServiceInfo(old) : null;
						if (replacement == null || !old.fullEquals(replacement))
							edits.getRemove().add(old);
					}
				}
				for (var it = infos.getInfosIterator(); it.moveToNext(); )
					edits.getAdd().addAll(it.value().getSortedIdentities());
				serviceInfos = infos;
			} finally {
				unlock();
			}
		}
	}

	protected static void verify(String identity) {
		if (!identity.startsWith("@") && !identity.startsWith("#")) {
			// 这里特意使用Integer.parseInt，因为现在只开放int(serverId)当作identity。
			//noinspection ResultOfMethodCallIgnored
			Integer.parseInt(identity);
		}
	}

	public void registerService(BServiceInfo info) {
		var edit = new BEditService();
		edit.getAdd().add(info);
		editService(edit);
	}

	public void unRegisterService(BServiceInfo info) {
		var edit = new BEditService();
		edit.getRemove().add(info);
		editService(edit);
	}

	public abstract void editService(BEditService arg);

	public SubscribeState subscribeService(BSubscribeInfo info) {
		var infos = new BSubscribeArgument();
		infos.subs.add(info);
		return subscribeServices(infos).getFirst();
	}

	public List<SubscribeState> subscribeServices(BSubscribeArgument infos) {
		try {
			return subscribeServicesAsync(infos).get();
		} catch (InterruptedException | ExecutionException e) {
			throw Task.forceThrow(e);
		}
	}

	public abstract CompletableFuture<List<SubscribeState>> subscribeServicesAsync(
			BSubscribeArgument info);

	public void unSubscribeService(String serviceName) {
		var arg = new BUnSubscribeArgument();
		arg.serviceNames.add(serviceName);
		unSubscribeService(arg);
	}

	public abstract void unSubscribeService(BUnSubscribeArgument arg);

	public AutoKey getAutoKey(String name) {
		return autoKeys.computeIfAbsent(name, k -> new AutoKey(k, this));
	}

	public abstract boolean setServerLoad(BServerLoad load);

	protected static void setCurrentAndCount(AutoKey autoKey, long current, int count) {
		autoKey.setCurrentAndCount(current, count);
	}

	public abstract @Nullable Threading getThreading();
}
