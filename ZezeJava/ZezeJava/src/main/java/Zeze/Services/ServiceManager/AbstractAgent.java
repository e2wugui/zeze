package Zeze.Services.ServiceManager;

import java.io.Closeable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Component.Threading;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Util.Action1;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/*
 * Agent发起协议	ServiceManager处理后通知		Agent接收通知后回调
 * EditService	给订阅者广播Edit				onChanged
 * Subscribe	给发起者回复BSubscribeResult	onChanged
 * UnSubscribe	无							无
 */
public abstract class AbstractAgent extends ReentrantLock implements Closeable {
	static final @NotNull Logger logger = LogManager.getLogger(AbstractAgent.class);

	// key is ServiceName。对于一个Agent，一个服务只能有一个订阅。
	// ServiceName ->
	protected final ConcurrentHashMap<String, Agent.SubscribeState> subscribeStates = new ConcurrentHashMap<>();

	protected Config config;

	/**
	 * 订阅服务状态发生变化时回调。 如果需要处理这个事件，请在订阅前设置回调。
	 */
	protected @Nullable Action1<BEditService> onChanged;
	protected @Nullable Action1<BServerLoad> onSetServerLoad;

	// 返回是否处理成功且不需要其它notifier继续处理
	protected final ConcurrentHashMap<String, Action1<BOfflineNotify>> onOfflineNotifies = new ConcurrentHashMap<>();

	// 应用可以在这个Action内起一个测试事务并执行一次。也可以实现其他检测。
	// ServiceManager 定时发送KeepAlive给Agent，并等待结果。超时则认为服务失效。
	protected @Nullable Runnable onKeepAlive;
	private volatile TaskCompletionSource<TidCache> lastTidCacheFuture;


	protected final ConcurrentHashMap<String, AutoKey> autoKeys = new ConcurrentHashMap<>();

	public final ConcurrentHashMap<String, BServerLoad> loads = new ConcurrentHashMap<>();

	public ConcurrentHashMap<String, Agent.SubscribeState> getSubscribeStates() {
		return subscribeStates;
	}

	public @NotNull Config getConfig() {
		return config;
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

	protected boolean triggerOfflineNotify(@NotNull BOfflineNotify notify) {
		var handle = onOfflineNotifies.get(notify.notifyId);
		if (null == handle)
			return false;

		try {
			handle.run(notify);
			return true;
		} catch (Exception ex) {
			logger.error("", ex);
			return false;
		}
	}

	public @Nullable Runnable getOnKeepAlive() {
		return onKeepAlive;
	}

	public void setOnKeepAlive(@Nullable Runnable value) {
		onKeepAlive = value;
	}

	protected abstract void allocate(@NotNull AutoKey autoKey, int pool);

	public TaskCompletionSource<TidCache> getLastTidCacheFuture() {
		return lastTidCacheFuture;
	}

	public @NotNull TaskCompletionSource<TidCache> allocateTidCacheFuture(String globalName) {
		var future = new TaskCompletionSource<TidCache>();
		var tmp = lastTidCacheFuture;
		var allocateCount = tmp == null ? TidCache.ALLOCATE_COUNT_MIN : tmp.get().allocateCount();
		var sent = allocateAsync(globalName, allocateCount, (rpc) -> {
			lock();
			try {
				if (rpc.getResultCode() == 0) {
					var newest = new TidCache(globalName, this, rpc.Result.getStartId(), rpc.Result.getCount());
					future.setResult(newest);
				} else {
					future.setException(new Exception("AllocateId rc=" + IModule.getErrorCode(rpc.getResultCode())));
				}
			} finally {
				unlock();
			}
			return 0;
		});
		if (!sent)
			future.setException(new Exception("AllocatedId send fail."));

		lock();
		try {
			lastTidCacheFuture = future;
		} finally {
			unlock();
		}
		return future;
	}

	protected abstract boolean allocateAsync(String globalName, int allocCount,
											 ProtocolHandle<Rpc<BAllocateIdArgument, BAllocateIdResult>> callback);

	public abstract void start() throws Exception;

	public abstract void waitReady();

	private static final String SMCallbackOneByOneKey = "SMCallbackOneByOneKey";

	protected void triggerOnChanged(@NotNull BEditService edit) {
		if (onChanged != null) {
			Task.getOneByOne().Execute(SMCallbackOneByOneKey, () -> {
				try {
					onChanged.run(edit);
				} catch (Throwable e) { // logger.error
					logger.error("", e);
				}
			});
		}
	}

	// 【警告】
	// 记住当前已经注册和订阅信息，当ServiceManager连接发生重连时，重新发送请求。
	// 维护这些状态数据都是先更新本地再发送远程请求，在失败的时候rollback。
	// 当同一个Key(比如ServiceName)存在并发时，现在处理所有情况，但不保证都是合理的。
	public static final class SubscribeState extends ReentrantLock {
		private final @NotNull BSubscribeInfo subscribeInfo;
		private volatile @NotNull BServiceInfosVersion serviceInfos = new BServiceInfosVersion();

		// 服务准备好。
		private final ConcurrentHashMap<String, Object> localStates = new ConcurrentHashMap<>();
		private @Nullable Iterator<Map.Entry<String, Object>> localStatesIterator;

		public SubscribeState(@NotNull BSubscribeInfo info) {
			subscribeInfo = info;
		}

		public @Nullable Map.Entry<String, Object> getNextStateEntry() {
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
		public @NotNull String toString() {
			return serviceInfos.toString();
		}

		public BSubscribeInfo getSubscribeInfo() {
			return subscribeInfo;
		}

		public @NotNull String getServiceName() {
			return subscribeInfo.getServiceName();
		}

		/**
		 * @return 只读, 禁止修改
		 */
		public @NotNull BServiceInfosVersion getServiceInfosVersion() {
			return serviceInfos;
		}

		/**
		 * @return 只读, 禁止修改
		 */
		public @Nullable BServiceInfos getServiceInfos(long version) {
			return serviceInfos.getInfos(version);
		}

		/**
		 * @return 只读, 禁止修改
		 */
		public @Nullable BServiceInfos findNewestInfos() {
			return serviceInfos.getNewestInfos();
		}

		public @Nullable BServiceInfo findServiceInfoByIdentity(@NotNull String identity) {
			for (var it = serviceInfos.getInfosIterator(); it.moveToNext(); ) {
				var info = it.value().findServiceInfoByIdentity(identity);
				if (info != null)
					return info;
			}
			return null;
		}

		public @Nullable BServiceInfo findServiceInfoByServerId(int serverId) {
			return findServiceInfoByIdentity(String.valueOf(serverId));
		}

		// NOT UNDER LOCK

		public @NotNull ConcurrentHashMap<String, Object> getLocalStates() {
			return localStates;
		}

		public void setIdentityLocalState(@NotNull String identity, @Nullable Object state) {
			if (state == null)
				localStates.remove(identity);
			else
				localStates.put(identity, state);
		}

		/**
		 * @return 被替换的旧BServiceInfo; 无更新或无变化则返回null
		 */
		public @Nullable BServiceInfo onRegister(@NotNull BServiceInfo info) {
			lock();
			try {
				var versions = serviceInfos.getInfos(info.getVersion());
				if (null != versions) {
					var exist = versions.insert(info);
					return null != exist && !exist.fullEquals(info) ? exist : null;
				}
				return null;
			} finally {
				unlock();
			}
		}

		public boolean onUnRegister(@NotNull BServiceInfo info) {
			lock();
			try {
				var versions = serviceInfos.getInfos(info.getVersion());
				if (null != versions)
					return (null != versions.remove(info));
				return false;
			} finally {
				unlock();
			}
		}

		public void onFirstCommit(@NotNull BServiceInfosVersion infos, @NotNull BEditService edits) {
			for (var it = infos.getInfosIterator(); it.moveToNext(); )
				edits.getAdd().addAll(it.value().getSortedIdentities());
			lock();
			try {
				serviceInfos = infos;
			} finally {
				unlock();
			}
		}
	}

	protected static void verify(@NotNull String identity) {
		if (!identity.startsWith("@") && !identity.startsWith("#")) {
			//noinspection ResultOfMethodCallIgnored
			Integer.parseInt(identity);
		}
	}

	public void registerService(@NotNull BServiceInfo info) {
		var edit = new BEditService();
		edit.getAdd().add(info);
		editService(edit);
	}

	public void unRegisterService(@NotNull BServiceInfo info) {
		var edit = new BEditService();
		edit.getRemove().add(info);
		editService(edit);
	}

	public abstract void editService(@NotNull BEditService arg);

	public @NotNull SubscribeState subscribeService(@NotNull BSubscribeInfo info) {
		var infos = new BSubscribeArgument();
		infos.subs.add(info);
		return subscribeServices(infos).get(0);
	}

	public @NotNull List<SubscribeState> subscribeServices(@NotNull BSubscribeArgument infos) {
		try {
			return subscribeServicesAsync(infos).get();
		} catch (InterruptedException | ExecutionException e) {
			Task.forceThrow(e);
			throw new AssertionError(); // never run here
		}
	}

	public abstract @NotNull CompletableFuture<List<SubscribeState>> subscribeServicesAsync(
			@NotNull BSubscribeArgument info);

	public void unSubscribeService(@NotNull String serviceName) {
		var arg = new BUnSubscribeArgument();
		arg.serviceNames.add(serviceName);
		unSubscribeService(arg);
	}

	public abstract void unSubscribeService(@NotNull BUnSubscribeArgument arg);

	public @NotNull AutoKey getAutoKey(@NotNull String name) {
		return autoKeys.computeIfAbsent(name, k -> new AutoKey(k, this));
	}

	public abstract boolean setServerLoad(@NotNull BServerLoad load);

	public abstract void offlineRegister(@NotNull BOfflineNotify argument, @NotNull Action1<BOfflineNotify> handle);

	protected static void setCurrentAndCount(@NotNull AutoKey autoKey, long current, int count) {
		autoKey.setCurrentAndCount(current, count);
	}

	public abstract @NotNull Threading getThreading();
}
