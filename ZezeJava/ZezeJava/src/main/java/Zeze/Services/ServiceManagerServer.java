package Zeze.Services;

import java.io.Closeable;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Component.ThreadingServer;
import Zeze.Config;
import Zeze.Net.Acceptor;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Raft.RaftConfig;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.*;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.FastLock;
import Zeze.Util.KV;
import Zeze.Util.LongHashMap;
import Zeze.Util.LongHashSet;
import Zeze.Util.LongList;
import Zeze.Util.PerfCounter;
import Zeze.Util.Random;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.w3c.dom.Element;

/**
 * 服务管理：注册和订阅
 * 【名词】
 * 动态服务(gs)
 * 动态服务器一般指启用cache-sync的逻辑服务器。比如gs。
 * 注册服务器（ServiceManager）
 * 支持更新服务器，这个服务一开始是为了启用cache-sync的服务器的查找。
 * 动态服务器列表使用者(linkd)
 * 当前使用动态服务的客户端主要是Game2/linkd，linkd在hash分配请求的时候需要一致动态服务器列表。
 * <p>
 * 【下面的流程都是用现有的服务名字（上面括号中的名字）】
 * <p>
 * 【本控制功能的目标】
 * 所有的linkd的可用动态服务列表的更新并不是原子的。
 * 1. 让所有的linkd的列表保持最新；
 * 2. 尽可能减少linkd上的服务列表不一致的时间（通过ready-commit机制）；
 * 3. 列表不一致时，分发请求可能引起cache不命中，但不影响正确性（cache-sync保证了正确性）；
 * <p>
 * 【主要事件和流程】
 * 1. gs停止时调用 registerService,unRegisterService 向ServiceManager声明自己服务状态。
 * 2. linkd启动时调用 subscribeService, unSubscribeService 向ServiceManager申请使用gs-list。
 * 3. ServiceManager在 registerService,unRegisterService 处理时发送 NotifyServiceList 给所有的 linkd。
 * 4. linkd收到NotifyServiceList先记录到本地，同时持续关注自己和gs之间的连接，
 * 当列表中的所有service都准备完成时调用 ReadyServiceList。
 * 5. ServiceManager收到所有的linkd的ReadyServiceList后，向所有的linkd广播 CommitServiceList。
 * 6. linkd 收到 CommitServiceList 时，启用新的服务列表。
 * <p>
 * 【特别规则和错误处理】
 * 1. linkd 异常停止，ServiceManager 按 unSubscribeService 处理，仅仅简单移除use-list。相当于减少了以后请求来源。
 * 2. gs 异常停止，ServiceManager 按 unRegisterService 处理，移除可用服务，并启动列表更新流程（NotifyServiceList）。
 * 3. linkd 处理 gs 关闭（在NotifyServiceList之前），仅仅更新本地服务列表状态，让该服务暂时不可用，但不改变列表。
 * linkd总是使用ServiceManager提交给他的服务列表，自己不主动增删。
 * linkd在NotifyServiceList的列表减少的处理：一般总是立即进入ready（因为其他gs都是可用状态）。
 * 4. ServiceManager 异常关闭：
 * a) 启用raft以后，新的master会有正确列表数据，但服务状态（连接）未知，此时等待gs的registerService一段时间,
 * 然后开启新的一轮NotifyServiceList，等待时间内没有再次注册的gs以后当作新的处理。
 * b) 启用raft的好处是raft的非master服务器会识别这种状态，并重定向请求到master，使得系统内只有一个master启用服务。
 * 实际上raft不需要维护相同数据状态（gs-list），从空的开始即可，启用raft的话仅使用他的选举功能。
 * #) 由于ServiceManager可以较快恢复，暂时不考虑使用Raft，实现无聊了再来加这个吧
 * 5. ServiceManager开启一轮变更通告过程中，有新的gs启动停止，将开启新的通告(NotifyServiceList)。
 * ReadyServiceList时会检查ready中的列表是否和当前ServiceManagerList一致，不一致直接忽略。
 * 新的通告流程会促使linkd继续发送ready。
 * 另外为了更健壮的处理通告，通告加一个超时机制。超时没有全部ready，就启动一次新的通告。
 * 原则是：总按最新的gs-list通告。中间不一致的ready全部忽略。
 */
public final class ServiceManagerServer extends ReentrantLock implements Closeable {
	static {
		var level = Level.toLevel(System.getProperty("logLevel"), Level.INFO);
		((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger().setLevel(level);
	}

	private static final @NotNull Logger logger = LogManager.getLogger(ServiceManagerServer.class);

	// ServiceInfo.Name -> ServiceState
	private final ConcurrentHashMap<String, ServiceState> serviceStates = new ConcurrentHashMap<>();

	// 简单负载广播，
	// 在registerService/updateService时自动订阅，会话关闭的时候删除。
	// ProcessSetLoad时广播，本来不需要记录负载数据的，但为了以后可能的查询，保存一份。
	private final ConcurrentHashMap<String, LoadObservers> loads = new ConcurrentHashMap<>();

	private static final class LoadObservers extends FastLock {
		private final @NotNull ServiceManagerServer serviceManager;
		private final LongHashSet observers = new LongHashSet();

		public LoadObservers(@NotNull ServiceManagerServer m) {
			serviceManager = m;
		}

		public void addObserver(long sessionId) {
			lock();
			try {
				observers.add(sessionId);
			} finally {
				unlock();
			}
		}

		public void setLoad(@NotNull BServerLoad load) {
			lock();
			try {
				var set = new SetServerLoad(load);
				LongList removed = null;
				for (var it = observers.iterator(); it.moveToNext(); ) {
					long observer = it.value();
					try {
						if (set.Send(serviceManager.server.GetSocket(observer)))
							continue;
					} catch (Throwable ignored) { // ignored
					}
					if (removed == null)
						removed = new LongList();
					removed.add(observer);
				}
				if (removed != null)
					removed.foreach(observers::remove);
			} finally {
				unlock();
			}
		}
	}

	// 需要从配置文件中读取，把这个引用加入：Zeze.Config.AddCustomize
	private final Conf conf = new Conf();
	private NetServer server;
	private final @NotNull AsyncSocket serverSocket;
	private final @NotNull RocksDB autoKeysDb;
	private final ConcurrentHashMap<String, AutoKey> autoKeys = new ConcurrentHashMap<>();

	public static final class Conf implements Config.ICustomize {
		public int keepAlivePeriod = -1;
		public int retryNotifyDelayWhenNotAllReady = 30 * 1000;
		public @NotNull String dbHome = ".";

		public long threadingReleaseTimeout = 30 * 60 * 1000;

		@Override
		public @NotNull String getName() {
			return "Zeze.Services.ServiceManager";
		}

		@Override
		public void parse(@NotNull Element self) {
			String attr = self.getAttribute("KeepAlivePeriod");
			if (!attr.isEmpty())
				keepAlivePeriod = Integer.parseInt(attr);
			attr = self.getAttribute("RetryNotifyDelayWhenNotAllReady");
			if (!attr.isEmpty())
				retryNotifyDelayWhenNotAllReady = Integer.parseInt(attr);
			dbHome = self.getAttribute("DbHome");
			if (dbHome.isEmpty())
				dbHome = ".";
			attr = self.getAttribute("ThreadingReleaseTimeout");
			if (!attr.isBlank())
				threadingReleaseTimeout = Long.parseLong(attr);
		}
	}

	// 每个服务的状态
	public static final class ServiceState {
		private final @NotNull ServiceManagerServer serviceManager;
		private final @NotNull String serviceName;
		// version -> map<identity, serviceInfo>
		// 记录一下SessionId，方便以后找到服务所在的连接。
		private final HashMap<Long, HashMap<String, BServiceInfo>> serviceInfos = new HashMap<>(); // <version,<serverId,info>>
		private final LongHashMap<BSubscribeInfo> simple = new LongHashMap<>(); // key:sessionId

		public ServiceState(@NotNull ServiceManagerServer sm, @NotNull String serviceName) {
			serviceManager = sm;
			this.serviceName = serviceName;
		}

		public void close() {
		}

		public HashMap<Long, HashMap<String, BServiceInfo>> getServiceInfos() {
			return serviceInfos;
		}

		public void addAndCollectNotify(@NotNull BServiceInfo info, @NotNull HashMap<AsyncSocket, EditService> result) {
			// AddOrUpdate，否则重连重新注册很难恢复到正确的状态。
			serviceInfos.computeIfAbsent(info.getVersion(), __ -> new HashMap<>()).put(info.getServiceIdentity(), info);
			for (var it = simple.iterator(); it.moveToNext(); ) {
				var itVersion = it.value().getVersion();
				if (itVersion == 0 || itVersion == info.getVersion()) {
					var peer = serviceManager.server.GetSocket(it.key());
					if (peer != null)
						result.computeIfAbsent(peer, __ -> new EditService()).Argument.getAdd().add(info);
				}
			}
		}

		public void removeAndCollectNotify(@NotNull BServiceInfo info, long sessionId,
										   @NotNull HashMap<AsyncSocket, EditService> result) {
			var versions = serviceInfos.get(info.getVersion());
			if (null == versions)
				return; // version not found

			var exist = versions.remove(info.getServiceIdentity());
			// 有可能当前连接没有注销，新的注册已经AddOrUpdate，此时忽略当前连接的注销。
			if (exist != null && exist.getSessionId() != null && exist.getSessionId() == sessionId) {
				for (var it = simple.iterator(); it.moveToNext(); ) {
					var itVersion = it.value().getVersion();
					if (itVersion == 0 || itVersion == info.getVersion()) {
						var peer = serviceManager.server.GetSocket(it.key());
						if (peer != null)
							result.computeIfAbsent(peer, __ -> new EditService()).Argument.getRemove().add(info);
					}
				}
			}
		}

		public void subscribeAndCollectResult(@NotNull Subscribe r, @NotNull BSubscribeInfo subInfo, long sessionId) {
			// 外面会话的 TryAdd 加入成功，下面TryAdd肯定也成功。
			simple.put(sessionId, subInfo);
			r.Result.map.put(serviceName, new BServiceInfosVersion(subInfo.getVersion(), this));
			for (var versions : serviceInfos.values()) {
				for (var info : versions.values())
					serviceManager.addLoadObserver(info.getPassiveIp(), info.getPassivePort(), r.getSender());
			}
		}
	}

	private final ConcurrentHashMap<Integer, Future<?>> offlineNotifyFutures = new ConcurrentHashMap<>();

	// 每个server连接的状态
	public static final class Session {
		private static final long eOfflineNotifyDelay = 600 * 1000;

		private final @NotNull ServiceManagerServer serviceManager;
		private final long sessionId;
		private final ConcurrentHashSet<BServiceInfo> registers = new ConcurrentHashSet<>(); // 以'服务名+ID'作为key的set
		// key is ServiceName: 会话订阅
		private final ConcurrentHashMap<String, BSubscribeInfo> subscribes = new ConcurrentHashMap<>();
		private final @Nullable Future<?> keepAliveTimerTask;
		private int offlineRegisterServerId; // 原样通知,服务端不关心!!!
		// 目前SM的客户端没有Id，只能使用这个区分来自哪里，所以对于Server来说，这个值必须填写。
		// 如果是负数，将不会进行延迟通知，即这种情况下，通知马上发出。

		private final FastLock offlineRegisterNotifiesLock = new FastLock();
		private final HashMap<String, BOfflineNotify> offlineRegisterNotifies = new HashMap<>(); // 使用的时候加锁保护。value:notifyId

		public Session(@NotNull ServiceManagerServer sm, long sid) {
			serviceManager = sm;
			sessionId = sid;
			if (serviceManager.conf.keepAlivePeriod > 0) {
				keepAliveTimerTask = Task.scheduleUnsafe(
						Random.getInstance().nextInt(serviceManager.conf.keepAlivePeriod),
						serviceManager.conf.keepAlivePeriod,
						() -> {
							AsyncSocket s = null;
							try {
								s = serviceManager.server.GetSocket(sessionId);
								var r = new KeepAlive();
								r.SendAndWaitCheckResultCode(s);
							} catch (Throwable ex) { // resource close. logger.error
								if (s != null)
									s.close(ex);
								else
									logger.error("ServiceManager.KeepAlive", ex);
							}
						});
			} else
				keepAliveTimerTask = null;
		}

		// 底层确保只会回调一次
		public void onClose() {
			if (keepAliveTimerTask != null)
				keepAliveTimerTask.cancel(false);

			var notifies = new HashMap<AsyncSocket, EditService>();
			serviceManager.editLock.lock();
			for (var info : subscribes.values())
				serviceManager.unSubscribeNow(sessionId, info.getServiceName());

			try {
				for (var unReg : registers) {
					var state = serviceManager.serviceStates.get(unReg.getServiceName());
					if (state != null)
						state.removeAndCollectNotify(unReg, sessionId, notifies);
				}
				ServiceManagerServer.sendNotifies(notifies);
			} finally {
				serviceManager.editLock.unlock();
			}

			// offline notify，开启一个线程执行，避免互等造成麻烦。
			// 这个操作不能cancel，即使Server重新起来了，通知也会进行下去。
			if (offlineRegisterServerId >= 0) {
				serviceManager.lock();
				try {
					if (!serviceManager.offlineNotifyFutures.containsKey(offlineRegisterServerId))
						serviceManager.offlineNotifyFutures.put(offlineRegisterServerId,
								Task.scheduleUnsafe(eOfflineNotifyDelay, () -> offlineNotify(true)));
				} finally {
					serviceManager.unlock();
				}
			} else {
				Task.run(() -> offlineNotify(false), "offlineNotifyImmediately");
			}
		}

		private void offlineNotify(boolean delay) {
			if (delay && null == serviceManager.offlineNotifyFutures.remove(offlineRegisterServerId))
				return; // 此serverId的新连接已经连上或者通知已经执行。

			BOfflineNotify[] notifyIds;
			offlineRegisterNotifiesLock.lock();
			try {
				if (offlineRegisterNotifies.isEmpty())
					return; // 不需要通知。
				var values = offlineRegisterNotifies.values();
				notifyIds = values.toArray(new BOfflineNotify[values.size()]);
			} finally {
				offlineRegisterNotifiesLock.unlock();
			}

			logger.info("offlineNotify: serverId={} notifyIds={} begin",
					offlineRegisterServerId, Arrays.toString(notifyIds));
			for (var notifyId : notifyIds) {
				var skips = new HashSet<Session>();
				var notify = new OfflineNotify(notifyId);
				while (true) {
					var selected = randomFor(notifyId.notifyId, skips);
					if (selected == null)
						break; // 没有找到可用的通知对象，放弃通知。
					try {
						notify.SendForWait(selected.getValue()).await();
						logger.info("offlineNotify: serverId={} notifyId={} selectSessionId={} resultCode={}",
								offlineRegisterServerId, notifyId, selected.getKey().sessionId, notify.getResultCode());
						if (notify.getResultCode() == 0)
							break; // 成功通知。done
					} catch (Throwable ignored) { // ignored
					}
					// 保存这一次通知失败session，下一次尝试选择的时候忽略。
					skips.add(selected.getKey());
				}
			}
			logger.info("offlineNotify: serverId={} end", offlineRegisterServerId);
		}

		// 从注册了这个notifyId的其他session中随机选择一个。【实际实现是从连接里面按顺序挑选的】
		private @Nullable KV<Session, AsyncSocket> randomFor(@NotNull String notifyId,
															 @NotNull HashSet<Session> skips) {
			var sessions = new ArrayList<KV<Session, AsyncSocket>>();
			try {
				serviceManager.server.foreach(socket -> {
					var session = (Session)socket.getUserState();
					if (session != null && session != this && !skips.contains(session)) {
						boolean contain;
						session.offlineRegisterNotifiesLock.lock();
						try {
							contain = session.offlineRegisterNotifies.containsKey(notifyId);
						} finally {
							session.offlineRegisterNotifiesLock.unlock();
						}
						if (contain)
							sessions.add(KV.create(session, socket));
					}
				});
			} catch (Exception e) {
				Task.forceThrow(e);
			}
			if (sessions.isEmpty())
				return null;
			return sessions.get(Random.getInstance().nextInt(sessions.size()));
		}
	}

	private void addLoadObserver(@NotNull String ip, int port, @NotNull AsyncSocket sender) {
		if (!ip.isEmpty() && port != 0)
			loads.computeIfAbsent(ip + "_" + port, __ -> new LoadObservers(this)).addObserver(sender.getSessionId());
	}

	private final ReentrantLock editLock = new ReentrantLock(); // 整个edit使用一把锁。不并发了。

	private static void sendNotifies(HashMap<AsyncSocket, EditService> notifies) {
		// todo 增加一些发送错误的日志。
		for (var e : notifies.entrySet()) {
			e.getValue().Send(e.getKey());
		}
	}

	private long processEditService(@NotNull EditService r) {
		var session = (Session)r.getSender().getUserState();
		var notifies = new HashMap<AsyncSocket, EditService>();
		// 原子的完成所有编辑的修改和通知。
		editLock.lock();
		try {
			// step 1: remove
			for (var unReg : r.Argument.getRemove()) {
				var info = session.registers.remove(unReg);
				if (info != null) {
					logger.info("{}: UnRegister {} version={} serverId={} ip={} port={}",
							r.getSender(), info.getServiceName(), info.getVersion(), info.getServiceIdentity(),
							info.getPassiveIp(), info.getPassivePort());
					var state = serviceStates.get(info.getServiceName());
					if (state != null)
						state.removeAndCollectNotify(info, r.getSender().getSessionId(), notifies);
				} else {
					logger.info("{}: Ignore UnRegister {} serverId={}",
							r.getSender(), unReg.getServiceName(), unReg.getServiceIdentity());
				}
			}

			// step 2: add
			// 允许重复登录，断线重连Agent不好原子实现重发。
			for (var reg : r.Argument.getAdd()) {
				if (session.registers.remove(reg) == null) { // 先删除再加入,确保key也更新成新的
					logger.info("{}: Register {} version={} serverId={} ip={} port={}",
							r.getSender(), reg.getServiceName(), reg.getVersion(), reg.getServiceIdentity(),
							reg.getPassiveIp(), reg.getPassivePort());
				} else {
					logger.info("{}: Overwrite Registered {} version={} serverId={} ip={} port={}",
							r.getSender(), reg.getServiceName(), reg.getVersion(), reg.getServiceIdentity(),
							reg.getPassiveIp(), reg.getPassivePort());
				}
				session.registers.add(reg);
				var state = serviceStates.computeIfAbsent(reg.getServiceName(), name -> new ServiceState(this, name));

				// 【警告】
				// 为了简单，这里没有创建新的对象，直接修改并引用了r.Argument。
				// 这个破坏了r.Argument只读的属性。另外引用同一个对象，也有点风险。
				// 在目前没有问题，因为r.Argument主要记录在state.ServiceInfos中，
				// 另外它也被Session引用（用于连接关闭时，自动注销）。
				// 这是专用程序，不是一个库，以后有修改时，小心就是了。
				reg.setSessionId(r.getSender().getSessionId());
				state.addAndCollectNotify(reg, notifies);
			}

			sendNotifies(notifies);
			r.SendResult();
		} finally {
			editLock.unlock();
		}
		return Procedure.Success;
	}

	private long processSubscribe(@NotNull Subscribe r) {
		logger.info("{}: Subscribe {}", r.getSender(), r.Argument);
		var session = (Session)r.getSender().getUserState();

		editLock.lock();
		try {
			for (var sub : r.Argument.subs) {
				session.subscribes.put(sub.getServiceName(), sub);
				serviceStates.computeIfAbsent(sub.getServiceName(), name -> new ServiceState(this, name))
						.subscribeAndCollectResult(r, sub, session.sessionId);
			}
			r.SendResult();
		} finally {
			editLock.unlock();
		}
		return Procedure.Success;
	}

	private void unSubscribeNow(long sessionId, @NotNull String serviceName) {
		var state = serviceStates.get(serviceName);
		if (state != null)
			state.simple.remove(sessionId);
	}

	private long processUnSubscribe(@NotNull UnSubscribe r) {
		logger.info("{}: UnSubscribe {}", r.getSender(), r.Argument);
		var session = (Session)r.getSender().getUserState();

		editLock.lock();
		try {
			for (var serviceName : r.Argument.serviceNames) {
				session.subscribes.remove(serviceName); // continue if not exist
				unSubscribeNow(session.sessionId, serviceName);
			}
			r.SendResult();
		} finally {
			editLock.unlock();
		}
		return Procedure.Success;
	}

	private long processSetLoad(@NotNull SetServerLoad setServerLoad) {
		loads.computeIfAbsent(setServerLoad.Argument.getName(), __ -> new LoadObservers(this))
				.setLoad(setServerLoad.Argument);
		return 0;
	}

	private long processOfflineRegister(@NotNull OfflineRegister r) {
		logger.info("{}: OfflineRegister serverId={} notifyId={}",
				r.getSender(), r.Argument.serverId, r.Argument.notifyId);
		var session = (Session)r.getSender().getUserState();
		// 允许重复注册：简化server注册逻辑。
		Future<?> future;
		session.offlineRegisterNotifiesLock.lock();
		try {
			session.offlineRegisterServerId = r.Argument.serverId;
			session.offlineRegisterNotifies.put(r.Argument.notifyId, r.Argument);
			future = offlineNotifyFutures.remove(session.offlineRegisterServerId);
		} finally {
			session.offlineRegisterNotifiesLock.unlock();
		}
		if (future != null)
			future.cancel(true);
		r.SendResult();
		return 0;
	}

	@SuppressWarnings("MethodMayBeStatic")
	private long processNormalClose(@NotNull NormalClose r) {
		var session = (Session)r.getSender().getUserState();
		session.offlineRegisterNotifiesLock.lock();
		try {
			// 正常关闭，不做异常下线通知。
			session.offlineRegisterNotifies.clear();
		} finally {
			session.offlineRegisterNotifiesLock.unlock();
		}
		r.SendResult();
		return 0;
	}

	@Override
	public void close() {
		try {
			stop();
		} catch (Exception e) {
			Task.forceThrow(e);
		}
	}

	private final @NotNull ThreadingServer threading;

	public ServiceManagerServer(@Nullable InetAddress ipaddress, int port,
								@NotNull Config config) throws Exception {
		this(ipaddress, port, config, "autokeys");
	}

	public ServiceManagerServer(@Nullable InetAddress ipaddress, int port,
								@NotNull Config config,
								@NotNull String autokeys) throws Exception {
		PerfCounter.instance.tryStartScheduledLog();
		config.parseCustomize(this.conf);

		server = new NetServer(this, config);

		server.AddFactoryHandle(EditService.TypeId_, new Service.ProtocolFactoryHandle<>(
				EditService::new, this::processEditService, TransactionLevel.None, DispatchMode.Critical));
		server.AddFactoryHandle(Subscribe.TypeId_, new Service.ProtocolFactoryHandle<>(
				Subscribe::new, this::processSubscribe, TransactionLevel.None, DispatchMode.Critical));
		server.AddFactoryHandle(UnSubscribe.TypeId_, new Service.ProtocolFactoryHandle<>(
				UnSubscribe::new, this::processUnSubscribe, TransactionLevel.None, DispatchMode.Critical));
		server.AddFactoryHandle(KeepAlive.TypeId_, new Service.ProtocolFactoryHandle<>(
				KeepAlive::new, null, TransactionLevel.None, DispatchMode.Direct));
		server.AddFactoryHandle(AllocateId.TypeId_, new Service.ProtocolFactoryHandle<>(
				AllocateId::new, this::processAllocateId, TransactionLevel.None, DispatchMode.Direct));
		server.AddFactoryHandle(SetServerLoad.TypeId_, new Service.ProtocolFactoryHandle<>(
				SetServerLoad::new, this::processSetLoad, TransactionLevel.None, DispatchMode.Critical));
		server.AddFactoryHandle(OfflineRegister.TypeId_, new Service.ProtocolFactoryHandle<>(
				OfflineRegister::new, this::processOfflineRegister, TransactionLevel.None, DispatchMode.Critical));
		server.AddFactoryHandle(OfflineNotify.TypeId_, new Service.ProtocolFactoryHandle<>(
				OfflineNotify::new, null, TransactionLevel.None, DispatchMode.Direct));
		server.AddFactoryHandle(NormalClose.TypeId_, new Service.ProtocolFactoryHandle<>(
				NormalClose::new, this::processNormalClose, TransactionLevel.None, DispatchMode.Critical));

		threading = new ThreadingServer(server, conf);
		threading.RegisterProtocols(server);

		autoKeysDb = RocksDatabase.open(Path.of(this.conf.dbHome, autokeys).toString());

		// 允许配置多个acceptor，如果有冲突，通过日志查看。
		serverSocket = server.newServerSocket(ipaddress, port,
				new Acceptor(port, ipaddress != null ? ipaddress.getHostAddress() : null));
		server.start();
	}

	private static final class AutoKey extends FastLock {
		private final @NotNull ServiceManagerServer sms;
		private final byte @NotNull [] key;
		private final AtomicLong current = new AtomicLong();
		private volatile long max; // 当前可分配的上限(不含)

		public AutoKey(@NotNull String name, @NotNull ServiceManagerServer sms) {
			this.sms = sms;
			var nameBytes = name.getBytes(StandardCharsets.UTF_8);
			var bb = ByteBuffer.Allocate(ByteBuffer.WriteUIntSize(nameBytes.length) + nameBytes.length);
			bb.WriteBytes(nameBytes);
			key = bb.Bytes;
			try {
				var value = sms.autoKeysDb.get(RocksDatabase.getDefaultReadOptions(), key);
				current.set(max = (value != null ? ByteBuffer.Wrap(value).ReadLong() : 1)); // 默认从1开始
			} catch (RocksDBException e) {
				Task.forceThrow(e);
			}
		}

		public void allocate(@NotNull AllocateId rpc) {
			var count = rpc.Argument.getCount();
			if (count < 0)
				count = 0;
			else if (count > 10000) //TODO: 随便修正一下分配数量
				count = 10000;
			for (; ; ) {
				var c = current.get();
				if (c + count <= max) {
					if (current.compareAndSet(c, c + count)) { // 乐观分配,失败重试
						rpc.Result.setStartId(c);
						rpc.Result.setCount(count);
						return;
					}
				} else {
					lock();
					try {
						var m = max; // 只有这里的锁范围才能修改max,所以这里缓存max也是稳定的
						if (c + count > m) { // 重试,也许刚刚提升了max,不够再真正去提升
							m += 10000; //TODO: 先随便用一个增量
							var bb = ByteBuffer.Allocate(ByteBuffer.WriteLongSize(m));
							bb.WriteLong(m);
							try {
								sms.autoKeysDb.put(RocksDatabase.getDefaultWriteOptions(), key, bb.Bytes);
							} catch (RocksDBException e) {
								Task.forceThrow(e);
							}
							max = m; // 确保数据库记下了再更新max,此时其它并发的allocate就可以分配了
						}
					} finally {
						unlock();
					}
				}
			}
		}
	}

	private long processAllocateId(@NotNull AllocateId r) {
		var name = r.Argument.getName();
		r.Result.setName(name);
		autoKeys.computeIfAbsent(name, key -> new AutoKey(key, this)).allocate(r);
		r.SendResult();
		return 0;
	}

	public void stop() throws Exception {
		lock();
		try {
			if (server == null)
				return;
			serverSocket.close();
			server.stop();
			server = null;
			serviceStates.values().forEach(ServiceState::close);
			logger.info("closeDb: {}, autokeys", this.conf.dbHome);
			autoKeysDb.close();
			threading.close();
		} finally {
			unlock();
		}
	}

	public static final class NetServer extends HandshakeServer {
		private final @NotNull ServiceManagerServer serviceManager;
		private final TaskOneByOneByKey oneByOneByKey = new TaskOneByOneByKey();

		public NetServer(@NotNull ServiceManagerServer sm, Config config) {
			super("Zeze.Services.ServiceManager", config);
			serviceManager = sm;
		}

		@Override
		public void OnSocketAccept(@NotNull AsyncSocket so) throws Exception {
			logger.info("OnSocketAccept: {} sessionId={}", so, so.getSessionId());
			so.setUserState(new Session(serviceManager, so.getSessionId()));
			super.OnSocketAccept(so);
		}

		@Override
		public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
			logger.info("OnSocketClose: {} sessionId={}", so, so.getSessionId());
			var session = (Session)so.getUserState();
			if (session != null)
				session.onClose();
			super.OnSocketClose(so, e);
		}

		@Override
		public void dispatchProtocol(long typeId, @NotNull ByteBuffer bb,
									 @NotNull ProtocolFactoryHandle<?> factoryHandle, @Nullable AsyncSocket so) {
			var p = decodeProtocol(typeId, bb, factoryHandle, so);
			if (factoryHandle.Mode == DispatchMode.Direct) {
				// 有几个direct方式的协议,为了性能就不考虑和其它非direct协议的处理顺序了,但因为在IO线程串行处理,这些协议本身的处理还是有顺序的
				Task.call(() -> p.handle(this, factoryHandle), p, Protocol::trySendResultCode);
			} else {
				oneByOneByKey.Execute(p.getSender(),
						() -> Task.call(() -> p.handle(this, factoryHandle), p, Protocol::trySendResultCode),
						factoryHandle.Mode);
			}
			// 不支持事务，由于这里直接OneByOne执行，所以下面两个方法就不重载了。
		}
		/*
		@Override
		public void dispatchProtocol(@NotNull Protocol<?> p, @NotNull ProtocolFactoryHandle<?> factoryHandle) throws Exception {
			// 不支持事务
			Task.executeUnsafe(() -> p.handle(this, factoryHandle),
					p, Protocol::trySendResultCode, null, factoryHandle.Mode);
		}

		@Override
		public <P extends Protocol<?>> void dispatchRpcResponse(@NotNull P rpc, @NotNull ProtocolHandle<P> responseHandle,
																@NotNull ProtocolFactoryHandle<?> factoryHandle) throws Exception {
			// 不支持事务
			Task.executeRpcResponseUnsafe(() -> responseHandle.handle(rpc), rpc, factoryHandle.Mode);
		}
		*/
	}

	public static void main(String[] args) throws Exception {
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			//noinspection CallToPrintStackTrace
			e.printStackTrace();
			logger.error("uncaught exception in {}:", t, e);
		});

		String ip = null;
		int port = 5001;

		String raftName = null;
		String raftConf = "servicemanager.raft.xml";
		String autokeys = "autokeys";

		Task.tryInitThreadPool();

		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-ip":
				ip = args[++i];
				break;
			case "-port":
				port = Integer.parseInt(args[++i]);
				break;
			case "-raft":
				raftName = args[++i];
				break;
			case "-raftConf":
				raftConf = args[++i];
				break;
			case "-threads":
				i++;
				// ThreadPool.SetMinThreads(int.Parse(args[i]), completionPortThreads);
				break;
			case "-autokeys":
				autokeys = args[++i];
				break;
			default:
				throw new IllegalArgumentException("unknown argument: " + args[i]);
			}
		}
		if (raftName == null || raftName.isEmpty()) {
			logger.info("Start {}:{}", ip != null ? ip : "any", port);
			InetAddress address = (ip != null && !ip.isBlank()) ? InetAddress.getByName(ip) : null;
			var conf = new ServiceManagerServer.Conf();
			var config = Config.load();
			config.parseCustomize(conf);
			try (var ignored = new ServiceManagerServer(address, port, config, autokeys)) {
				synchronized (Thread.currentThread()) {
					Thread.currentThread().wait();
				}
			}
		} else if (raftName.equals("RunAllNodes")) {
			logger.info("Start Raft=RunAllNodes");
			//noinspection unused
			try (var raft1 = new ServiceManagerWithRaft("127.0.0.1:6556", RaftConfig.load(raftConf));
				 var raft2 = new ServiceManagerWithRaft("127.0.0.1:6557", RaftConfig.load(raftConf));
				 var raft3 = new ServiceManagerWithRaft("127.0.0.1:6558", RaftConfig.load(raftConf))) {
				synchronized (Thread.currentThread()) {
					Thread.currentThread().wait();
				}
			}
		} else {
			logger.info("Start Raft={},{}", raftName, raftConf);
			//noinspection unused
			try (var raft = new ServiceManagerWithRaft(raftName, RaftConfig.load(raftConf))) {
				synchronized (Thread.currentThread()) {
					Thread.currentThread().wait();
				}
			}
		}
	}
}
