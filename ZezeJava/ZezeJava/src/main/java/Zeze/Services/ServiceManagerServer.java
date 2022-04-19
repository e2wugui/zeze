package Zeze.Services;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.AllocateId;
import Zeze.Services.ServiceManager.CommitServiceList;
import Zeze.Services.ServiceManager.KeepAlive;
import Zeze.Services.ServiceManager.NotifyServiceList;
import Zeze.Services.ServiceManager.ReadyServiceList;
import Zeze.Services.ServiceManager.Register;
import Zeze.Services.ServiceManager.ServiceInfo;
import Zeze.Services.ServiceManager.ServiceInfos;
import Zeze.Services.ServiceManager.SetServerLoad;
import Zeze.Services.ServiceManager.Subscribe;
import Zeze.Services.ServiceManager.SubscribeFirstCommit;
import Zeze.Services.ServiceManager.SubscribeInfo;
import Zeze.Services.ServiceManager.UnRegister;
import Zeze.Services.ServiceManager.UnSubscribe;
import Zeze.Services.ServiceManager.Update;
import Zeze.Transaction.Procedure;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.IdentityHashSet;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Random;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;
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
	private static final Logger logger = LogManager.getLogger(ServiceManagerServer.class);

	// ServiceInfo.Name -> ServiceState
	private final ConcurrentHashMap<String, ServerState> ServerStates = new ConcurrentHashMap<>();

	// 简单负载广播，
	// 在RegisterService/UpdateService时自动订阅，会话关闭的时候删除。
	// ProcessSetLoad时广播，本来不需要记录负载数据的，但为了以后可能的查询，保存一份。
	private final ConcurrentHashMap<String, LoadObservers> Loads = new ConcurrentHashMap<>();

	public static class LoadObservers {
		public final ServiceManagerServer ServiceManager;
		public Zeze.Services.ServiceManager.ServerLoad Load;
		public final IdentityHashSet<Long> Observers = new IdentityHashSet<>();

		public LoadObservers(ServiceManagerServer m) {
			ServiceManager = m;
		}

		// synchronized big?
		public synchronized void SetLoad(Zeze.Services.ServiceManager.ServerLoad load) {
			Load = load;
			var set = new SetServerLoad();
			set.Argument = load;
			for (var it = Observers.iterator(); it.hasNext(); ) {
				Long observer = it.next();
				try {
					// skip rpc result
					if (set.Send(ServiceManager.Server.GetSocket(observer)))
						continue;
				} catch (Throwable ex) {
					// skip error
				}
				it.remove();
			}
		}
	}

	/**
	 * 需要从配置文件中读取，把这个引用加入： Zeze.Config.AddCustomize
	 */
	private final Conf Config;
	private NetServer Server;
	private final AsyncSocket ServerSocket;
	private final RocksDB AutoKeysDb;
	private final WriteOptions WriteOptions;
	private final ConcurrentHashMap<String, AutoKey> AutoKeys = new ConcurrentHashMap<>();
	private volatile Future<?> StartNotifyDelayTask;

	public Conf getConfig() {
		return Config;
	}

	public NetServer getServer() {
		return Server;
	}

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

		public int getKeepAlivePeriod() {
			return KeepAlivePeriod;
		}

		public void setKeepAlivePeriod(int value) {
			KeepAlivePeriod = value;
		}

		public int getStartNotifyDelay() {
			return StartNotifyDelay;
		}

		public void setStartNotifyDelay(int value) {
			StartNotifyDelay = value;
		}

		public int getRetryNotifyDelayWhenNotAllReady() {
			return RetryNotifyDelayWhenNotAllReady;
		}

		public void setRetryNotifyDelayWhenNotAllReady(int value) {
			RetryNotifyDelayWhenNotAllReady = value;
		}

		public String getDbHome() {
			return DbHome;
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

	public static final class ServerState {
		private final ServiceManagerServer ServiceManager;
		private final String ServiceName;
		// identity ->
		// 记录一下SessionId，方便以后找到服务所在的连接。
		private final ConcurrentHashMap<String, ServiceInfo> ServiceInfos = new ConcurrentHashMap<>(); // key:serverId
		private final LongConcurrentHashMap<SubscribeState> Simple = new LongConcurrentHashMap<>(); // key:sessionId
		private final LongConcurrentHashMap<SubscribeState> ReadyCommit = new LongConcurrentHashMap<>(); // key:sessionId
		private Future<?> NotifyTimeoutTask;
		private long SerialId;

		public String getServiceName() {
			return ServiceName;
		}

		public ConcurrentHashMap<String, ServiceInfo> getServiceInfos() {
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
			var notify = new NotifyServiceList();
			notify.Argument = new ServiceInfos(ServiceName, this, ++SerialId);
			logger.debug("StartNotify {}", notify.Argument);
			var notifyBytes = notify.Encode();

			if (notifySimple) {
				for (var it = Simple.keyIterator(); it.hasNext(); ) {
					var s = ServiceManager.Server.GetSocket(it.next());
					if (s != null)
						s.Send(notifyBytes);
				}
			}

			for (var it = ReadyCommit.entryIterator(); it.moveToNext(); ) {
				it.value().setReady(false);
				var s = ServiceManager.Server.GetSocket(it.key());
				if (s != null)
					s.Send(notifyBytes);
			}
			if (!ReadyCommit.isEmpty()) {
				// 只有两段公告模式需要回应处理。
				if (NotifyTimeoutTask != null)
					NotifyTimeoutTask.cancel(false);
				NotifyTimeoutTask = Task.schedule(ServiceManager.Config.getRetryNotifyDelayWhenNotAllReady(),
						() -> {
							// NotifyTimeoutTask 会在下面两种情况下被修改：
							// 1. 在 Notify.ReadyCommit 完成以后会被清空。
							// 2. 启动了新的 Notify。
							StartReadyCommitNotify(); // restart
						});
			}
		}

		public synchronized void NotifySimpleOnRegister(ServiceInfo info) {
			for (var it = Simple.keyIterator(); it.hasNext(); ) {
				var r = new Register();
				r.Argument = info;
				r.Send(ServiceManager.Server.GetSocket(it.next()));
			}
		}

		public synchronized void NotifySimpleOnUnRegister(ServiceInfo info) {
			for (var it = Simple.keyIterator(); it.hasNext(); ) {
				var r = new UnRegister();
				r.Argument = info;
				r.Send(ServiceManager.Server.GetSocket(it.next()));
			}
		}

		public synchronized int UpdateAndNotify(ServiceInfo info) {
			var current = ServiceInfos.get(info.getServiceIdentity());
			if (current == null)
				return Update.ServiceIdentityNotExist;

			current.setPassiveIp(info.getPassiveIp());
			current.setPassivePort(info.getPassivePort());
			current.setExtraInfo(info.getExtraInfo());

			// 简单广播。
			for (var it = Simple.keyIterator(); it.hasNext(); ) {
				var r = new Update();
				r.Argument = current;
				r.Send(ServiceManager.Server.GetSocket(it.next()));
			}
			for (var it = ReadyCommit.keyIterator(); it.hasNext(); ) {
				var r = new Update();
				r.Argument = current;
				r.Send(ServiceManager.Server.GetSocket(it.next()));
			}
			return 0;
		}

		public synchronized void TryCommit() {
			if (NotifyTimeoutTask == null)
				return; // no pending notify

			for (var e : ReadyCommit) {
				if (!e.Ready)
					return;
			}
			logger.debug("Ready Broadcast.");
			var commit = new CommitServiceList();
			commit.Argument.ServiceName = ServiceName;
			commit.Argument.SerialId = SerialId;
			for (var it = ReadyCommit.keyIterator(); it.hasNext(); ) {
				var so = ServiceManager.Server.GetSocket(it.next());
				if (so != null)
					so.Send(commit);
			}
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
			case SubscribeInfo.SubscribeTypeSimple:
				Simple.putIfAbsent(session.getSessionId(), new SubscribeState(session.getSessionId()));
				if (ServiceManager.StartNotifyDelayTask == null) {
					var arg = new ServiceInfos(ServiceName, this, SerialId);
					SubscribeFirstCommit tempVar = new SubscribeFirstCommit();
					tempVar.Argument = arg;
					tempVar.Send(r.getSender());
				}
				break;
			case SubscribeInfo.SubscribeTypeReadyCommit:
				ReadyCommit.putIfAbsent(session.getSessionId(), new SubscribeState(session.getSessionId()));
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

		@SuppressWarnings("unused")
		<T> boolean SequenceEqual(List<T> a, List<T> b) {
			int size = a.size();
			if (size != b.size())
				return false;
			for (int i = 0; i < size; ++i) {
				if (!a.get(i).equals(b.get(i)))
					return false;
			}
			return true;
		}

		public synchronized void SetReady(ReadyServiceList p, Session session) {
			if (p.Argument.SerialId != SerialId) {
				logger.debug("Ready Skip: SerialId Not Equal." + p.Argument.SerialId + " Now=" + SerialId);
				return;
			}
			//logger.debug("Ready:" + p.Argument.SerialId + " Now=" + SerialId);
			var subscribeState = ReadyCommit.get(session.getSessionId());
			if (subscribeState == null)
				return;
			subscribeState.Ready = true;
			TryCommit();
		}
	}

	public static final class SubscribeState {
		private final long SessionId;
		private boolean Ready;

		public long getSessionId() {
			return SessionId;
		}

		public void setReady(boolean value) {
			Ready = value;
		}

		public SubscribeState(long ssid) {
			SessionId = ssid;
		}
	}

	public static final class Session {
		private final ServiceManagerServer ServiceManager;
		private final long SessionId;
		private final ConcurrentHashSet<ServiceInfo> Registers = new ConcurrentHashSet<>();
		// key is ServiceName: 会话订阅
		private final ConcurrentHashMap<String, SubscribeInfo> Subscribes = new ConcurrentHashMap<>();
		private Future<?> KeepAliveTimerTask;

		public ServiceManagerServer getServiceManager() {
			return ServiceManager;
		}

		public long getSessionId() {
			return SessionId;
		}

		public ConcurrentHashSet<ServiceInfo> getRegisters() {
			return Registers;
		}

		public ConcurrentHashMap<String, SubscribeInfo> getSubscribes() {
			return Subscribes;
		}

		public Session(ServiceManagerServer sm, long ssid) {
			ServiceManager = sm;
			SessionId = ssid;
			if (ServiceManager.Config.getKeepAlivePeriod() > 0) {
				KeepAliveTimerTask = Task.schedule(
						Random.getInstance().nextInt(ServiceManager.Config.getKeepAlivePeriod()),
						ServiceManager.Config.getKeepAlivePeriod(),
						() -> {
							AsyncSocket s = null;
							try {
								s = ServiceManager.Server.GetSocket(getSessionId());
								var r = new KeepAlive();
								r.SendAndWaitCheckResultCode(s);
							} catch (Throwable ex) {
								if (s != null)
									s.Close(null);
								logger.error("ServiceManager.KeepAlive", ex);
							}
						});
			}
		}

		public void OnClose() {
			if (KeepAliveTimerTask != null) {
				KeepAliveTimerTask.cancel(false);
				KeepAliveTimerTask = null;
			}

			for (var info : getSubscribes().values())
				ServiceManager.UnSubscribeNow(getSessionId(), info);

			HashMap<String, ServerState> changed = new HashMap<>(Registers.size());

			for (var info : Registers) {
				var state = ServiceManager.UnRegisterNow(getSessionId(), info);
				if (state != null)
					changed.putIfAbsent(state.getServiceName(), state);
			}

			for (var state : changed.values())
				state.StartReadyCommitNotify();
		}
	}

	static {
		System.setProperty("log4j.configurationFile", "log4j2.xml");
	}

	private void AddLoadObserver(String ip, int port, AsyncSocket sender) {
		if (ip.isEmpty() || port == 0)
			return;
		var host = ip + ":" + port;
		Loads.computeIfAbsent(host, (key) -> new LoadObservers(this)).Observers.Add(sender.getSessionId());
	}

	private long ProcessRegister(Register r) {
		var session = (Session)r.getSender().getUserState();

		// 允许重复登录，断线重连Agent不好原子实现重发。
		if (session.getRegisters().add(r.Argument))
			logger.info("Register {}, {}", r.Argument.getServiceName(), r.Argument.getServiceIdentity());
		else
			logger.info("already Registered {}, {}", r.Argument.getServiceName(), r.Argument.getServiceIdentity());
		var state = ServerStates.computeIfAbsent(r.Argument.getServiceName(), name -> new ServerState(this, name));

		// 【警告】
		// 为了简单，这里没有创建新的对象，直接修改并引用了r.Argument。
		// 这个破坏了r.Argument只读的属性。另外引用同一个对象，也有点风险。
		// 在目前没有问题，因为r.Argument主要记录在state.ServiceInfos中，
		// 另外它也被Session引用（用于连接关闭时，自动注销）。
		// 这是专用程序，不是一个库，以后有修改时，小心就是了。
		r.Argument.setLocalState(r.getSender().getSessionId());

		// AddOrUpdate，否则重连重新注册很难恢复到正确的状态。
		state.ServiceInfos.put(r.Argument.getServiceIdentity(), r.Argument);
		r.SendResultCode(Register.Success);
		state.StartReadyCommitNotify();
		state.NotifySimpleOnRegister(r.Argument);
		return Procedure.Success;
	}

	private long ProcessUpdate(Update r) {
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

	public ServerState UnRegisterNow(long sessionId, ServiceInfo info) {
		var state = ServerStates.get(info.getServiceName());
		if (state != null) {
			var exist = state.ServiceInfos.get(info.getServiceIdentity());
			if (exist != null) {
				// 这里存在一个时间窗口，可能使得重复的注销会成功。注销一般比较特殊，忽略这个问题。
				Long existSessionId = (Long)exist.getLocalState();
				if (existSessionId == null || sessionId == existSessionId) {
					// 有可能当前连接没有注销，新的注册已经AddOrUpdate，此时忽略当前连接的注销。
					if (state.ServiceInfos.remove(info.getServiceIdentity(), exist)) {
						state.NotifySimpleOnUnRegister(exist);
						return state;
					}
				}
			}
		}
		return null;
	}

	private long ProcessUnRegister(UnRegister r) {
		if (UnRegisterNow(r.getSender().getSessionId(), r.Argument) != null) {
			// ignore TryRemove failed.
			var session = (Session)r.getSender().getUserState();
			session.getRegisters().remove(r.Argument);
			//r.SendResultCode(UnRegister.Success);
			//return Procedure.Success;
		}
		// 注销不存在也返回成功，否则Agent处理比较麻烦。
		r.SendResultCode(UnRegister.Success);
		return Procedure.Success;
	}

	private long ProcessSubscribe(Subscribe r) {
		var session = (Session)r.getSender().getUserState();
		session.getSubscribes().putIfAbsent(r.Argument.getServiceName(), r.Argument);
		var state = ServerStates.computeIfAbsent(r.Argument.getServiceName(),
				name -> new ServerState(this, name));
		return state.SubscribeAndSend(r, session);
	}

	public ServerState UnSubscribeNow(long sessionId, SubscribeInfo info) {
		var state = ServerStates.get(info.getServiceName());
		if (state != null) {
			switch (info.getSubscribeType()) {
			case SubscribeInfo.SubscribeTypeSimple:
				if (state.Simple.remove(sessionId) != null)
					return state;
				break;
			case SubscribeInfo.SubscribeTypeReadyCommit:
				if (state.ReadyCommit.remove(sessionId) != null)
					return state;
				break;
			}
		}
		return null;
	}

	private long ProcessUnSubscribe(UnSubscribe r) {
		var session = (Session)r.getSender().getUserState();
		var sub = session.getSubscribes().remove(r.Argument.getServiceName());
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
		Loads.computeIfAbsent(setServerLoad.Argument.getName(), (key) -> new LoadObservers(this)).SetLoad(setServerLoad.Argument);
		return 0;
	}

	@Override
	public void close() throws IOException {
		try {
			Stop();
		} catch (Throwable e) {
			throw new IOException(e);
		}
	}

	public ServiceManagerServer(InetAddress ipaddress, int port, Zeze.Config config) throws Throwable {
		this(ipaddress, port, config, -1);
	}

	public ServiceManagerServer(InetAddress ipaddress, int port, Zeze.Config config, int startNotifyDelay) throws Throwable {
		Config = config.GetCustomize(new Conf());

		if (startNotifyDelay >= 0)
			Config.setStartNotifyDelay(startNotifyDelay);

		Server = new NetServer(this, config);

		Server.AddFactoryHandle(Register.TypeId_,
				new Service.ProtocolFactoryHandle<>(Register::new, this::ProcessRegister));

		Server.AddFactoryHandle(Update.TypeId_,
				new Service.ProtocolFactoryHandle<>(Update::new, this::ProcessUpdate));

		Server.AddFactoryHandle(UnRegister.TypeId_,
				new Service.ProtocolFactoryHandle<>(UnRegister::new, this::ProcessUnRegister));

		Server.AddFactoryHandle(Subscribe.TypeId_,
				new Service.ProtocolFactoryHandle<>(Subscribe::new, this::ProcessSubscribe));

		Server.AddFactoryHandle(UnSubscribe.TypeId_,
				new Service.ProtocolFactoryHandle<>(UnSubscribe::new, this::ProcessUnSubscribe));

		Server.AddFactoryHandle(ReadyServiceList.TypeId_,
				new Service.ProtocolFactoryHandle<>(ReadyServiceList::new, this::ProcessReadyServiceList));

		Server.AddFactoryHandle(KeepAlive.TypeId_,
				new Service.ProtocolFactoryHandle<>(KeepAlive::new));

		Server.AddFactoryHandle(AllocateId.TypeId_,
				new Service.ProtocolFactoryHandle<>(AllocateId::new, this::ProcessAllocateId));

		Server.AddFactoryHandle(SetServerLoad.TypeId_,
				new Service.ProtocolFactoryHandle<>(SetServerLoad::new, this::ProcessSetLoad));

		if (Config.getStartNotifyDelay() > 0)
			StartNotifyDelayTask = Task.schedule(Config.getStartNotifyDelay(), this::StartNotifyAll);

		var options = new Options().setCreateIfMissing(true);

		AutoKeysDb = RocksDB.open(options, Paths.get(Config.getDbHome(), "autokeys").toString());
		WriteOptions = new WriteOptions().setSync(true);

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
		private final String Name;
		private final byte[] Key;
		private long Current;

		public String getName() {
			return Name;
		}

		private byte[] getKey() {
			return Key;
		}

		private long getCurrent() {
			return Current;
		}

		private void setCurrent(long value) {
			Current = value;
		}

		public AutoKey(String name, ServiceManagerServer sms) {
			Name = name;
			SMS = sms;
			byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
			var bb = ByteBuffer.Allocate(ByteBuffer.writeUIntSize(nameBytes.length) + nameBytes.length);
			bb.WriteBytes(nameBytes);
			Key = bb.Bytes;
			byte[] value;
			try {
				value = SMS.AutoKeysDb.get(getKey());
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
			if (value != null) {
				bb = ByteBuffer.Wrap(value);
				setCurrent(bb.ReadLong());
			}
		}

		public synchronized void Allocate(AllocateId rpc) {
			rpc.Result.setStartId(getCurrent());

			var count = rpc.Argument.getCount();

			// 随便修正一下分配数量。
			if (count < 256)
				count = 256;
			else if (count > 10000)
				count = 10000;

			setCurrent(getCurrent() + count);
			long current = getCurrent();
			var bb = ByteBuffer.Allocate(ByteBuffer.writeLongSize(current));
			bb.WriteLong(current);
			try {
				SMS.AutoKeysDb.put(SMS.WriteOptions, getKey(), bb.Bytes);
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}

			rpc.Result.setCount(count);
		}
	}

	private long ProcessAllocateId(AllocateId r) {
		var n = r.Argument.getName();
		r.Result.setName(n);
		AutoKeys.computeIfAbsent(n, key -> new AutoKey(key, this)).Allocate(r);
		r.SendResult();
		return 0;
	}

	private void StartNotifyAll() {
		StartNotifyDelayTask = null;
		for (var e : ServerStates.entrySet())
			e.getValue().StartReadyCommitNotify(true);
	}

	public synchronized void Stop() throws Throwable {
		if (Server == null)
			return;
		Future<?> startNotifyDelayTask = StartNotifyDelayTask;
		if (startNotifyDelayTask != null)
			startNotifyDelayTask.cancel(false);
		ServerSocket.Close(null);
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

		public ServiceManagerServer getServiceManager() {
			return ServiceManager;
		}

		public NetServer(ServiceManagerServer sm, Zeze.Config config) throws Throwable {
			super("Zeze.Services.ServiceManager", config);
			ServiceManager = sm;
		}

		@Override
		public void OnSocketAccept(AsyncSocket so) throws Throwable {
			so.setUserState(new Session(ServiceManager, so.getSessionId()));
			super.OnSocketAccept(so);
		}

		@Override
		public void OnSocketClose(AsyncSocket so, Throwable e) throws Throwable {
			var session = (Session)so.getUserState();
			if (session != null)
				session.OnClose();
			super.OnSocketClose(so, e);
		}

		@Override
		public <P extends Protocol<?>> void DispatchProtocol(P p, ProtocolFactoryHandle<P> factoryHandle) {
			if (factoryHandle.Handle != null) {
				oneByOneByKey.Execute(p.getSender(),
						() -> Task.Call(() -> factoryHandle.Handle.handle(p), p, Protocol::SendResultCode));
			}
		}
	}

	public static void main(String[] args) throws Throwable {
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
