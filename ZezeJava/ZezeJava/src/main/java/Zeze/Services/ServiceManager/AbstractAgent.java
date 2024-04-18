package Zeze.Services.ServiceManager;

import java.io.Closeable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Component.Threading;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Util.Action1;
import Zeze.Util.Action2;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

/*
 * Agent发起协议	ServiceManager处理后通知			Agent接收通知后回调
 * Register		给订阅者广播Register				优先onUpdate,没有则onChanged
 * UnRegister	给订阅者广播UnRegister			优先onRemove,没有则onChanged
 * Update		给订阅者广播Update				优先onUpdate,没有则onChanged
 * Subscribe	给发起者通知SubscribeFirstCommit	onChanged
 * UnSubscribe	无								无
 */
public abstract class AbstractAgent extends ReentrantLock implements Closeable {
	static final Logger logger = LogManager.getLogger(AbstractAgent.class);

	// key is ServiceName。对于一个Agent，一个服务只能有一个订阅。
	// ServiceName ->
	protected final ConcurrentHashMap<String, Agent.SubscribeState> subscribeStates = new ConcurrentHashMap<>();

	protected Config config;

	/**
	 * 订阅服务状态发生变化时回调。 如果需要处理这个事件，请在订阅前设置回调。
	 */
	protected Action1<Agent.SubscribeState> onChanged;
	protected Action2<Agent.SubscribeState, BServiceInfo> onUpdate;
	protected Action2<Agent.SubscribeState, BServiceInfo> onRemove;
	protected Action1<BServerLoad> onSetServerLoad;

	// 返回是否处理成功且不需要其它notifier继续处理
	protected final ConcurrentHashMap<String, Action1<BOfflineNotify>> onOfflineNotifies = new ConcurrentHashMap<>();

	// 应用可以在这个Action内起一个测试事务并执行一次。也可以实现其他检测。
	// ServiceManager 定时发送KeepAlive给Agent，并等待结果。超时则认为服务失效。
	protected Runnable onKeepAlive;

	protected final ConcurrentHashMap<String, AutoKey> autoKeys = new ConcurrentHashMap<>();

	public final ConcurrentHashMap<String, BServerLoad> loads = new ConcurrentHashMap<>();

	public ConcurrentHashMap<String, Agent.SubscribeState> getSubscribeStates() {
		return subscribeStates;
	}

	public Config getConfig() {
		return config;
	}

	public Action1<Agent.SubscribeState> getOnChanged() {
		return onChanged;
	}

	public void setOnChanged(Action1<Agent.SubscribeState> value) {
		onChanged = value;
	}

	public void setOnSetServerLoad(Action1<BServerLoad> value) {
		onSetServerLoad = value;
	}

	protected boolean triggerOfflineNotify(BOfflineNotify notify) {
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

	public Action2<Agent.SubscribeState, BServiceInfo> getOnRemoved() {
		return onRemove;
	}

	public void setOnRemoved(Action2<Agent.SubscribeState, BServiceInfo> value) {
		onRemove = value;
	}

	public Action2<Agent.SubscribeState, BServiceInfo> getOnUpdate() {
		return onUpdate;
	}

	public void setOnUpdate(Action2<Agent.SubscribeState, BServiceInfo> value) {
		onUpdate = value;
	}

	public Runnable getOnKeepAlive() {
		return onKeepAlive;
	}

	public void setOnKeepAlive(Runnable value) {
		onKeepAlive = value;
	}

	protected abstract void allocate(AutoKey autoKey, int pool);

	public abstract void start() throws Exception;

	public abstract void waitReady();

	// 【警告】
	// 记住当前已经注册和订阅信息，当ServiceManager连接发生重连时，重新发送请求。
	// 维护这些状态数据都是先更新本地再发送远程请求，在失败的时候rollback。
	// 当同一个Key(比如ServiceName)存在并发时，现在处理所有情况，但不保证都是合理的。
	public final class SubscribeState extends ReentrantLock {
		public final BSubscribeInfo subscribeInfo;
		public volatile BServiceInfos serviceInfos;

		// 服务准备好。
		private final ConcurrentHashMap<String, Object> localStates = new ConcurrentHashMap<>();
		private @Nullable Iterator<Map.Entry<String, Object>> localStatesIterator;

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
		public String toString() {
			return serviceInfos.toString();
		}

		public BSubscribeInfo getSubscribeInfo() {
			return subscribeInfo;
		}

		public String getServiceName() {
			return subscribeInfo.getServiceName();
		}

		public BServiceInfos getServiceInfos() {
			return serviceInfos;
		}

		public SubscribeState(BSubscribeInfo info) {
			subscribeInfo = info;
			serviceInfos = new BServiceInfos(info.getServiceName());
		}

		// NOT UNDER LOCK

		public ConcurrentHashMap<String, Object> getLocalStates() {
			return localStates;
		}

		public void setIdentityLocalState(String identity, Object state) {
			if (state == null)
				localStates.remove(identity);
			else
				localStates.put(identity, state);
		}

		private void prepareAndTriggerOnChanged() {
			if (onChanged != null) {
				Task.getCriticalThreadPool().execute(() -> {
					try {
						onChanged.run(this);
					} catch (Throwable e) { // logger.error
						logger.error("", e);
					}
				});
			}
		}

		public void onRegister(BServiceInfo info) {
			lock();
			try {
				serviceInfos.insert(info);
				if (onUpdate != null) {
					Task.getCriticalThreadPool().execute(() -> {
						try {
							onUpdate.run(this, info);
						} catch (Throwable e) { // logger.error
							logger.error("", e);
						}
					});
				} else if (onChanged != null) {
					Task.getCriticalThreadPool().execute(() -> {
						try {
							onChanged.run(this);
						} catch (Throwable e) { // logger.error
							logger.error("", e);
						}
					});
				}
			} finally {
				unlock();
			}
		}

		public void onUnRegister(BServiceInfo info) {
			lock();
			try {
				var removed = serviceInfos.remove(info);
				if (removed == null)
					return;
				if (onRemove != null) {
					Task.getCriticalThreadPool().execute(() -> {
						try {
							onRemove.run(this, removed);
						} catch (Throwable e) { // logger.error
							logger.error("", e);
						}
					});
				} else if (onChanged != null) {
					Task.getCriticalThreadPool().execute(() -> {
						try {
							onChanged.run(this);
						} catch (Throwable e) { // logger.error
							logger.error("", e);
						}
					});
				}
			} finally {
				unlock();
			}
		}

		public void onUpdate(BServiceInfo info) {
			lock();
			try {
				var exist = serviceInfos.findServiceInfo(info);
				if (exist == null)
					return;
				exist.setPassiveIp(info.getPassiveIp());
				exist.setPassivePort(info.getPassivePort());
				exist.setExtraInfo(info.getExtraInfo());

				if (onUpdate != null) {
					Task.getCriticalThreadPool().execute(() -> {
						try {
							onUpdate.run(this, exist);
						} catch (Throwable e) { // logger.error
							logger.error("", e);
						}
					});
				} else if (onChanged != null) {
					Task.getCriticalThreadPool().execute(() -> {
						try {
							onChanged.run(this);
						} catch (Throwable e) { // logger.error
							logger.error("", e);
						}
					});
				}
			} finally {
				unlock();
			}
		}

		public void onFirstCommit(BServiceInfos infos) {
			lock();
			try {
				serviceInfos = infos;
				prepareAndTriggerOnChanged();
			} finally {
				unlock();
			}
		}
	}

	public BServiceInfo registerService(String name, String identity) {
		return registerService(name, identity, null, 0, null);
	}

	public BServiceInfo registerService(String name, String identity, String ip) {
		return registerService(name, identity, ip, 0, null);
	}

	public BServiceInfo registerService(String name, String identity, String ip, int port) {
		return registerService(name, identity, ip, port, null);
	}

	public BServiceInfo registerService(String name, String identity, String ip, int port, Binary extraInfo) {
		return registerService(new BServiceInfo(name, identity, ip, port, extraInfo));
	}

	public BServiceInfo updateService(String name, String identity, String ip, int port, Binary extraInfo) {
		return updateService(new BServiceInfo(name, identity, ip, port, extraInfo));
	}

	public abstract BServiceInfo registerService(BServiceInfo info);

	public abstract BServiceInfo updateService(BServiceInfo info);

	protected static void verify(String identity) {
		if (!identity.startsWith("@") && !identity.startsWith("#")) {
			//noinspection ResultOfMethodCallIgnored
			Integer.parseInt(identity);
		}
	}

	public void unRegisterService(String name, String identity) {
		unRegisterService(new BServiceInfo(name, identity));
	}

	public abstract void unRegisterService(BServiceInfo info);

	public SubscribeState subscribeService(String serviceName) {
		return subscribeService(serviceName, null);
	}

	public SubscribeState subscribeService(String serviceName, Object state) {
		var info = new BSubscribeInfo();
		info.setServiceName(serviceName);
		info.setLocalState(state);
		return subscribeService(info);
	}

	public abstract SubscribeState subscribeService(BSubscribeInfo info);

	public abstract void unSubscribeService(String serviceName);

	public AutoKey getAutoKey(String name) {
		return autoKeys.computeIfAbsent(name, k -> new AutoKey(k, this));
	}

	public abstract boolean setServerLoad(BServerLoad load);

	public abstract void offlineRegister(BOfflineNotify argument, Action1<BOfflineNotify> handle);

	protected static void setCurrentAndCount(AutoKey autoKey, long current, int count) {
		autoKey.setCurrentAndCount(current, count);
	}

	public abstract Threading getThreading();
}
