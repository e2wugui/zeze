package Zeze.Services;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Net.RpcTimeoutException;
import Zeze.Net.Service;
import Zeze.Raft.RaftConfig;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.GlobalCacheManager.Acquire;
import Zeze.Services.GlobalCacheManager.Cleanup;
import Zeze.Services.GlobalCacheManager.GlobalKeyState;
import Zeze.Services.GlobalCacheManager.KeepAlive;
import Zeze.Services.GlobalCacheManager.Login;
import Zeze.Services.GlobalCacheManager.NormalClose;
import Zeze.Services.GlobalCacheManager.ReLogin;
import Zeze.Services.GlobalCacheManager.Reduce;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.IdentityHashSet;
import Zeze.Util.KV;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.OutInt;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.w3c.dom.Element;

public final class GlobalCacheManagerServer implements GlobalCacheManagerConst {
	static {
		System.setProperty("log4j.configurationFile", "log4j2.xml");
		var level = Level.toLevel(System.getProperty("logLevel"), Level.INFO);
		((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger().setLevel(level);
	}

	private static final boolean ENABLE_PERF = true;
	private static final Logger logger = LogManager.getLogger(GlobalCacheManagerServer.class);
	private static final boolean isDebugEnabled = logger.isDebugEnabled();
	private static final GlobalCacheManagerServer Instance = new GlobalCacheManagerServer();

	public static GlobalCacheManagerServer getInstance() {
		return Instance;
	}

	private ServerService Server;
	private AsyncSocket ServerSocket;
	private ConcurrentHashMap<Binary, CacheState> global;
	private final AtomicLong SerialIdGenerator = new AtomicLong();
	/*
	 * 会话。
	 * key是 LogicServer.Id，现在的实现就是Zeze.Config.ServerId。
	 * 在连接建立后收到的Login Or ReLogin 中设置。
	 * 每个会话记住分配给自己的GlobalTableKey，用来在正常退出的时候释放。
	 * 每个会话还需要记录该会话的Socket.SessionId。在连接重新建立时更新。
	 * 总是GetOrAdd，不删除。按现在的cache-sync设计，
	 * ServerId 是及其有限的。不会一直增长。
	 * 简化实现。
	 */
	private LongConcurrentHashMap<CacheHolder> Sessions;
	private final GCMConfig Config = new GCMConfig();
	private AchillesHeelConfig AchillesHeelConfig;
	private GlobalCacheManagerPerf perf;

	public static final class GCMConfig implements Zeze.Config.ICustomize {
		// 设置了这么大，开始使用后，大概会占用700M的内存，作为全局服务器，先这么大吧。
		// 尽量不重新调整ConcurrentHashMap。
		int InitialCapacity = 10_000_000;

		int MaxNetPing = 1_500;
		int ServerProcessTime = 1_000;
		int ServerReleaseTimeout = 10_000;

		@Override
		public String getName() {
			return "GlobalCacheManager";
		}

		@Override
		public void Parse(Element self) {
			var attr = self.getAttribute("InitialCapacity");
			if (!attr.isBlank())
				InitialCapacity = Math.max(Integer.parseInt(attr), 31);

			attr = self.getAttribute("MaxNetPing");
			if (!attr.isEmpty())
				MaxNetPing = Integer.parseInt(attr);
			attr = self.getAttribute("ServerProcessTime");
			if (!attr.isEmpty())
				ServerProcessTime = Integer.parseInt(attr);
			attr = self.getAttribute("ServerReleaseTimeout");
			if (!attr.isEmpty())
				ServerReleaseTimeout = Integer.parseInt(attr);
		}
	}

	// 外面主动提供装载配置，需要在Load之前把这个实例注册进去。
	public GlobalCacheManagerServer.GCMConfig getConfig() {
		return Config;
	}

	private GlobalCacheManagerServer() {
	}

	public void Start(InetAddress ipaddress, int port) throws Throwable {
		Start(ipaddress, port, null);
	}

	public synchronized void Start(InetAddress ipaddress, int port, Zeze.Config config) throws Throwable {
		if (Server != null)
			return;

		if (ENABLE_PERF)
			perf = new GlobalCacheManagerPerf("", SerialIdGenerator);

		if (config == null)
			config = new Zeze.Config().AddCustomize(Config).LoadAndParse();

		Sessions = new LongConcurrentHashMap<>(4096);
		global = new ConcurrentHashMap<>(Config.InitialCapacity);

		Server = new ServerService(config);

		Server.AddFactoryHandle(Acquire.TypeId_,
				new Service.ProtocolFactoryHandle<>(Acquire::new, this::ProcessAcquireRequest, TransactionLevel.None));

		Server.AddFactoryHandle(Reduce.TypeId_,
				new Service.ProtocolFactoryHandle<>(Reduce::new));

		Server.AddFactoryHandle(Login.TypeId_,
				new Service.ProtocolFactoryHandle<>(Login::new, this::ProcessLogin, TransactionLevel.None));

		Server.AddFactoryHandle(ReLogin.TypeId_,
				new Service.ProtocolFactoryHandle<>(ReLogin::new, this::ProcessReLogin, TransactionLevel.None));

		Server.AddFactoryHandle(NormalClose.TypeId_,
				new Service.ProtocolFactoryHandle<>(NormalClose::new, this::ProcessNormalClose, TransactionLevel.None));

		// 临时注册到这里，安全起见应该起一个新的Service，并且仅绑定到 localhost。
		Server.AddFactoryHandle(Cleanup.TypeId_,
				new Service.ProtocolFactoryHandle<>(Cleanup::new, this::ProcessCleanup, TransactionLevel.None));

		Server.AddFactoryHandle(KeepAlive.TypeId_,
				new Service.ProtocolFactoryHandle<>(KeepAlive::new, this::ProcessKeepAliveRequest, TransactionLevel.None));

		ServerSocket = Server.NewServerSocket(ipaddress, port, null);

		// Global的守护不需要独立线程。当出现异常问题不能工作时，没有释放锁是不会造成致命问题的。
		AchillesHeelConfig = new AchillesHeelConfig(Config.MaxNetPing, Config.ServerProcessTime, Config.ServerReleaseTimeout);
		Task.schedule(5000, 5000, this::AchillesHeelDaemon);
	}

	private void AchillesHeelDaemon() {
		var now = System.currentTimeMillis();

		Sessions.forEach(session -> {
			if (now - session.getActiveTime() > AchillesHeelConfig.GlobalDaemonTimeout) {
				//noinspection SynchronizationOnLocalVariableOrMethodParameter
				synchronized (session) {
					session.kick();
					if (!session.Acquired.isEmpty()) {
						logger.info("AchillesHeelDaemon.Release begin {}", session);
						for (var k : session.Acquired.keySet()) {
							// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
							try {
								Release(session, k, false);
							} catch (InterruptedException ex) {
								logger.error("", ex);
							}
						}
						session.setActiveTime(System.currentTimeMillis());
						logger.info("AchillesHeelDaemon.Release end {}", session);
					}
				}
			}
		});
	}

	public synchronized void Stop() throws Throwable {
		if (Server == null)
			return;
		ServerSocket.close();
		ServerSocket = null;
		Server.Stop();
		Server = null;
	}

	/**
	 * 报告错误的时候带上相关信息（包括GlobalCacheManager和LogicServer等等）
	 * 手动Cleanup时，连接正确的服务器执行。
	 */
	private long ProcessCleanup(Cleanup rpc) {
		logger.info("ProcessCleanup: {} RequestId={} {}", rpc.getSender(), rpc.getSessionId(), rpc.Argument);
		if (AchillesHeelConfig != null) // disable cleanup.
			return 0;

		// 安全性以后加强。
		if (!rpc.Argument.SecureKey.equals("Ok! verify secure.")) {
			logger.warn("ProcessCleanup: {} RequestId={} result={}", rpc.getSender(), rpc.getSessionId(), CleanupErrorSecureKey);
			rpc.SendResultCode(CleanupErrorSecureKey);
			return 0;
		}

		var session = Sessions.computeIfAbsent(rpc.Argument.ServerId, __ -> new CacheHolder());
		if (session.GlobalCacheManagerHashIndex != rpc.Argument.GlobalCacheManagerHashIndex) {
			// 多点验证
			logger.warn("ProcessCleanup: {} RequestId={} result={}", rpc.getSender(), rpc.getSessionId(), CleanupErrorGlobalCacheManagerHashIndex);
			rpc.SendResultCode(CleanupErrorGlobalCacheManagerHashIndex);
			return 0;
		}

		if (Server.GetSocket(session.SessionId) != null) {
			// 连接存在，禁止cleanup。
			logger.warn("ProcessCleanup: {} RequestId={} result={}", rpc.getSender(), rpc.getSessionId(), CleanupErrorHasConnection);
			rpc.SendResultCode(CleanupErrorHasConnection);
			return 0;
		}

		// 还有更多的防止出错的手段吗？

		// XXX verify danger
		Task.schedule(5 * 60 * 1000, () -> { // delay 5 mins
			for (var k : session.Acquired.keySet()) {
				// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
				Release(session, k, false);
			}
			rpc.SendResultCode(0);
		});

		return 0;
	}

	private long ProcessLogin(Login rpc) throws Throwable {
		logger.info("ProcessLogin: {} RequestId={} {}", rpc.getSender(), rpc.getSessionId(), rpc.Argument);
		var session = Sessions.computeIfAbsent(rpc.Argument.ServerId, __ -> new CacheHolder());
		if (!session.TryBindSocket(rpc.getSender(), rpc.Argument.GlobalCacheManagerHashIndex, true)) {
			rpc.SendResultCode(LoginBindSocketFail);
			return 0;
		}
		// new login, 比如逻辑服务器重启。release old acquired.
		for (var k : session.Acquired.keySet()) {
			// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
			Release(session, k, false);
		}
		session.setActiveTime(System.currentTimeMillis());
		rpc.Result.MaxNetPing = Config.MaxNetPing;
		rpc.Result.ServerProcessTime = Config.ServerProcessTime;
		rpc.Result.ServerReleaseTimeout = Config.ServerReleaseTimeout;
		rpc.SendResultCode(0);
		return 0;
	}

	private long ProcessReLogin(ReLogin rpc) {
		logger.info("ProcessReLogin: {} RequestId={} {}", rpc.getSender(), rpc.getSessionId(), rpc.Argument);
		var session = Sessions.computeIfAbsent(rpc.Argument.ServerId, __ -> new CacheHolder());
		if (!session.TryBindSocket(rpc.getSender(), rpc.Argument.GlobalCacheManagerHashIndex, false)) {
			rpc.SendResultCode(ReLoginBindSocketFail);
			return 0;
		}
		session.setActiveTime(System.currentTimeMillis());
		rpc.SendResultCode(0);
		return 0;
	}

	private long ProcessNormalClose(NormalClose rpc) throws Throwable {
		logger.info("ProcessNormalClose: {} RequestId={}", rpc.getSender(), rpc.getSessionId());
		var session = (CacheHolder)rpc.getSender().getUserState();
		if (session == null) {
			logger.warn("ProcessNormalClose: {} RequestId={} result={}", rpc.getSender(), rpc.getSessionId(), AcquireNotLogin);
			rpc.SendResultCode(AcquireNotLogin);
			return 0; // not login
		}
		if (!session.TryUnBindSocket(rpc.getSender())) {
			logger.warn("ProcessNormalClose: {} RequestId={} result={}", rpc.getSender(), rpc.getSessionId(), NormalCloseUnbindFail);
			rpc.SendResultCode(NormalCloseUnbindFail);
			return 0;
		}
		for (var k : session.Acquired.keySet()) {
			// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
			Release(session, k, false);
		}
		rpc.SendResultCode(0);
		logger.info("After NormalClose global.Count={}", global.size());
		return 0;
	}

	private long ProcessKeepAliveRequest(KeepAlive rpc) {
		if (rpc.getSender().getUserState() == null) {
			rpc.SendResultCode(AcquireNotLogin);
			return 0;
		}
		var sender = (CacheHolder)rpc.getSender().getUserState();
		sender.setActiveTime(System.currentTimeMillis());
		rpc.SendResult();
		return 0;
	}

	private long ProcessAcquireRequest(Acquire rpc) {
		var acquireState = rpc.Argument.State;
		if (ENABLE_PERF)
			perf.onAcquireBegin(rpc, acquireState);
		rpc.Result.GlobalKey = rpc.Argument.GlobalKey;
		rpc.Result.State = acquireState; // default success

		long result = 0;
		if (rpc.getSender().getUserState() == null) {
			rpc.Result.State = StateInvalid;
			rpc.SendResultCode(AcquireNotLogin);
		} else {
			try {
				var sender = (CacheHolder)rpc.getSender().getUserState();
				sender.setActiveTime(System.currentTimeMillis());
				switch (acquireState) {
				case StateInvalid: // release
					rpc.Result.State = Release(sender, rpc.Argument.GlobalKey, true); //await 方法内有等待
					rpc.SendResultCode(0);
					break;
				case StateShare:
					result = AcquireShare(rpc); //await 方法内有等待
					break;
				case StateModify:
					result = AcquireModify(rpc); //await 方法内有等待
					break;
				default:
					rpc.Result.State = StateInvalid;
					rpc.SendResultCode(AcquireErrorState);
					break;
				}
			} catch (Throwable ex) {
				logger.error("ProcessAcquireRequest", ex);
				rpc.Result.State = StateInvalid;
				rpc.SendResultCode(AcquireException);
			}
		}
		if (ENABLE_PERF)
			perf.onAcquireEnd(rpc, acquireState);
		return result;
	}

	private int Release(CacheHolder sender, Binary _gKey, boolean noWait) throws InterruptedException {
		while (true) {
			CacheState cs = global.computeIfAbsent(_gKey, CacheState::new);
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (cs) { //await 等锁
				if (cs.AcquireStatePending == StateRemoved) {
					// 这个是不可能的，因为有Release请求进来意味着肯定有拥有者(share or modify)，此时不可能进入StateRemoved。
					continue;
				}

				var gKey = cs.GlobalKey; // release 应该不需要引用到同一个key，统一写成这样了。
				while (cs.AcquireStatePending != StateInvalid && cs.AcquireStatePending != StateRemoved) {
					switch (cs.AcquireStatePending) {
					case StateShare:
					case StateModify:
						if (isDebugEnabled)
							logger.debug("Release 0 {} {} {}", sender, gKey, cs);
						if (noWait)
							return cs.GetSenderCacheState(sender);
						break;
					case StateRemoving:
						// release 不会导致死锁，等待即可。
						break;
					}
					cs.wait(); //await 等通知
				}
				if (cs.AcquireStatePending == StateRemoved)
					continue;
				cs.AcquireStatePending = StateRemoving;

				if (cs.Modify == sender)
					cs.Modify = null;
				cs.Share.remove(sender); // always try remove

				if (cs.Modify == null && cs.Share.isEmpty()) {
					// 安全的从global中删除，没有并发问题。
					cs.AcquireStatePending = StateRemoved;
					global.remove(gKey);
				} else
					cs.AcquireStatePending = StateInvalid;
				sender.Acquired.remove(gKey);
				cs.notifyAll(); //notify
				return cs.GetSenderCacheState(sender);
			} //notify
		}
	}

	private int AcquireShare(Acquire rpc) throws InterruptedException {
		CacheHolder sender = (CacheHolder)rpc.getSender().getUserState();
		while (true) {
			CacheState cs = global.computeIfAbsent(rpc.Argument.GlobalKey, CacheState::new);
			synchronized (cs) { //await 等锁
				if (cs.AcquireStatePending == StateRemoved)
					continue;

				if (cs.Modify != null && !cs.Share.isEmpty())
					throw new IllegalStateException("CacheState state error");

				while (cs.AcquireStatePending != StateInvalid && cs.AcquireStatePending != StateRemoved) {
					switch (cs.AcquireStatePending) {
					case StateShare:
						if (cs.Modify == null)
							throw new IllegalStateException("CacheState state error");
						if (cs.Modify == sender) {
							if (isDebugEnabled)
								logger.debug("1 {} {} {}", sender, StateShare, cs);
							rpc.Result.State = StateInvalid;
							rpc.SendResultCode(AcquireShareDeadLockFound);
							return 0;
						}
						break;
					case StateModify:
						if (cs.Modify == sender || cs.Share.contains(sender)) {
							if (isDebugEnabled)
								logger.debug("2 {} {} {}", sender, StateShare, cs);
							rpc.Result.State = StateInvalid;
							rpc.SendResultCode(AcquireShareDeadLockFound);
							return 0;
						}
						break;
					case StateRemoving:
						break;
					}
					if (isDebugEnabled)
						logger.debug("3 {} {} {}", sender, StateShare, cs);
					cs.wait(); //await 等通知
					if (cs.Modify != null && !cs.Share.isEmpty())
						throw new IllegalStateException("CacheState state error");
				}
				if (cs.AcquireStatePending == StateRemoved)
					continue; // concurrent release

				cs.AcquireStatePending = StateShare;
				SerialIdGenerator.getAndIncrement();

				var gKey = cs.GlobalKey;
				if (cs.Modify != null) {
					if (cs.Modify == sender) {
						// 已经是Modify又申请，可能是sender异常关闭，
						// 又重启连上。更新一下。应该是不需要的。
						sender.Acquired.put(gKey, StateModify);
						cs.AcquireStatePending = StateInvalid;
						if (isDebugEnabled)
							logger.debug("4 {} {} {}", sender, StateShare, cs);
						rpc.Result.State = StateModify;
						rpc.SendResultCode(AcquireShareAlreadyIsModify);
						return 0;
					}

					var reduceResultState = new OutInt(StateReduceNetError); // 默认网络错误。
					if (cs.Modify.Reduce(gKey, rpc.getResultCode(), r -> { //await 方法内有等待
						if (ENABLE_PERF)
							perf.onReduceEnd(r);
						reduceResultState.Value = r.isTimeout() ? StateReduceRpcTimeout : r.Result.State;
						synchronized (cs) {
							cs.notifyAll(); //notify
						}
						return 0;
					})) {
						if (isDebugEnabled)
							logger.debug("5 {} {} {}", sender, StateShare, cs);
						cs.wait();
					}
					switch (reduceResultState.Value) {
					case StateShare:
						cs.Modify.Acquired.put(gKey, StateShare);
						cs.Share.add(cs.Modify); // 降级成功。
						break;

					case StateInvalid:
						// 降到了 Invalid，此时就不需要加入 Share 了。
						cs.Modify.Acquired.remove(gKey);
						break;

					case StateReduceErrorFreshAcquire:
						cs.AcquireStatePending = StateInvalid;
						cs.notifyAll(); //notify
						if (ENABLE_PERF)
							perf.onOthers("XXX Fresh " + StateShare);
						// logger.error("XXX Fresh {} {} {}", sender, StateShare, cs);
						rpc.Result.State = StateInvalid;
						rpc.SendResultCode(StateReduceErrorFreshAcquire);
						return 0;

					default:
						// 包含协议返回错误的值的情况。
						// case StateReduceRpcTimeout: // 11
						// case StateReduceException: // 12
						// case StateReduceNetError: // 13
						cs.AcquireStatePending = StateInvalid;
						cs.notifyAll(); //notify
						if (ENABLE_PERF)
							perf.onOthers("XXX 8 " + StateShare + " " + reduceResultState.Value);
						// logger.error("XXX 8 {} {} {} {}", sender, StateShare, cs, reduceResultState.Value);
						rpc.Result.State = StateInvalid;
						rpc.SendResultCode(AcquireShareFailed);
						return 0;
					}

					sender.Acquired.put(gKey, StateShare);
					cs.Modify = null;
					cs.Share.add(sender);
					cs.AcquireStatePending = StateInvalid;
					cs.notifyAll(); //notify
					if (isDebugEnabled)
						logger.debug("6 {} {} {}", sender, StateShare, cs);
					rpc.SendResultCode(0);
					return 0;
				}

				sender.Acquired.put(gKey, StateShare);
				cs.Share.add(sender);
				cs.AcquireStatePending = StateInvalid;
				cs.notifyAll(); //notify
				if (isDebugEnabled)
					logger.debug("7 {} {} {}", sender, StateShare, cs);
				rpc.SendResultCode(0);
				return 0;
			} //notify
		}
	}

	private int AcquireModify(Acquire rpc) throws InterruptedException {
		CacheHolder sender = (CacheHolder)rpc.getSender().getUserState();
		while (true) {
			CacheState cs = global.computeIfAbsent(rpc.Argument.GlobalKey, CacheState::new);
			synchronized (cs) { //await 等锁
				if (cs.AcquireStatePending == StateRemoved)
					continue;

				if (cs.Modify != null && !cs.Share.isEmpty())
					throw new IllegalStateException("CacheState state error");

				while (cs.AcquireStatePending != StateInvalid && cs.AcquireStatePending != StateRemoved) {
					switch (cs.AcquireStatePending) {
					case StateShare:
						if (cs.Modify == null)
							throw new IllegalStateException("CacheState state error");

						if (cs.Modify == sender) {
							if (isDebugEnabled)
								logger.debug("1 {} {} {}", sender, StateModify, cs);
							rpc.Result.State = StateInvalid;
							rpc.SendResultCode(AcquireModifyDeadLockFound);
							return 0;
						}
						break;
					case StateModify:
						if (cs.Modify == sender || cs.Share.contains(sender)) {
							if (isDebugEnabled)
								logger.debug("2 {} {} {}", sender, StateModify, cs);
							rpc.Result.State = StateInvalid;
							rpc.SendResultCode(AcquireModifyDeadLockFound);
							return 0;
						}
						break;
					case StateRemoving:
						break;
					}
					if (isDebugEnabled)
						logger.debug("3 {} {} {}", sender, StateModify, cs);
					cs.wait(); //await 等通知
					if (cs.Modify != null && !cs.Share.isEmpty())
						throw new IllegalStateException("CacheState state error");
				}
				if (cs.AcquireStatePending == StateRemoved)
					continue; // concurrent release

				cs.AcquireStatePending = StateModify;
				SerialIdGenerator.getAndIncrement();

				var gKey = cs.GlobalKey;
				if (cs.Modify != null) {
					if (cs.Modify == sender) {
						if (isDebugEnabled)
							logger.debug("4 {} {} {}", sender, StateModify, cs);
						// 已经是Modify又申请，可能是sender异常关闭，又重启连上。
						// 更新一下。应该是不需要的。
						sender.Acquired.put(gKey, StateModify);
						cs.AcquireStatePending = StateInvalid;
						cs.notifyAll(); //notify
						rpc.SendResultCode(AcquireModifyAlreadyIsModify);
						return 0;
					}

					var reduceResultState = new OutInt(StateReduceNetError); // 默认网络错误。
					if (cs.Modify.Reduce(gKey, rpc.getResultCode(), r -> { //await 方法内有等待
						if (ENABLE_PERF)
							perf.onReduceEnd(r);
						reduceResultState.Value = r.isTimeout() ? StateReduceRpcTimeout : r.Result.State;
						synchronized (cs) {
							cs.notifyAll(); //notify
						}
						return 0;
					})) {
						if (isDebugEnabled)
							logger.debug("5 {} {} {}", sender, StateModify, cs);
						cs.wait(); //await 等通知
					}

					switch (reduceResultState.Value) {
					case StateInvalid:
						cs.Modify.Acquired.remove(gKey);
						break; // reduce success

					case StateReduceErrorFreshAcquire:
						cs.AcquireStatePending = StateInvalid;
						cs.notifyAll(); //notify
						if (ENABLE_PERF)
							perf.onOthers("XXX Fresh " + StateModify);
						// logger.error("XXX Fresh {} {} {}", sender, StateModify, cs);
						rpc.Result.State = StateInvalid;
						rpc.SendResultCode(StateReduceErrorFreshAcquire);
						return 0;

					default:
						// case StateReduceRpcTimeout: // 11
						// case StateReduceException: // 12
						// case StateReduceNetError: // 13
						cs.AcquireStatePending = StateInvalid;
						cs.notifyAll(); //notify
						if (ENABLE_PERF)
							perf.onOthers("XXX 9 " + StateModify + " " + reduceResultState.Value);
						// logger.error("XXX 9 {} {} {} {}", sender, StateModify, cs, reduceResultState.Value);
						rpc.Result.State = StateInvalid;
						rpc.SendResultCode(AcquireModifyFailed);
						return 0;
					}

					sender.Acquired.put(gKey, StateModify);
					cs.Modify = sender;
					cs.Share.remove(sender);
					cs.AcquireStatePending = StateInvalid;
					cs.notifyAll(); //notify
					if (isDebugEnabled)
						logger.debug("6 {} {} {}", sender, StateModify, cs);
					rpc.SendResultCode(0);
					return 0;
				}

				ArrayList<KV<CacheHolder, Reduce>> reducePending = new ArrayList<>();
				IdentityHashSet<CacheHolder> reduceSucceed = new IdentityHashSet<>();
				boolean senderIsShare = false;
				// 先把降级请求全部发送给出去。
				for (var it = cs.Share.iterator(); it.moveToNext(); ) {
					var c = it.value();
					if (c == sender) {
						// 申请者不需要降级，直接加入成功。
						senderIsShare = true;
						reduceSucceed.add(sender);
						continue;
					}
					Reduce reduce = c.ReduceWaitLater(gKey, rpc.getResultCode());
					if (reduce == null) {
						// 网络错误不再认为成功。整个降级失败，要中断降级。
						// 已经发出去的降级请求要等待并处理结果。后面处理。
						break;
					}
					reducePending.add(KV.Create(c, reduce));
				}

				// 两种情况不需要发reduce
				// 1. share是空的, 可以直接升为Modify
				// 2. sender是share, 而且reducePending的size是0
				var errorFreshAcquire = new OutObject<>(Boolean.FALSE);
				if (!cs.Share.isEmpty() && (!senderIsShare || !reducePending.isEmpty())) {
					Task.run(() -> {
						// 一个个等待是否成功。WaitAll 碰到错误不知道怎么处理的，
						// 应该也会等待所有任务结束（包括错误）。
						var freshAcquire = false;
						for (var reduce : reducePending) {
							try {
								reduce.getValue().getFuture().await(); //await 等RPC回复
								switch (reduce.getValue().Result.State) {
								case StateInvalid:
									reduceSucceed.add(reduce.getKey());
									break;
								case StateReduceErrorFreshAcquire:
									// 这个错误不进入Forbid状态。
									freshAcquire = true;
									break;
								default:
									reduce.getKey().SetError();
									break;
								}
							} catch (Throwable ex) {
								reduce.getKey().SetError();
								// 等待失败不再看作成功。
								if (Task.getRootCause(ex) instanceof RpcTimeoutException) {
									logger.warn("Reduce Timeout {} AcquireState={} CacheState={} arg={}",
											sender, StateModify, cs, reduce.getValue().Argument);
								} else {
									logger.error("Reduce {} AcquireState={} CacheState={} arg={}",
											sender, StateModify, cs, reduce.getValue().Argument, ex);
								}
							}
						}
						errorFreshAcquire.Value = freshAcquire;
						synchronized (cs) {
							// 需要唤醒等待任务结束的，但没法指定，只能全部唤醒。
							cs.notifyAll(); //notify
						}
					}, "GlobalCacheManager.AcquireModify.WaitReduce");
					if (isDebugEnabled)
						logger.debug("7 {} {} {}", sender, StateModify, cs);
					cs.wait(); //await 等通知
				}

				// 移除成功的。
				for (var it = reduceSucceed.iterator(); it.moveToNext(); ) {
					var succeed = it.value();
					if (succeed != sender) {
						// sender 不移除：
						// 1. 如果申请成功，后面会更新到Modify状态。
						// 2. 如果申请不成功，恢复 cs.Share，保持 Acquired 不变。
						succeed.Acquired.remove(gKey);
					}
					cs.Share.remove(succeed);
				}
				// 如果前面降级发生中断(break)，这里就不会为0。
				if (cs.Share.isEmpty()) {
					sender.Acquired.put(gKey, StateModify);
					cs.Modify = sender;
					cs.AcquireStatePending = StateInvalid;
					cs.notifyAll(); //notify
					if (isDebugEnabled)
						logger.debug("8 {} {} {}", sender, StateModify, cs);
					rpc.SendResultCode(0);
				} else {
					// senderIsShare 在失败的时候，Acquired 没有变化，不需要更新。
					// 失败了，要把原来是share的sender恢复。先这样吧。
					if (senderIsShare)
						cs.Share.add(sender);
					cs.AcquireStatePending = StateInvalid;
					cs.notifyAll(); //notify
					if (ENABLE_PERF)
						perf.onOthers("XXX 10 " + StateModify);
					// logger.error("XXX 10 {} {} {}", sender, StateModify, cs);
					rpc.Result.State = StateInvalid;
					if (errorFreshAcquire.Value)
						rpc.SendResultCode(StateReduceErrorFreshAcquire); // 这个错误不看做失败，允许发送方继续尝试。
					else
						rpc.SendResultCode(AcquireModifyFailed);
				}
				// 很好，网络失败不再看成成功，发现除了加break，
				// 其他处理已经能包容这个改动，都不用动。
				return 0;
			} //notify
		}
	}

	private static final class CacheState {
		final Binary GlobalKey; // 这里的引用同global map的key,用于给CacheHolder里的map相同的key引用
		final IdentityHashSet<CacheHolder> Share = new IdentityHashSet<>();
		CacheHolder Modify;
		int AcquireStatePending = StateInvalid;

		public CacheState(Binary gKey) {
			GlobalKey = gKey;
		}

		int GetSenderCacheState(CacheHolder sender) {
			if (Modify == sender)
				return StateModify;
			if (Share.contains(sender))
				return StateShare;
			return StateInvalid;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			ByteBuffer.BuildString(sb, Share);
			return String.format("P%d M%s S%s", AcquireStatePending, Modify, sb);
		}
	}

	private static final class CacheHolder {
		final ConcurrentHashMap<Binary, Integer> Acquired = new ConcurrentHashMap<>();
		long SessionId;
		int GlobalCacheManagerHashIndex;
		private volatile long ActiveTime = System.currentTimeMillis();
		private volatile long LastErrorTime;
		private boolean Logined = false;

		// not under lock
		void kick() {
			var peer = Instance.Server.GetSocket(SessionId);
			if (null != peer) {
				peer.setUserState(null); // 来自这个Agent的所有请求都会失败。
				peer.close(); // 关闭连接，强制Agent重新登录。
			}
			SessionId = 0; // 清除网络状态。
		}

		long getActiveTime() {
			return ActiveTime;
		}

		void setActiveTime(long value) {
			ActiveTime = value;
		}

		synchronized boolean TryBindSocket(AsyncSocket newSocket, int _GlobalCacheManagerHashIndex, boolean login) {
			if (login) {
				// login 相当于重置，允许再次Login。
				Logined = true;
			} else {
				// relogin 必须login之后才允许ReLogin。这个用来检测Global宕机并重启。
				if (!Logined)
					return false;
			}
			if (newSocket.getUserState() != null) {
				logger.warn("TryBindSocket: already bound! newSocket.getUserState() != null, SessionId={}", newSocket.getSessionId());
				return false; // 不允许再次绑定。Login Or ReLogin 只能发一次。
			}

			var socket = Instance.Server.GetSocket(SessionId);
			if (socket == null) {
				// old socket not exist or has lost.
				SessionId = newSocket.getSessionId();
				newSocket.setUserState(this);
				GlobalCacheManagerHashIndex = _GlobalCacheManagerHashIndex;
				return true;
			}
			// 每个AutoKeyLocalId只允许一个实例，已经存在了以后，旧的实例上有状态，阻止新的实例登录成功。
			logger.warn("TryBindSocket: already bound! GetSocket(SessionId={}) != null", newSocket.getSessionId());
			return false;
		}

		synchronized boolean TryUnBindSocket(AsyncSocket oldSocket) {
			// 这里检查比较严格，但是这些检查应该都不会出现。

			if (oldSocket.getUserState() != this)
				return false; // not bind to this

			var current = Instance.Server.GetSocket(SessionId);
			if (current != null && current != oldSocket)
				return false; // not same socket

			SessionId = 0;
			return true;
		}

		@Override
		public String toString() {
			return String.valueOf(SessionId);
		}

		boolean Reduce(Binary gkey, long fresh, ProtocolHandle<Rpc<GlobalKeyState, GlobalKeyState>> response) {
			try {
				if (System.currentTimeMillis() - LastErrorTime < Instance.AchillesHeelConfig.GlobalForbidPeriod)
					return false;
				AsyncSocket peer = Instance.Server.GetSocket(SessionId);
				if (peer != null) {
					var reduce = new Reduce(gkey, StateInvalid);
					reduce.setResultCode(fresh);
					if (ENABLE_PERF)
						Instance.perf.onReduceBegin(reduce);
					if (reduce.Send(peer, response, Instance.AchillesHeelConfig.ReduceTimeout)) //await 等RPC回复
						return true;
					if (ENABLE_PERF)
						Instance.perf.onReduceCancel(reduce);
				}
				logger.warn("Send Reduce failed. SessionId={}, peer={}, gkey={}", SessionId, peer, gkey);
			} catch (Exception ex) {
				// 这里的异常只应该是网络发送异常。
				logger.error("Reduce Exception {}", gkey, ex);
			}
			SetError();
			return false;
		}

		void SetError() {
			long now = System.currentTimeMillis();
			if (now - LastErrorTime > Instance.AchillesHeelConfig.GlobalForbidPeriod)
				LastErrorTime = now;
		}

		/**
		 * 返回null表示发生了网络错误，或者应用服务器已经关闭。
		 */
		Reduce ReduceWaitLater(Binary gkey, long fresh) {
			try {
				if (System.currentTimeMillis() - LastErrorTime < Instance.AchillesHeelConfig.GlobalForbidPeriod)
					return null;
				AsyncSocket peer = Instance.Server.GetSocket(SessionId);
				if (peer != null) {
					var reduce = new Reduce(gkey, StateInvalid);
					reduce.setResultCode(fresh);
					if (ENABLE_PERF)
						Instance.perf.onReduceBegin(reduce);
					reduce.SendForWait(peer, Instance.AchillesHeelConfig.ReduceTimeout);
					if (ENABLE_PERF) {
						if (reduce.getFuture().isCompletedExceptionally() && !reduce.isTimeout())
							Instance.perf.onReduceCancel(reduce);
						else
							Instance.perf.onReduceEnd(reduce);
					}
					return reduce;
				}
				logger.warn("Send Reduce failed. SessionId={}, gkey={}", SessionId, gkey);
			} catch (Throwable ex) {
				// 这里的异常只应该是网络发送异常。
				logger.error("ReduceWaitLater Exception {}", gkey, ex);
			}
			SetError();
			return null;
		}
	}

	private static final class ServerService extends Service {
		ServerService(Zeze.Config config) throws Throwable {
			super("GlobalCacheManager", config);
		}

		@Override
		public void OnSocketAccept(AsyncSocket so) throws Throwable {
			logger.info("OnSocketAccept {}", so);
			// so.UserState = new CacheHolder(so.SessionId); // Login ReLogin 的时候初始化。
			super.OnSocketAccept(so);
		}

		@Override
		public void OnSocketClose(AsyncSocket so, Throwable e) throws Throwable {
			logger.info("OnSocketClose {}", so);
			var session = (CacheHolder)so.getUserState();
			if (session != null)
				session.TryUnBindSocket(so); // unbind when login
			super.OnSocketClose(so, e);
		}

		@Override
		public <P extends Protocol<?>> void DispatchRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
																ProtocolFactoryHandle<?> factoryHandle) {
			try {
				responseHandle.handle(rpc);
			} catch (Throwable e) {
				logger.error("DispatchRpcResponse exception:", e);
			}
		}
	}

	public static void main(String[] args) throws Throwable {
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			e.printStackTrace();
			logger.fatal("uncaught exception in {}:", t, e);
		});

		String ip = null;
		int port = 5555;
		String raftName = null;
		String raftConf = "global.raft.xml";

		Task.tryInitThreadPool(null, null, null);

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
			default:
				throw new IllegalArgumentException("unknown argument: " + args[i]);
			}
		}

		if (raftName == null || raftName.isEmpty()) {
			logger.info("Start {}:{}", ip != null ? ip : "any", port);
			InetAddress address = (ip != null && !ip.isBlank()) ? InetAddress.getByName(ip) : null;
			Instance.Start(address, port);
			synchronized (Thread.currentThread()) {
				Thread.currentThread().wait();
			}
		} else if (raftName.equals("RunAllNodes")) {
			logger.info("Start Raft=RunAllNodes");
			//noinspection unused
			try (var GlobalRaft1 = new GlobalCacheManagerWithRaft("127.0.0.1:5556", RaftConfig.Load(raftConf));
				 var GlobalRaft2 = new GlobalCacheManagerWithRaft("127.0.0.1:5557", RaftConfig.Load(raftConf));
				 var GlobalRaft3 = new GlobalCacheManagerWithRaft("127.0.0.1:5558", RaftConfig.Load(raftConf))) {
				synchronized (Thread.currentThread()) {
					Thread.currentThread().wait();
				}
			}
		} else {
			logger.info("Start Raft={},{}", raftName, raftConf);
			//noinspection unused
			try (var GlobalRaft = new GlobalCacheManagerWithRaft(raftName, RaftConfig.Load(raftConf))) {
				synchronized (Thread.currentThread()) {
					Thread.currentThread().wait();
				}
			}
		}
	}
}
