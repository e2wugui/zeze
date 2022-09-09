package Zeze.Services;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.AllocateId;
import Zeze.Services.ServiceManager.BOfflineNotify;
import Zeze.Services.ServiceManager.BServerLoad;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BServiceInfos;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Services.ServiceManager.CommitServiceList;
import Zeze.Services.ServiceManager.KeepAlive;
import Zeze.Services.ServiceManager.NotifyServiceList;
import Zeze.Services.ServiceManager.OfflineNotify;
import Zeze.Services.ServiceManager.OfflineRegister;
import Zeze.Services.ServiceManager.ReadyServiceList;
import Zeze.Services.ServiceManager.Register;
import Zeze.Services.ServiceManager.SetServerLoad;
import Zeze.Services.ServiceManager.Subscribe;
import Zeze.Services.ServiceManager.SubscribeFirstCommit;
import Zeze.Services.ServiceManager.UnRegister;
import Zeze.Services.ServiceManager.UnSubscribe;
import Zeze.Services.ServiceManager.Update;
import Zeze.Transaction.DatabaseRocksDb;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.KV;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.LongHashMap;
import Zeze.Util.LongHashSet;
import Zeze.Util.LongList;
import Zeze.Util.Random;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
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
		System.setProperty("log4j.configurationFile", "log4j2.xml");
		var level = Level.toLevel(System.getProperty("logLevel"), Level.INFO);
		((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger().setLevel(level);
	}

	private static final Logger logger = LogManager.getLogger(ServiceManagerServer.class);

	// ServiceInfo.Name -> ServiceState
	private final ConcurrentHashMap<String, ServerState> ServerStates = new ConcurrentHashMap<>();

	// 简单负载广播，
	// 在RegisterService/UpdateService时自动订阅，会话关闭的时候删除。
	// ProcessSetLoad时广播，本来不需要记录负载数据的，但为了以后可能的查询，保存一份。
	private final ConcurrentHashMap<String, LoadObservers> Loads = new ConcurrentHashMap<>();

	public static final class LoadObservers {
		private final ServiceManagerServer ServiceManager;
		private final LongHashSet Observers = new LongHashSet();

		public LoadObservers(ServiceManagerServer m) {
			ServiceManager = m;
		}

		public synchronized void AddObserver(long sessionId) {
			Observers.add(sessionId);
		}

		// synchronized big?
		public synchronized void SetLoad(BServerLoad load) {
			var set = new SetServerLoad(load);
			LongList removed = null;
			for (var it = Observers.iterator(); it.moveToNext(); ) {
				long observer = it.value();
				try {
					if (set.Send(ServiceManager.Server.GetSocket(observer)))
						continue;
				} catch (Throwable ignored) {
				}
				if (removed == null)
					removed = new LongList();
				removed.add(observer);
			}
			if (removed != null)
				removed.foreach(Observers::remove);
		}
	}

	// 需要从配置文件中读取，把这个引用加入：Zeze.Config.AddCustomize
	private final Conf Config;
	private NetServer Server;
	private final AsyncSocket ServerSocket;
	private final RocksDB AutoKeysDb;
	private final ConcurrentHashMap<String, AutoKey> AutoKeys = new ConcurrentHashMap<>();
	private volatile Future<?> StartNotifyDelayTask;

	public static final class Conf implements Zeze.Config.ICustomize {
		private int KeepAlivePeriod = -1;
		/**
		 * 启动以后接收注册和订阅，一段时间内不进行通知。
		 * 用来处理ServiceManager异常重启导致服务列表重置的问题。
		 * 在Delay时间内，希望所有的服务都重新连接上来并注册和订阅。
		 * Delay到达时，全部通知一遍，以后正常工作。
		 */
		private int StartNotifyDelay = 12 * 1000;
		private int RetryNotifyDelayWhenNotAllReady = 30 * 1000;
		private String DbHome = ".";

		@Override
		public String getName() {
			return "Zeze.Services.ServiceManager";
		}

		@Override
		public void Parse(Element self) {
			String attr = self.getAttribute("KeepAlivePeriod");
			if (!attr.isEmpty())
				KeepAlivePeriod = Integer.parseInt(attr);
			attr = self.getAttribute("StartNotifyDelay");
			if (!attr.isEmpty())
				StartNotifyDelay = Integer.parseInt(attr);
			attr = self.getAttribute("RetryNotifyDelayWhenNotAllReady");
			if (!attr.isEmpty())
				RetryNotifyDelayWhenNotAllReady = Integer.parseInt(attr);
			DbHome = self.getAttribute("DbHome");
			if (DbHome.isEmpty())
				DbHome = ".";
		}
	}

	// 每个服务的状态
	public static final class ServerState {
		private final ServiceManagerServer ServiceManager;
		private final String ServiceName;
		// identity ->
		// 记录一下SessionId，方便以后找到服务所在的连接。
		private final HashMap<String, BServiceInfo> ServiceInfos = new HashMap<>(); // key:serverId
		private final LongHashMap<SubscribeState> Simple = new LongHashMap<>(); // key:sessionId
		private final LongHashMap<SubscribeState> ReadyCommit = new LongHashMap<>(); // key:sessionId
		private Future<?> NotifyTimeoutTask;
		private long SerialId;

		public HashMap<String, BServiceInfo> getServiceInfos() {
			return ServiceInfos;
		}

		public ServerState(ServiceManagerServer sm, String serviceName) {
			ServiceManager = sm;
			ServiceName = serviceName;
		}

		public synchronized void Close() {
			if (NotifyTimeoutTask != null) {
				NotifyTimeoutTask.cancel(false);
				NotifyTimeoutTask = null;
			}
		}

		public void StartReadyCommitNotify() {
			StartReadyCommitNotify(false);
		}

		public synchronized void StartReadyCommitNotify(boolean notifySimple) {
			if (ServiceManager.StartNotifyDelayTask != null)
				return;
			var notify = new NotifyServiceList(new BServiceInfos(ServiceName, this, ++SerialId));
			var notifyBytes = notify.Encode();
			var sb = new StringBuilder();
			if (notifySimple) {
				for (var it = Simple.iterator(); it.moveToNext(); ) {
					var s = ServiceManager.Server.GetSocket(it.key());
					if (s != null && s.Send(notifyBytes))
						sb.append(s.getSessionId()).append(',');
				}
			}
			var n = sb.length();
			if (n > 0)
				sb.setCharAt(n - 1, ';');
			else
				sb.append(';');
			for (var it = ReadyCommit.iterator(); it.moveToNext(); ) {
				it.value().Ready = false;
				var s = ServiceManager.Server.GetSocket(it.key());
				if (s != null && s.Send(notifyBytes))
					sb.append(s.getSessionId()).append(',');
			}
			if (sb.length() > 1)
				AsyncSocket.logger.info("SEND[{}]: NotifyServiceList: {}", sb, notify.Argument);

			if (!ReadyCommit.isEmpty()) {
				// 只有两段公告模式需要回应处理。
				if (NotifyTimeoutTask != null)
					NotifyTimeoutTask.cancel(false);
				NotifyTimeoutTask = Task.scheduleUnsafe(ServiceManager.Config.RetryNotifyDelayWhenNotAllReady,
						() -> {
							// NotifyTimeoutTask 会在下面两种情况下被修改：
							// 1. 在 Notify.ReadyCommit 完成以后会被清空。
							// 2. 启动了新的 Notify。
							StartReadyCommitNotify(); // restart
						});
			}
		}

		public synchronized void NotifySimpleOnRegister(BServiceInfo info) {
			var sb = new StringBuilder();
			for (var it = Simple.iterator(); it.moveToNext(); ) {
				var sessionId = it.key();
				sb.append(sessionId).append(',');
				if (!new Register(info).Send(ServiceManager.Server.GetSocket(sessionId)))
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

		public synchronized void NotifySimpleOnUnRegister(BServiceInfo info) {
			if (Simple.isEmpty())
				return;
			var sb = new StringBuilder();
			for (var it = Simple.iterator(); it.moveToNext(); ) {
				var sessionId = it.key();
				sb.append(sessionId).append(',');
				new UnRegister(info).Send(ServiceManager.Server.GetSocket(sessionId));
			}
			var n = sb.length();
			if (n > 0) {
				sb.setLength(n - 1);
				logger.info("NotifySimpleOnUnRegister {} serverId({}) => sessionIds({})",
						info.getServiceName(), info.getServiceIdentity(), sb);
			}
		}

		public synchronized int UpdateAndNotify(BServiceInfo info) {
			var current = ServiceInfos.get(info.getServiceIdentity());
			if (current == null)
				return Update.ServiceIdentityNotExist;

			current.setPassiveIp(info.getPassiveIp());
			current.setPassivePort(info.getPassivePort());
			current.setExtraInfo(info.getExtraInfo());

			// 简单广播。
			var sb = new StringBuilder();
			for (var it = Simple.iterator(); it.moveToNext(); ) {
				var sessionId = it.key();
				sb.append(sessionId).append(',');
				new Update(current).Send(ServiceManager.Server.GetSocket(sessionId));
			}
			var n = sb.length();
			if (n > 0)
				sb.setCharAt(n - 1, ';');
			else
				sb.append(';');
			for (var it = ReadyCommit.iterator(); it.moveToNext(); ) {
				var sessionId = it.key();
				sb.append(sessionId).append(',');
				new Update(current).Send(ServiceManager.Server.GetSocket(sessionId));
			}
			if (sb.length() > 1)
				logger.info("UpdateAndNotify {} serverId({}) => sessionIds({})",
						info.getServiceName(), info.getServiceIdentity(), sb);
			return 0;
		}

		public synchronized void TryCommit() {
			if (NotifyTimeoutTask == null)
				return; // no pending notify

			for (var it = ReadyCommit.iterator(); it.moveToNext(); ) {
				if (!it.value().Ready)
					return;
			}
			logger.debug("Ready Broadcast.");
			var commit = new CommitServiceList();
			commit.Argument.ServiceName = ServiceName;
			commit.Argument.SerialId = SerialId;
			for (var it = ReadyCommit.iterator(); it.moveToNext(); )
				commit.Send(ServiceManager.Server.GetSocket(it.key()));
			if (NotifyTimeoutTask != null) {
				NotifyTimeoutTask.cancel(false);
				NotifyTimeoutTask = null;
			}
		}

		/**
		 * 订阅时候返回的ServiceInfos，必须和Notify流程互斥。
		 * 原子的得到当前信息并发送，然后加入订阅(simple or readyCommit)。
		 */
		public synchronized long SubscribeAndSend(Subscribe r, Session session) {
			// 外面会话的 TryAdd 加入成功，下面TryAdd肯定也成功。
			switch (r.Argument.getSubscribeType()) {
			case BSubscribeInfo.SubscribeTypeSimple:
				Simple.computeIfAbsent(session.SessionId, __ -> new SubscribeState());
				if (ServiceManager.StartNotifyDelayTask == null)
					new SubscribeFirstCommit(new BServiceInfos(ServiceName, this, SerialId)).Send(r.getSender());
				break;
			case BSubscribeInfo.SubscribeTypeReadyCommit:
				ReadyCommit.computeIfAbsent(session.SessionId, __ -> new SubscribeState());
				StartReadyCommitNotify();
				break;
			default:
				r.SendResultCode(Subscribe.UnknownSubscribeType);
				return Procedure.LogicError;
			}
			for (var info : ServiceInfos.values()) {
				ServiceManager.AddLoadObserver(info.getPassiveIp(), info.getPassivePort(), r.getSender());
			}
			r.SendResultCode(Subscribe.Success);
			return Procedure.Success;
		}

		public synchronized void SetReady(ReadyServiceList p, Session session) {
			if (p.Argument.SerialId != SerialId) {
				logger.debug("Ready Skip: SerialId Not Equal. {} Now={}", p.Argument.SerialId, SerialId);
				return;
			}
			// logger.debug("Ready:{} Now={}", p.Argument.SerialId, SerialId);
			var subscribeState = ReadyCommit.get(session.SessionId);
			if (subscribeState == null)
				return;
			subscribeState.Ready = true;
			TryCommit();
		}
	}

	public static final class SubscribeState {
		private boolean Ready;
	}

	// 每个server连接的状态
	public static final class Session {
		private final ServiceManagerServer ServiceManager;
		private final long SessionId;
		private final ConcurrentHashSet<BServiceInfo> Registers = new ConcurrentHashSet<>();
		// key is ServiceName: 会话订阅
		private final ConcurrentHashMap<String, BSubscribeInfo> Subscribes = new ConcurrentHashMap<>();
		private final Future<?> KeepAliveTimerTask;
		private int OfflineRegisterServerId; // 原样通知,服务端不关心
		private final HashMap<String, BOfflineNotify> OfflineRegisterNotifies = new HashMap<>(); // 使用的时候加锁保护。value:notifyId

		public Session(ServiceManagerServer sm, long sid) {
			ServiceManager = sm;
			SessionId = sid;
			if (ServiceManager.Config.KeepAlivePeriod > 0) {
				KeepAliveTimerTask = Task.scheduleUnsafe(
						Random.getInstance().nextInt(ServiceManager.Config.KeepAlivePeriod),
						ServiceManager.Config.KeepAlivePeriod,
						() -> {
							AsyncSocket s = null;
							try {
								s = ServiceManager.Server.GetSocket(SessionId);
								var r = new KeepAlive();
								r.SendAndWaitCheckResultCode(s);
							} catch (Throwable ex) {
								if (s != null)
									s.close();
								logger.error("ServiceManager.KeepAlive", ex);
							}
						});
			} else
				KeepAliveTimerTask = null;
		}

		// 底层确保只会回调一次
		public void OnClose() {
			if (KeepAliveTimerTask != null)
				KeepAliveTimerTask.cancel(false);

			for (var info : Subscribes.values())
				ServiceManager.UnSubscribeNow(SessionId, info);

			HashMap<String, ServerState> changed = new HashMap<>(Registers.size());

			for (var info : Registers) {
				var state = ServiceManager.UnRegisterNow(SessionId, info);
				if (state != null)
					changed.putIfAbsent(state.ServiceName, state);
			}

			for (var state : changed.values())
				state.StartReadyCommitNotify();

			// offline notify，开启一个线程执行，避免互等造成麻烦。
			// 这个操作不能cancel，即使Server重新起来了，通知也会进行下去。
			Task.run(() -> {
				BOfflineNotify[] notifyIds;
				synchronized (OfflineRegisterNotifies) {
					if (OfflineRegisterNotifies.isEmpty())
						return; // 不需要通知。
					Collection<BOfflineNotify> values = OfflineRegisterNotifies.values();
					notifyIds = values.toArray(new BOfflineNotify[values.size()]);
				}

				logger.info("OfflineNotify: serverId={} notifyIds={} begin",
						OfflineRegisterServerId, Arrays.toString(notifyIds));
				for (var notifyId : notifyIds) {
					var skips = new HashSet<Session>();
					var notify = new OfflineNotify(notifyId);
					while (true) {
						var selected = randomFor(notifyId.NotifyId, skips);
						if (selected == null)
							break; // 没有找到可用的通知对象，放弃通知。
						try {
							notify.SendForWait(selected.getValue()).await();
							logger.info("OfflineNotify: serverId={} notifyId={} selectSessionId={} resultCode={}",
									OfflineRegisterServerId, notifyId, selected.getKey().SessionId, notify.getResultCode());
							if (notify.getResultCode() == 0)
								break; // 成功通知。done
						} catch (Throwable ignored) {
						}
						// 保存这一次通知失败session，下一次尝试选择的时候忽略。
						skips.add(selected.getKey());
					}
				}
				logger.info("OfflineNotify: serverId={} end", OfflineRegisterServerId);
			}, "OfflineNotify");
		}

		// 从注册了这个notifyId的其他session中随机选择一个。
		private KV<Session, AsyncSocket> randomFor(String notifyId, HashSet<Session> skips) {
			var sessions = new ArrayList<KV<Session, AsyncSocket>>();
			ServiceManager.Server.getAllSocks().forEach(socket -> {
				var session = (Session)socket.getUserState();
				if (session != null && session != this && !skips.contains(session)) {
					boolean contain;
					synchronized (session.OfflineRegisterNotifies) {
						contain = session.OfflineRegisterNotifies.containsKey(notifyId);
					}
					if (contain)
						sessions.add(KV.Create(session, socket));
				}
			});
			if (sessions.isEmpty())
				return null;
			return sessions.get(Random.getInstance().nextInt(sessions.size()));
		}
	}

	private void AddLoadObserver(String ip, int port, AsyncSocket sender) {
		if (!ip.isEmpty() && port != 0)
			Loads.computeIfAbsent(ip + ":" + port, __ -> new LoadObservers(this)).AddObserver(sender.getSessionId());
	}

	private long ProcessRegister(Register r) {
		var session = (Session)r.getSender().getUserState();

		// 允许重复登录，断线重连Agent不好原子实现重发。
		if (session.Registers.add(r.Argument)) {
			logger.info("{}: Register {} id={} ip={} port={}", r.getSender(), r.Argument.getServiceName(),
					r.Argument.getServiceIdentity(), r.Argument.getPassiveIp(), r.Argument.getPassivePort());
		} else {
			logger.info("{}: already Registered {} id={} ip={} port={}", r.getSender(), r.Argument.getServiceName(),
					r.Argument.getServiceIdentity(), r.Argument.getPassiveIp(), r.Argument.getPassivePort());
		}
		var state = ServerStates.computeIfAbsent(r.Argument.getServiceName(), name -> new ServerState(this, name));

		// 【警告】
		// 为了简单，这里没有创建新的对象，直接修改并引用了r.Argument。
		// 这个破坏了r.Argument只读的属性。另外引用同一个对象，也有点风险。
		// 在目前没有问题，因为r.Argument主要记录在state.ServiceInfos中，
		// 另外它也被Session引用（用于连接关闭时，自动注销）。
		// 这是专用程序，不是一个库，以后有修改时，小心就是了。
		r.Argument.SessionId = r.getSender().getSessionId();

		// AddOrUpdate，否则重连重新注册很难恢复到正确的状态。
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (state) {
			state.ServiceInfos.put(r.Argument.getServiceIdentity(), r.Argument);
			r.SendResultCode(Register.Success);
			state.StartReadyCommitNotify();
			state.NotifySimpleOnRegister(r.Argument);
		}
		return Procedure.Success;
	}

	private long ProcessUpdate(Update r) {
		logger.info("{}: Update {} id={} ip={} port={}", r.getSender(), r.Argument.getServiceName(),
				r.Argument.getServiceIdentity(), r.Argument.getPassiveIp(), r.Argument.getPassivePort());
		var session = (Session)r.getSender().getUserState();
		if (!session.Registers.containsKey(r.Argument))
			return Update.ServiceNotRegister;

		var state = ServerStates.get(r.Argument.getServiceName());
		if (state == null)
			return Update.ServerStateError;

		var rc = state.UpdateAndNotify(r.Argument);
		if (rc != 0)
			return rc;
		r.SendResult();
		return 0;
	}

	public ServerState UnRegisterNow(long sessionId, BServiceInfo info) {
		var state = ServerStates.get(info.getServiceName());
		if (state != null) {
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (state) {
				var exist = state.ServiceInfos.get(info.getServiceIdentity());
				if (exist != null) {
					// 这里存在一个时间窗口，可能使得重复的注销会成功。注销一般比较特殊，忽略这个问题。
					var existSessionId = exist.SessionId;
					if (existSessionId == null || sessionId == existSessionId) {
						// 有可能当前连接没有注销，新的注册已经AddOrUpdate，此时忽略当前连接的注销。
						if (state.ServiceInfos.remove(info.getServiceIdentity(), exist)) {
							state.NotifySimpleOnUnRegister(exist);
							return state;
						}
					}
				}
			}
		}
		return null;
	}

	private long ProcessUnRegister(UnRegister r) {
		logger.info("{}: UnRegister {} id={}",
				r.getSender(), r.Argument.getServiceName(), r.Argument.getServiceIdentity());
		if (UnRegisterNow(r.getSender().getSessionId(), r.Argument) != null) {
			// ignore TryRemove failed.
			var session = (Session)r.getSender().getUserState();
			session.Registers.remove(r.Argument);
			//r.SendResultCode(UnRegister.Success);
			//return Procedure.Success;
		}
		// 注销不存在也返回成功，否则Agent处理比较麻烦。
		r.SendResultCode(UnRegister.Success);
		return Procedure.Success;
	}

	private long ProcessSubscribe(Subscribe r) {
		logger.info("{}: Subscribe {} type={}",
				r.getSender(), r.Argument.getServiceName(), r.Argument.getSubscribeType());
		var session = (Session)r.getSender().getUserState();
		session.Subscribes.putIfAbsent(r.Argument.getServiceName(), r.Argument);
		var state = ServerStates.computeIfAbsent(r.Argument.getServiceName(), name -> new ServerState(this, name));
		return state.SubscribeAndSend(r, session);
	}

	public ServerState UnSubscribeNow(long sessionId, BSubscribeInfo info) {
		var state = ServerStates.get(info.getServiceName());
		if (state != null) {
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (state) {
				switch (info.getSubscribeType()) {
				case BSubscribeInfo.SubscribeTypeSimple:
					if (state.Simple.remove(sessionId) != null)
						return state;
					break;
				case BSubscribeInfo.SubscribeTypeReadyCommit:
					if (state.ReadyCommit.remove(sessionId) != null)
						return state;
					break;
				}
			}
		}
		return null;
	}

	private long ProcessUnSubscribe(UnSubscribe r) {
		logger.info("{}: UnSubscribe {} type={}",
				r.getSender(), r.Argument.getServiceName(), r.Argument.getSubscribeType());
		var session = (Session)r.getSender().getUserState();
		var sub = session.Subscribes.remove(r.Argument.getServiceName());
		if (sub != null) {
			if (r.Argument.getSubscribeType() == sub.getSubscribeType()) {
				var changed = UnSubscribeNow(r.getSender().getSessionId(), r.Argument);
				if (changed != null) {
					r.setResultCode(UnSubscribe.Success);
					r.SendResult();
					changed.TryCommit();
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

	private long ProcessReadyServiceList(ReadyServiceList r) {
		var session = (Session)r.getSender().getUserState();
		var state = ServerStates.computeIfAbsent(r.Argument.ServiceName, name -> new ServerState(this, name));
		state.SetReady(r, session);
		return Procedure.Success;
	}

	private long ProcessSetLoad(SetServerLoad setServerLoad) {
		Loads.computeIfAbsent(setServerLoad.Argument.getName(), __ -> new LoadObservers(this))
				.SetLoad(setServerLoad.Argument);
		return 0;
	}

	private static long ProcessOfflineRegister(OfflineRegister r) {
		logger.info("{}: OfflineRegister serverId={} notifyId={}",
				r.getSender(), r.Argument.ServerId, r.Argument.NotifyId);
		var session = (Session)r.getSender().getUserState();
		// 允许重复注册：简化server注册逻辑。
		synchronized (session.OfflineRegisterNotifies) {
			session.OfflineRegisterServerId = r.Argument.ServerId;
			session.OfflineRegisterNotifies.put(r.Argument.NotifyId, r.Argument);
		}
		r.SendResult();
		return 0;
	}

	@Override
	public void close() throws IOException {
		try {
			Stop();
		} catch (RuntimeException | IOException e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public ServiceManagerServer(InetAddress ipaddress, int port, Zeze.Config config) throws Throwable {
		this(ipaddress, port, config, -1);
	}

	public ServiceManagerServer(InetAddress ipaddress, int port, Zeze.Config config, int startNotifyDelay)
			throws Throwable {
		Config = config.GetCustomize(new Conf());

		if (startNotifyDelay >= 0)
			Config.StartNotifyDelay = startNotifyDelay;

		Server = new NetServer(this, config);

		Server.AddFactoryHandle(Register.TypeId_, new Service.ProtocolFactoryHandle<>(
				Register::new, this::ProcessRegister, TransactionLevel.None, DispatchMode.Critical));
		Server.AddFactoryHandle(Update.TypeId_, new Service.ProtocolFactoryHandle<>(
				Update::new, this::ProcessUpdate, TransactionLevel.None, DispatchMode.Critical));
		Server.AddFactoryHandle(UnRegister.TypeId_, new Service.ProtocolFactoryHandle<>(
				UnRegister::new, this::ProcessUnRegister, TransactionLevel.None, DispatchMode.Critical));
		Server.AddFactoryHandle(Subscribe.TypeId_, new Service.ProtocolFactoryHandle<>(
				Subscribe::new, this::ProcessSubscribe, TransactionLevel.None, DispatchMode.Critical));
		Server.AddFactoryHandle(UnSubscribe.TypeId_, new Service.ProtocolFactoryHandle<>(
				UnSubscribe::new, this::ProcessUnSubscribe, TransactionLevel.None, DispatchMode.Critical));
		Server.AddFactoryHandle(ReadyServiceList.TypeId_, new Service.ProtocolFactoryHandle<>(
				ReadyServiceList::new, this::ProcessReadyServiceList, TransactionLevel.None, DispatchMode.Critical));
		Server.AddFactoryHandle(KeepAlive.TypeId_, new Service.ProtocolFactoryHandle<>(
				KeepAlive::new, null, TransactionLevel.None, DispatchMode.Direct));
		Server.AddFactoryHandle(AllocateId.TypeId_, new Service.ProtocolFactoryHandle<>(
				AllocateId::new, this::ProcessAllocateId, TransactionLevel.None, DispatchMode.Critical));
		Server.AddFactoryHandle(SetServerLoad.TypeId_, new Service.ProtocolFactoryHandle<>(
				SetServerLoad::new, this::ProcessSetLoad, TransactionLevel.None, DispatchMode.Critical));
		Server.AddFactoryHandle(OfflineRegister.TypeId_, new Service.ProtocolFactoryHandle<>(
				OfflineRegister::new, ServiceManagerServer::ProcessOfflineRegister, TransactionLevel.None, DispatchMode.Critical));
		Server.AddFactoryHandle(OfflineNotify.TypeId_, new Service.ProtocolFactoryHandle<>(
				OfflineNotify::new, null, TransactionLevel.None, DispatchMode.Direct));

		if (Config.StartNotifyDelay > 0) {
			//noinspection NonAtomicOperationOnVolatileField
			StartNotifyDelayTask = Task.scheduleUnsafe(Config.StartNotifyDelay, () -> {
				StartNotifyDelayTask = null;
				for (var v : ServerStates.values())
					v.StartReadyCommitNotify(true);
			});
		}

		AutoKeysDb = RocksDB.open(DatabaseRocksDb.getCommonOptions(), Paths.get(Config.DbHome, "autokeys").toString());

		// 允许配置多个acceptor，如果有冲突，通过日志查看。
		ServerSocket = Server.NewServerSocket(ipaddress, port, null);
		Server.Start();
		/*
		try {
			Server.NewServerSocket("127.0.0.1", port, null);
		} catch (Throwable skip) {
			skip.printStackTrace();
		}
		try {
			Server.NewServerSocket("::1", port, null);
		} catch (Throwable skip) {
			skip.printStackTrace();
		}
		*/
	}

	public static final class AutoKey {
		private final ServiceManagerServer SMS;
		private final byte[] Key;
		private long Current;

		public AutoKey(String name, ServiceManagerServer sms) {
			SMS = sms;
			byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
			var bb = ByteBuffer.Allocate(ByteBuffer.writeUIntSize(nameBytes.length) + nameBytes.length);
			bb.WriteBytes(nameBytes);
			Key = bb.Bytes;
			try {
				byte[] value = SMS.AutoKeysDb.get(DatabaseRocksDb.getDefaultReadOptions(), Key);
				if (value != null)
					Current = ByteBuffer.Wrap(value).ReadLong();
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		}

		public synchronized void Allocate(AllocateId rpc) {
			rpc.Result.setStartId(Current);

			var count = rpc.Argument.getCount();

			// 随便修正一下分配数量。
			if (count < 256)
				count = 256;
			else if (count > 10000)
				count = 10000;

			long current = Current + count;
			Current = current;
			var bb = ByteBuffer.Allocate(ByteBuffer.writeLongSize(current));
			bb.WriteLong(current);
			try {
				SMS.AutoKeysDb.put(DatabaseRocksDb.getSyncWriteOptions(), Key, bb.Bytes);
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}

			rpc.Result.setCount(count);
		}
	}

	private long ProcessAllocateId(AllocateId r) {
		var name = r.Argument.getName();
		r.Result.setName(name);
		AutoKeys.computeIfAbsent(name, key -> new AutoKey(key, this)).Allocate(r);
		r.SendResult();
		return 0;
	}

	public synchronized void Stop() throws Throwable {
		if (Server == null)
			return;
		var startNotifyDelayTask = StartNotifyDelayTask;
		if (startNotifyDelayTask != null)
			startNotifyDelayTask.cancel(false);
		ServerSocket.close();
		Server.Stop();
		Server = null;

		for (var ss : ServerStates.values())
			ss.Close();
		if (AutoKeysDb != null)
			AutoKeysDb.close();
	}

	public static final class NetServer extends HandshakeServer {
		private final ServiceManagerServer ServiceManager;
		private final TaskOneByOneByKey oneByOneByKey = new TaskOneByOneByKey();

		public NetServer(ServiceManagerServer sm, Zeze.Config config) throws Throwable {
			super("Zeze.Services.ServiceManager", config);
			ServiceManager = sm;
		}

		LongConcurrentHashMap<AsyncSocket> getAllSocks() {
			return SocketMap;
		}

		@Override
		public void OnSocketAccept(AsyncSocket so) throws Throwable {
			logger.info("OnSocketAccept: {} sessionId={}", so, so.getSessionId());
			so.setUserState(new Session(ServiceManager, so.getSessionId()));
			super.OnSocketAccept(so);
		}

		@Override
		public void OnSocketClose(AsyncSocket so, Throwable e) throws Throwable {
			logger.info("OnSocketClose: {} sessionId={}", so, so.getSessionId());
			var session = (Session)so.getUserState();
			if (session != null)
				session.OnClose();
			super.OnSocketClose(so, e);
		}

		@Override
		public <P extends Protocol<?>> void DispatchProtocol(P p, ProtocolFactoryHandle<P> factoryHandle) {
			if (factoryHandle.Handle != null) {
				oneByOneByKey.Execute(p.getSender(),
						() -> Task.Call(() -> factoryHandle.Handle.handle(p), p, Protocol::trySendResultCode),
						factoryHandle.Mode);
			}
		}
	}

	public static void main(String[] args) throws Throwable {
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			e.printStackTrace();
			logger.fatal("uncaught exception in {}:", t, e);
		});

		String ip = null;
		int port = 5001;

		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-ip":
				ip = args[++i];
				break;
			case "-port":
				port = Integer.parseInt(args[++i]);
				break;
			}
		}
		logger.info("Start {}:{}", ip != null ? ip : "any", port);

		Task.tryInitThreadPool(null, null, null);

		InetAddress address = (ip != null && !ip.isBlank()) ? InetAddress.getByName(ip) : null;
		var config = new Zeze.Config().AddCustomize(new ServiceManagerServer.Conf()).LoadAndParse();

		try (var ignored = new ServiceManagerServer(address, port, config)) {
			synchronized (Thread.currentThread()) {
				Thread.currentThread().wait();
			}
		}
	}
}
