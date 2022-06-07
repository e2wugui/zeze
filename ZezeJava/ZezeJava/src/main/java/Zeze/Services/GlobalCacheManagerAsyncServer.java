package Zeze.Services;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Arch.RedirectFuture;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Net.Selectors;
import Zeze.Net.Service;
import Zeze.Raft.RaftConfig;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.GlobalCacheManager.Acquire;
import Zeze.Services.GlobalCacheManager.Cleanup;
import Zeze.Services.GlobalCacheManager.KeepAlive;
import Zeze.Services.GlobalCacheManager.Login;
import Zeze.Services.GlobalCacheManager.NormalClose;
import Zeze.Services.GlobalCacheManager.Param2;
import Zeze.Services.GlobalCacheManager.ReLogin;
import Zeze.Services.GlobalCacheManager.Reduce;
import Zeze.Util.Action0;
import Zeze.Util.Action1;
import Zeze.Util.AsyncLock;
import Zeze.Util.IdentityHashSet;
import Zeze.Util.KV;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import Zeze.Util.ThreadFactoryWithName;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

public final class GlobalCacheManagerAsyncServer implements GlobalCacheManagerConst {
	static {
		System.setProperty("log4j.configurationFile", "log4j2.xml");
		var levelProp = System.getProperty("logLevel");
		var level = Level.INFO;
		if ("trace".equalsIgnoreCase(levelProp))
			level = Level.TRACE;
		else if ("debug".equalsIgnoreCase(levelProp))
			level = Level.DEBUG;
		((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger().setLevel(level);
	}

	private static final boolean ENABLE_PERF = true;
	private static final Logger logger = LogManager.getLogger(GlobalCacheManagerAsyncServer.class);
	private static final GlobalCacheManagerAsyncServer Instance = new GlobalCacheManagerAsyncServer();

	public static GlobalCacheManagerAsyncServer getInstance() {
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
	private final GlobalCacheManagerServer.GCMConfig Config = new GlobalCacheManagerServer.GCMConfig();
	private AchillesHeelConfig AchillesHeelConfig;
	private GlobalCacheManagerPerf perf;

	private GlobalCacheManagerAsyncServer() {
	}

	// 外面主动提供装载配置，需要在Load之前把这个实例注册进去。
	public GlobalCacheManagerServer.GCMConfig getConfig() {
		return Config;
	}

	public void Start(InetAddress ipaddress, int port) throws Throwable {
		Start(ipaddress, port, null);
	}

	public synchronized void Start(InetAddress ipaddress, int port, Zeze.Config config) throws Throwable {
		if (Server != null)
			return;

		if (ENABLE_PERF)
			perf = new GlobalCacheManagerPerf(SerialIdGenerator);

		if (config == null)
			config = new Zeze.Config().AddCustomize(Config).LoadAndParse();

		Sessions = new LongConcurrentHashMap<>(4096);
		global = new ConcurrentHashMap<>(Config.InitialCapacity);

		Server = new ServerService(config);

		Server.AddFactoryHandle(Acquire.TypeId_,
				new Service.ProtocolFactoryHandle<>(Acquire::new, this::ProcessAcquireRequest));

		Server.AddFactoryHandle(Reduce.TypeId_,
				new Service.ProtocolFactoryHandle<>(Reduce::new));

		Server.AddFactoryHandle(Login.TypeId_,
				new Service.ProtocolFactoryHandle<>(Login::new, this::ProcessLogin));

		Server.AddFactoryHandle(ReLogin.TypeId_,
				new Service.ProtocolFactoryHandle<>(ReLogin::new, this::ProcessReLogin));

		Server.AddFactoryHandle(NormalClose.TypeId_,
				new Service.ProtocolFactoryHandle<>(NormalClose::new, this::ProcessNormalClose));

		// 临时注册到这里，安全起见应该起一个新的Service，并且仅绑定到 localhost。
		Server.AddFactoryHandle(Cleanup.TypeId_,
				new Service.ProtocolFactoryHandle<>(Cleanup::new, this::ProcessCleanup));

		Server.AddFactoryHandle(KeepAlive.TypeId_,
				new Service.ProtocolFactoryHandle<>(KeepAlive::new, this::ProcessKeepAliveRequest));

		ServerSocket = Server.NewServerSocket(ipaddress, port, null);

		// Global的守护不需要独立线程。当出现异常问题不能工作时，没有释放锁是不会造成致命问题的。
		AchillesHeelConfig = new AchillesHeelConfig(Config.MaxNetPing, Config.ServerProcessTime, Config.ServerReleaseTimeout);
		Task.schedule(5000, 5000, this::AchillesHeelDaemon);
	}

	private void AchillesHeelDaemon() {
		var now = System.currentTimeMillis();

		Sessions.forEach(session -> {
			if (now - session.getActiveTime() > AchillesHeelConfig.GlobalDaemonTimeout) {
				var allReleaseFuture = new CountDownFuture();
				for (var e : session.Acquired.entrySet()) {
					// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
					ReleaseAsync(session, e.getKey(), allReleaseFuture.createOne());
				}
				// skip allReleaseFuture result
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
			var allReleaseFuture = new CountDownFuture();
			for (var e : session.Acquired.entrySet()) {
				// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
				ReleaseAsync(session, e.getKey(), allReleaseFuture.createOne());
			}
			allReleaseFuture.then(__ -> rpc.SendResultCode(0));
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
		session.setActiveTime(System.currentTimeMillis());
		// new login, 比如逻辑服务器重启。release old acquired.
		var allReleaseFuture = new CountDownFuture();
		for (var e : session.Acquired.entrySet()) {
			// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
			ReleaseAsync(session, e.getKey(), allReleaseFuture.createOne());
		}
		rpc.Result.MaxNetPing = Config.MaxNetPing;
		rpc.Result.ServerProcessTime = Config.ServerProcessTime;
		rpc.Result.ServerReleaseTimeout = Config.ServerReleaseTimeout;
		allReleaseFuture.then(__ -> rpc.SendResultCode(0));
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
		var allReleaseFuture = new CountDownFuture();
		for (var e : session.Acquired.entrySet()) {
			// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
			ReleaseAsync(session, e.getKey(), allReleaseFuture.createOne());
		}
		allReleaseFuture.then(__ -> {
			rpc.SendResultCode(0);
			logger.info("After NormalClose global.Count={}", global.size());
		});
		return 0;
	}

	private long ProcessKeepAliveRequest(KeepAlive rpc) {
		if (rpc.getSender().getUserState() == null) {
			rpc.SendResultCode(AcquireNotLogin);
			return 0;
		}
		var sender = (GlobalCacheManagerAsyncServer.CacheHolder)rpc.getSender().getUserState();
		sender.setActiveTime(System.currentTimeMillis());
		rpc.SendResult();
		return 0;
	}

	private long ProcessAcquireRequest(Acquire rpc) {
		if (ENABLE_PERF)
			perf.onAcquireBegin(rpc, rpc.Argument.State);
		rpc.Result.GlobalKey = rpc.Argument.GlobalKey;
		rpc.Result.State = rpc.Argument.State; // default success

		if (rpc.getSender().getUserState() == null) {
			rpc.Result.State = StateInvalid;
			rpc.SendResultCode(AcquireNotLogin);
		} else {
			try {
				var sender = (GlobalCacheManagerAsyncServer.CacheHolder)rpc.getSender().getUserState();
				sender.setActiveTime(System.currentTimeMillis());
				switch (rpc.Argument.State) {
				case StateInvalid: // release
					ReleaseAsync(rpc);
					return 0;
				case StateShare:
					AcquireShareAsync(rpc);
					return 0;
				case StateModify:
					AcquireModifyAsync(rpc);
					return 0;
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
			perf.onAcquireEnd(rpc, rpc.Argument.State);
		return 0;
	}

	public static final class CountDownFuture extends RedirectFuture<Object> {
		private final AtomicInteger counter = new AtomicInteger(1);

		public CountDownFuture createOne() {
			counter.incrementAndGet();
			return this;
		}

		public void finishOne() {
			if (counter.decrementAndGet() == 0)
				SetResult(null);
		}

		@Override
		public RedirectFuture<Object> then(Action1<Object> onResult) throws Throwable {
			finishOne();
			return super.then(onResult);
		}
	}

	private void ReleaseAsync(CacheHolder sender, Binary _gKey, CountDownFuture future) {
		var cs = global.computeIfAbsent(_gKey, CacheState::new);
		var state = new Object() {
			int stage;
		};
		cs.lock.enter(() -> {
			var gKey = cs.GlobalKey;
			if (state.stage == 1) {
				if (cs.Modify != null && !cs.Share.isEmpty())
					throw new IllegalStateException("CacheState state error");
			} else if (state.stage == 0 && cs.AcquireStatePending == StateRemoved) {
				// 这个是不可能的，因为有Release请求进来意味着肯定有拥有者(share or modify)，此时不可能进入StateRemoved。
				cs.lock.leave();
				ReleaseAsync(sender, gKey, future); // retry
				return;
			}

			if (cs.AcquireStatePending != StateInvalid && cs.AcquireStatePending != StateRemoved) {
				switch (cs.AcquireStatePending) {
				case StateShare:
				case StateModify:
					logger.debug("Release 0 {} {} {}", sender, gKey, cs);
					break;
				case StateRemoving:
					// release 不会导致死锁，等待即可。
					break;
				}
				state.stage = 1;
				cs.lock.leaveAndWaitNotify();
				return;
			}
			if (cs.AcquireStatePending == StateRemoved) {
				cs.lock.leave();
				ReleaseAsync(sender, gKey, future); // retry
				return;
			}
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
			cs.lock.notifyAllWait();
			future.finishOne();
		});
	}

	private void ReleaseAsync(Acquire rpc) {
		var cs = global.computeIfAbsent(rpc.Argument.GlobalKey, CacheState::new);
		var state = new Object() {
			int stage;
		};
		cs.lock.enter(() -> {
			if (state.stage == 1) {
				if (cs.Modify != null && !cs.Share.isEmpty())
					throw new IllegalStateException("CacheState state error");
			} else if (state.stage == 0 && cs.AcquireStatePending == StateRemoved) {
				// 这个是不可能的，因为有Release请求进来意味着肯定有拥有者(share or modify)，此时不可能进入StateRemoved。
				cs.lock.leave();
				ReleaseAsync(rpc); // retry
				return;
			}

			var sender = (CacheHolder)rpc.getSender().getUserState();
			var gKey = cs.GlobalKey;
			if (cs.AcquireStatePending != StateInvalid && cs.AcquireStatePending != StateRemoved) {
				switch (cs.AcquireStatePending) {
				case StateShare:
				case StateModify:
					logger.debug("Release 0 {} {} {}", sender, gKey, cs);
					rpc.Result.State = cs.GetSenderCacheState(sender);
					rpc.SendResultCode(0);
					if (ENABLE_PERF)
						perf.onAcquireEnd(rpc, StateInvalid);
					return;
				case StateRemoving:
					// release 不会导致死锁，等待即可。
					break;
				}
				state.stage = 1;
				cs.lock.leaveAndWaitNotify();
				return;
			}
			if (cs.AcquireStatePending == StateRemoved) {
				cs.lock.leave();
				ReleaseAsync(rpc); // retry
				return;
			}

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
			cs.lock.notifyAllWait();
			rpc.Result.State = cs.GetSenderCacheState(sender);
			rpc.SendResultCode(0);
			if (ENABLE_PERF)
				perf.onAcquireEnd(rpc, StateInvalid);
		});
	}

	private void AcquireShareAsync(Acquire rpc) {
		var cs = global.computeIfAbsent(rpc.Argument.GlobalKey, CacheState::new);
		var state = new Object() {
			int stage;
			int reduceResultState;
		};
		cs.lock.enter(() -> {
			if (state.stage == 0) {
				if (cs.AcquireStatePending == StateRemoved) {
					cs.lock.leave();
					AcquireShareAsync(rpc); // retry
					return;
				}
			}
			if (state.stage <= 1 && cs.Modify != null && !cs.Share.isEmpty())
				throw new IllegalStateException("CacheState state error");

			var sender = (CacheHolder)rpc.getSender().getUserState();
			if (state.stage <= 1) {
				if (cs.AcquireStatePending != StateInvalid && cs.AcquireStatePending != StateRemoved) {
					switch (cs.AcquireStatePending) {
					case StateShare:
						if (cs.Modify == null)
							throw new IllegalStateException("CacheState state error");
						if (cs.Modify == sender) {
							logger.debug("1 {} {} {}", sender, StateShare, cs);
							rpc.Result.State = StateInvalid;
							rpc.SendResultCode(AcquireShareDeadLockFound);
							if (ENABLE_PERF)
								perf.onAcquireEnd(rpc, StateShare);
							return;
						}
						break;
					case StateModify:
						if (cs.Modify == sender || cs.Share.contains(sender)) {
							logger.debug("2 {} {} {}", sender, StateShare, cs);
							rpc.Result.State = StateInvalid;
							rpc.SendResultCode(AcquireShareDeadLockFound);
							if (ENABLE_PERF)
								perf.onAcquireEnd(rpc, StateShare);
							return;
						}
						break;
					case StateRemoving:
						break;
					}
					logger.debug("3 {} {} {}", sender, StateShare, cs);
					state.stage = 1;
					cs.lock.leaveAndWaitNotify();
					return;
				}
				if (cs.AcquireStatePending == StateRemoved) {
					cs.lock.leave();
					AcquireShareAsync(rpc); // retry
					return; // concurrent release
				}

				cs.AcquireStatePending = StateShare;
				SerialIdGenerator.incrementAndGet();
			}

			var gKey = cs.GlobalKey;
			if (cs.Modify != null || state.stage == 2) {
				if (state.stage != 2) {
					if (cs.Modify == sender) {
						// 已经是Modify又申请，可能是sender异常关闭，
						// 又重启连上。更新一下。应该是不需要的。
						sender.Acquired.put(gKey, StateModify);
						cs.AcquireStatePending = StateInvalid;
						logger.debug("4 {} {} {}", sender, StateShare, cs);
						rpc.Result.State = StateModify;
						rpc.SendResultCode(AcquireShareAlreadyIsModify);
						if (ENABLE_PERF)
							perf.onAcquireEnd(rpc, StateShare);
						return;
					}

					state.reduceResultState = StateReduceNetError; // 默认网络错误。。
					if (cs.Modify.ReduceWaitLater(gKey, rpc.getResultCode(), r -> {
						if (ENABLE_PERF)
							perf.onReduceEnd(r);
						state.reduceResultState = r.isTimeout() ? StateReduceRpcTimeout : r.Result.State;
						cs.lock.enter(cs.lock::notifyAllWait);
						return 0;
					}) != null) {
						logger.debug("5 {} {} {}", sender, StateShare, cs);
						state.stage = 2;
						cs.lock.leaveAndWaitNotify();
						return;
					}
				}

				switch (state.reduceResultState) {
				case StateShare:
					assert cs.Modify != null;
					cs.Modify.Acquired.put(gKey, StateShare);
					cs.Share.add(cs.Modify); // 降级成功。
					break;

				case StateInvalid:
					// 降到了 Invalid，此时就不需要加入 Share 了。
					assert cs.Modify != null;
					cs.Modify.Acquired.remove(gKey);
					break;

				case StateReduceErrorFreshAcquire:
					cs.AcquireStatePending = StateInvalid;
					cs.lock.notifyAllWait();
					if (ENABLE_PERF)
						perf.onOthers("XXX Fresh " + StateShare);
					rpc.Result.State = StateInvalid;
					rpc.SendResultCode(StateReduceErrorFreshAcquire);
					if (ENABLE_PERF)
						perf.onAcquireEnd(rpc, StateShare);
					return;

				default:
					// 包含协议返回错误的值的情况。
					// case StateReduceRpcTimeout: // 11
					// case StateReduceException: // 12
					// case StateReduceNetError: // 13
					cs.AcquireStatePending = StateInvalid;
					cs.lock.notifyAllWait();
					if (ENABLE_PERF)
						perf.onOthers("XXX 8 " + StateShare + " " + state.reduceResultState);
					// logger.error("XXX 8 {} {} {} {}", sender, StateShare, cs, state.reduceResultState);
					rpc.Result.State = StateInvalid;
					rpc.SendResultCode(AcquireShareFailed);
					if (ENABLE_PERF)
						perf.onAcquireEnd(rpc, StateShare);
					return;
				}

				sender.Acquired.put(gKey, StateShare);
				cs.Modify = null;
				cs.Share.add(sender);
				cs.AcquireStatePending = StateInvalid;
				cs.lock.notifyAllWait();
				logger.debug("6 {} {} {}", sender, StateShare, cs);
				rpc.SendResultCode(0);
				if (ENABLE_PERF)
					perf.onAcquireEnd(rpc, StateShare);
				return;
			}

			sender.Acquired.put(gKey, StateShare);
			cs.Share.add(sender);
			cs.AcquireStatePending = StateInvalid;
			cs.lock.notifyAllWait();
			logger.debug("7 {} {} {}", sender, StateShare, cs);
			rpc.SendResultCode(0);
			if (ENABLE_PERF)
				perf.onAcquireEnd(rpc, StateShare);
		});
	}

	private void AcquireModifyAsync(Acquire rpc) {
		var cs = global.computeIfAbsent(rpc.Argument.GlobalKey, CacheState::new);
		var state = new Object() {
			int stage;
			int reduceResultState;
		};
		cs.lock.enter(() -> {
			if (state.stage == 0) {
				if (cs.AcquireStatePending == StateRemoved) {
					cs.lock.leave();
					AcquireModifyAsync(rpc); // retry
					return;
				}
			}
			if (state.stage <= 1) {
				if (cs.Modify != null && !cs.Share.isEmpty())
					throw new IllegalStateException("CacheState state error");
			}

			var sender = (CacheHolder)rpc.getSender().getUserState();
			if (state.stage <= 1) {
				if (cs.AcquireStatePending != StateInvalid && cs.AcquireStatePending != StateRemoved) {
					switch (cs.AcquireStatePending) {
					case StateShare:
						if (cs.Modify == null)
							throw new IllegalStateException("CacheState state error");

						if (cs.Modify == sender) {
							logger.debug("1 {} {} {}", sender, StateModify, cs);
							rpc.Result.State = StateInvalid;
							rpc.SendResultCode(AcquireModifyDeadLockFound);
							if (ENABLE_PERF)
								perf.onAcquireEnd(rpc, StateModify);
							return;
						}
						break;
					case StateModify:
						if (cs.Modify == sender || cs.Share.contains(sender)) {
							logger.debug("2 {} {} {}", sender, StateModify, cs);
							rpc.Result.State = StateInvalid;
							rpc.SendResultCode(AcquireModifyDeadLockFound);
							if (ENABLE_PERF)
								perf.onAcquireEnd(rpc, StateModify);
							return;
						}
						break;
					case StateRemoving:
						break;
					}
					logger.debug("3 {} {} {}", sender, StateModify, cs);
					state.stage = 1;
					cs.lock.leaveAndWaitNotify();
					return;
				}
				if (cs.AcquireStatePending == StateRemoved) {
					cs.lock.leave();
					AcquireModifyAsync(rpc); // retry
					return; // concurrent release
				}

				cs.AcquireStatePending = StateModify;
				SerialIdGenerator.incrementAndGet();
			}

			var gKey = cs.GlobalKey;
			if (cs.Modify != null || state.stage == 2) {
				if (state.stage != 2) {
					if (cs.Modify == sender) {
						logger.debug("4 {} {} {}", sender, StateModify, cs);
						// 已经是Modify又申请，可能是sender异常关闭，又重启连上。
						// 更新一下。应该是不需要的。
						sender.Acquired.put(gKey, StateModify);
						cs.AcquireStatePending = StateInvalid;
						cs.lock.notifyAllWait();
						rpc.SendResultCode(AcquireModifyAlreadyIsModify);
						if (ENABLE_PERF)
							perf.onAcquireEnd(rpc, StateModify);
						return;
					}

					state.reduceResultState = StateReduceNetError; // 默认网络错误。
					if (cs.Modify.ReduceWaitLater(gKey, rpc.getResultCode(), r -> {
						if (ENABLE_PERF)
							perf.onReduceEnd(r);
						state.reduceResultState = r.isTimeout() ? StateReduceRpcTimeout : r.Result.State;
						cs.lock.enter(cs.lock::notifyAllWait);
						return 0;
					}) != null) {
						logger.debug("5 {} {} {}", sender, StateModify, cs);
						state.stage = 2;
						cs.lock.leaveAndWaitNotify();
						return;
					}
				}

				switch (state.reduceResultState) {
				case StateInvalid:
					assert cs.Modify != null;
					cs.Modify.Acquired.remove(gKey);
					break; // reduce success

				case StateReduceErrorFreshAcquire:
					cs.AcquireStatePending = StateInvalid;
					cs.lock.notifyAllWait();
					if (ENABLE_PERF)
						perf.onOthers("XXX Fresh " + StateModify);
					rpc.Result.State = StateInvalid;
					rpc.SendResultCode(StateReduceErrorFreshAcquire);
					if (ENABLE_PERF)
						perf.onAcquireEnd(rpc, StateModify);
					return;

				default:
					// case StateReduceRpcTimeout: // 11
					// case StateReduceException: // 12
					// case StateReduceNetError: // 13
					cs.AcquireStatePending = StateInvalid;
					cs.lock.notifyAllWait();
					if (ENABLE_PERF)
						perf.onOthers("XXX 9 " + StateModify + " " + state.reduceResultState);
					// logger.error("XXX 9 {} {} {} {}", sender, StateModify, cs, state.reduceResultState);
					rpc.Result.State = StateInvalid;
					rpc.SendResultCode(AcquireModifyFailed);
					if (ENABLE_PERF)
						perf.onAcquireEnd(rpc, StateModify);
					return;
				}

				sender.Acquired.put(gKey, StateModify);
				cs.Modify = sender;
				cs.Share.remove(sender);
				cs.AcquireStatePending = StateInvalid;
				cs.lock.notifyAllWait();
				logger.debug("6 {} {} {}", sender, StateModify, cs);
				rpc.SendResultCode(0);
				if (ENABLE_PERF)
					perf.onAcquireEnd(rpc, StateModify);
				return;
			}

			var reducePending = new ArrayList<KV<CacheHolder, Reduce>>();
			var reduceSucceed = new IdentityHashSet<CacheHolder>();
			var allReduceFuture = new CountDownFuture();
			var senderIsShareTmp = false;
			// 先把降级请求全部发送给出去。
			for (var it = cs.Share.iterator(); it.moveToNext(); ) {
				CacheHolder c = it.value();
				if (c == sender) {
					// 申请者不需要降级，直接加入成功。
					senderIsShareTmp = true;
					reduceSucceed.add(sender);
					continue;
				}
				allReduceFuture.createOne();
				Reduce reduce = c.ReduceWaitLater(gKey, rpc.getResultCode(), r -> {
					if (ENABLE_PERF)
						perf.onReduceEnd(r);
					// cs.lock.enter(() -> {
					// 	cs.Share.remove(c);
					allReduceFuture.finishOne();
					// });
					return 0;
				});
				if (reduce == null) {
					// 网络错误不再认为成功。整个降级失败，要中断降级。
					// 已经发出去的降级请求要等待并处理结果。后面处理。
					allReduceFuture.finishOne();
					break;
				}
				reducePending.add(KV.Create(c, reduce));
			}
			boolean senderIsShare = senderIsShareTmp;

			var errorFreshAcquire = new OutObject<Boolean>();
			Action0 lastStage = () -> {
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
					cs.lock.notifyAllWait();
					logger.debug("8 {} {} {}", sender, StateModify, cs);
					rpc.SendResultCode(0);
				} else {
					// senderIsShare 在失败的时候，Acquired 没有变化，不需要更新。
					// 失败了，要把原来是share的sender恢复。先这样吧。
					if (senderIsShare)
						cs.Share.add(sender);
					cs.AcquireStatePending = StateInvalid;
					cs.lock.notifyAllWait();
					if (ENABLE_PERF)
						perf.onOthers("XXX 10 " + StateModify);
					// logger.error("XXX 10 {} {} {}", sender, StateModify, cs);
					rpc.Result.State = StateInvalid;
					if (errorFreshAcquire.Value)
						rpc.SendResultCode(StateReduceErrorFreshAcquire); // 这个错误不看做失败，允许发送方继续尝试。
					else
						rpc.SendResultCode(AcquireModifyFailed);
				}
				if (ENABLE_PERF)
					perf.onAcquireEnd(rpc, StateModify);
				// 很好，网络失败不再看成成功，发现除了加break，
				// 其他处理已经能包容这个改动，都不用动。
			};

			// 两种情况不需要发reduce
			// 1. share是空的, 可以直接升为Modify
			// 2. sender是share, 而且reducePending的size是0
			if (!cs.Share.isEmpty() && (!senderIsShare || !reducePending.isEmpty())) {
				logger.debug("7 {} {} {}", sender, StateModify, cs);
				allReduceFuture.then(__ -> {
					// 一个个等待是否成功。WaitAll 碰到错误不知道怎么处理的，
					// 应该也会等待所有任务结束（包括错误）。
					var freshAcquire = false;
					for (var e : reducePending) {
						var cacheHolder = e.getKey();
						var reduce = e.getValue();
						try {
							switch (reduce.Result.State) {
							case StateInvalid:
								reduceSucceed.add(cacheHolder);
								break;
							case StateReduceErrorFreshAcquire:
								// 这个错误不进入Forbid状态。
								freshAcquire = true;
								break;
							default:
								cacheHolder.SetError();
							}
							if (reduce.Result.State == StateInvalid)
								reduceSucceed.add(cacheHolder);
							else
								cacheHolder.SetError();
						} catch (Throwable ex) {
							cacheHolder.SetError();
							// 等待失败不再看作成功。
							logger.error(String.format("Reduce %s AcquireState=%d CacheState=%s arg=%s",
									rpc.getSender().getUserState(), StateModify, cs, reduce.Argument), ex);
						}
					}
					errorFreshAcquire.Value = freshAcquire;
					cs.lock.enter(lastStage);
				});
			} else
				lastStage.run();
		});
	}

	private static final class CacheState {
		final Binary GlobalKey; // 这里的引用同global map的key,用于给CacheHolder里的map相同的key引用
		final IdentityHashSet<CacheHolder> Share = new IdentityHashSet<>();
		final AsyncLock lock = new AsyncLock();
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
		private volatile long ActiveTime;
		private volatile long LastErrorTime;
		private boolean Logined = false;

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
			// 每个ServerId只允许一个实例，已经存在了以后，旧的实例上有状态，阻止新的实例登录成功。
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

		void SetError() {
			long now = System.currentTimeMillis();
			if (now - LastErrorTime > Instance.AchillesHeelConfig.GlobalForbidPeriod)
				LastErrorTime = now;
		}

		/**
		 * 返回null表示发生了网络错误，或者应用服务器已经关闭。
		 */
		Reduce ReduceWaitLater(Binary gkey, long fresh, ProtocolHandle<Rpc<Param2, Param2>> handle) {
			try {
				if (System.currentTimeMillis() - LastErrorTime < Instance.AchillesHeelConfig.GlobalForbidPeriod)
					return null;
				AsyncSocket peer = Instance.Server.GetSocket(SessionId);
				if (peer != null) {
					var reduce = new Reduce(gkey, StateInvalid);
					reduce.setResultCode(fresh);
					if (ENABLE_PERF)
						Instance.perf.onReduceBegin(reduce);
					if (reduce.Send(peer, handle, Instance.AchillesHeelConfig.ReduceTimeout))
						return reduce;
					if (ENABLE_PERF)
						Instance.perf.onReduceCancel(reduce);
				}
				logger.warn("Send Reduce failed. SessionId={}, peer={}, gkey={}", SessionId, peer, gkey);
			} catch (Throwable ex) {
				// 这里的异常只应该是网络发送异常。
				logger.error("ReduceWaitLater Exception " + gkey, ex);
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
		public <P extends Protocol<?>> void DispatchProtocol(P p, ProtocolFactoryHandle<P> factoryHandle) {
			var handle = factoryHandle.Handle;
			if (handle != null) {
				try {
					handle.handle(p); // 所有协议处理几乎无阻塞,可放心直接跑在IO线程上
				} catch (Throwable e) {
					logger.error("DispatchProtocol exception:", e);
				}
			} else
				logger.warn("DispatchProtocol: Protocol Handle Not Found: {}", p);
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
		String ip = null;
		int port = 5555;
		int threadCount = 0;
		String raftName = null;
		String raftConf = "global.raft.xml";

		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-ip":
				ip = args[++i];
				break;
			case "-port":
				port = Integer.parseInt(args[++i]);
				break;
			case "-threads":
				threadCount = Integer.parseInt(args[++i]);
				break;
			case "-raft":
				raftName = args[++i];
				break;
			case "-raftConf":
				raftConf = args[++i];
				break;
			case "-tryNextSync":
				AsyncLock.tryNextSync = true;
				break;
			default:
				throw new IllegalArgumentException("unknown argument: " + args[i]);
			}
		}

		int cpuCount = Runtime.getRuntime().availableProcessors();
		if (threadCount < 1)
			threadCount = cpuCount;
		Task.initThreadPool(Task.newFixedThreadPool(threadCount, "ZezeTaskPool"),
				Executors.newSingleThreadScheduledExecutor(new ThreadFactoryWithName("ZezeScheduledPool")));
		if (Selectors.getInstance().getCount() < cpuCount)
			Selectors.getInstance().add(cpuCount - Selectors.getInstance().getCount());

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
