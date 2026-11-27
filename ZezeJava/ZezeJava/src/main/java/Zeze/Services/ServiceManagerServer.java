package Zeze.Services;

import java.util.Objects;
import java.io.Closeable;
import java.util.Collection;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Component.ThreadingServer;
import Zeze.Config;
import Zeze.Net.Acceptor;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Raft.RaftConfig;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.*;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.FastLock;
import Zeze.Util.LongHashMap;
import Zeze.Util.LongHashSet;
import Zeze.Util.LongList;
import Zeze.Util.Random;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import Zeze.Util.TaskSpec;
import Zeze.Util.ZezeCounter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.jspecify.annotations.Nullable;
import org.rocksdb.RocksDBException;
import org.w3c.dom.Element;
import static Zeze.Util.Args.requireInt;
import static Zeze.Util.Args.requireValue;

/**
 * 服务管理器服务器（非 Raft 版）：提供动态服务注册/订阅与增量推送、负载转发、
 * 全局 AutoKey 与 Id128 发号、Takeover 断线梯队通知及 Threading 协调服务；
 * Raft 版见 {@link ServiceManagerWithRaft}。
 */
public final class ServiceManagerServer extends ReentrantLock implements Closeable {
	// 仅在显式配置 logLevel 系统属性时修改根日志级别，避免类加载期静默影响同 JVM 其他模块
	static void applyLogLevelProperty() {
		var levelProperty = System.getProperty("logLevel");
		if (levelProperty != null)
			((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger()
				.setLevel(Level.toLevel(levelProperty, Level.INFO));
	}

	private static final Logger logger = LogManager.getLogger(ServiceManagerServer.class);

	// ServiceInfo.Name -> ServiceState
	private final ConcurrentHashMap<String, ServiceState> serviceStates = new ConcurrentHashMap<>();

	// 简单负载广播，
	// 在registerService/updateService时自动订阅，会话关闭的时候删除。
	// ProcessSetLoad 只向已登记的观察者广播。
	private final ConcurrentHashMap<String, LoadObservers> loads = new ConcurrentHashMap<>();

	private static final class LoadObservers extends FastLock {
		private final ServiceManagerServer serviceManager;
		private final LongHashSet observers = new LongHashSet();

		public LoadObservers(ServiceManagerServer m) {
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

		public void setLoad(BServerLoad load) {
			lock();
			try {
				var set = new SetServerLoad(load);
				LongList removed = null;
				for (var it = observers.iterator(); it.moveToNext(); ) {
					long observer = it.value();
					try {
						//noinspection DataFlowIssue
						if (set.Send(serviceManager.server.GetSocket(observer)))
							continue;
					} catch (Throwable ignored) {
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

		// 移除指定会话的观察者，返回观察者集合是否已空以便回收地址条目
		public boolean removeObserver(long sessionId) {
			lock();
			try {
				observers.remove(sessionId);
				return observers.isEmpty();
			} finally {
				unlock();
			}
		}
	}

	// 需要从配置文件中读取，把这个引用加入：Zeze.Config.AddCustomize
	private final Conf conf = new Conf();
	private @Nullable NetServer server;
	private final AsyncSocket serverSocket;
	private final RocksDatabase autoKeysDb;
	private final RocksDatabase.Table autoKeyTable;
	private final ConcurrentHashMap<String, AutoKey> autoKeys = new ConcurrentHashMap<>();
	private final Id128UdpServer id128Server;

	public static final class Conf implements Config.ICustomize {
		public int keepAlivePeriod = -1;
		// Takeover 梯队通知的档间超时（毫秒），默认 900s（约 1.5 倍 GS TakeoverTtl），超时后升至下一更高版本档
		public long takeoverNotifyStepMs = 900_000;
		public int retryNotifyDelayWhenNotAllReady = 30 * 1000;
		public String dbHome = ".";

		public long threadingReleaseTimeout = 30 * 60 * 1000;

		@Override
		public String getName() {
			return "Zeze.Services.ServiceManager";
		}

		@Override
		public void parse(Element self) {
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
		private final ServiceManagerServer serviceManager;
		private final String serviceName;
		// version -> map<identity, serviceInfo>
		// 记录一下SessionId，方便以后找到服务所在的连接。
		private final HashMap<Long, HashMap<String, BServiceInfo>> serviceInfos = new HashMap<>(); // <version,<serverId,info>>
		private final LongHashMap<BSubscribeInfo> simple = new LongHashMap<>(); // key:sessionId

		public ServiceState(ServiceManagerServer sm, String serviceName) {
			serviceManager = sm;
			this.serviceName = serviceName;
		}

		public HashMap<Long, HashMap<String, BServiceInfo>> getServiceInfos() {
			return serviceInfos;
		}

		// 通知订阅了info版本的会话（version==0订阅全部版本）。info的版本决定通知过滤。
		private void collectNotify(BServiceInfo info, boolean isAdd,
								   HashMap<AsyncSocket, EditService> result) {
			for (var it = simple.iterator(); it.moveToNext(); ) {
				var itVersion = it.value().getVersion();
				if (itVersion == 0 || itVersion == info.getVersion()) {
					//noinspection DataFlowIssue
					var peer = serviceManager.server.GetSocket(it.key());
					if (peer != null) {
						var notify = result.computeIfAbsent(peer, __ -> new EditService());
						if (isAdd)
							notify.Argument.getAdd().add(info);
						else
							notify.Argument.getRemove().add(info);
					}
				}
			}
		}

		public void addAndCollectNotify(BServiceInfo info, HashMap<AsyncSocket, EditService> result) {
			// AddOrUpdate 语义：同 identity 跨版本重注册时先从其他版本桶移除旧记录并通知其订阅者
			for (var e : serviceInfos.entrySet()) {
				if (e.getKey() == info.getVersion())
					continue;
				var old = e.getValue().remove(info.getServiceIdentity());
				if (old != null)
					collectNotify(old, false, result);
			}
			serviceInfos.values().removeIf(HashMap::isEmpty);
			serviceInfos.computeIfAbsent(info.getVersion(), __ -> new HashMap<>()).put(info.getServiceIdentity(), info);
			collectNotify(info, true, result);
			// 为现有订阅者补登新注册实例的负载观察者（editLock 内迭代 simple 安全）
			for (var it = simple.iterator(); it.moveToNext(); )
				serviceManager.addLoadObserver(info.getPassiveIp(), info.getPassivePort(), it.key());
		}

		public void removeAndCollectNotify(BServiceInfo info, long sessionId,
										   HashMap<AsyncSocket, EditService> result) {
			// 注销同样以name+id为key跨全部版本桶收敛（与addAndCollectNotify、客户端onUnRegister一致）。
			for (var e : serviceInfos.entrySet()) {
				var exist = e.getValue().get(info.getServiceIdentity());
				// 有可能当前连接没有注销，新的注册已经AddOrUpdate，此时忽略当前连接的注销。
				if (exist == null || exist.getSessionId() == null || exist.getSessionId() != sessionId)
					continue;
				e.getValue().remove(info.getServiceIdentity());
				collectNotify(exist, false, result);
			}
			serviceInfos.values().removeIf(HashMap::isEmpty);
		}

		public void subscribeAndCollectResult(Subscribe r, BSubscribeInfo subInfo, long sessionId) {
			// 外面会话的 TryAdd 加入成功，下面TryAdd肯定也成功。
			simple.put(sessionId, subInfo);
			r.Result.map.put(serviceName, new BServiceInfosVersion(subInfo.getVersion(), this));
			for (var versions : serviceInfos.values()) {
				for (var info : versions.values())
					serviceManager.addLoadObserver(info.getPassiveIp(), info.getPassivePort(), Objects.requireNonNull(r.getSender()));
			}
		}
	}

	// 每个server连接的状态
	public static final class Session {
		private final ServiceManagerServer serviceManager;
		private final long sessionId;
		private final ConcurrentHashSet<BServiceInfo> registers = new ConcurrentHashSet<>(); // 以'服务名+ID'作为key的set
		// key is ServiceName: 会话订阅
		private final ConcurrentHashMap<String, BSubscribeInfo> subscribes = new ConcurrentHashMap<>();
		private final @Nullable Future<?> keepAliveTimerTask;
		private volatile int identifyServerId = -1; // Identify上报的serverId；-1=未上报。断线时据此广播Suspect。
		private volatile long identifyVersion = -1; // Identify上报的进程数据格式版本；-1=未上报（旧客户端按0）。断线时据此做接管梯队。

		public Session(ServiceManagerServer sm, long sid) {
			serviceManager = sm;
			sessionId = sid;
			if (serviceManager.conf.keepAlivePeriod > 0) {
				keepAliveTimerTask = TaskSpec.ofAction(() -> {
					AsyncSocket s = null;
					try {
						//noinspection DataFlowIssue
						s = serviceManager.server.GetSocket(sessionId);
						if (s == null)
							return; // 会话已关闭/已被清理，KeepAlive 无事可做
						var r = new KeepAlive();
						// 异步发送并在回调中判活关闭，避免在调度线程同步等待耗尽调度池
						final var sock = s;
						if (!r.Send(s, response -> {
							if (response.isTimeout() || response.getResultCode() != KeepAlive.Success)
								sock.close(new java.io.IOException("KeepAlive fail: " + response));
							return 0;
						}))
							sock.close(new java.io.IOException("KeepAlive send fail"));
					} catch (Throwable ex) { // resource close. logger.error
						if (s != null)
							s.close(ex);
						else
							logger.error("ServiceManager.KeepAlive", ex);
					}
				}).schedulePeriodNow(
					Random.getInstance().nextInt(serviceManager.conf.keepAlivePeriod),
					serviceManager.conf.keepAlivePeriod);
			} else
				keepAliveTimerTask = null;
		}

		// 底层确保只会回调一次
		public void onClose() {
			if (keepAliveTimerTask != null)
				keepAliveTimerTask.cancel(false);

			// 不持 editLock 立即发起 Suspect 梯队广播（仅作提示，实际接管由租约表裁决）
			var suspectServerId = identifyServerId;
			if (suspectServerId >= 0) {
				var deadVersion = identifyVersion >= 0 ? identifyVersion : 0; // 未上报version（旧客户端）按0
				serviceManager.startSuspectTier(suspectServerId, sessionId, deadVersion);
			}

			var notifies = new HashMap<AsyncSocket, EditService>();
			serviceManager.editLock.lock();

			try {
				// 联动清理该会话登记的全部负载观察者（地址行随之回收）。
				serviceManager.removeLoadObservers(sessionId);

				for (var info : subscribes.values())
					serviceManager.unSubscribeNow(sessionId, info.getServiceName());

				for (var unReg : registers) {
					var state = serviceManager.serviceStates.get(unReg.getServiceName());
					if (state != null)
						state.removeAndCollectNotify(unReg, sessionId, notifies);
				}
				ServiceManagerServer.sendNotifies(notifies);
			} finally {
				serviceManager.editLock.unlock();
			}
		}

	}

	private void addLoadObserver(String ip, int port, AsyncSocket sender) {
		addLoadObserver(ip, port, sender.getSessionId());
	}

	// 观察者按 sessionId 登记，会话关闭时联动清理，转发失败时也会惰性剔除
	private void addLoadObserver(String ip, int port, long sessionId) {
		if (!ip.isEmpty() && port != 0)
			loads.computeIfAbsent(ip + "_" + port, __ -> new LoadObservers(this)).addObserver(sessionId);
	}

	// 会话关闭时清理其登记的观察者，并回收已空的地址条目
	private void removeLoadObservers(long sessionId) {
		loads.entrySet().removeIf(entry -> entry.getValue().removeObserver(sessionId));
	}

	private final ReentrantLock editLock = new ReentrantLock(); // 整个edit使用一把锁。不并发了。

	// 必须在 editLock 内调用：与 OnSocketClose 先摘除 socketMap 再持锁清理构成全序，防止迟到协议复活死会话状态
	private boolean isSenderAlive(AsyncSocket sender) {
		//noinspection DataFlowIssue
		return server.GetSocket(sender.getSessionId()) == sender;
	}

	private static void sendNotifies(HashMap<AsyncSocket, EditService> notifies) {
		// todo 增加一些发送错误的日志。
		for (var e : notifies.entrySet()) {
			e.getValue().Send(e.getKey());
		}
	}

	// 校验 identity 格式（非 '@'/'#' 前缀须可解析为 long），防止非法值破坏订阅端排序
	private static boolean isLegalServiceIdentity(String identity) {
		if (identity.startsWith("@") || identity.startsWith("#"))
			return true;
		try {
			Long.parseLong(identity);
			return true;
		} catch (NumberFormatException e) {
			logger.warn("illegal service identity: '{}'", identity);
			return false;
		}
	}

	// 入口字段长度、单批条数与单会话上限（全局唯一 serviceName 上限复用 Id128UdpServer.MAX_UNIQUE_NAMES）
	private static final int SVC_NAME_MAX_BYTES = 128;
	private static final int SVC_IDENTITY_MAX_BYTES = 128;
	private static final int SVC_IP_MAX_BYTES = 64;
	private static final int SVC_EXTRA_MAX_BYTES = 256;
	private static final int SVC_EDIT_BATCH_MAX = 128;
	private static final int SVC_PER_SESSION_MAX = 64;

	private static boolean isOverUtf8Bytes(@Nullable String s, int maxBytes) {
		return s != null && s.getBytes(StandardCharsets.UTF_8).length > maxBytes;
	}

	// 拒绝告警限频（每 60 秒至多一条）
	private volatile long lastSvcRejectLogMs;

	private void warnSvcRejected(String reason) {
		var now = System.currentTimeMillis();
		var last = lastSvcRejectLogMs;
		if (now - last < 60_000)
			return;
		lastSvcRejectLogMs = now;
		logger.error("SM edit/subscribe rejected (possible attack or misbehaving client), serviceStates={}: {}",
			serviceStates.size(), reason);
	}

	// 满员时在 editLock 内逐出一个无注册无订阅的空壳行腾位
	private boolean evictIdleServiceState(String excludeName) {
		for (var it = serviceStates.entrySet().iterator(); it.hasNext(); ) {
			var e = it.next();
			if (!e.getKey().equals(excludeName)
				&& e.getValue().getServiceInfos().isEmpty() && e.getValue().simple.isEmpty()) {
				it.remove();
				return true;
			}
		}
		return false;
	}

	// 只规划、不修改：整批验证成功后，先注销，再应用逐出计划，最后注册/订阅并通知。
	private @Nullable ArrayList<String> planServiceNames(Set<String> requested, Set<BServiceInfo> removes, long sessionId) {
		int needed = 0;
		for (var name : requested) {
			if (!serviceStates.containsKey(name))
				++needed;
		}
		int deficit = serviceStates.size() + needed - Id128UdpServer.MAX_UNIQUE_NAMES;
		var evictions = new ArrayList<String>();
		if (deficit <= 0)
			return evictions;
		for (var entry : serviceStates.entrySet()) {
			var state = entry.getValue();
			if (requested.contains(entry.getKey()) || !state.simple.isEmpty())
				continue;
			boolean idle = true;
			for (var bucket : state.serviceInfos.values()) {
				for (var info : bucket.values()) {
					if (!Long.valueOf(sessionId).equals(info.getSessionId()) || !removes.contains(info)) {
						idle = false;
						break;
					}
				}
				if (!idle)
					break;
			}
			if (idle) {
				evictions.add(entry.getKey());
				if (evictions.size() == deficit)
					return evictions;
			}
		}
		return null;
	}

	static Collection<BServiceInfo> finalRegistrations(List<BServiceInfo> adds) {
		if (adds.size() < 2)
			return adds;
		// 一批编辑只发布每个 name+identity 的最终版本。否则中间版本的订阅者可能
		// 收到同一地址的 add 和 remove，按协议先删后加之后留下已不存在的地址。
		var byIdentity = new LinkedHashMap<BServiceInfo, BServiceInfo>();
		for (var info : adds)
			byIdentity.putLast(info, info);
		return byIdentity.values();
	}

	private long processEditService(EditService r) {
		var add = r.Argument.getAdd();
		var remove = r.Argument.getRemove();
		if (add.size() + remove.size() > SVC_EDIT_BATCH_MAX) {
			warnSvcRejected("edit batch exceeded " + SVC_EDIT_BATCH_MAX + ": " + (add.size() + remove.size()));
			return Procedure.ErrorRequestId;
		}
		for (var info : add) {
			if (!isLegalServiceIdentity(info.getServiceIdentity()))
				return Procedure.ErrorRequestId;
				// 字段长度上限（identity非'@'/'#'通道已被isLegalServiceIdentity数字化封顶）。
			if (isOverUtf8Bytes(info.getServiceName(), SVC_NAME_MAX_BYTES) ||
				isOverUtf8Bytes(info.getServiceIdentity(), SVC_IDENTITY_MAX_BYTES) ||
				isOverUtf8Bytes(info.getPassiveIp(), SVC_IP_MAX_BYTES) ||
				info.getExtraInfo().size() > SVC_EXTRA_MAX_BYTES) {
				warnSvcRejected("edit field over size: " + info.getServiceName());
				return Procedure.ErrorRequestId;
			}
		}
		for (var info : r.Argument.getRemove())
			if (!isLegalServiceIdentity(info.getServiceIdentity()))
				return Procedure.ErrorRequestId;
		var finalAdds = finalRegistrations(add);
		//noinspection DataFlowIssue
		var session = (Session)r.getSender().getUserState();
		var notifies = new HashMap<AsyncSocket, EditService>();
		// 原子的完成所有编辑的修改和通知。
		editLock.lock();
		try {
			if (!isSenderAlive(r.getSender())) { // 迟到协议，会话已清理——拒绝防死注册复活
				r.SendResultCode(ServiceManagerWithRaft.ErrorNotLogin);
				return Procedure.Success;
			}
			var finalRegisters = new HashSet<BServiceInfo>();
			//noinspection DataFlowIssue
			for (var info : session.registers)
				finalRegisters.add(info);
			remove.forEach(finalRegisters::remove);
			finalRegisters.addAll(finalAdds);
			var requestedNames = new HashSet<String>();
			for (var info : finalAdds)
				requestedNames.add(info.getServiceName());
			var evictions = planServiceNames(requestedNames, new HashSet<>(remove), session.sessionId);
			if (finalRegisters.size() > SVC_PER_SESSION_MAX || evictions == null) {
				warnSvcRejected("edit final capacity exceeded");
				r.SendResultCode(Procedure.ErrorRequestId);
				return Procedure.Success;
			}
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

			for (var name : evictions)
				serviceStates.remove(name);

			// step 2: add
			// 允许重复登录，断线重连Agent不好原子实现重发。
			for (var reg : finalAdds) {
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

	private long processSubscribe(Subscribe r) {
		logger.info("{}: Subscribe {}", r.getSender(), r.Argument);
		//noinspection DataFlowIssue
		var session = (Session)r.getSender().getUserState();

		if (r.Argument.subs.size() > SVC_EDIT_BATCH_MAX) {
			warnSvcRejected("subscribe batch exceeded " + SVC_EDIT_BATCH_MAX + ": " + r.Argument.subs.size());
			return Procedure.ErrorRequestId;
		}
		for (var sub : r.Argument.subs)
			if (isOverUtf8Bytes(sub.getServiceName(), SVC_NAME_MAX_BYTES)) {
				warnSvcRejected("subscribe name over size");
				return Procedure.ErrorRequestId;
			}
		editLock.lock();
		try {
			if (!isSenderAlive(r.getSender())) { // 迟到协议，会话已清理——拒绝防死订阅残留
				r.SendResultCode(ServiceManagerWithRaft.ErrorNotLogin);
				return Procedure.Success;
			}
			//noinspection DataFlowIssue
			var finalSubscriptions = new HashSet<>(session.subscribes.keySet());
			var requestedNames = new HashSet<String>();
			for (var sub : r.Argument.subs) {
				finalSubscriptions.add(sub.getServiceName());
				requestedNames.add(sub.getServiceName());
			}
			var evictions = planServiceNames(requestedNames, Set.of(), session.sessionId);
			if (finalSubscriptions.size() > SVC_PER_SESSION_MAX || evictions == null) {
				warnSvcRejected("subscribe final capacity exceeded");
				r.SendResultCode(Procedure.ErrorRequestId);
				return Procedure.Success;
			}
			for (var name : evictions)
				serviceStates.remove(name);
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

	private void unSubscribeNow(long sessionId, String serviceName) {
		var state = serviceStates.get(serviceName);
		if (state != null)
			state.simple.remove(sessionId);
	}

	private long processUnSubscribe(UnSubscribe r) {
		logger.info("{}: UnSubscribe {}", r.getSender(), r.Argument);
		//noinspection DataFlowIssue
		var session = (Session)r.getSender().getUserState();

		editLock.lock();
		try {
			if (!isSenderAlive(r.getSender())) { // 迟到协议，会话已清理——拒绝防死退订写脏状态
				r.SendResultCode(ServiceManagerWithRaft.ErrorNotLogin);
				return Procedure.Success;
			}
			for (var serviceName : r.Argument.serviceNames) {
				//noinspection DataFlowIssue
				session.subscribes.remove(serviceName); // continue if not exist
				unSubscribeNow(session.sessionId, serviceName);
			}
			r.SendResult();
		} finally {
			editLock.unlock();
		}
		return Procedure.Success;
	}

	private long processSetLoad(SetServerLoad setServerLoad) {
		if (isOverUtf8Bytes(setServerLoad.Argument.getName(), SVC_NAME_MAX_BYTES)
			|| isOverUtf8Bytes(setServerLoad.Argument.ip, SVC_IP_MAX_BYTES)) {
			warnSvcRejected("setLoad field over size");
			return 0; // 非Rpc：拒绝即静默丢弃
		}
		editLock.lock();
		try {
			// 判活与会话清理串行。上报本身没有观察者，也不保存历史负载。
			if (!isSenderAlive(Objects.requireNonNull(setServerLoad.getSender())))
				return 0;
			var observers = loads.get(setServerLoad.Argument.getName());
			if (observers != null)
				observers.setLoad(setServerLoad.Argument);
			return 0;
		} finally {
			editLock.unlock();
		}
	}

	// 只写session上一个int（Direct派发内完成、无锁、无取消语义）。
	@SuppressWarnings("MethodMayBeStatic")
	private long processIdentify(Identify r) {
		//noinspection DataFlowIssue
		var session = (Session)r.getSender().getUserState();
		if (session != null) {
			session.identifyServerId = r.Argument.serverId;
			session.identifyVersion = r.Argument.version;
			logger.info("{}: Identify serverId={} version={}", r.getSender(), r.Argument.serverId, r.Argument.version);
		}
		return 0;
	}

	// Takeover 版本梯队通知：优先通知同 version 节点，档间超时无 TakeoverDone 回执则按 version 升序推进，
	// 永不通知低于死者 version 的节点；状态仅驻留内存，SM 重启后由 GS 侧定期扫描兜底。
	private static final class SuspectTier {
		final long deadVersion;
		volatile long tierVersion; // 已广播到的档位
		volatile @Nullable Future<?> timer;
		SuspectTier(long deadVersion, long tierVersion) {
			this.deadVersion = deadVersion;
			this.tierVersion = tierVersion;
		}
	}

	private final ConcurrentHashMap<Integer, SuspectTier> suspectTiers = new ConcurrentHashMap<>();
	private volatile boolean closed;

	/** 梯队广播一档：只发 identifyVersion==tierVersion 的存活会话（死者会话排除）。 */
	private void broadcastSuspectTier(int deadServerId, long deadSessionId, long deadVersion, long tierVersion) {
		var srv = server;
		if (closed || srv == null)
			return;
		var suspect = new Suspect();
		suspect.Argument.serverId = deadServerId;
		suspect.Argument.version = deadVersion;
		try {
			srv.foreach(so -> {
				if (so.getSessionId() == deadSessionId)
					return; // 刚断线的会话本身不报信
				var netSession = (Session)so.getUserState();
				if (netSession == null || netSession.identifyServerId < 0)
					return; // 未Identify的会话不参与
				if (netSession.identifyVersion != tierVersion)
					return; // 梯队过滤（需求①③）
				so.Send(suspect);
			});
		} catch (Exception e) {
			logger.warn("Suspect tier broadcast for serverId={} tier={} failed", deadServerId, tierVersion, e);
		}
	}

	/** onClose入口：广播首档（==死者version）并安排升档链。 */
	private void startSuspectTier(int deadServerId, long deadSessionId, long deadVersion) {
		if (closed)
			return;
		var tier = new SuspectTier(deadVersion, deadVersion);
		var old = suspectTiers.put(deadServerId, tier);
		if (old != null && old.timer != null)
			//noinspection DataFlowIssue
			old.timer.cancel(false); // 死者反复闪断：新周期覆盖旧升档链
		if (closed) {
			suspectTiers.remove(deadServerId, tier);
			return;
		}
		broadcastSuspectTier(deadServerId, deadSessionId, deadVersion, deadVersion);
		scheduleNextTier(deadServerId, deadSessionId, tier);
	}

	/** 档间超时：无回执升到"更高存活version的最小值"；没有更高档=放弃（scan兜底）。 */
	private void scheduleNextTier(int deadServerId, long deadSessionId, SuspectTier tier) {
		if (closed)
			return;
		tier.timer = TaskSpec.ofAction(() -> {
			if (closed)
				return;
			var cur = suspectTiers.get(deadServerId);
			if (cur != tier)
				return; // 已被回执移除/被新周期覆盖
			var srv = server;
			if (srv == null)
				return;
			var next = new long[]{Long.MAX_VALUE};
			final var deadSessionIdRef = deadSessionId;
			try {
				srv.foreach(so -> {
					if (so.getSessionId() == deadSessionIdRef)
						return;
					var netSession = (Session)so.getUserState();
					if (netSession == null || netSession.identifyServerId < 0)
						return;
					var v = netSession.identifyVersion;
					if (v > cur.tierVersion && v < next[0])
						next[0] = v; // 下一档=更高存活version的最小值（需求②升序）
				});
			} catch (Exception e) {
				logger.warn("Suspect tier scan for serverId={} failed", deadServerId, e);
			}
			if (closed)
				return;
			if (next[0] == Long.MAX_VALUE) {
				suspectTiers.remove(deadServerId, cur); // 没有更高档：放弃升档
				logger.info("Suspect tier exhausted for serverId={} lastTier={}", deadServerId, cur.tierVersion);
				return;
			}
			cur.tierVersion = next[0];
			broadcastSuspectTier(deadServerId, deadSessionIdRef, cur.deadVersion, next[0]);
			scheduleNextTier(deadServerId, deadSessionIdRef, cur);
		}).scheduleNow(conf.takeoverNotifyStepMs);
		if (closed) {
			var timer = tier.timer;
			if (timer != null)
				timer.cancel(false);
			suspectTiers.remove(deadServerId, tier);
		}
	}

	private void cancelSuspectTiers() {
		for (var tier : suspectTiers.values()) {
			var timer = tier.timer;
			if (timer != null)
				timer.cancel(false);
		}
		suspectTiers.clear();
	}

	// 只写session上字段（Direct派发内完成、无锁）。回执到达即整条梯队停止（需求④）。
	@SuppressWarnings("MethodMayBeStatic")
	private long processTakeoverDone(TakeoverDone r) {
		var tier = suspectTiers.remove(r.Argument.deadServerId);
		if (tier != null && tier.timer != null)
			//noinspection DataFlowIssue
			tier.timer.cancel(false);
		logger.info("TakeoverDone serverId={} taker={} version={} tier={}",
				r.Argument.deadServerId, r.Argument.takerServerId, r.Argument.takerVersion,
				tier == null ? -1 : tier.tierVersion);
		return 0;
	}

	@Override
	public void close() {
		try {
			stop();
		} catch (Exception e) {
			throw Task.forceThrow(e);
		}
	}

	private final ThreadingServer threading;

	public ServiceManagerServer(@Nullable InetAddress ipaddress, int port,
								Config config) throws Exception {
		this(ipaddress, port, config, "autokeys");
	}

	public ServiceManagerServer(@Nullable InetAddress ipaddress, int port,
								Config config,
								String autokeys) throws Exception {
		ZezeCounter.tryInit();
		applyLogLevelProperty(); // 显式启动动作（仅显式指定logLevel属性才动配置）
		config.parseCustomize(this.conf);

		// 资源先在局部变量上构造并在异常时逆序清理；回调依赖的字段须在端口监听前发布
		var localServer = new NetServer(this, config);
		var localThreading = new ThreadingServer(localServer, conf);
		RocksDatabase localDb = null;
		AsyncSocket localSocket = null;
		Id128UdpServer localId128 = null;
		try {
			localServer.AddFactoryHandle(EditService.TypeId_, new Service.ProtocolFactoryHandle<>(
				EditService::new, this::processEditService, TransactionLevel.None, DispatchMode.Critical));
			localServer.AddFactoryHandle(Subscribe.TypeId_, new Service.ProtocolFactoryHandle<>(
				Subscribe::new, this::processSubscribe, TransactionLevel.None, DispatchMode.Critical));
			localServer.AddFactoryHandle(UnSubscribe.TypeId_, new Service.ProtocolFactoryHandle<>(
				UnSubscribe::new, this::processUnSubscribe, TransactionLevel.None, DispatchMode.Critical));
			localServer.AddFactoryHandle(KeepAlive.TypeId_, new Service.ProtocolFactoryHandle<>(
				KeepAlive::new, null, TransactionLevel.None, DispatchMode.Direct));
			localServer.AddFactoryHandle(AllocateId.TypeId_, new Service.ProtocolFactoryHandle<>(
				AllocateId::new, this::processAllocateId, TransactionLevel.None, DispatchMode.Direct));
			localServer.AddFactoryHandle(SetServerLoad.TypeId_, new Service.ProtocolFactoryHandle<>(
				SetServerLoad::new, this::processSetLoad, TransactionLevel.None, DispatchMode.Critical));
			localServer.AddFactoryHandle(Identify.TypeId_, new Service.ProtocolFactoryHandle<>(
				Identify::new, this::processIdentify, TransactionLevel.None, DispatchMode.Direct));
			localServer.AddFactoryHandle(TakeoverDone.TypeId_, new Service.ProtocolFactoryHandle<>(
				TakeoverDone::new, this::processTakeoverDone, TransactionLevel.None, DispatchMode.Direct));

			localThreading.RegisterProtocols(localServer);

			localDb = new RocksDatabase(Path.of(this.conf.dbHome, autokeys).toString());
			var localAutoKeyTable = localDb.getOrAddTable("autokey");
			var id128Table = localDb.getOrAddTable("id128");

			// 在 newServerSocket 开放监听前发布网络回调依赖的字段，避免首包到达时读到 null
			server = localServer;
			threading = localThreading;
			autoKeysDb = localDb;
			autoKeyTable = localAutoKeyTable;

			// 允许配置多个acceptor，如果有冲突，通过日志查看。
			localSocket = localServer.newServerSocket(ipaddress, port,
				new Acceptor(port, ipaddress != null ? ipaddress.getHostAddress() : null));
			localServer.start();
			localId128 = new Id128UdpServer(id128Table, ipaddress != null ? ipaddress.getHostAddress() : null, port); // todo 先使用和tcp一样的端口.自动选择下一步.
			localId128.start();

			// 全部成功，发布完成态到字段
			serverSocket = localSocket;
			id128Server = localId128;
		} catch (Throwable e) {
			if (localId128 != null) {
				try {
					localId128.stop();
				} catch (Throwable ce) {
					e.addSuppressed(ce);
				}
			}
			if (localSocket != null) {
				try {
					localSocket.close();
				} catch (Throwable ce) {
					e.addSuppressed(ce);
				}
			}
			try {
				localServer.stop();
			} catch (Throwable ce) {
				e.addSuppressed(ce);
			}
			if (localDb != null) {
				try {
					localDb.close();
				} catch (Throwable ce) {
					e.addSuppressed(ce);
				}
			}
			try {
				localThreading.close();
			} catch (Throwable ce) {
				e.addSuppressed(ce);
			}
			throw e;
		}
	}

	private static final class AutoKey extends FastLock {
		private final ServiceManagerServer sms;
		private final byte [] key;
		private final AtomicLong current = new AtomicLong();
		private volatile long max; // 当前可分配的上限(不含)

		public AutoKey(String name, ServiceManagerServer sms) {
			this.sms = sms;
			var nameBytes = name.getBytes(StandardCharsets.UTF_8);
			var bb = ByteBuffer.Allocate(ByteBuffer.WriteUIntSize(nameBytes.length) + nameBytes.length);
			bb.WriteBytes(nameBytes);
			key = bb.Bytes;
			try {
				var value = sms.autoKeyTable.get(key);
				current.set(max = (value != null ? ByteBuffer.Wrap(value).ReadLong() : 1)); // 默认从1开始
			} catch (RocksDBException e) {
				throw Task.forceThrow(e);
			}
		}

		// 须在持锁且确认仍在册后调用，保证与满员逐出（tryLock）互斥，避免重号
		public void allocateLocked(AllocateId rpc, int count) {
			var c = current.get();
			if (c + count > max) {
				var m = max + count * 10L;
				var bb = ByteBuffer.Allocate(ByteBuffer.WriteLongSize(m));
				bb.WriteLong(m);
				try {
					sms.autoKeyTable.put(key, bb.Bytes);
				} catch (RocksDBException e) {
					throw Task.forceThrow(e);
				}
				max = m; // 确保数据库记下了再更新max
			}
			current.set(c + count);
			rpc.Result.setStartId(c);
			rpc.Result.setCount(count);
		}
	}

	private long processAllocateId(AllocateId r) {
		var name = r.Argument.getName();
		var count = r.Argument.getCount();
		// 校验分配数量与 name 长度上限，防止恶意大 count 烧号或超长 name 膨胀存储
		if (count < 1 || count > Tid128Cache.ALLOCATE_COUNT_MAX) {
			warnAllocateIdRejected("invalid count=" + count);
			r.SendResultCode(Procedure.ErrorRequestId);
			return Procedure.Success;
		}
		//noinspection DataFlowIssue
		if (name.getBytes(StandardCharsets.UTF_8).length > 128) {
			warnAllocateIdRejected("name too long: chars=" + name.length());
			r.SendResultCode(Procedure.ErrorRequestId);
			return Procedure.Success;
		}
		// 唯一名满员时逐出闲置条目腾位（RocksDB 高水位保留，重建不会重号）
		if (!autoKeys.containsKey(name) && autoKeys.size() >= Id128UdpServer.MAX_UNIQUE_NAMES
			&& !evictIdleAutoKey()) {
			warnAllocateIdRejected("unique names exceeded " + Id128UdpServer.MAX_UNIQUE_NAMES);
			r.SendResultCode(Procedure.ErrorRequestId);
			return Procedure.Success;
		}
		for (; ; ) {
			var autoKey = autoKeys.computeIfAbsent(name, key -> new AutoKey(key, this));
			autoKey.lock();
			if (autoKeys.get(name) == autoKey) { // 持锁且在册，阻止并发逐出
				try {
					autoKey.allocateLocked(r, count);
				} finally {
					autoKey.unlock();
				}
				break;
			}
			autoKey.unlock(); // 已被逐出，重新获取
		}
		r.SendResult();
		return 0;
	}

	// 逐出未持锁的闲置 AutoKey 条目腾位；RocksDB 持久高水位保留，重建安全
	private boolean evictIdleAutoKey() {
		for (var e : autoKeys.entrySet()) {
			var autoKey = e.getValue();
			if (autoKey.tryLock()) {
				try {
					if (autoKeys.remove(e.getKey(), autoKey))
						return true;
				} finally {
					autoKey.unlock();
				}
			}
		}
		return false;
	}

	// 拒绝告警限频（每 60 秒至多一条）
	private volatile long lastAllocateIdRejectLogMs;

	private void warnAllocateIdRejected(String reason) {
		var now = System.currentTimeMillis();
		var last = lastAllocateIdRejectLogMs;
		if (now - last < 60_000)
			return;
		lastAllocateIdRejectLogMs = now;
		logger.error("AllocateId rejected (possible attack or misbehaving client), cached names={}: {}",
			autoKeys.size(), reason);
	}

	public void stop() throws Exception {
		closed = true;
		lock();
		try {
			id128Server.stop();
			if (server == null) {
				cancelSuspectTiers();
				return;
			}
			serverSocket.close();
			server.stop();
			server = null;
			// 置于 server.stop() 之后，确保断连回调不再插入新的升档定时器
			cancelSuspectTiers();
			logger.info("closeDb: {}, autokeys", this.conf.dbHome);
			autoKeysDb.close();
			threading.close();
		} finally {
			unlock();
		}
	}

	public static final class NetServer extends HandshakeServer {
		private final ServiceManagerServer serviceManager;
		private final TaskOneByOneByKey oneByOneByKey = new TaskOneByOneByKey();

		public NetServer(ServiceManagerServer sm, Config config) {
			super("Zeze.Services.ServiceManager", config);
			serviceManager = sm;
		}

		@Override
		public void OnSocketAccept(AsyncSocket so) throws Exception {
			logger.info("OnSocketAccept: {} sessionId={}", so, so.getSessionId());
			so.setUserState(new Session(serviceManager, so.getSessionId()));
			super.OnSocketAccept(so);
		}

		@Override
		public void OnSocketClose(AsyncSocket so, @Nullable Throwable e) throws Exception {
			logger.info("OnSocketClose: {} sessionId={}", so, so.getSessionId());
			// 先经基类从 socketMap 摘除再清理会话，确保锁内判活（isSenderAlive）具备严格先后序
			super.OnSocketClose(so, e);
			var session = (Session)so.getUserState();
			if (session != null)
				session.onClose();
		}

		@Override
		public void dispatchProtocol(long typeId, ByteBuffer bb,
									 ProtocolFactoryHandle<?> factoryHandle, @Nullable AsyncSocket so) {
			var p = decodeProtocol(typeId, bb, factoryHandle, so);
			if (factoryHandle.Mode == DispatchMode.Direct) {
				// 有几个direct方式的协议,为了性能就不考虑和其它非direct协议的处理顺序了,但因为在IO线程串行处理,这些协议本身的处理还是有顺序的
				TaskSpec.ofFunc(() -> p.handle(this, factoryHandle), p, (__, code) -> p.trySendResultCode(code)).call();
			} else {
				TaskSpec.ofFunc(() -> p.handle(this, factoryHandle), p, (__, code) -> p.trySendResultCode(code))
					.dispatchMode(factoryHandle.Mode)
					.executeOneByOne(Objects.requireNonNull(p.getSender()), oneByOneByKey);
			}
			// 不支持事务，由于这里直接OneByOne执行，所以下面两个方法就不重载了。
		}
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
				ip = requireValue(args, ++i, "-ip");
				break;
			case "-port":
				port = requireInt(args, ++i, "-port");
				break;
			case "-raft":
				raftName = requireValue(args, ++i, "-raft");
				break;
			case "-raftConf":
				raftConf = requireValue(args, ++i, "-raftConf");
				break;
			case "-threads":
				i++;
				break;
			case "-autokeys":
				autokeys = requireValue(args, ++i, "-autokeys");
				break;
			default:
				throw new IllegalArgumentException("unknown argument: " + args[i]);
			}
		}
		if (raftName == null || raftName.isEmpty()) {
			logger.info("Start {}:{}", ip != null ? ip : "any", port);
			InetAddress address = (ip != null && !ip.isBlank()) ? InetAddress.getByName(ip) : null;
			var config = Config.load();
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
