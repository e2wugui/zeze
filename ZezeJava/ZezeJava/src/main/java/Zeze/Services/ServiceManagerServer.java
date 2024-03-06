package Zeze.Services;

import java.io.Closeable;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
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
 * 1. gs停止时调用 RegisterService,UnRegisterService 向ServiceManager声明自己服务状态。
 * 2. linkd启动时调用 UseService, UnUseService 向ServiceManager申请使用gs-list。
 * 3. ServiceManager在RegisterService,UnRegisterService处理时发送 NotifyServiceList 给所有的 linkd。
 * 4. linkd收到NotifyServiceList先记录到本地，同时持续关注自己和gs之间的连接，
 * 当列表中的所有service都准备完成时调用 ReadyServiceList。
 * 5. ServiceManager收到所有的linkd的ReadyServiceList后，向所有的linkd广播 CommitServiceList。
 * 6. linkd 收到 CommitServiceList 时，启用新的服务列表。
 * <p>
 * 【特别规则和错误处理】
 * 1. linkd 异常停止，ServiceManager 按 UnUseService 处理，仅仅简单移除use-list。相当于减少了以后请求来源。
 * 2. gs 异常停止，ServiceManager 按 UnRegisterService 处理，移除可用服务，并启动列表更新流程（NotifyServiceList）。
 * 3. linkd 处理 gs 关闭（在NotifyServiceList之前），仅仅更新本地服务列表状态，让该服务暂时不可用，但不改变列表。
 * linkd总是使用ServiceManager提交给他的服务列表，自己不主动增删。
 * linkd在NotifyServiceList的列表减少的处理：一般总是立即进入ready（因为其他gs都是可用状态）。
 * 4. ServiceManager 异常关闭：
 * a) 启用raft以后，新的master会有正确列表数据，但服务状态（连接）未知，此时等待gs的RegisterService一段时间,
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
public final class ServiceManagerServer implements Closeable {
	static {
		var level = Level.toLevel(System.getProperty("logLevel"), Level.INFO);
		((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger().setLevel(level);
	}

	private static final Logger logger = LogManager.getLogger(ServiceManagerServer.class);

	// ServiceInfo.Name -> ServiceState
	private final ConcurrentHashMap<String, ServerState> serverStates = new ConcurrentHashMap<>();

	// 简单负载广播，
	// 在RegisterService/UpdateService时自动订阅，会话关闭的时候删除。
	// ProcessSetLoad时广播，本来不需要记录负载数据的，但为了以后可能的查询，保存一份。
	private final ConcurrentHashMap<String, LoadObservers> loads = new ConcurrentHashMap<>();

	public static final class LoadObservers {
		private final ServiceManagerServer serviceManager;
		private final LongHashSet observers = new LongHashSet();

		public LoadObservers(ServiceManagerServer m) {
			serviceManager = m;
		}

		public synchronized void addObserver(long sessionId) {
			observers.add(sessionId);
		}

		// synchronized big?
		public synchronized void setLoad(BServerLoad load) {
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
		}
	}

	// 需要从配置文件中读取，把这个引用加入：Zeze.Config.AddCustomize
	private final Conf conf = new Conf();
	private NetServer server;
	private final AsyncSocket serverSocket;
	private final RocksDB autoKeysDb;
	private final ConcurrentHashMap<String, AutoKey> autoKeys = new ConcurrentHashMap<>();
	private volatile Future<?> startNotifyDelayTask;

	public static final class Conf implements Config.ICustomize {
		public int keepAlivePeriod = -1;
		/**
		 * 启动以后接收注册和订阅，一段时间内不进行通知。
		 * 用来处理ServiceManager异常重启导致服务列表重置的问题。
		 * 在Delay时间内，希望所有的服务都重新连接上来并注册和订阅。
		 * Delay到达时，全部通知一遍，以后正常工作。
		 */
		public int startNotifyDelay = 12 * 1000;
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
			attr = self.getAttribute("StartNotifyDelay");
			if (!attr.isEmpty())
				startNotifyDelay = Integer.parseInt(attr);
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
	public static final class ServerState {
		private final ServiceManagerServer serviceManager;
		private final String serviceName;
		// identity ->
		// 记录一下SessionId，方便以后找到服务所在的连接。
		private final HashMap<String, BServiceInfo> serviceInfos = new HashMap<>(); // key:serverId
		private final LongHashMap<SubscribeState> simple = new LongHashMap<>(); // key:sessionId
		private final LongHashMap<SubscribeState> readyCommit = new LongHashMap<>(); // key:sessionId
		private Future<?> notifyTimeoutTask;
		private long serialId;

		public ServerState(ServiceManagerServer sm, String serviceName) {
			serviceManager = sm;
			this.serviceName = serviceName;
		}

		public synchronized void close() {
			if (notifyTimeoutTask != null) {
				notifyTimeoutTask.cancel(false);
				notifyTimeoutTask = null;
			}
		}

		public synchronized void getServiceInfos(List<BServiceInfo> target) {
			target.addAll(serviceInfos.values());
		}

		public void startReadyCommitNotify() {
			startReadyCommitNotify(false);
		}

		public synchronized void startReadyCommitNotify(boolean notifySimple) {
			if (serviceManager.startNotifyDelayTask != null)
				return;
			var notify = new NotifyServiceList(new BServiceInfos(serviceName, this, ++serialId));
			var notifyBytes = notify.encode();
			var sb = new StringBuilder();
			if (notifySimple) {
				for (var it = simple.iterator(); it.moveToNext(); ) {
					var s = serviceManager.server.GetSocket(it.key());
					if (s != null && s.Send(notifyBytes))
						sb.append(s.getSessionId()).append(',');
				}
			}
			var n = sb.length();
			if (n > 0)
				sb.setCharAt(n - 1, ';');
			else
				sb.append(';');
			for (var it = readyCommit.iterator(); it.moveToNext(); ) {
				it.value().ready = false;
				var s = serviceManager.server.GetSocket(it.key());
				if (s != null && s.Send(notifyBytes))
					sb.append(s.getSessionId()).append(',');
			}
			if (sb.length() > 1)
				AsyncSocket.logger.info("SEND[{}]: NotifyServiceList: {}", sb, notify.Argument);

			if (!readyCommit.isEmpty()) {
				// 只有两段公告模式需要回应处理。
				if (notifyTimeoutTask != null)
					notifyTimeoutTask.cancel(false);
				notifyTimeoutTask = Task.scheduleUnsafe(serviceManager.conf.retryNotifyDelayWhenNotAllReady,
						() -> {
							// NotifyTimeoutTask 会在下面两种情况下被修改：
							// 1. 在 Notify.ReadyCommit 完成以后会被清空。
							// 2. 启动了新的 Notify。
							startReadyCommitNotify(); // restart
						});
			}
		}

		public synchronized void notifySimpleOnRegister(BServiceInfo info) {
			if (simple.isEmpty())
				return;
			var sb = new StringBuilder();
			for (var it = simple.iterator(); it.moveToNext(); ) {
				var sessionId = it.key();
				if (new Register(info).Send(serviceManager.server.GetSocket(sessionId)))
					sb.append(sessionId).append(',');
				else
					logger.warn("NotifySimpleOnRegister {} failed: serverId({}) => sessionId({})",
							info.getServiceName(), info.getServiceIdentity(), sessionId);
			}
			var n = sb.length();
			if (n > 0) {
				sb.setLength(n - 1);
				logger.info("NotifySimpleOnRegister {} serverId({}) => sessionIds({})",
						info.getServiceName(), info.getServiceIdentity(), sb);
			}
		}

		public synchronized void notifySimpleOnUnRegister(BServiceInfo info) {
			if (simple.isEmpty())
				return;
			var sb = new StringBuilder();
			for (var it = simple.iterator(); it.moveToNext(); ) {
				var sessionId = it.key();
				if (new UnRegister(info).Send(serviceManager.server.GetSocket(sessionId)))
					sb.append(sessionId).append(',');
				else
					logger.warn("NotifySimpleOnUnRegister {} failed: serverId({}) => sessionId({})",
							info.getServiceName(), info.getServiceIdentity(), sessionId);
			}
			var n = sb.length();
			if (n > 0) {
				sb.setLength(n - 1);
				logger.info("NotifySimpleOnUnRegister {} serverId({}) => sessionIds({})",
						info.getServiceName(), info.getServiceIdentity(), sb);
			}
		}

		public synchronized int updateAndNotify(BServiceInfo info) {
			var current = serviceInfos.get(info.getServiceIdentity());
			if (current == null)
				return Update.ServiceIdentityNotExist;

			current.setPassiveIp(info.getPassiveIp());
			current.setPassivePort(info.getPassivePort());
			current.setExtraInfo(info.getExtraInfo());

			// 简单广播。
			var sb = new StringBuilder();
			for (var it = simple.iterator(); it.moveToNext(); ) {
				var sessionId = it.key();
				if (new Update(current).Send(serviceManager.server.GetSocket(sessionId)))
					sb.append(sessionId).append(',');
				else
					logger.warn("UpdateAndNotify {} failed: serverId({}) => sessionId({})",
							info.getServiceName(), info.getServiceIdentity(), sessionId);
			}
			var n = sb.length();
			if (n > 0)
				sb.setCharAt(n - 1, ';');
			else
				sb.append(';');
			for (var it = readyCommit.iterator(); it.moveToNext(); ) {
				var sessionId = it.key();
				if (new Update(current).Send(serviceManager.server.GetSocket(sessionId)))
					sb.append(sessionId).append(',');
				else
					logger.warn("UpdateAndNotify {} failed: serverId({}) => sessionId({})",
							info.getServiceName(), info.getServiceIdentity(), sessionId);
			}
			if (sb.length() > 1)
				logger.info("UpdateAndNotify {} serverId({}) => sessionIds({})",
						info.getServiceName(), info.getServiceIdentity(), sb);
			return 0;
		}

		public synchronized void tryCommit() {
			if (notifyTimeoutTask == null)
				return; // no pending notify

			for (var it = readyCommit.iterator(); it.moveToNext(); ) {
				if (!it.value().ready)
					return;
			}
			logger.debug("Ready Broadcast.");
			var commit = new CommitServiceList();
			commit.Argument.serviceName = serviceName;
			commit.Argument.serialId = serialId;
			for (var it = readyCommit.iterator(); it.moveToNext(); )
				commit.Send(serviceManager.server.GetSocket(it.key()));
			if (notifyTimeoutTask != null) {
				notifyTimeoutTask.cancel(false);
				notifyTimeoutTask = null;
			}
		}

		/**
		 * 订阅时候返回的ServiceInfos，必须和Notify流程互斥。
		 * 原子的得到当前信息并发送，然后加入订阅(simple or readyCommit)。
		 */
		public synchronized long subscribeAndSend(Subscribe r, long sessionId) {
			// 外面会话的 TryAdd 加入成功，下面TryAdd肯定也成功。
			switch (r.Argument.getSubscribeType()) {
			case BSubscribeInfo.SubscribeTypeSimple:
				simple.computeIfAbsent(sessionId, __ -> new SubscribeState());
				if (serviceManager.startNotifyDelayTask == null)
					new SubscribeFirstCommit(new BServiceInfos(serviceName, this, serialId)).Send(r.getSender());
				break;
			case BSubscribeInfo.SubscribeTypeReadyCommit:
				readyCommit.computeIfAbsent(sessionId, __ -> new SubscribeState());
				startReadyCommitNotify();
				break;
			default:
				r.SendResultCode(Subscribe.UnknownSubscribeType);
				return Procedure.LogicError;
			}
			for (var info : serviceInfos.values())
				serviceManager.addLoadObserver(info.getPassiveIp(), info.getPassivePort(), r.getSender());
			r.SendResultCode(Subscribe.Success);
			return Procedure.Success;
		}

		public synchronized void setReady(ReadyServiceList p, long sessionId) {
			if (p.Argument.serialId != serialId) {
				logger.debug("Ready Skip: SerialId Not Equal. {} Now={}", p.Argument.serialId, serialId);
				return;
			}
			// logger.debug("Ready:{} Now={}", p.Argument.SerialId, SerialId);
			var subscribeState = readyCommit.get(sessionId);
			if (subscribeState != null) {
				subscribeState.ready = true;
				tryCommit();
			}
		}
	}

	public static final class SubscribeState {
		private boolean ready;
	}

	private final ConcurrentHashMap<Integer, Future<?>> offlineNotifyFutures = new ConcurrentHashMap<>();

	// 每个server连接的状态
	public static final class Session {
		private final ServiceManagerServer serviceManager;
		private final long sessionId;
		private final ConcurrentHashSet<BServiceInfo> registers = new ConcurrentHashSet<>(); // 以'服务名+ID'作为key的set
		// key is ServiceName: 会话订阅
		private final ConcurrentHashMap<String, BSubscribeInfo> subscribes = new ConcurrentHashMap<>();
		private final Future<?> keepAliveTimerTask;
		private int offlineRegisterServerId; // 原样通知,服务端不关心!!!
		// 目前SM的客户端没有Id，只能使用这个区分来自哪里，所以对于Server来说，这个值必须填写。
		// 如果是负数，将不会进行延迟通知，即这种情况下，通知马上发出。

		private final HashMap<String, BOfflineNotify> offlineRegisterNotifies = new HashMap<>(); // 使用的时候加锁保护。value:notifyId
		public static final long eOfflineNotifyDelay = 600 * 1000;

		public Session(ServiceManagerServer sm, long sid) {
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

			for (var info : subscribes.values())
				serviceManager.unSubscribeNow(sessionId, info);

			var changed = new HashMap<String, ServerState>(registers.size());
			for (var info : registers) {
				var state = serviceManager.unRegisterNow(sessionId, info);
				if (state != null)
					changed.putIfAbsent(state.serviceName, state);
			}
			changed.values().forEach(ServerState::startReadyCommitNotify);

			// offline notify，开启一个线程执行，避免互等造成麻烦。
			// 这个操作不能cancel，即使Server重新起来了，通知也会进行下去。
			if (offlineRegisterServerId >= 0) {
				synchronized (serviceManager) {
					// 对于java，这里可以使用computeIfAbsent去掉这个synchronized，
					// 为了理解简单，还是使用同步吧。
					if (!serviceManager.offlineNotifyFutures.containsKey(offlineRegisterServerId))
						serviceManager.offlineNotifyFutures.put(offlineRegisterServerId,
								Task.scheduleUnsafe(eOfflineNotifyDelay, () -> offlineNotify(true)));
				}
			} else {
				Task.run(() -> offlineNotify(false), "offlineNotifyImmediately");
			}
		}

		private void offlineNotify(boolean delay) {
			if (delay && null == serviceManager.offlineNotifyFutures.remove(offlineRegisterServerId))
				return; // 此serverId的新连接已经连上或者通知已经执行。

			BOfflineNotify[] notifyIds;
			synchronized (offlineRegisterNotifies) {
				if (offlineRegisterNotifies.isEmpty())
					return; // 不需要通知。
				var values = offlineRegisterNotifies.values();
				notifyIds = values.toArray(new BOfflineNotify[values.size()]);
			}

			logger.info("OfflineNotify: serverId={} notifyIds={} begin",
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
						logger.info("OfflineNotify: serverId={} notifyId={} selectSessionId={} resultCode={}",
								offlineRegisterServerId, notifyId, selected.getKey().sessionId, notify.getResultCode());
						if (notify.getResultCode() == 0)
							break; // 成功通知。done
					} catch (Throwable ignored) { // ignored
					}
					// 保存这一次通知失败session，下一次尝试选择的时候忽略。
					skips.add(selected.getKey());
				}
			}
			logger.info("OfflineNotify: serverId={} end", offlineRegisterServerId);
		}

		// 从注册了这个notifyId的其他session中随机选择一个。【实际实现是从连接里面按顺序挑选的】
		private KV<Session, AsyncSocket> randomFor(String notifyId, HashSet<Session> skips) {
			var sessions = new ArrayList<KV<Session, AsyncSocket>>();
			try {
				serviceManager.server.foreach(socket -> {
					var session = (Session)socket.getUserState();
					if (session != null && session != this && !skips.contains(session)) {
						boolean contain;
						synchronized (session.offlineRegisterNotifies) {
							contain = session.offlineRegisterNotifies.containsKey(notifyId);
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

	private void addLoadObserver(String ip, int port, AsyncSocket sender) {
		if (!ip.isEmpty() && port != 0)
			loads.computeIfAbsent(ip + "_" + port, __ -> new LoadObservers(this)).addObserver(sender.getSessionId());
	}

	private long processRegister(Register r) {
		var session = (Session)r.getSender().getUserState();

		// 允许重复登录，断线重连Agent不好原子实现重发。
		if (session.registers.add(r.Argument)) {
			logger.info("{}: Register {} serverId={} ip={} port={}", r.getSender(), r.Argument.getServiceName(),
					r.Argument.getServiceIdentity(), r.Argument.getPassiveIp(), r.Argument.getPassivePort());
		} else {
			logger.info("{}: Already Registered {} serverId={} ip={} port={}", r.getSender(), r.Argument.getServiceName(),
					r.Argument.getServiceIdentity(), r.Argument.getPassiveIp(), r.Argument.getPassivePort());
		}
		var state = serverStates.computeIfAbsent(r.Argument.getServiceName(), name -> new ServerState(this, name));

		// 【警告】
		// 为了简单，这里没有创建新的对象，直接修改并引用了r.Argument。
		// 这个破坏了r.Argument只读的属性。另外引用同一个对象，也有点风险。
		// 在目前没有问题，因为r.Argument主要记录在state.ServiceInfos中，
		// 另外它也被Session引用（用于连接关闭时，自动注销）。
		// 这是专用程序，不是一个库，以后有修改时，小心就是了。
		r.Argument.sessionId = r.getSender().getSessionId();

		// AddOrUpdate，否则重连重新注册很难恢复到正确的状态。
		synchronized (state) {
			state.serviceInfos.put(r.Argument.getServiceIdentity(), r.Argument);
			r.SendResultCode(Register.Success);
			state.startReadyCommitNotify();
			state.notifySimpleOnRegister(r.Argument);
		}
		return Procedure.Success;
	}

	public ServerState unRegisterNow(long sessionId, BServiceInfo info) {
		var state = serverStates.get(info.getServiceName());
		if (state != null) {
			synchronized (state) {
				var exist = state.serviceInfos.remove(info.getServiceIdentity());
				if (exist != null && exist.sessionId == sessionId) {
					// 有可能当前连接没有注销，新的注册已经AddOrUpdate，此时忽略当前连接的注销。
					state.notifySimpleOnUnRegister(exist);
					return state;
				}
			}
		}
		return null;
	}

	private long processUnRegister(UnRegister r) {
		logger.info("{}: UnRegister {} serverId={}",
				r.getSender(), r.Argument.getServiceName(), r.Argument.getServiceIdentity());
		unRegisterNow(r.getSender().getSessionId(), r.Argument); // ignore UnRegisterNow failed
		((Session)r.getSender().getUserState()).registers.remove(r.Argument); // ignore remove failed
		// 注销不存在也返回成功，否则Agent处理比较麻烦。
		r.SendResultCode(UnRegister.Success);
		return Procedure.Success;
	}

	private long processUpdate(Update r) {
		logger.info("{}: Update {} serverId={} ip={} port={}", r.getSender(), r.Argument.getServiceName(),
				r.Argument.getServiceIdentity(), r.Argument.getPassiveIp(), r.Argument.getPassivePort());
		var session = (Session)r.getSender().getUserState();
		if (!session.registers.containsKey(r.Argument))
			return Update.ServiceNotRegister;

		var state = serverStates.get(r.Argument.getServiceName());
		if (state == null)
			return Update.ServerStateError;

		var rc = state.updateAndNotify(r.Argument);
		if (rc != 0)
			return rc;
		r.SendResult();
		return 0;
	}

	private long processSubscribe(Subscribe r) {
		logger.info("{}: Subscribe {} type={}",
				r.getSender(), r.Argument.getServiceName(), r.Argument.getSubscribeType());
		var session = (Session)r.getSender().getUserState();
		session.subscribes.putIfAbsent(r.Argument.getServiceName(), r.Argument);
		return serverStates.computeIfAbsent(r.Argument.getServiceName(), name -> new ServerState(this, name))
				.subscribeAndSend(r, session.sessionId);
	}

	public ServerState unSubscribeNow(long sessionId, BSubscribeInfo info) {
		var state = serverStates.get(info.getServiceName());
		if (state != null) {
			LongHashMap<SubscribeState> subState;
			switch (info.getSubscribeType()) {
			case BSubscribeInfo.SubscribeTypeSimple:
				subState = state.simple;
				break;
			case BSubscribeInfo.SubscribeTypeReadyCommit:
				subState = state.readyCommit;
				break;
			default:
				return null;
			}
			synchronized (state) {
				if (subState.remove(sessionId) != null)
					return state;
			}
		}
		return null;
	}

	private long processUnSubscribe(UnSubscribe r) {
		logger.info("{}: UnSubscribe {} type={}",
				r.getSender(), r.Argument.getServiceName(), r.Argument.getSubscribeType());
		var session = (Session)r.getSender().getUserState();
		var sub = session.subscribes.remove(r.Argument.getServiceName());
		if (sub != null) {
			if (r.Argument.getSubscribeType() == sub.getSubscribeType()) {
				var changed = unSubscribeNow(r.getSender().getSessionId(), r.Argument);
				if (changed != null) {
					r.setResultCode(UnSubscribe.Success);
					r.SendResult();
					changed.tryCommit();
					return Procedure.Success;
				}
			}
		}
		// 取消订阅不能存在返回成功。否则Agent比较麻烦。
		//r.ResultCode = UnSubscribe.NotExist;
		//r.SendResult();
		//return Procedure.LogicError;
		r.setResultCode(UnRegister.Success);
		r.SendResult();
		return Procedure.Success;
	}

	private long processReadyServiceList(ReadyServiceList r) {
		serverStates.computeIfAbsent(r.Argument.serviceName, name -> new ServerState(this, name))
				.setReady(r, ((Session)r.getSender().getUserState()).sessionId);
		return Procedure.Success;
	}

	private long processSetLoad(SetServerLoad setServerLoad) {
		loads.computeIfAbsent(setServerLoad.Argument.getName(), __ -> new LoadObservers(this))
				.setLoad(setServerLoad.Argument);
		return 0;
	}

	private long processOfflineRegister(OfflineRegister r) {
		logger.info("{}: OfflineRegister serverId={} notifyId={}",
				r.getSender(), r.Argument.serverId, r.Argument.notifyId);
		var session = (Session)r.getSender().getUserState();
		// 允许重复注册：简化server注册逻辑。
		synchronized (session.offlineRegisterNotifies) {
			session.offlineRegisterServerId = r.Argument.serverId;
			session.offlineRegisterNotifies.put(r.Argument.notifyId, r.Argument);
			var future = offlineNotifyFutures.remove(session.offlineRegisterServerId);
			if (null != future)
				future.cancel(true);
		}
		r.SendResult();
		return 0;
	}

	@SuppressWarnings("MethodMayBeStatic")
	private long processNormalClose(NormalClose r) {
		var session = (Session)r.getSender().getUserState();
		synchronized (session.offlineRegisterNotifies) {
			// 正常关闭，不做异常下线通知。
			session.offlineRegisterNotifies.clear();
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

	private final ThreadingServer threading;

	public ServiceManagerServer(@Nullable InetAddress ipaddress, int port, @NotNull Config config) throws Exception {
		this(ipaddress, port, config, -1);
	}

	public ServiceManagerServer(@Nullable InetAddress ipaddress, int port,
								@NotNull Config config, int startNotifyDelay) throws Exception{
		this(ipaddress, port, config, startNotifyDelay, "autokeys");
	}

	public ServiceManagerServer(@Nullable InetAddress ipaddress, int port,
								@NotNull Config config, int startNotifyDelay,
								String autokeys) throws Exception {
		PerfCounter.instance.tryStartScheduledLog();
		config.parseCustomize(this.conf);

		if (startNotifyDelay >= 0)
			this.conf.startNotifyDelay = startNotifyDelay;

		server = new NetServer(this, config);

		server.AddFactoryHandle(Register.TypeId_, new Service.ProtocolFactoryHandle<>(
				Register::new, this::processRegister, TransactionLevel.None, DispatchMode.Critical));
		server.AddFactoryHandle(Update.TypeId_, new Service.ProtocolFactoryHandle<>(
				Update::new, this::processUpdate, TransactionLevel.None, DispatchMode.Critical));
		server.AddFactoryHandle(UnRegister.TypeId_, new Service.ProtocolFactoryHandle<>(
				UnRegister::new, this::processUnRegister, TransactionLevel.None, DispatchMode.Critical));
		server.AddFactoryHandle(Subscribe.TypeId_, new Service.ProtocolFactoryHandle<>(
				Subscribe::new, this::processSubscribe, TransactionLevel.None, DispatchMode.Critical));
		server.AddFactoryHandle(UnSubscribe.TypeId_, new Service.ProtocolFactoryHandle<>(
				UnSubscribe::new, this::processUnSubscribe, TransactionLevel.None, DispatchMode.Critical));
		server.AddFactoryHandle(ReadyServiceList.TypeId_, new Service.ProtocolFactoryHandle<>(
				ReadyServiceList::new, this::processReadyServiceList, TransactionLevel.None, DispatchMode.Critical));
		server.AddFactoryHandle(KeepAlive.TypeId_, new Service.ProtocolFactoryHandle<>(
				KeepAlive::new, null, TransactionLevel.None, DispatchMode.Direct));
		server.AddFactoryHandle(AllocateId.TypeId_, new Service.ProtocolFactoryHandle<>(
				AllocateId::new, this::processAllocateId, TransactionLevel.None, DispatchMode.Critical));
		server.AddFactoryHandle(SetServerLoad.TypeId_, new Service.ProtocolFactoryHandle<>(
				SetServerLoad::new, this::processSetLoad, TransactionLevel.None, DispatchMode.Critical));
		server.AddFactoryHandle(OfflineRegister.TypeId_, new Service.ProtocolFactoryHandle<>(
				OfflineRegister::new, this::processOfflineRegister, TransactionLevel.None, DispatchMode.Critical));
		server.AddFactoryHandle(OfflineNotify.TypeId_, new Service.ProtocolFactoryHandle<>(
				OfflineNotify::new, null, TransactionLevel.None, DispatchMode.Direct));
		server.AddFactoryHandle(NormalClose.TypeId_, new Service.ProtocolFactoryHandle<>(
				NormalClose::new, this::processNormalClose, TransactionLevel.None, DispatchMode.Critical));
		if (this.conf.startNotifyDelay > 0) {
			//noinspection NonAtomicOperationOnVolatileField
			startNotifyDelayTask = Task.scheduleUnsafe(this.conf.startNotifyDelay, () -> {
				startNotifyDelayTask = null;
				serverStates.values().forEach(s -> s.startReadyCommitNotify(true));
			});
		}

		threading = new ThreadingServer(server, conf);
		threading.RegisterProtocols(server);

		autoKeysDb = RocksDatabase.open(Path.of(this.conf.dbHome, autokeys).toString());

		// 允许配置多个acceptor，如果有冲突，通过日志查看。
		serverSocket = server.newServerSocket(ipaddress, port,
				new Acceptor(port, ipaddress != null ? ipaddress.getHostAddress() : null));
		server.start();
	}

	public static final class AutoKey {
		private final ServiceManagerServer sms;
		private final byte[] key;
		private long current;

		public AutoKey(String name, ServiceManagerServer sms) {
			this.sms = sms;
			byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
			var bb = ByteBuffer.Allocate(ByteBuffer.WriteUIntSize(nameBytes.length) + nameBytes.length);
			bb.WriteBytes(nameBytes);
			key = bb.Bytes;
			try {
				byte[] value = this.sms.autoKeysDb.get(RocksDatabase.getDefaultReadOptions(), key);
				if (value != null)
					current = ByteBuffer.Wrap(value).ReadLong();
			} catch (RocksDBException e) {
				Task.forceThrow(e);
			}
		}

		public synchronized void allocate(AllocateId rpc) {
			rpc.Result.setStartId(current);

			var count = rpc.Argument.getCount();

			// 随便修正一下分配数量。
			if (count < 256)
				count = 256;
			else if (count > 10000)
				count = 10000;

			long current = this.current + count;
			this.current = current;
			var bb = ByteBuffer.Allocate(ByteBuffer.WriteLongSize(current));
			bb.WriteLong(current);
			try {
				sms.autoKeysDb.put(RocksDatabase.getDefaultWriteOptions(), key, bb.Bytes);
			} catch (RocksDBException e) {
				Task.forceThrow(e);
			}

			rpc.Result.setCount(count);
		}
	}

	private long processAllocateId(AllocateId r) {
		var name = r.Argument.getName();
		r.Result.setName(name);
		autoKeys.computeIfAbsent(name, key -> new AutoKey(key, this)).allocate(r);
		r.SendResult();
		return 0;
	}

	public synchronized void stop() throws Exception {
		if (server == null)
			return;
		var startNotifyDelayTask = this.startNotifyDelayTask;
		if (startNotifyDelayTask != null)
			startNotifyDelayTask.cancel(false);
		serverSocket.close();
		server.stop();
		server = null;
		serverStates.values().forEach(ServerState::close);
		if (autoKeysDb != null) {
			logger.info("closeDb: {}, autokeys", this.conf.dbHome);
			autoKeysDb.close();
		}
		threading.close();
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
		public void OnSocketClose(AsyncSocket so, Throwable e) throws Exception {
			logger.info("OnSocketClose: {} sessionId={}", so, so.getSessionId());
			var session = (Session)so.getUserState();
			if (session != null)
				session.onClose();
			super.OnSocketClose(so, e);
		}

		@Override
		public void dispatchProtocol(long typeId, ByteBuffer bb, ProtocolFactoryHandle<?> factoryHandle, AsyncSocket so) {
			var p = decodeProtocol(typeId, bb, factoryHandle, so);
			oneByOneByKey.Execute(p.getSender(),
					() -> Task.call(() -> p.handle(this, factoryHandle), p, Protocol::trySendResultCode), factoryHandle.Mode);
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
		int startNotifyDelay = -1;

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
			case "-startNotifyDelay":
				startNotifyDelay = Integer.parseInt(args[++i]);
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
			try (var ignored = new ServiceManagerServer(address, port, config, startNotifyDelay, autokeys)) {
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
