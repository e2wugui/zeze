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

/*
 * Agent发起协议	ServiceManager处理后通知		Agent接收通知后回调
 * EditService		给订阅者广播Edit				onChanged
 * Subscribe		给发起者回复BSubscribeResult	onChanged
 * UnSubscribe		无								无
 */
public abstract class AbstractAgent extends ReentrantLock implements Closeable {
	static final Logger logger = LogManager.getLogger(AbstractAgent.class);

	// key is ServiceName。对于一个Agent，一个服务只能有一个订阅。
	// ServiceName ->
	protected final ConcurrentHashMap<String, Agent.SubscribeState> subscribeStates = new ConcurrentHashMap<>();

	protected @Nullable Config config;

	/**
	 * 订阅服务状态发生变化时回调。 如果需要处理这个事件，请在订阅前设置回调。
	 */
	protected @Nullable Action1<BEditService> onChanged;
	protected @Nullable Action1<BServerLoad> onSetServerLoad;

	// 应用可以在这个Action内起一个测试事务并执行一次。也可以实现其他检测。
	// ServiceManager 定时发送KeepAlive给Agent，并等待结果。超时则认为服务失效。
	protected @Nullable Runnable onKeepAlive;
	// SM Suspect提示回调：参数=疑似死者的serverId。应用转化为takeover.tryTransfer。
	protected volatile @Nullable Action1<Integer> onSuspect;
	private volatile @Nullable TaskCompletionSource<TidCache> lastTidCacheFuture;
	// 按globalName记录最近一次分配的tid128 future：原agent级单槽不区分名字，多名字使用时
	// getUsableTid128CacheFuture(B)会返回为A分配的future，跨名号段串用（静默发错号）。
	private final ConcurrentHashMap<String, Id128UdpClient.FutureNode> tid128CacheFutures = new ConcurrentHashMap<>();
	// 最近一次分配的future（无论名字）：仅作为无参getLastTid128CacheFuture()的兼容镜像（供测试/诊断），
	// 正确性路径一律按名字存取，不读它。
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
	 * Takeover全部scope搬运完成（立墓碑）后上报SM：SM据此停止version梯队升档。
	 * fire-and-forget：丢失由梯队超时升档的重复Suspect兜底（已接管者tryTransfer幂等空转，
	 * 并经此路径补发回执自愈）。默认空实现（无SM连接时）；子类按所连SM版型发对应协议。
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
		edit.getRemove().removeIf(edit.getAdd()::contains);
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
		private @Nullable Future<?> retryTask; // guarded by this，绝不持此锁等待 RPC
		private boolean closed; // guarded by this

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
				// 锁内只做非阻塞探测（FND3-39）：等待未完成的future需要它的完成回调先拿本锁
				// （回调在IO线程内联），互相等待即死锁并冻结selector。未完成时退用默认档位
				// 立即分配——档位只是优化参数，号段允许并发分配，各自future各自消费。
				if (lastFuture.isCompletedExceptionally()) {
					// 上一次分配异常完成（如发送失败）：以默认档位重新分配并替换（自愈）。
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
					if (rpc.getResultCode() == 0) {
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
	 * 分配一段id并缓存到tid128CacheFutures（按globalName），段耗尽后由Tid128Cache.next()递归调用本方法换新段。
	 * @param globalName 全局history名字
	 * @param allocateCount 一次分配的个数，&lt;=0 表示自适应（上次档位/最小档位）；
	 *                      &gt;0（如多app部署的1）时每次按该个数分配。
	 * @return 可等待的future，到达后get()得到Tid128Cache号段。
	 */
	public Id128UdpClient.FutureNode allocateTid128CacheFuture(String globalName, int allocateCount) {
		lock();
		try {
			var future = tid128CacheFutures.get(globalName);
			if (allocateCount <= 0) {
				allocateCount = Tid128Cache.ALLOCATE_COUNT_MIN;
				if (future != null) {
					// 锁内只做非阻塞探测（FND3-39衍生，与32位路径统一锁纪律）：本路径完成虽在
					// UDP接收线程、不经agent锁（无死锁），但等待未完成future会把锁占住剩余RTT，
					// 丢包时要等重传超时检查（eRpcTimeout=5s）毒化才跳出，期间其他分配调用者
					// 全被卡。未完成退用默认档位立即分配——档位只是优化参数。
					if (future.isCompletedExceptionally()) {
						// 上一次分配异常完成（如Udp超时）：以默认档位重新分配并替换（自愈）。
						logger.warn("allocateTid128CacheFuture('{}'): last future failed, re-allocate with default count",
								globalName);
					} else {
						var last = future.getNow();
						if (last != null)
							allocateCount = last.allocateCount();
					}
				}
			}
			// raft版SM不初始化tid128UdpClient（不支持Id128 UDP发号）：这里给出明确的
			// 错误而不是裸NPE；该组合应在子类构造时被fail-fast拦截（见ServiceManagerAgentWithRaft）。
			if (tid128UdpClient == null)
				throw new IllegalStateException("tid128UdpClient is not available (unsupported combination, " +
						"e.g. ServiceManager=raft): allocateTid128CacheFuture('" + globalName + "')");
			future = tid128UdpClient.allocateFuture(globalName, allocateCount);
			tid128CacheFutures.put(globalName, future);
			lastTid128CacheFuture = future; // 无参getLastTid128CacheFuture()的兼容镜像
			return future;
		} finally {
			unlock();
		}
	}

	/**
	 * finalCommit等只读入口使用：返回可用的tid128分配future。
	 * 上一次分配异常完成（Udp超时等毒化状态）或尚未分配过时，发起新分配并替换（自愈），
	 * 避免读取毒化的future后get()抛异常，导致finalCommit失败halt。
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
				} catch (Throwable e) { // logger.error
					logger.error("", e);
				}
			}).executeOneByOne(SMCallbackOneByOneKey, Task.getOneByOne());
		}
	}

	// 【警告】
	// 记住当前已经注册和订阅信息，当ServiceManager连接发生重连时，重新发送请求。
	// 维护这些状态数据都是先更新本地再发送远程请求，在失败的时候rollback。
	// 当同一个Key(比如ServiceName)存在并发时，现在处理所有情况，但不保证都是合理的。
	public static final class SubscribeState extends ReentrantLock {
		private volatile BSubscribeInfo subscribeInfo;
		private volatile BServiceInfosVersion serviceInfos = new BServiceInfosVersion();

		// 服务准备好。
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

		// 同名重订阅时同步本地过滤版本（FND-S2-8）：请求subs里携带的是新version，服务器simple表
		// 已被本次请求覆盖，但computeIfAbsent命中旧state保留旧subscribeInfo——断线重连的
		// onConnected会用它重放，把服务器订阅过滤回退到旧版本桶（灰度切换被重连悄悄回滚）。
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
		 * @return 只读快照（FND-S2-5）：持锁浅拷贝。此前返回活对象，无锁读者（如Exporter.onEdit
		 * 在回调线程）与onRegister/onUnRegister持锁写并发访问同一LongHashMap，结构性修改/扩容
		 * 期间get可错桶漏桶。桶内BServiceInfos引用共享，其读API（getSortedIdentities）为
		 * copy-on-read快照，仍然安全。
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
		 * @return 只读, 禁止修改。持锁get（FND-S2-5）：LongHashMap.get与getOrAddInfos的
		 * 结构性插入/扩容并发时无保护，get必须在锁内完成；返回的BServiceInfos读为
		 * copy-on-read快照，锁外读安全。
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

		// NOT UNDER LOCK

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
				// BEditService.add声明AddOrUpdate以name+id为key：同identity重注册到新版本时，
				// 先从其他版本桶移除旧记录，保证一个identity最多存在于一个版本桶，
				// 否则实例下线后旧版本桶残留幽灵地址（与服务端addAndCollectNotify一致）。
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
				// 注销同样以name+id为key跨全部版本桶收敛（与onRegister、服务端removeAndCollectNotify
				// 一致），移除同identity的全部残留，避免死地址留存在任何版本桶里。
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
						if (current == null || current.findServiceInfo(old) == null)
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
