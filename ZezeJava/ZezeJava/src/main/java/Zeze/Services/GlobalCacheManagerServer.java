package Zeze.Services;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Beans.GlobalCacheManagerWithRaft.GlobalTableKey;
import Zeze.Net.AsyncSocket;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Net.Service;
import Zeze.Raft.RaftConfig;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.GlobalCacheManager.Acquire;
import Zeze.Services.GlobalCacheManager.Cleanup;
import Zeze.Services.GlobalCacheManager.Login;
import Zeze.Services.GlobalCacheManager.NormalClose;
import Zeze.Services.GlobalCacheManager.Param2;
import Zeze.Services.GlobalCacheManager.ReLogin;
import Zeze.Services.GlobalCacheManager.Reduce;
import Zeze.Util.KV;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;

public final class GlobalCacheManagerServer implements GlobalCacheManagerConst {
	static {
		System.setProperty("log4j.configurationFile", "log4j2.xml");
	}

	private static final Logger logger = LogManager.getLogger(GlobalCacheManagerServer.class);
	private static final GlobalCacheManagerServer Instance = new GlobalCacheManagerServer();

	public static GlobalCacheManagerServer getInstance() {
		return Instance;
	}

	private ServerService Server;
	private AsyncSocket ServerSocket;
	private ConcurrentHashMap<GlobalTableKey, CacheState> global;
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

	static final class GCMConfig implements Zeze.Config.ICustomize {
		private int ConcurrencyLevel = 1024;
		// 设置了这么大，开始使用后，大概会占用700M的内存，作为全局服务器，先这么大吧。
		// 尽量不重新调整ConcurrentDictionary。
		private int InitialCapacity = 10000000;

		@Override
		public String getName() {
			return "GlobalCacheManager";
		}

		int getConcurrencyLevel() {
			return ConcurrencyLevel;
		}

		int getInitialCapacity() {
			return InitialCapacity;
		}

		@Override
		public void Parse(Element self) {
			String attr = self.getAttribute("ConcurrencyLevel");
			if (attr.length() > 0)
				ConcurrencyLevel = Integer.parseInt(attr);
			if (ConcurrencyLevel < Runtime.getRuntime().availableProcessors())
				ConcurrencyLevel = Runtime.getRuntime().availableProcessors();

			attr = self.getAttribute("InitialCapacity");
			if (attr.length() > 0)
				InitialCapacity = Integer.parseInt(attr);
			if (InitialCapacity < 31)
				InitialCapacity = 31;
		}
	}

	private GlobalCacheManagerServer() {
	}

	public void Start(InetAddress ipaddress, int port) throws Throwable {
		Start(ipaddress, port, null);
	}

	public synchronized void Start(InetAddress ipaddress, int port, Zeze.Config config) throws Throwable {
		if (Server != null)
			return;

		if (config == null)
			config = new Zeze.Config().AddCustomize(Config).LoadAndParse();

		Sessions = new LongConcurrentHashMap<>(4096, 0.75f, Config.getConcurrencyLevel());
		global = new ConcurrentHashMap<>(Config.getInitialCapacity(), 0.75f, Config.getConcurrencyLevel());

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

		ServerSocket = Server.NewServerSocket(ipaddress, port, null);
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
		// 安全性以后加强。
		if (!rpc.Argument.SecureKey.equals("Ok! verify secure.")) {
			rpc.SendResultCode(CleanupErrorSecureKey);
			return 0;
		}

		var session = Sessions.computeIfAbsent(rpc.Argument.ServerId, __ -> new CacheHolder(Config));
		if (session.GlobalCacheManagerHashIndex != rpc.Argument.GlobalCacheManagerHashIndex) {
			// 多点验证
			rpc.SendResultCode(CleanupErrorGlobalCacheManagerHashIndex);
			return 0;
		}

		if (Server.GetSocket(session.SessionId) != null) {
			// 连接存在，禁止cleanup。
			rpc.SendResultCode(CleanupErrorHasConnection);
			return 0;
		}

		// 还有更多的防止出错的手段吗？

		// XXX verify danger
		Task.schedule(5 * 60 * 1000, () -> { // delay 5 mins
			for (var e : session.Acquired.entrySet()) {
				// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
				Release(session, e.getKey(), false);
			}
			rpc.SendResultCode(0);
		});

		return 0;
	}

	private long ProcessLogin(Login rpc) throws Throwable {
		logger.debug("ProcessLogin: {}", rpc);
		var session = Sessions.computeIfAbsent(rpc.Argument.ServerId, __ -> new CacheHolder(Config));
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (session) {
			if (!session.TryBindSocket(rpc.getSender(), rpc.Argument.GlobalCacheManagerHashIndex)) {
				rpc.SendResultCode(LoginBindSocketFail);
				return 0;
			}
			// new login, 比如逻辑服务器重启。release old acquired.
			for (var e : session.Acquired.entrySet()) {
				// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
				Release(session, e.getKey(), false);
			}
			rpc.SendResultCode(0);
		}
		return 0;
	}

	private long ProcessReLogin(ReLogin rpc) {
		logger.debug("ProcessReLogin: {}", rpc);
		var session = Sessions.computeIfAbsent(rpc.Argument.ServerId, __ -> new CacheHolder(Config));
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (session) {
			if (!session.TryBindSocket(rpc.getSender(), rpc.Argument.GlobalCacheManagerHashIndex)) {
				rpc.SendResultCode(ReLoginBindSocketFail);
				return 0;
			}
			rpc.SendResultCode(0);
		}
		return 0;
	}

	private long ProcessNormalClose(NormalClose rpc) throws Throwable {
		var session = (CacheHolder)rpc.getSender().getUserState();
		if (session == null) {
			rpc.SendResultCode(AcquireNotLogin);
			return 0; // not login
		}
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (session) {
			if (!session.TryUnBindSocket(rpc.getSender())) {
				rpc.SendResultCode(NormalCloseUnbindFail);
				return 0;
			}
			for (var e : session.Acquired.entrySet()) {
				// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
				Release(session, e.getKey(), false);
			}
			rpc.SendResultCode(0);
			logger.debug("After NormalClose global.Count={}", global.size());
		}
		return 0;
	}

	private long ProcessAcquireRequest(Acquire rpc) {
		rpc.Result.GlobalTableKey = rpc.Argument.GlobalTableKey;
		rpc.Result.State = rpc.Argument.State; // default success

		if (rpc.getSender().getUserState() == null) {
			rpc.Result.State = StateInvalid;
			rpc.SendResultCode(AcquireNotLogin);
			return 0;
		}
		try {
			switch (rpc.Argument.State) {
			case StateInvalid: // release
				var sender = (CacheHolder)rpc.getSender().getUserState();
				rpc.Result.State = Release(sender, rpc.Argument.GlobalTableKey, true);
				rpc.SendResult();
				return 0;

			case StateShare:
				return AcquireShare(rpc);

			case StateModify:
				return AcquireModify(rpc);

			default:
				rpc.Result.State = StateInvalid;
				rpc.SendResultCode(AcquireErrorState);
				return 0;
			}
		} catch (Throwable ex) {
			logger.error("ProcessAcquireRequest", ex);
			rpc.Result.State = StateInvalid;
			rpc.SendResultCode(AcquireException);
		}
		return 0;
	}

	private int Release(CacheHolder sender, GlobalTableKey gkey, boolean noWait) throws InterruptedException {
		while (true) {
			CacheState cs = global.computeIfAbsent(gkey, __ -> new CacheState());
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (cs) {
				if (cs.AcquireStatePending == StateRemoved) {
					// 这个是不可能的，因为有Release请求进来意味着肯定有拥有者(share or modify)，此时不可能进入StateRemoved。
					continue;
				}

				while (cs.AcquireStatePending != StateInvalid && cs.AcquireStatePending != StateRemoved) {
					switch (cs.AcquireStatePending) {
					case StateShare:
					case StateModify:
						logger.debug("Release 0 {} {} {}", sender, gkey, cs);
						if (noWait)
							return cs.GetSenderCacheState(sender);
						break;
					case StateRemoving:
						// release 不会导致死锁，等待即可。
						break;
					}
					cs.wait();
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
					global.remove(gkey);
				} else
					cs.AcquireStatePending = StateInvalid;
				sender.Acquired.remove(gkey);
				cs.notifyAll();
				return cs.GetSenderCacheState(sender);
			}
		}
	}

	private int AcquireShare(Acquire rpc) throws InterruptedException {
		CacheHolder sender = (CacheHolder)rpc.getSender().getUserState();
		while (true) {
			CacheState cs = global.computeIfAbsent(rpc.Argument.GlobalTableKey, __ -> new CacheState());
			synchronized (cs) {
				if (cs.AcquireStatePending == StateRemoved)
					continue;

				if (cs.Modify != null && cs.Share.size() != 0)
					throw new IllegalStateException("CacheState state error");

				while (cs.AcquireStatePending != StateInvalid && cs.AcquireStatePending != StateRemoved) {
					switch (cs.AcquireStatePending) {
					case StateShare:
						if (cs.Modify == null)
							throw new IllegalStateException("CacheState state error");
						if (cs.Modify == sender) {
							logger.debug("1 {} {} {}", sender, rpc.Argument.State, cs);
							rpc.Result.State = StateInvalid;
							rpc.Result.GlobalSerialId = cs.GlobalSerialId;
							rpc.SendResultCode(AcquireShareDeadLockFound);
							return 0;
						}
						break;
					case StateModify:
						if (cs.Modify == sender || cs.Share.contains(sender)) {
							logger.debug("2 {} {} {}", sender, rpc.Argument.State, cs);
							rpc.Result.State = StateInvalid;
							rpc.Result.GlobalSerialId = cs.GlobalSerialId;
							rpc.SendResultCode(AcquireShareDeadLockFound);
							return 0;
						}
						break;
					case StateRemoving:
						break;
					}
					logger.debug("3 {} {} {}", sender, rpc.Argument.State, cs);
					cs.wait();
					if (cs.Modify != null && cs.Share.size() != 0)
						throw new IllegalStateException("CacheState state error");
				}
				if (cs.AcquireStatePending == StateRemoved)
					continue; // concurrent release

				cs.AcquireStatePending = StateShare;
				cs.GlobalSerialId = SerialIdGenerator.incrementAndGet();

				if (cs.Modify != null) {
					if (cs.Modify == sender) {
						// 已经是Modify又申请，可能是sender异常关闭，
						// 又重启连上。更新一下。应该是不需要的。
						sender.Acquired.put(rpc.Argument.GlobalTableKey, StateModify);
						cs.AcquireStatePending = StateInvalid;
						logger.debug("4 {} {} {}", sender, rpc.Argument.State, cs);
						rpc.Result.State = StateModify;
						rpc.Result.GlobalSerialId = cs.GlobalSerialId;
						rpc.SendResultCode(AcquireShareAlreadyIsModify);
						return 0;
					}

					OutObject<Integer> reduceResultState = new OutObject<>(StateReduceNetError); // 默认网络错误。
					if (cs.Modify.Reduce(rpc.Argument.GlobalTableKey, StateInvalid, cs.GlobalSerialId, p -> {
						var r = (Reduce)p;
						reduceResultState.Value = r.isTimeout() ? StateReduceRpcTimeout : r.Result.State;
						synchronized (cs) {
							cs.notifyAll();
						}
						return 0;
					})) {
						logger.debug("5 {} {} {}", sender, rpc.Argument.State, cs);
						cs.wait();
					}
					switch (reduceResultState.Value) {
					case StateShare:
						cs.Modify.Acquired.put(rpc.Argument.GlobalTableKey, StateShare);
						cs.Share.add(cs.Modify); // 降级成功。
						break;

					case StateInvalid:
						// 降到了 Invalid，此时就不需要加入 Share 了。
						cs.Modify.Acquired.remove(rpc.Argument.GlobalTableKey);
						break;

					default:
						// 包含协议返回错误的值的情况。
						// case StateReduceRpcTimeout: // 11
						// case StateReduceException: // 12
						// case StateReduceNetError: // 13
						cs.AcquireStatePending = StateInvalid;
						cs.notifyAll();
						logger.error("XXX 8 {} {} {}", sender, rpc.Argument.State, cs);
						rpc.Result.State = StateInvalid;
						rpc.Result.GlobalSerialId = cs.GlobalSerialId;
						rpc.SendResultCode(AcquireShareFailed);
						return 0;
					}

					sender.Acquired.put(rpc.Argument.GlobalTableKey, StateShare);
					cs.Modify = null;
					cs.Share.add(sender);
					cs.AcquireStatePending = StateInvalid;
					cs.notifyAll();
					logger.debug("6 {} {} {}", sender, rpc.Argument.State, cs);
					rpc.Result.GlobalSerialId = cs.GlobalSerialId;
					rpc.SendResult();
					return 0;
				}

				sender.Acquired.put(rpc.Argument.GlobalTableKey, StateShare);
				cs.Share.add(sender);
				cs.AcquireStatePending = StateInvalid;
				cs.notifyAll();
				logger.debug("7 {} {} {}", sender, rpc.Argument.State, cs);
				rpc.Result.GlobalSerialId = cs.GlobalSerialId;
				rpc.SendResult();
				return 0;
			}
		}
	}

	private int AcquireModify(Acquire rpc) throws InterruptedException {
		CacheHolder sender = (CacheHolder)rpc.getSender().getUserState();
		while (true) {
			CacheState cs = global.computeIfAbsent(rpc.Argument.GlobalTableKey, __ -> new CacheState());
			synchronized (cs) {
				if (cs.AcquireStatePending == StateRemoved)
					continue;

				if (cs.Modify != null && cs.Share.size() != 0)
					throw new IllegalStateException("CacheState state error");

				while (cs.AcquireStatePending != StateInvalid && cs.AcquireStatePending != StateRemoved) {
					switch (cs.AcquireStatePending) {
					case StateShare:
						if (cs.Modify == null)
							throw new IllegalStateException("CacheState state error");

						if (cs.Modify == sender) {
							logger.debug("1 {} {} {}", sender, rpc.Argument.State, cs);
							rpc.Result.State = StateInvalid;
							rpc.Result.GlobalSerialId = cs.GlobalSerialId;
							rpc.SendResultCode(AcquireModifyDeadLockFound);
							return 0;
						}
						break;
					case StateModify:
						if (cs.Modify == sender || cs.Share.contains(sender)) {
							logger.debug("2 {} {} {}", sender, rpc.Argument.State, cs);
							rpc.Result.State = StateInvalid;
							rpc.Result.GlobalSerialId = cs.GlobalSerialId;
							rpc.SendResultCode(AcquireModifyDeadLockFound);
							return 0;
						}
						break;
					case StateRemoving:
						break;
					}
					logger.debug("3 {} {} {}", sender, rpc.Argument.State, cs);
					cs.wait();
					if (cs.Modify != null && cs.Share.size() != 0)
						throw new IllegalStateException("CacheState state error");
				}
				if (cs.AcquireStatePending == StateRemoved)
					continue; // concurrent release

				cs.AcquireStatePending = StateModify;
				cs.GlobalSerialId = SerialIdGenerator.incrementAndGet();

				if (cs.Modify != null) {
					if (cs.Modify == sender) {
						logger.debug("4 {} {} {}", sender, rpc.Argument.State, cs);
						// 已经是Modify又申请，可能是sender异常关闭，又重启连上。
						// 更新一下。应该是不需要的。
						sender.Acquired.put(rpc.Argument.GlobalTableKey, StateModify);
						cs.AcquireStatePending = StateInvalid;
						cs.notifyAll();
						rpc.Result.GlobalSerialId = cs.GlobalSerialId;
						rpc.SendResultCode(AcquireModifyAlreadyIsModify);
						return 0;
					}

					OutObject<Integer> reduceResultState = new OutObject<>(StateReduceNetError); // 默认网络错误。
					if (cs.Modify.Reduce(rpc.Argument.GlobalTableKey, StateInvalid, cs.GlobalSerialId, p -> {
						var r = (Reduce)p;
						reduceResultState.Value = r.isTimeout() ? StateReduceRpcTimeout : r.Result.State;
						synchronized (cs) {
							cs.notifyAll();
						}
						return 0;
					})) {
						logger.debug("5 {} {} {}", sender, rpc.Argument.State, cs);
						cs.wait();
					}

					//noinspection SwitchStatementWithTooFewBranches
					switch (reduceResultState.Value) {
					case StateInvalid:
						cs.Modify.Acquired.remove(rpc.Argument.GlobalTableKey);
						break; // reduce success

					default:
						// case StateReduceRpcTimeout: // 11
						// case StateReduceException: // 12
						// case StateReduceNetError: // 13
						cs.AcquireStatePending = StateInvalid;
						cs.notifyAll();
						logger.error("XXX 9 {} {} {}", sender, rpc.Argument.State, cs);
						rpc.Result.State = StateInvalid;
						rpc.Result.GlobalSerialId = cs.GlobalSerialId;
						rpc.SendResultCode(AcquireModifyFailed);
						return 0;
					}

					sender.Acquired.put(rpc.Argument.GlobalTableKey, StateModify);
					cs.Modify = sender;
					cs.Share.remove(sender);
					cs.AcquireStatePending = StateInvalid;
					cs.notifyAll();
					logger.debug("6 {} {} {}", sender, rpc.Argument.State, cs);
					rpc.Result.GlobalSerialId = cs.GlobalSerialId;
					rpc.SendResult();
					return 0;
				}

				ArrayList<KV<CacheHolder, Reduce>> reducePending = new ArrayList<>();
				HashSet<CacheHolder> reduceSucceed = new HashSet<>();
				boolean senderIsShare = false;
				// 先把降级请求全部发送给出去。
				for (CacheHolder c : cs.Share) {
					if (c == sender) {
						// 申请者不需要降级，直接加入成功。
						senderIsShare = true;
						reduceSucceed.add(sender);
						continue;
					}
					Reduce reduce = c.ReduceWaitLater(rpc.Argument.GlobalTableKey, cs.GlobalSerialId);
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
				if (!cs.Share.isEmpty() && (!senderIsShare || reducePending.size() != 0)) {
					Task.run(() -> {
						// 一个个等待是否成功。WaitAll 碰到错误不知道怎么处理的，
						// 应该也会等待所有任务结束（包括错误）。
						for (var reduce : reducePending) {
							try {
								reduce.getValue().getFuture().Wait();
								if (reduce.getValue().Result.State == StateInvalid)
									reduceSucceed.add(reduce.getKey());
								else
									reduce.getKey().SetError();
							} catch (Throwable ex) {
								reduce.getKey().SetError();
								// 等待失败不再看作成功。
								logger.error(String.format("Reduce %s AcquireState=%d CacheState=%s arg=%s",
										sender, rpc.Argument.State, cs, reduce.getValue().Argument), ex);
							}
						}
						synchronized (cs) {
							// 需要唤醒等待任务结束的，但没法指定，只能全部唤醒。
							cs.notifyAll();
						}
					}, "GlobalCacheManager.AcquireModify.WaitReduce");
					logger.debug("7 {} {} {}", sender, rpc.Argument.State, cs);
					cs.wait();
				}

				// 移除成功的。
				for (CacheHolder succeed : reduceSucceed) {
					if (succeed != sender) {
						// sender 不移除：
						// 1. 如果申请成功，后面会更新到Modify状态。
						// 2. 如果申请不成功，恢复 cs.Share，保持 Acquired 不变。
						succeed.Acquired.remove(rpc.Argument.GlobalTableKey);
					}
					cs.Share.remove(succeed);
				}
				// 如果前面降级发生中断(break)，这里就不会为0。
				if (cs.Share.isEmpty()) {
					sender.Acquired.put(rpc.Argument.GlobalTableKey, StateModify);
					cs.Modify = sender;
					cs.AcquireStatePending = StateInvalid;
					cs.notifyAll();
					logger.debug("8 {} {} {}", sender, rpc.Argument.State, cs);
					rpc.Result.GlobalSerialId = cs.GlobalSerialId;
					rpc.SendResult();
				} else {
					// senderIsShare 在失败的时候，Acquired 没有变化，不需要更新。
					// 失败了，要把原来是share的sender恢复。先这样吧。
					if (senderIsShare)
						cs.Share.add(sender);
					cs.AcquireStatePending = StateInvalid;
					cs.notifyAll();
					logger.error("XXX 10 {} {} {}", sender, rpc.Argument.State, cs);
					rpc.Result.State = StateInvalid;
					rpc.Result.GlobalSerialId = cs.GlobalSerialId;
					rpc.SendResultCode(AcquireModifyFailed);
				}
				// 很好，网络失败不再看成成功，发现除了加break，
				// 其他处理已经能包容这个改动，都不用动。
				return 0;
			}
		}
	}

	static final class CacheState {
		CacheHolder Modify;
		int AcquireStatePending = StateInvalid;
		long GlobalSerialId;
		final HashSet<CacheHolder> Share = new HashSet<>();

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

	static final class CacheHolder {
		static final long ForbidPeriod = 10 * 1000; // 10 seconds

		long SessionId;
		int GlobalCacheManagerHashIndex;
		final ConcurrentHashMap<GlobalTableKey, Integer> Acquired;
		private long LastErrorTime;

		CacheHolder(GCMConfig config) {
			Acquired = new ConcurrentHashMap<>(config.getInitialCapacity(), 0.75f, config.getConcurrencyLevel());
		}

		boolean TryBindSocket(AsyncSocket newSocket, int _GlobalCacheManagerHashIndex) {
			if (newSocket.getUserState() != null)
				return false; // 不允许再次绑定。Login Or ReLogin 只能发一次。

			var socket = Instance.Server.GetSocket(SessionId);
			if (socket == null) {
				// old socket not exist or has lost.
				SessionId = newSocket.getSessionId();
				newSocket.setUserState(this);
				GlobalCacheManagerHashIndex = _GlobalCacheManagerHashIndex;
				return true;
			}
			// 每个AutoKeyLocalId只允许一个实例，已经存在了以后，旧的实例上有状态，阻止新的实例登录成功。
			return false;
		}

		boolean TryUnBindSocket(AsyncSocket oldSocket) {
			// 这里检查比较严格，但是这些检查应该都不会出现。

			if (oldSocket.getUserState() != this)
				return false; // not bind to this

			if (Instance.Server.GetSocket(SessionId) != oldSocket)
				return false; // not same socket

			SessionId = 0;
			return true;
		}

		@Override
		public String toString() {
			return String.valueOf(SessionId);
		}

		boolean Reduce(GlobalTableKey gkey, @SuppressWarnings("SameParameterValue") int state, long globalSerialId,
					   ProtocolHandle<Rpc<Param2, Param2>> response) {
			try {
				synchronized (this) {
					if (System.currentTimeMillis() - LastErrorTime < ForbidPeriod)
						return false;
				}
				AsyncSocket peer = Instance.Server.GetSocket(SessionId);
				if (peer != null && new Reduce(gkey, state, globalSerialId).Send(peer, response, 10000))
					return true;
				logger.warn("Send Reduce failed. SessionId={}, peer={}, gkey={}, state={}, globalSerialId={}",
						SessionId, peer, gkey, state, globalSerialId);
			} catch (Exception ex) {
				// 这里的异常只应该是网络发送异常。
				logger.error("ReduceWaitLater Exception " + gkey, ex);
			}
			SetError();
			return false;
		}

		synchronized void SetError() {
			long now = System.currentTimeMillis();
			if (now - LastErrorTime > ForbidPeriod)
				LastErrorTime = now;
		}

		/**
		 * 返回null表示发生了网络错误，或者应用服务器已经关闭。
		 */
		Reduce ReduceWaitLater(GlobalTableKey gkey, long globalSerialId) {
			try {
				synchronized (this) {
					if (System.currentTimeMillis() - LastErrorTime < ForbidPeriod)
						return null;
				}
				AsyncSocket peer = Instance.Server.GetSocket(SessionId);
				if (peer != null) {
					Reduce reduce = new Reduce(gkey, StateInvalid, globalSerialId);
					reduce.SendForWait(peer, 10000);
					return reduce;
				}
			} catch (Throwable ex) {
				// 这里的异常只应该是网络发送异常。
				logger.error("ReduceWaitLater Exception " + gkey, ex);
			}
			SetError();
			return null;
		}
	}

	static final class ServerService extends Service {
		ServerService(Zeze.Config config) throws Throwable {
			super("GlobalCacheManager", config);
		}

		@Override
		public void OnSocketAccept(AsyncSocket so) throws Throwable {
			// so.UserState = new CacheHolder(so.SessionId); // Login ReLogin 的时候初始化。
			super.OnSocketAccept(so);
		}

		@Override
		public void OnSocketClose(AsyncSocket so, Throwable e) throws Throwable {
			var session = (CacheHolder)so.getUserState();
			if (session != null)
				session.TryUnBindSocket(so); // unbind when login
			super.OnSocketClose(so, e);
		}
	}

	public static void main(String[] args) throws Throwable {
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
