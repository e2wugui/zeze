package Zeze.Services;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.Services.ServiceManager.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;
import org.w3c.dom.Element;
import java.util.concurrent.ConcurrentHashMap;

public final class ServiceManagerServer implements Closeable {
	//////////////////////////////////////////////////////////////////////////////
	/** 服务管理：注册和订阅
	 【名词】
	 动态服务(gs)
		 动态服务器一般指启用cache-sync的逻辑服务器。比如gs。
	 注册服务器（ServiceManager）
		 支持更新服务器，这个服务一开始是为了启用cache-sync的服务器的查找。
	 动态服务器列表使用者(linkd)
		 当前使用动态服务的客户端主要是Game2/linkd，linkd在hash分配请求的时候需要一致动态服务器列表。
	
	 【下面的流程都是用现有的服务名字（上面括号中的名字）】
	 
	 【本控制功能的目标】
	 所有的linkd的可用动态服务列表的更新并不是原子的。
	 1. 让所有的linkd的列表保持最新；
	 2. 尽可能减少linkd上的服务列表不一致的时间（通过ready-commit机制）；
	 3. 列表不一致时，分发请求可能引起cache不命中，但不影响正确性（cache-sync保证了正确性）；
	 
	 【主要事件和流程】
	 1. gs停止时调用 RegisterService,UnRegisterService 向ServiceManager声明自己服务状态。
	 2. linkd启动时调用 UseService, UnUseService 向ServiceManager申请使用gs-list。
	 3. ServiceManager在RegisterService,UnRegisterService处理时发送 NotifyServiceList 给所有的 linkd。
	 4. linkd收到NotifyServiceList先记录到本地，同时持续关注自己和gs之间的连接，
		当列表中的所有serivce都准备完成时调用 ReadyServiceList。
	 5. ServiceManager收到所有的linkd的ReadyServiceList后，向所有的linkd广播 CommitServiceList。
	 6. linkd 收到 CommitServiceList 时，启用新的服务列表。
	 
	 【特别规则和错误处理】
	 1. linkd 异常停止，ServiceManager 按 UnUseService 处理，仅仅简单移除use-list。相当于减少了以后请求来源。
	 2. gs 异常停止，ServiceManager 按 UnRegisterService 处理，移除可用服务，并启动列表更新流程（NotifyServiceList）。
	 3. linkd 处理 gs 关闭（在NotifyServiceList之前），仅仅更新本地服务列表状态，让该服务暂时不可用，但不改变列表。
		linkd总是使用ServiceManager提交给他的服务列表，自己不主动增删。
		linkd在NotifyServiceList的列表减少的处理：一般总是立即进入ready（因为其他gs都是可用状态）。
	 4. ServiceManager 异常关闭：
		a) 启用raft以后，新的master会有正确列表数据，但服务状态（连接）未知，此时等待gs的RegisterService一段时间,
		   然后开启新的一轮NotifyServiceList，等待时间内没有再次注册的gs以后当作新的处理。
		b) 启用raft的好处是raft的非master服务器会识别这种状态，并重定向请求到master，使得系统内只有一个master启用服务。
		   实际上raft不需要维护相同数据状态（gs-list），从空的开始即可，启用raft的话仅使用他的选举功能。
		#) 由于ServiceManager可以较快恢复，暂时不考虑使用Raft，实现无聊了再来加这个吧
	 5. ServiceManager开启一轮变更通告过程中，有新的gs启动停止，将开启新的通告(NotifyServiceList)。
		ReadyServiceList时会检查ready中的列表是否和当前ServiceManagerlist一致，不一致直接忽略。
		新的通告流程会促使linkd继续发送ready。
		另外为了更健壮的处理通告，通告加一个超时机制。超时没有全部ready，就启动一次新的通告。
		原则是：总按最新的gs-list通告。中间不一致的ready全部忽略。
	*/

	// ServiceInfo.Name -> ServiceState
	private final ConcurrentHashMap<String, ServerState> ServerStates = new ConcurrentHashMap<>();
	private NetServer Server;
	public NetServer getServer() {
		return Server;
	}
	private void setServer(NetServer value) {
		Server = value;
	}
	private AsyncSocket ServerSocket;
	private volatile Zeze.Util.Task StartNotifyDelayTask;

	public final static class Conf implements Zeze.Config.ICustomize {
		public String getName() {
			return "Zeze.Services.ServiceManager";
		}

		private int KeepAlivePeriod = 300 * 1000;
		public int getKeepAlivePeriod() {
			return KeepAlivePeriod;
		}
		public void setKeepAlivePeriod(int value) {
			KeepAlivePeriod = value;
		}

		/** 
		 启动以后接收注册和订阅，一段时间内不进行通知。
		 用来处理ServiceManager异常重启导致服务列表重置的问题。
		 在Delay时间内，希望所有的服务都重新连接上来并注册和订阅。
		 Delay到达时，全部通知一遍，以后正常工作。
		*/
		private int StartNotifyDelay = 12 * 1000;
		public int getStartNotifyDelay() {
			return StartNotifyDelay;
		}
		public void setStartNotifyDelay(int value) {
			StartNotifyDelay = value;
		}

		private int RetryNotifyDelayWhenNotAllReady = 30 * 1000;
		public int getRetryNotifyDelayWhenNotAllReady() {
			return RetryNotifyDelayWhenNotAllReady;
		}
		public void setRetryNotifyDelayWhenNotAllReady(int value) {
			RetryNotifyDelayWhenNotAllReady = value;
		}
		private String DbHome = ".";
		public String getDbHome() {
			return DbHome;
		}
		private void setDbHome(String value) {
			DbHome = value;
		}

		@Override
		public void Parse(Element self) {
			String attr = self.getAttribute("KeepAlivePeriod");
			if (!attr.isEmpty()) {
				setKeepAlivePeriod(Integer.parseInt(attr));
			}
			attr = self.getAttribute("StartNotifyDelay");
			if (!attr.isEmpty()) {
				setStartNotifyDelay(Integer.parseInt(attr));
			}
			attr = self.getAttribute("RetryNotifyDelayWhenNotAllReady");
			if (!attr.isEmpty()) {
				setRetryNotifyDelayWhenNotAllReady(Integer.parseInt(attr));
			}
			setDbHome(self.getAttribute("DbHome"));
			if (getDbHome().isEmpty()) {
				setDbHome(".");
			}
		}
	}

	/** 
	 需要从配置文件中读取，把这个引用加入： Zeze.Config.AddCustomize
	*/
	private final Conf Config;
	public Conf getConfig() {
		return Config;
	}

	public final static class ServerState {
		private final ServiceManagerServer ServiceManager;
		public ServiceManagerServer getServiceManager() {
			return ServiceManager;
		}
		private final String ServiceName;
		public String getServiceName() {
			return ServiceName;
		}

		// identity ->
		// 记录一下SessionId，方便以后找到服务所在的连接。
		private final ConcurrentHashMap<String, ServiceInfo> ServiceInfos = new ConcurrentHashMap<> ();
		public ConcurrentHashMap<String, ServiceInfo> getServiceInfos() {
			return ServiceInfos;
		}
		private final ConcurrentHashMap<Long, SubscribeState> Simple = new ConcurrentHashMap<> ();
		public ConcurrentHashMap<Long, SubscribeState> getSimple() {
			return Simple;
		}
		private final ConcurrentHashMap<Long, SubscribeState> ReadyCommit = new ConcurrentHashMap<> ();
		public ConcurrentHashMap<Long, SubscribeState> getReadyCommit() {
			return ReadyCommit;
		}

		private Zeze.Util.Task NotifyTimeoutTask;
		private long SerialId;

		public ServerState(ServiceManagerServer sm, String serviceName) {
			ServiceManager = sm;
			ServiceName = serviceName;
		}

		public void Close() {
			if (NotifyTimeoutTask != null) {
				NotifyTimeoutTask.Cancel();
			}
			NotifyTimeoutTask = null;
		}

		public void StartNotify() {
			synchronized (this) {
				if (null != getServiceManager().StartNotifyDelayTask) {
					return;
				}
				var notify = new NotifyServiceList();
				notify.Argument = new ServiceInfos(getServiceName(), this, ++SerialId);
				logger.debug("StartNotify {}", notify.Argument);
				var notifyBytes = notify.Encode();

				for (var e : getSimple().entrySet()) {
					var s = getServiceManager().getServer().GetSocket(e.getKey());
					if (null != s)
						s.Send(notifyBytes);
				}
				for (var e : getReadyCommit().entrySet()) {
					e.getValue().setReady(false);
					var s = getServiceManager().getServer().GetSocket(e.getKey());
					if (null != s)
						s.Send(notifyBytes);
				}
				if (!getReadyCommit().isEmpty()) {
					// 只有两段公告模式需要回应处理。
					NotifyTimeoutTask = Zeze.Util.Task.schedule(
							(ThisTask) -> {
								if (NotifyTimeoutTask == ThisTask) {
									// NotifyTimeoutTask 会在下面两种情况下被修改：
									// 1. 在 Notify.ReadyCommit 完成以后会被清空。
									// 2. 启动了新的 Notify。
									StartNotify(); // restart
								}
							},
							getServiceManager().getConfig().getRetryNotifyDelayWhenNotAllReady());
				}
			}
		}

		public void TryCommit() {
			synchronized (this) {
				if (NotifyTimeoutTask == null) {
					return; // no pending notify
				}

				for (var e : getReadyCommit().entrySet()) {
					if (!e.getValue().Ready) {
						return;
					}
				}
				var commit = new CommitServiceList();
				commit.Argument = new ServiceInfos(getServiceName(), this, 0);
				for (var e : getReadyCommit().entrySet()) {
					if (getServiceManager().getServer().GetSocket(e.getKey()) != null) {
						getServiceManager().getServer().GetSocket(e.getKey()).Send(commit);
					}
				}
				if (NotifyTimeoutTask != null) {
					NotifyTimeoutTask.Cancel();
				}
				NotifyTimeoutTask = null;
			}
		}

		/** 
		 订阅时候返回的ServiceInfos，必须和Notify流程互斥。
		 原子的得到当前信息并发送，然后加入订阅(simple or readycommit)。
		*/
		public long SubscribeAndSend(Subscribe r, Session session) {
			synchronized (this) {
				// 外面会话的 TryAdd 加入成功，下面TryAdd肯定也成功。
				switch (r.Argument.getSubscribeType()) {
					case SubscribeInfo.SubscribeTypeSimple:
						getSimple().putIfAbsent(session.getSessionId(), new SubscribeState(session.getSessionId()));
						break;
					case SubscribeInfo.SubscribeTypeReadyCommit:
						getReadyCommit().putIfAbsent(session.getSessionId(), new SubscribeState(session.getSessionId()));
						break;
					default:
						r.setResultCode(Subscribe.UnknownSubscribeType);
						r.SendResult();
						return Procedure.LogicError;
				}
				r.SendResultCode(Subscribe.Success);
				if (null == getServiceManager().StartNotifyDelayTask) {
					var arg = new ServiceInfos(getServiceName(), this, ++SerialId);
					SubscribeFirstCommit tempVar = new SubscribeFirstCommit();
					tempVar.Argument = arg;
					tempVar.Send(r.getSender());
				}
				return Procedure.Success;
			}
		}

		<T> boolean SequenceEqual(List<T> a, List<T> b) {
			if (a.size() != b.size())
				return false;
			int size = a.size();
			for (int i = 0; i < size; ++i) {
				if (!a.get(i).equals(b.get(i)))
					return false;
			}
			return true;
		}

		public void SetReady(ReadyServiceList p, Session session) {
			synchronized (this) {
				if (p.Argument.getSerialId() != SerialId) {
					logger.debug("Skip Ready: SerialId Not Equal.");
					return;
				}
				var ordered = new ServiceInfos(getServiceName(), this, 0);

				// 忽略旧的Ready。
				if (!SequenceEqual(ordered.getServiceInfoListSortedByIdentity(), p.Argument.getServiceInfoListSortedByIdentity())) {
					String sb = "SequenceNotEqual:" + " Current=" + ordered + " Ready=" + p.Argument;
					logger.debug(sb);
					return;
				}

				var subcribeState = getReadyCommit().get(session.getSessionId());
				if (null == subcribeState) {
					return;
				}
				subcribeState.Ready = true;
				TryCommit();
			}
		}
	}

	public final static class SubscribeState {
		private final long SessionId;
		public long getSessionId() {
			return SessionId;
		}
		private boolean Ready;
		public void setReady(boolean value) {
			Ready = value;
		}
		public SubscribeState(long ssid) {
			SessionId = ssid;
		}
	}

	public final static class Session {
		private final ServiceManagerServer ServiceManager;
		public ServiceManagerServer getServiceManager() {
			return ServiceManager;
		}
		private final long SessionId;
		public long getSessionId() {
			return SessionId;
		}
		private final ConcurrentHashMap<ServiceInfo, ServiceInfo> Registers = new ConcurrentHashMap<> ();
		public ConcurrentHashMap<ServiceInfo, ServiceInfo> getRegisters() {
			return Registers;
		}
		// key is ServiceName: 会话订阅
		private final ConcurrentHashMap<String, SubscribeInfo> Subscribes = new ConcurrentHashMap<> ();
		public ConcurrentHashMap<String, SubscribeInfo> getSubscribes() {
			return Subscribes;
		}
		private Zeze.Util.Task KeepAliveTimerTask;

		public Session(ServiceManagerServer sm, long ssid) {
			ServiceManager = sm;
			SessionId = ssid;
			KeepAliveTimerTask = Zeze.Util.Task.schedule(
					(ThisTask) -> {
						AsyncSocket s = null;
						try {
							s = getServiceManager().getServer().GetSocket(getSessionId());
							var r = new Keepalive();
							r.SendAndWaitCheckResultCode(s);
						}
						catch (Throwable ex) {
							if (s != null) {
								s.Close(null);
							}
							logger.error("ServiceManager.KeepAlive", ex);
						}
					},
					Zeze.Util.Random.getInstance().nextInt(getServiceManager().getConfig().getKeepAlivePeriod()),
					getServiceManager().getConfig().getKeepAlivePeriod());
		}

		public void OnClose() {
			if (KeepAliveTimerTask != null) {
				KeepAliveTimerTask.Cancel();
			}
			KeepAliveTimerTask = null;

			for (var info : getSubscribes().values()) {
				getServiceManager().UnSubscribeNow(getSessionId(), info);
			}

			HashMap<String, ServerState> changed = new HashMap<>(getRegisters().size());

			for (var info : getRegisters().values()) {
				var state = getServiceManager().UnRegisterNow(getSessionId(), info);
				if (null != state) {
					changed.putIfAbsent(state.getServiceName(), state);
				}
			}

			for (var state : changed.values()) {
				state.StartNotify();
			}
		}
	}

	private static final Logger logger = LogManager.getLogger(ServiceManagerServer.class);

	private long ProcessRegister(Protocol p) {
		var r = (Register)p;
		var session = (Session)r.getSender().getUserState();

		if (null != session.getRegisters().putIfAbsent(r.Argument, r.Argument)) {
			// 允许重复登录，断线重连Agent不好原子实现重发。
			// r.SendResultCode(Register.DuplicateRegister);
			r.SendResultCode(Register.Success);
			return Procedure.Success;
		}
		var state = ServerStates.computeIfAbsent(r.Argument.getServiceName(),
				(name) -> new ServerState(this, name));

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
		state.StartNotify();
		return Procedure.Success;
	}

	public ServerState UnRegisterNow(long sessionId, ServiceInfo info) {
		var state = ServerStates.get(info.getServiceName());
		if (null != state) {
			var exist = state.ServiceInfos.get(info.getServiceIdentity());
			if (null != exist) {
				// 这里存在一个时间窗口，可能使得重复的注销会成功。注销一般比较特殊，忽略这个问题。
				Long existSessionId = (Long)exist.getLocalState();
				if (existSessionId == null || sessionId == existSessionId) {
					// 有可能当前连接没有注销，新的注册已经AddOrUpdate，此时忽略当前连接的注销。
					state.ServiceInfos.remove(info.getServiceIdentity());
					return state;
				}
			}
		}
		return null;
	}

	private long ProcessUnRegister(Protocol p) {
		var r = (UnRegister)p;
		var session = (Session)r.getSender().getUserState();
		if (null != UnRegisterNow(r.getSender().getSessionId(), r.Argument)) {
			// ignore TryRemove failed.
			session.getRegisters().remove(r.Argument);
			//r.SendResultCode(UnRegister.Success);
			//return Procedure.Success;
		}
		// 注销不存在也返回成功，否则Agent处理比较麻烦。
		r.SendResultCode(UnRegister.Success);
		return Procedure.Success;
	}

	private long ProcessSubscribe(Protocol p) {
		var r = (Subscribe)p;
		var session = (Session)r.getSender().getUserState();
		if (null == session.getSubscribes().putIfAbsent(r.Argument.getServiceName(), r.Argument)) {
			r.setResultCode(Subscribe.DuplicateSubscribe);
			r.SendResult();
			return Procedure.LogicError;
		}
		var state = ServerStates.computeIfAbsent(r.Argument.getServiceName(),
				(name) -> new ServerState(this, name));
		return state.SubscribeAndSend(r, session);
	}

	public ServerState UnSubscribeNow(long sessionId, SubscribeInfo info) {
		var state = ServerStates.get(info.getServiceName());
		if (null != state) {
			switch (info.getSubscribeType()) {
				case SubscribeInfo.SubscribeTypeSimple:
					if (state.Simple.remove(sessionId) != null) {
						return state;
					}
					break;
				case SubscribeInfo.SubscribeTypeReadyCommit:
					if (state.ReadyCommit.remove(sessionId) != null) {
						return state;
					}
					break;
			}
		}
		return null;
	}

	private long ProcessUnSubscribe(Protocol p) {
		var r = (UnSubscribe)p;
		var session = (Session)r.getSender().getUserState();
		var sub = session.getSubscribes().remove(r.Argument.getServiceName());
		if (null != sub) {
			if (r.Argument.getSubscribeType() == sub.getSubscribeType()) {
				var changed = UnSubscribeNow(r.getSender().getSessionId(), r.Argument);
				if (null != changed) {
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

	private long ProcessReadyServiceList(Protocol p) {
		var r = (ReadyServiceList)p;
		var session = (Session)r.getSender().getUserState();
		var state = ServerStates.computeIfAbsent(
				r.Argument.getServiceName(), (name) -> new ServerState(this, name));
		state.SetReady(r, session);
		return Procedure.Success;
	}

	public void close() throws IOException {
		Stop();
	}


	public ServiceManagerServer(InetAddress ipaddress, int port, Zeze.Config config) {
		this(ipaddress, port, config, -1);
	}

	public ServiceManagerServer(InetAddress ipaddress, int port, Zeze.Config config, int startNotifyDelay) {
		Config = config.GetCustomize(new Conf());

		if (startNotifyDelay >= 0) {
			getConfig().setStartNotifyDelay(startNotifyDelay);
		}

		setServer(new NetServer(this, config));

		getServer().AddFactoryHandle((new Register()).getTypeId(),
				new Service.ProtocolFactoryHandle(Register::new, this::ProcessRegister));

		getServer().AddFactoryHandle((new UnRegister()).getTypeId(),
				new Service.ProtocolFactoryHandle(UnRegister::new, this::ProcessUnRegister));

		getServer().AddFactoryHandle((new Subscribe()).getTypeId(),
				new Service.ProtocolFactoryHandle(Subscribe::new, this::ProcessSubscribe));

		getServer().AddFactoryHandle((new UnSubscribe()).getTypeId(),
				new Service.ProtocolFactoryHandle(UnSubscribe::new, this::ProcessUnSubscribe));

		getServer().AddFactoryHandle((new ReadyServiceList()).getTypeId(),
				new Service.ProtocolFactoryHandle(ReadyServiceList::new, this::ProcessReadyServiceList));

		getServer().AddFactoryHandle((new Keepalive()).getTypeId(),
				new Service.ProtocolFactoryHandle(Keepalive::new));

		getServer().AddFactoryHandle((new AllocateId()).getTypeId(),
				new Service.ProtocolFactoryHandle(AllocateId::new, this::ProcessAllocateId));

		if (getConfig().getStartNotifyDelay() > 0) {
			StartNotifyDelayTask = Zeze.Util.Task.schedule(this::StartNotifyAll, getConfig().getStartNotifyDelay());
		}

		var options = (new org.rocksdb.Options()).setCreateIfMissing(true);
		try {
			AutoKeysDb = org.rocksdb.RocksDB.open(options, Paths.get(getConfig().getDbHome()).resolve("autokeys").toString());
			WriteOptions = new org.rocksdb.WriteOptions().setSync(true);
		} catch (RocksDBException e) {
			throw new RuntimeException(e);
		}

		// 允许配置多个acceptor，如果有冲突，通过日志查看。
		ServerSocket = getServer().NewServerSocket(ipaddress, port, null);
		getServer().Start();
		// try
		Server.NewServerSocket("127.0.0.1", port, null);
		Server.NewServerSocket("::1", port, null);
	}

	private final org.rocksdb.RocksDB AutoKeysDb;
	private final org.rocksdb.WriteOptions WriteOptions;
	private final ConcurrentHashMap<String, AutoKey> AutoKeys = new ConcurrentHashMap<> ();
	private ConcurrentHashMap<String, AutoKey> getAutoKeys() {
		return AutoKeys;
	}

	public final static class AutoKey {
		private final ServiceManagerServer SMS;

		private final String Name;
		public String getName() {
			return Name;
		}
		private final byte[] Key;
		private byte[] getKey() {
			return Key;
		}
		private long Current;
		private long getCurrent() {
			return Current;
		}
		private void setCurrent(long value) {
			Current = value;
		}

		public AutoKey(String name, ServiceManagerServer sms) {
			Name = name;
			SMS = sms;
			{
				var bb = ByteBuffer.Allocate();
				bb.WriteString(getName());
				Key = bb.Copy();
			}
			byte[] value;
			try {
				value = SMS.AutoKeysDb.get(getKey());
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
			if (null != value) {
				var bb = ByteBuffer.Wrap(value);
				setCurrent(bb.ReadLong());
			}
		}

		public void Allocate(AllocateId rpc) {
			synchronized (this) {
				rpc.Result.setStartId(getCurrent());

				var count = rpc.Argument.getCount();

				// 随便修正一下分配数量。
				if (count < 256) {
					count = 256;
				}
				else if (count > 10000) {
					count = 10000;
				}

				setCurrent(getCurrent() + count);
				var bb = ByteBuffer.Allocate();
				bb.WriteLong(getCurrent());
				try {
					SMS.AutoKeysDb.put(SMS.WriteOptions,
							java.nio.ByteBuffer.wrap(getKey(), 0, getKey().length),
							java.nio.ByteBuffer.wrap(bb.Bytes, bb.ReadIndex, bb.Size()));
				} catch (RocksDBException e) {
					throw new RuntimeException(e);
				}

				rpc.Result.setCount(count);
			}
		}
	}

	private long ProcessAllocateId(Protocol p) {
		var r = (AllocateId)p;
		var n = r.Argument.getName();
		r.Result.setName(n);
		getAutoKeys().computeIfAbsent(n, (key) -> new AutoKey(n, this)).Allocate(r);
		r.SendResult();
		return 0;
	}

	private void StartNotifyAll(Zeze.Util.Task ThisTask) {
		StartNotifyDelayTask = null;
		for (var e : ServerStates.entrySet()) {
			e.getValue().StartNotify();
		}
	}

	public void Stop() {
		synchronized (this) {
			if (null == getServer()) {
				return;
			}
			if (StartNotifyDelayTask != null) {
				StartNotifyDelayTask.Cancel();
			}
			ServerSocket.Close(null);
			ServerSocket = null;
			getServer().Stop();
			setServer(null);

			for (var ss : ServerStates.values()) {
				ss.Close();
			}
			if (AutoKeysDb != null) {
				AutoKeysDb.close();
			}
		}
	}

	public final static class NetServer extends HandshakeServer {
		private final ServiceManagerServer ServiceManager;
		public ServiceManagerServer getServiceManager() {
			return ServiceManager;
		}

		public NetServer(ServiceManagerServer sm, Zeze.Config config) {
			super("Zeze.Services.ServiceManager", config);
			ServiceManager = sm;
		}

		@Override
		public void OnSocketAccept(AsyncSocket so) {
			so.setUserState(new Session(getServiceManager(), so.getSessionId()));
			super.OnSocketAccept(so);
		}

		@Override
		public void OnSocketClose(AsyncSocket so, Throwable e) {
			var session = (Session)so.getUserState();
			if (session != null) {
				session.OnClose();
			}
			super.OnSocketClose(so, e);
		}
	}

	public static void main(String[] args) throws Exception {
		String ip = null;
		int port = 5001;

		for (int i = 0; i < args.length; ++i) {
			switch (args[i])
			{
				case "-ip": ip = args[++i]; break;
				case "-port": port = Integer.parseInt(args[++i]); break;

			}
		}

		InetAddress address = (null == ip || ip.isEmpty()) ?
				new InetSocketAddress(0).getAddress() : InetAddress.getByName(ip);

		var config = new Zeze.Config();
		var smconfig = new Zeze.Services.ServiceManagerServer.Conf();
		config.AddCustomize(smconfig);
		config.LoadAndParse();

		try (var sm = new Zeze.Services.ServiceManagerServer(address, port, config)) {
			while (true) {
				Thread.sleep(10000);
			}
		}
	}
}
