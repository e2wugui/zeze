package Zeze.Services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Future;
import Zeze.Builtin.ServiceManagerWithRaft.*;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolDispatch;
import Zeze.Net.ProtocolHandle;
import Zeze.Raft.IRaftRpc;
import Zeze.Raft.Raft;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.Procedure;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Raft.RocksRaft.Table;
import Zeze.Raft.RocksRaft.Transaction;
import Zeze.Raft.Server;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BServiceInfosVersion;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Transaction.DispatchMode;
import Zeze.Util.Action0;
import Zeze.Util.FuncLong;
import Zeze.Util.Random;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import Zeze.Util.TaskSpec;
import Zeze.Util.ZezeCounter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.jetbrains.annotations.NotNull;

public final class ServiceManagerWithRaft extends AbstractServiceManagerWithRaft implements AutoCloseable {
	static {
		var level = Level.toLevel(System.getProperty("logLevel"), Level.INFO);
		((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger().setLevel(level);
	}

	private static final @NotNull Logger logger = LogManager.getLogger(ServiceManagerWithRaft.class);
	private final @NotNull Rocks rocks;
	private final @NotNull Table<String, BAutoKey> tableAutoKey;
	private final @NotNull Table<String, BId128> tableId128;
	private final @NotNull Table<String, BSession> tableSession;
	private final @NotNull Table<String, BLoadObservers> tableLoadObservers;
	private final @NotNull Table<String, BServerState> tableServerState;

	// 需要从配置文件中读取，把这个引用加入：Zeze.Config.AddCustomize
	private final ServiceManagerServer.Conf conf = new ServiceManagerServer.Conf();

	public ServiceManagerWithRaft(String raftName, RaftConfig raftConf) throws Exception {
		this(raftName, raftConf, Config.load(), false);
	}

	public ServiceManagerWithRaft(String raftName, RaftConfig raftConf, Config config,
								  boolean RocksDbWriteOptionSync) throws Exception {
		ZezeCounter.tryInit();

		if (config == null)
			config = Config.load();
		config.parseCustomize(conf);

		rocks = new Rocks(raftName, RocksMode.Pessimism, raftConf, config, RocksDbWriteOptionSync,
				SMServer::new, new TaskOneByOneByKey());

		RegisterRocksTables(rocks);
		RegisterProtocols(rocks.getRaft().getServer());
		rocks.getRaft().getServer().start();

		tableAutoKey = rocks.<String, BAutoKey>getTableTemplate("tAutoKey").openTable();
		tableId128 = rocks.<String, BId128>getTableTemplate("tId128").openTable();
		tableSession = rocks.<String, BSession>getTableTemplate("tSession").openTable();
		tableLoadObservers = rocks.<String, BLoadObservers>getTableTemplate("tLoadObservers").openTable();
		tableServerState = rocks.<String, BServerState>getTableTemplate("tServerState").openTable();
	}

	@Override
	public void close() {
		rocks.close();
	}

	/**
	 * 所有Raft网络层收到的请求和Rpc的结果，全部加锁，直接运行。
	 * 这样整个程序就单线程化了。
	 */
	public class SMServer extends Server {
		public SMServer(Raft raft, String name, Config config) {
			super(raft, name, config);
		}

		@Override
		public <P extends Protocol<?>> void dispatchRaftRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
																	ProtocolFactoryHandle<?> factoryHandle) {
			lock();
			try {
				if (logger.isDebugEnabled())
					logger.debug("dispatchRaftRpcResponse: {}{}", rpc.getClass().getName(), rpc);
				var procedure = rocks.newProcedure(() -> responseHandle.handle(rpc));
				ProtocolDispatch.ofFunc(procedure::call, rpc).call();
			} finally {
				unlock();
			}
		}

		@Override
		public void dispatchRaftRequest(Protocol<?> p, FuncLong func, String name, Action0 cancel,
										DispatchMode mode) {
			// 不能在调用线程（Selector IO 线程）上内联执行：raft 提交的 appendLog 等待期间
			// 该 IO 线程被冻结，心跳与 AppendEntries 应答处理停摆，负载/抖动下引发选主动摇；
			// 且 TaskCompletionSource.get 对 Selector 线程有防御断言（测试 JVM 默认 -ea），
			// 等待即 AssertionError 被包装成 RaftRetry(-15)。按传入的 mode 派发到线程池执行
			// （对齐 Raft.Server 基类实现），SM 锁移入任务内，保持单写者串行语义不变。
			TaskSpec.ofFunc(() -> {
				lock();
				try {
					if (logger.isDebugEnabled()) {
						var netSession = (Session)p.getSender().getUserState();
						var ssName = null != netSession ? netSession.name : "";
						logger.debug("dispatchRaftRequest: {}@{}{}", p.getClass().getName(), ssName, p);
					}
					var procedure = new Procedure(rocks, func);
					return ProtocolDispatch.ofFunc(procedure::call, p).onError(Protocol::SendResultCode).call();
				} finally {
					unlock();
				}
			}).name(name).onCancel(cancel).dispatchMode(mode)
					.executeOneByOne(((IRaftRpc)p).getUnique(), taskOneByOne);
		}

		@Override
		public void OnSocketClose(@NotNull AsyncSocket so, Throwable e) throws Exception {
			var netSession = (Session)so.getUserState();
			if (null != netSession) {
				if (logger.isDebugEnabled())
					logger.info("OnSocketClose: {}", netSession.name);
				// 同 dispatchRaftRequest：netSession.onClose 的 raft 提交不能在 IO 线程上等待。
				Raft.executeImportantTask(() -> {
					lock();
					try {
						var procedure = rocks.newProcedure(() -> {
							netSession.onClose();
							return 0;
						});
						procedure.call();
					} catch (Throwable ex) {
						logger.error("OnSocketClose session close failed: {}", netSession.name, ex);
					} finally {
						unlock();
					}
				});
			}
			super.OnSocketClose(so, e);
		}
	}

	/*
	private static BSubscribeInfo fromRocks(BSubscribeInfoRocks rocks) {
		return new BSubscribeInfo(rocks.getServiceName(), rocks.getVersion());
	}
	*/

	public class Session {
		private final String name;
		private final long sessionId;
		private final Future<?> keepAliveTimerTask;
		// Identify上报的serverId；-1=未上报。断线时据此广播Suspect。
		// 仅内存：换leader后agent重新Login+Identify（raftOnSetLeader），无需raft持久化。
		private volatile int identifyServerId = -1;

		public Session(String name, long sessionId) {
			this.name = name;
			this.sessionId = sessionId;

			if (conf.keepAlivePeriod > 0) {
				keepAliveTimerTask = TaskSpec.ofAction(() -> {
					AsyncSocket s = null;
					try {
						s = rocks.getRaft().getServer().GetSocket(sessionId);
						var r = new KeepAlive();
						r.SendAndWaitCheckResultCode(s);
					} catch (Throwable ex) { // logger.error
						if (s != null)
							s.close(ex);
						else
							logger.error("ServiceManager.KeepAlive", ex);
					}
				}).schedulePeriodNow(
						Random.getInstance().nextInt(conf.keepAlivePeriod),
						conf.keepAlivePeriod);
			} else
				keepAliveTimerTask = null;
		}

		public void onClose() {
			if (keepAliveTimerTask != null)
				keepAliveTimerTask.cancel(false);

			// Suspect广播：立即、不延迟、不挑选目标（对齐非raft版）。仅是提示（hint），
			// 接收方转化为takeover.tryTransfer，由租约表裁决；未过期租约会被安排到过期时刻精确重试。
			// 短暂掉线误判不存在：接管前租约必须过期，死者重启会claim新epoch。
			var suspectServerId = identifyServerId;
			if (suspectServerId >= 0) {
				try {
					rocks.getRaft().getServer().foreach(so -> {
						if (so.getSessionId() == sessionId)
							return; // 刚断线的会话本身不报信
						var netSession = (Session)so.getUserState();
						if (netSession == null)
							return; // raft节点间连接没有Login过
						var suspect = new Suspect();
						suspect.Argument.serverId = suspectServerId;
						so.Send(suspect);
					});
				} catch (Exception e) {
					logger.warn("Suspect broadcast for serverId={} failed", suspectServerId, e);
				}
			}

			var session = tableSession.get(name);
			// 关闭事件的检测可能晚于同名新连接的Login（keepalive超时等），此时行已被新连接接管，
			// 校验归属后跳过清理，否则新连接的注册/订阅被误清并删行，其后续请求NPE。
			if (null != session && session.getSessionId() != sessionId)
				return;
			if (null != session) {
				for (var info : session.getSubscribes().values())
					unSubscribeNow(name, info.getServiceName());

				// 注销跨全部版本桶（会话registers以name+id为key只保留最后一次注册，
				// 若按unReg.getVersion()单桶删，跨版本重注册后旧版本桶残留幽灵地址）。
				var notifies = new HashMap<AsyncSocket, Edit>();
				for (var unReg : session.getRegisters().values()) {
					var state = tableServerState.get(unReg.getServiceName());
					if (state != null)
						removeAndCollectNotifyAllVersions(state, unReg.getServiceIdentity(), name, notifies);
				}
				ServiceManagerWithRaft.sendNotifies(notifies);
			}
			tableSession.remove(name);
		}
	}

	@Override
	protected long ProcessLoginRequest(Login r) {
		var session = tableSession.getOrAdd(r.Argument.getSessionName());
		r.getSender().setUserState(new Session(r.Argument.getSessionName(), r.getSender().getSessionId()));
		session.setSessionId(r.getSender().getSessionId());
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessAllocateId128Request(AllocateId128 r) {
		// 随便写写! 这个实际上没用,因为id128需要通过udp,这里是tcp.
		if (r.Argument.getCount() < 1)
			return Zeze.Transaction.Procedure.ErrorRequestId;

		var id128 = tableId128.getOrAdd(r.Argument.getName());
		r.Result.setStartId(id128.getCurrent());
		var count = r.Argument.getCount();
		id128.setCurrent(id128.getCurrent().add(count)); // 不能直接修改当前值,因为没有受事务保护.
		r.Result.setCount(count);
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessAllocateIdRequest(AllocateId r) {
		if (r.Argument.getCount() < 1)
			return Zeze.Transaction.Procedure.ErrorRequestId;

		var autoKey = tableAutoKey.getOrAdd(r.Argument.getName());
		r.Result.setStartId(autoKey.getCurrent());
		// 随便修正一下分配数量。
		var count = r.Argument.getCount();
		long current = autoKey.getCurrent() + count;
		autoKey.setCurrent(current);
		r.Result.setCount(count);

		// 号段必须raft提交成功后再应答：appendLog失败（失主RaftRetry/复制异常）会回滚current，
		// 提交前应答会让客户端把已回滚的号段投入使用，下一次AllocateId重复发放同一号段。
		// 对齐GCM-raft的proc.autoResponse（响应由_final_commit_在appendLog之后发出）；
		// result已填的startId/count在回滚路径随错误码一起发送，客户端按resultCode!=0丢弃。
		// 非事务上下文（不应发生）保持立即应答。
		var t = Transaction.getCurrent();
		if (t != null)
			t.runWhileCommit(r::SendResult);
		else
			r.SendResult();
		return 0;
	}

	// 只写session上一个int（对齐非raft版：无状态簿记、无取消语义）。虽然走raft请求通道，
	// 但不写rocks表，不产生共识复制；换leader后agent重新Login时会重发Identify。
	@Override
	protected long ProcessIdentifyRequest(Identify r) {
		var netSession = (Session)r.getSender().getUserState();
		if (netSession != null) {
			netSession.identifyServerId = r.Argument.serverId;
			logger.info("{}: Identify serverId={}", r.getSender(), r.Argument.serverId);
		}
		r.SendResult();
		return 0;
	}

	private void addLoadObserver(String ip, int port, String sessionName) {
		if (!ip.isEmpty() && port != 0) {
			var loadObservers = tableLoadObservers.getOrAdd(ip + "_" + port);
			loadObservers.getObservers().add(sessionName);
		}
	}

	@Override
	protected long ProcessSetServerLoadRequest(SetServerLoad r) {
		var loadObservers = tableLoadObservers.getOrAdd(r.Argument.ip + "_" + r.Argument.port);
		var observers = loadObservers.getObservers();

		var set = new SetServerLoad();
		set.Argument = r.Argument;

		ArrayList<String> removed = null;
		for (var observer : observers) {
			try {
				var session = tableSession.get(observer);
				if (null != session && set.Send(rocks.getRaft().getServer().GetSocket(session.getSessionId())))
					continue;
			} catch (Throwable ignored) { // ignored
			}
			if (removed == null)
				removed = new ArrayList<>();
			removed.add(observer);
		}
		if (removed != null) {
			for (var remove : removed)
				observers.remove(remove);
		}
		r.SendResult();
		return 0;
	}

	private static BServiceInfoRocks toRocks(BServiceInfo serverInfo, String sessionName) {
		return new BServiceInfoRocks(serverInfo.getServiceName(), serverInfo.getServiceIdentity(),
				serverInfo.getPassiveIp(), serverInfo.getPassivePort(), serverInfo.getExtraInfo(),
				sessionName, serverInfo.getVersion());
	}

	private static BServiceInfoKeyRocks toRocksKey(BServiceInfo serverInfo) {
		return new BServiceInfoKeyRocks(serverInfo.getServiceName(), serverInfo.getServiceIdentity());
	}

	private static BSubscribeInfoRocks toRocks(BSubscribeInfo si) {
		return new BSubscribeInfoRocks(si.getServiceName(), si.getVersion());
	}

	private static void sendNotifies(HashMap<AsyncSocket, Edit> notifies) {
		// todo 增加一些发送错误的日志。
		for (var e : notifies.entrySet()) {
			e.getValue().Send(e.getKey());
		}
	}

	@Override
	protected long ProcessEditRequest(Edit r) {
		var netSession = (Session)r.getSender().getUserState();
		var notifies = new HashMap<AsyncSocket, Edit>();

		// step 1: remove
		for (var unReg : r.Argument.getRemove()) {
			var state = tableServerState.get(unReg.getServiceName());
			if (state != null)
				removeAndCollectNotifyAllVersions(state, unReg.getServiceIdentity(), netSession.name, notifies);
			var session = tableSession.get(netSession.name);
			session.getRegisters().remove(toRocksKey(unReg)); // ignore remove failed
		}

		// step 2: add
		for (var reg : r.Argument.getAdd()) {
			var session = tableSession.get(netSession.name);
			// 允许重复登录，断线重连Agent不好原子实现重发。
			session.getRegisters().put(toRocksKey(reg), toRocks(reg, netSession.name));
			var state = tableServerState.getOrAdd(reg.getServiceName());
			if (!state.getServiceName().equals(reg.getServiceName()))
				state.setServiceName(reg.getServiceName());
			addAndCollectNotify(state, reg, netSession.name, notifies);
		}

		sendNotifies(notifies);
		r.SendResult();
		return 0;
	}

	private void addAndCollectNotify(BServerState state, BServiceInfo info, String sessionName,
									 HashMap<AsyncSocket, Edit> notifies) {
		// BEditService.add声明AddOrUpdate以name+id为key：同identity重注册到新版本时，
		// 先从其他版本桶移除旧记录并通知其版本订阅者remove（对齐非raft版ServiceManagerServer），
		// 否则实例下线后旧版本桶残留幽灵地址（会话registers以name+id为key只保留最后一次注册）。
		for (var e : state.getServiceInfosVersion().entrySet()) {
			if (e.getKey() == info.getVersion())
				continue;
			// CollMap2.remove 返回 void：先取旧值再移除，有旧值才通知其版本订阅者。
			var old = e.getValue().getServiceInfos().get(info.getServiceIdentity());
			if (old != null) {
				e.getValue().getServiceInfos().remove(info.getServiceIdentity());
				collectNotify(state, fromRocks(old), false, notifies);
			}
		}
		var versions = state.getServiceInfosVersion().get(info.getVersion());
		if (null == versions)
			state.getServiceInfosVersion().put(info.getVersion(), versions = new BServiceInfosVersionRocks());
		// AddOrUpdate，否则重连重新注册很难恢复到正确的状态。
		versions.getServiceInfos().put(info.getServiceIdentity(), toRocks(info, sessionName));
		collectNotify(state, info, true, notifies);
	}

	// 通知订阅了info版本的会话（version==0订阅全部版本）。info的版本决定通知过滤。
	private void collectNotify(BServerState state, BServiceInfo info, boolean isAdd,
							   HashMap<AsyncSocket, Edit> notifies) {
		for (var e : state.getSimple().entrySet()) {
			var subVersion = e.getValue().getVersion();
			if (subVersion == 0 || subVersion == info.getVersion()) {
				var sessionName = e.getKey();
				var session = tableSession.get(sessionName);
				if (null == session)
					continue;
				var peer = rocks.getRaft().getServer().GetSocket(session.getSessionId());
				if (null == peer)
					continue;

				var notify = notifies.computeIfAbsent(peer, __ -> new Edit());
				if (isAdd)
					notify.Argument.getAdd().add(info);
				else
					notify.Argument.getRemove().add(info);
			}
		}
	}

	@Override
	protected long ProcessSubscribeRequest(Subscribe r) {
		logger.info("{}: Subscribe {}", r.getSender(), r.Argument);
		var netSession = (Session)r.getSender().getUserState();
		var session = tableSession.get(netSession.name);
		for (var info : r.Argument.subs) {
			session.getSubscribes().put(info.getServiceName(), toRocks(info));
			var state = tableServerState.getOrAdd(info.getServiceName());
			if (!state.getServiceName().equals(info.getServiceName()))
				state.setServiceName(info.getServiceName());
			subscribeAndCollect(state, r, info, netSession.name);
		}
		r.SendResult();
		return 0;
	}

	private static BServiceInfo fromRocks(BServiceInfoRocks rocks) {
		return new BServiceInfo(rocks.getServiceName(), rocks.getServiceIdentity(),
				rocks.getVersion(),
				rocks.getPassiveIp(), rocks.getPassivePort(), rocks.getExtraInfo());
	}

	public void removeAndCollectNotify(BServerState state, BServiceInfo info, HashMap<AsyncSocket, Edit> notifies) {
		collectNotify(state, info, false, notifies);
	}

	// 注销以name+id为key跨全部版本桶收敛（与addAndCollectNotify、非raft版onClose一致）：
	// 仅移除属于本会话的记录；归属不符时不删不通知（新会话的AddOrUpdate注册不被静默删除）。
	private void removeAndCollectNotifyAllVersions(BServerState state, String serviceIdentity, String sessionName,
												   HashMap<AsyncSocket, Edit> notifies) {
		for (var e : state.getServiceInfosVersion().entrySet()) {
			var exist = e.getValue().getServiceInfos().get(serviceIdentity);
			// 有可能当前连接没有注销，新的注册已经AddOrUpdate，此时忽略当前连接的注销。
			if (exist == null || !exist.getSessionName().equals(sessionName))
				continue;
			e.getValue().getServiceInfos().remove(serviceIdentity);
			removeAndCollectNotify(state, fromRocks(exist), notifies);
		}
	}

	@Override
	protected long ProcessUnSubscribeRequest(UnSubscribe r) {
		logger.info("{}: UnSubscribe {}", r.getSender(), r.Argument);
		var netSession = (Session)r.getSender().getUserState();
		var session = tableSession.get(netSession.name);
		for (var serviceName : r.Argument.serviceNames) {
			var sub = session.getSubscribes().get(serviceName);
			session.getSubscribes().remove(serviceName);
			if (sub != null) {
				unSubscribeNow(netSession.name, serviceName);
			}
		}
		r.SendResult();
		return 0;
	}

	public BServerState unSubscribeNow(String sessionName, String serviceName) {
		var state = tableServerState.get(serviceName);
		if (state != null) {
			var removed = state.getSimple().get(sessionName);
			state.getSimple().remove(sessionName);
			if (removed != null)
				return state;
		}
		return null;
	}

	private void subscribeAndCollect(BServerState state, Subscribe r, BSubscribeInfo subInfo, String ssName) {
		// 外面会话的 TryAdd 加入成功，下面TryAdd肯定也成功。
		state.getSimple().put(ssName, toRocks(subInfo));
		r.Result.map.put(state.getServiceName(), new BServiceInfosVersion(subInfo.getVersion(), state));

		var netSession = (Session)r.getSender().getUserState();
		for (var versions : state.getServiceInfosVersion().values())
			for (var info : versions.getServiceInfos().values())
				addLoadObserver(info.getPassiveIp(), info.getPassivePort(), netSession.name);
	}
}
