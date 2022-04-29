package Zeze.Services;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Arch.RedirectFuture;
import Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
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
import Zeze.Util.Action0;
import Zeze.Util.Action1;
import Zeze.Util.AsyncLock;
import Zeze.Util.KV;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Task;
import Zeze.Util.ThreadFactoryWithName;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.w3c.dom.Element;

public final class GlobalCacheManagerAsyncServer implements GlobalCacheManagerConst {
	static {
		System.setProperty("log4j.configurationFile", "log4j2.xml");
		((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger().setLevel(Level.INFO);
	}

	private static final Logger logger = LogManager.getLogger(GlobalCacheManagerAsyncServer.class);
	private static final GlobalCacheManagerAsyncServer Instance = new GlobalCacheManagerAsyncServer();

	public static GlobalCacheManagerAsyncServer getInstance() {
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

	private static final class GCMConfig implements Zeze.Config.ICustomize {
		// 设置了这么大，开始使用后，大概会占用700M的内存，作为全局服务器，先这么大吧。
		// 尽量不重新调整ConcurrentHashMap。
		private int InitialCapacity = 10_000_000;

		@Override
		public String getName() {
			return "GlobalCacheManager";
		}

		int getInitialCapacity() {
			return InitialCapacity;
		}

		@Override
		public void Parse(Element self) {
			var attr = self.getAttribute("InitialCapacity");
			if (!attr.isBlank())
				InitialCapacity = Integer.parseInt(attr);
			if (InitialCapacity < 31)
				InitialCapacity = 31;
		}
	}

	private GlobalCacheManagerAsyncServer() {
	}

	public void Start(InetAddress ipaddress, int port) throws Throwable {
		Start(ipaddress, port, null);
	}

	public synchronized void Start(InetAddress ipaddress, int port, Zeze.Config config) throws Throwable {
		if (Server != null)
			return;

		if (config == null)
			config = new Zeze.Config().AddCustomize(Config).LoadAndParse();

		Sessions = new LongConcurrentHashMap<>(4096);
		global = new ConcurrentHashMap<>(Config.getInitialCapacity());

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

		var session = Sessions.computeIfAbsent(rpc.Argument.ServerId, __ -> new CacheHolder());
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
		logger.debug("ProcessLogin: {}", rpc);
		var session = Sessions.computeIfAbsent(rpc.Argument.ServerId, __ -> new CacheHolder());
		if (!session.TryBindSocket(rpc.getSender(), rpc.Argument.GlobalCacheManagerHashIndex)) {
			rpc.SendResultCode(LoginBindSocketFail);
			return 0;
		}
		// new login, 比如逻辑服务器重启。release old acquired.
		var allReleaseFuture = new CountDownFuture();
		for (var e : session.Acquired.entrySet()) {
			// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
			ReleaseAsync(session, e.getKey(), allReleaseFuture.createOne());
		}
		allReleaseFuture.then(__ -> rpc.SendResultCode(0));
		return 0;
	}

	private long ProcessReLogin(ReLogin rpc) {
		logger.debug("ProcessReLogin: {}", rpc);
		var session = Sessions.computeIfAbsent(rpc.Argument.ServerId, __ -> new CacheHolder());
		if (!session.TryBindSocket(rpc.getSender(), rpc.Argument.GlobalCacheManagerHashIndex)) {
			rpc.SendResultCode(ReLoginBindSocketFail);
			return 0;
		}
		rpc.SendResultCode(0);
		return 0;
	}

	private long ProcessNormalClose(NormalClose rpc) throws Throwable {
		var session = (CacheHolder)rpc.getSender().getUserState();
		if (session == null) {
			rpc.SendResultCode(AcquireNotLogin);
			return 0; // not login
		}
		if (!session.TryUnBindSocket(rpc.getSender())) {
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
			logger.debug("After NormalClose global.Count={}", global.size());
		});
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
				return 0;
			}
		} catch (Throwable ex) {
			logger.error("ProcessAcquireRequest", ex);
			rpc.Result.State = StateInvalid;
			rpc.SendResultCode(AcquireException);
		}
		return 0;
	}

	private static final class CountDownFuture extends RedirectFuture<Object> {
		private final AtomicInteger counter = new AtomicInteger(1);

		CountDownFuture createOne() {
			counter.incrementAndGet();
			return this;
		}

		void finishOne() {
			if (counter.decrementAndGet() == 0)
				SetResult(null);
		}

		@Override
		public RedirectFuture<Object> then(Action1<Object> onResult) throws Throwable {
			finishOne();
			return super.then(onResult);
		}
	}

	private void ReleaseAsync(CacheHolder sender, GlobalTableKey gkey, CountDownFuture future) {
		var cs = global.computeIfAbsent(gkey, __ -> new CacheState());
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
				ReleaseAsync(sender, gkey, future); // retry
				return;
			}

			if (cs.AcquireStatePending != StateInvalid && cs.AcquireStatePending != StateRemoved) {
				switch (cs.AcquireStatePending) {
				case StateShare:
				case StateModify:
					logger.debug("Release 0 {} {} {}", sender, gkey, cs);
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
				ReleaseAsync(sender, gkey, future); // retry
				return;
			}
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
			cs.lock.notifyAllWait();
			future.finishOne();
		});
	}

	private void ReleaseAsync(Acquire rpc) {
		var cs = global.computeIfAbsent(rpc.Argument.GlobalTableKey, __ -> new CacheState());
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
			var gkey = rpc.Argument.GlobalTableKey;
			if (cs.AcquireStatePending != StateInvalid && cs.AcquireStatePending != StateRemoved) {
				switch (cs.AcquireStatePending) {
				case StateShare:
				case StateModify:
					logger.debug("Release 0 {} {} {}", sender, gkey, cs);
					rpc.Result.State = cs.GetSenderCacheState(sender);
					rpc.SendResult();
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
				global.remove(gkey);
			} else
				cs.AcquireStatePending = StateInvalid;
			sender.Acquired.remove(gkey);
			cs.lock.notifyAllWait();
			rpc.Result.State = cs.GetSenderCacheState(sender);
			rpc.SendResult();
		});
	}

	private void AcquireShareAsync(Acquire rpc) {
		var cs = global.computeIfAbsent(rpc.Argument.GlobalTableKey, __ -> new CacheState());
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
							logger.debug("1 {} {} {}", sender, rpc.Argument.State, cs);
							rpc.Result.State = StateInvalid;
							rpc.Result.GlobalSerialId = cs.GlobalSerialId;
							rpc.SendResultCode(AcquireShareDeadLockFound);
							return;
						}
						break;
					case StateModify:
						if (cs.Modify == sender || cs.Share.contains(sender)) {
							logger.debug("2 {} {} {}", sender, rpc.Argument.State, cs);
							rpc.Result.State = StateInvalid;
							rpc.Result.GlobalSerialId = cs.GlobalSerialId;
							rpc.SendResultCode(AcquireShareDeadLockFound);
							return;
						}
						break;
					case StateRemoving:
						break;
					}
					logger.debug("3 {} {} {}", sender, rpc.Argument.State, cs);
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
				cs.GlobalSerialId = SerialIdGenerator.incrementAndGet();
			}

			if (cs.Modify != null || state.stage == 2) {
				if (state.stage != 2) {
					if (cs.Modify == sender) {
						// 已经是Modify又申请，可能是sender异常关闭，
						// 又重启连上。更新一下。应该是不需要的。
						sender.Acquired.put(rpc.Argument.GlobalTableKey, StateModify);
						cs.AcquireStatePending = StateInvalid;
						logger.debug("4 {} {} {}", sender, rpc.Argument.State, cs);
						rpc.Result.State = StateModify;
						rpc.Result.GlobalSerialId = cs.GlobalSerialId;
						rpc.SendResultCode(AcquireShareAlreadyIsModify);
						return;
					}

					state.reduceResultState = StateReduceNetError; // 默认网络错误。。
					if (cs.Modify.Reduce(rpc.Argument.GlobalTableKey, StateInvalid, cs.GlobalSerialId, r -> {
						state.reduceResultState = r.isTimeout() ? StateReduceRpcTimeout : r.Result.State;
						cs.lock.enter(cs.lock::notifyAllWait);
						return 0;
					})) {
						logger.debug("5 {} {} {}", sender, rpc.Argument.State, cs);
						state.stage = 2;
						cs.lock.leaveAndWaitNotify();
						return;
					}
				}

				switch (state.reduceResultState) {
				case StateShare:
					assert cs.Modify != null;
					cs.Modify.Acquired.put(rpc.Argument.GlobalTableKey, StateShare);
					cs.Share.add(cs.Modify); // 降级成功。
					break;

				case StateInvalid:
					// 降到了 Invalid，此时就不需要加入 Share 了。
					assert cs.Modify != null;
					cs.Modify.Acquired.remove(rpc.Argument.GlobalTableKey);
					break;

				default:
					// 包含协议返回错误的值的情况。
					// case StateReduceRpcTimeout: // 11
					// case StateReduceException: // 12
					// case StateReduceNetError: // 13
					cs.AcquireStatePending = StateInvalid;
					cs.lock.notifyAllWait();
					logger.error("XXX 8 {} {} {} {}", sender, rpc.Argument.State, cs, state.reduceResultState);
					rpc.Result.State = StateInvalid;
					rpc.Result.GlobalSerialId = cs.GlobalSerialId;
					rpc.SendResultCode(AcquireShareFailed);
					return;
				}

				sender.Acquired.put(rpc.Argument.GlobalTableKey, StateShare);
				cs.Modify = null;
				cs.Share.add(sender);
				cs.AcquireStatePending = StateInvalid;
				cs.lock.notifyAllWait();
				logger.debug("6 {} {} {}", sender, rpc.Argument.State, cs);
				rpc.Result.GlobalSerialId = cs.GlobalSerialId;
				rpc.SendResult();
				return;
			}

			sender.Acquired.put(rpc.Argument.GlobalTableKey, StateShare);
			cs.Share.add(sender);
			cs.AcquireStatePending = StateInvalid;
			cs.lock.notifyAllWait();
			logger.debug("7 {} {} {}", sender, rpc.Argument.State, cs);
			rpc.Result.GlobalSerialId = cs.GlobalSerialId;
			rpc.SendResult();
		});
	}

	private void AcquireModifyAsync(Acquire rpc) {
		var cs = global.computeIfAbsent(rpc.Argument.GlobalTableKey, __ -> new CacheState());
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
							logger.debug("1 {} {} {}", sender, rpc.Argument.State, cs);
							rpc.Result.State = StateInvalid;
							rpc.Result.GlobalSerialId = cs.GlobalSerialId;
							rpc.SendResultCode(AcquireModifyDeadLockFound);
							return;
						}
						break;
					case StateModify:
						if (cs.Modify == sender || cs.Share.contains(sender)) {
							logger.debug("2 {} {} {}", sender, rpc.Argument.State, cs);
							rpc.Result.State = StateInvalid;
							rpc.Result.GlobalSerialId = cs.GlobalSerialId;
							rpc.SendResultCode(AcquireModifyDeadLockFound);
							return;
						}
						break;
					case StateRemoving:
						break;
					}
					logger.debug("3 {} {} {}", sender, rpc.Argument.State, cs);
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
				cs.GlobalSerialId = SerialIdGenerator.incrementAndGet();
			}

			if (cs.Modify != null || state.stage == 2) {
				if (state.stage != 2) {
					if (cs.Modify == sender) {
						logger.debug("4 {} {} {}", sender, rpc.Argument.State, cs);
						// 已经是Modify又申请，可能是sender异常关闭，又重启连上。
						// 更新一下。应该是不需要的。
						sender.Acquired.put(rpc.Argument.GlobalTableKey, StateModify);
						cs.AcquireStatePending = StateInvalid;
						cs.lock.notifyAllWait();
						rpc.Result.GlobalSerialId = cs.GlobalSerialId;
						rpc.SendResultCode(AcquireModifyAlreadyIsModify);
						return;
					}

					state.reduceResultState = StateReduceNetError; // 默认网络错误。
					if (cs.Modify.Reduce(rpc.Argument.GlobalTableKey, StateInvalid, cs.GlobalSerialId, r -> {
						state.reduceResultState = r.isTimeout() ? StateReduceRpcTimeout : r.Result.State;
						cs.lock.enter(cs.lock::notifyAllWait);
						return 0;
					})) {
						logger.debug("5 {} {} {}", sender, rpc.Argument.State, cs);
						state.stage = 2;
						cs.lock.leaveAndWaitNotify();
						return;
					}
				}

				//noinspection SwitchStatementWithTooFewBranches
				switch (state.reduceResultState) {
				case StateInvalid:
					assert cs.Modify != null;
					cs.Modify.Acquired.remove(rpc.Argument.GlobalTableKey);
					break; // reduce success

				default:
					// case StateReduceRpcTimeout: // 11
					// case StateReduceException: // 12
					// case StateReduceNetError: // 13
					cs.AcquireStatePending = StateInvalid;
					cs.lock.notifyAllWait();
					logger.error("XXX 9 {} {} {} {}", sender, rpc.Argument.State, cs, state.reduceResultState);
					rpc.Result.State = StateInvalid;
					rpc.Result.GlobalSerialId = cs.GlobalSerialId;
					rpc.SendResultCode(AcquireModifyFailed);
					return;
				}

				sender.Acquired.put(rpc.Argument.GlobalTableKey, StateModify);
				cs.Modify = sender;
				cs.Share.remove(sender);
				cs.AcquireStatePending = StateInvalid;
				cs.lock.notifyAllWait();
				logger.debug("6 {} {} {}", sender, rpc.Argument.State, cs);
				rpc.Result.GlobalSerialId = cs.GlobalSerialId;
				rpc.SendResult();
				return;
			}

			var reducePending = new ArrayList<KV<CacheHolder, Reduce>>();
			var reduceSucceed = new HashSet<CacheHolder>();
			var allReduceFuture = new CountDownFuture();
			var senderIsShareTmp = false;
			var reduceFailed = false;
			// 先把降级请求全部发送给出去。
			for (CacheHolder c : cs.Share) {
				if (reduceFailed)
					continue;
				if (c == sender) {
					// 申请者不需要降级，直接加入成功。
					senderIsShareTmp = true;
					reduceSucceed.add(sender);
					continue;
				}
				allReduceFuture.createOne();
				Reduce reduce = c.ReduceWaitLater(rpc.Argument.GlobalTableKey, cs.GlobalSerialId, r -> {
					allReduceFuture.finishOne();
					return 0;
				});
				if (reduce == null) {
					// 网络错误不再认为成功。整个降级失败，要中断降级。
					// 已经发出去的降级请求要等待并处理结果。后面处理。
					reduceFailed = true;
					allReduceFuture.finishOne();
				} else
					reducePending.add(KV.Create(c, reduce));
			}
			boolean senderIsShare = senderIsShareTmp;

			Action0 lastStage = () -> {
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
					cs.lock.notifyAllWait();
					logger.debug("8 {} {} {}", sender, rpc.Argument.State, cs);
					rpc.Result.GlobalSerialId = cs.GlobalSerialId;
					rpc.SendResult();
				} else {
					// senderIsShare 在失败的时候，Acquired 没有变化，不需要更新。
					// 失败了，要把原来是share的sender恢复。先这样吧。
					if (senderIsShare)
						cs.Share.add(sender);
					cs.AcquireStatePending = StateInvalid;
					cs.lock.notifyAllWait();
					logger.error("XXX 10 {} {} {}", sender, rpc.Argument.State, cs);
					rpc.Result.State = StateInvalid;
					rpc.Result.GlobalSerialId = cs.GlobalSerialId;
					rpc.SendResultCode(AcquireModifyFailed);
				}
				// 很好，网络失败不再看成成功，发现除了加break，
				// 其他处理已经能包容这个改动，都不用动。
			};

			// 两种情况不需要发reduce
			// 1. share是空的, 可以直接升为Modify
			// 2. sender是share, 而且reducePending的size是0
			if (!cs.Share.isEmpty() && (!senderIsShare || !reducePending.isEmpty())) {
				logger.debug("7 {} {} {}", sender, rpc.Argument.State, cs);
				allReduceFuture.then(__ -> {
					// 一个个等待是否成功。WaitAll 碰到错误不知道怎么处理的，
					// 应该也会等待所有任务结束（包括错误）。
					for (var e : reducePending) {
						var cacheHolder = e.getKey();
						var reduce = e.getValue();
						try {
							if (reduce.Result.State == StateInvalid)
								reduceSucceed.add(cacheHolder);
							else
								cacheHolder.SetError();
						} catch (Throwable ex) {
							cacheHolder.SetError();
							// 等待失败不再看作成功。
							logger.error(String.format("Reduce %s AcquireState=%d CacheState=%s arg=%s",
									rpc.getSender().getUserState(), rpc.Argument.State, cs, reduce.Argument), ex);
						}
					}
					cs.lock.enter(lastStage); // 需要唤醒等待任务结束的，但没法指定，只能全部唤醒。
				});
			} else
				lastStage.run();
		});
	}

	private static final class CacheState {
		CacheHolder Modify;
		int AcquireStatePending = StateInvalid;
		long GlobalSerialId;
		final HashSet<CacheHolder> Share = new HashSet<>();
		final AsyncLock lock = new AsyncLock();

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
		static final long ForbidPeriod = 10 * 1000; // 10 seconds

		long SessionId;
		int GlobalCacheManagerHashIndex;
		final ConcurrentHashMap<GlobalTableKey, Integer> Acquired;
		private volatile long LastErrorTime;

		CacheHolder() {
			Acquired = new ConcurrentHashMap<>(Instance.Config.getInitialCapacity());
		}

		synchronized boolean TryBindSocket(AsyncSocket newSocket, int _GlobalCacheManagerHashIndex) {
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

		boolean Reduce(GlobalTableKey gkey, @SuppressWarnings("SameParameterValue") int state, long globalSerialId,
					   ProtocolHandle<Rpc<Param2, Param2>> response) {
			try {
				if (System.currentTimeMillis() - LastErrorTime < ForbidPeriod)
					return false;
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

		void SetError() {
			long now = System.currentTimeMillis();
			if (now - LastErrorTime > ForbidPeriod)
				LastErrorTime = now;
		}

		/**
		 * 返回null表示发生了网络错误，或者应用服务器已经关闭。
		 */
		Reduce ReduceWaitLater(GlobalTableKey gkey, long globalSerialId, ProtocolHandle<Rpc<Param2, Param2>> handle) {
			try {
				if (System.currentTimeMillis() - LastErrorTime < ForbidPeriod)
					return null;
				AsyncSocket peer = Instance.Server.GetSocket(SessionId);
				if (peer != null) {
					Reduce reduce = new Reduce(gkey, StateInvalid, globalSerialId);
					reduce.Send(peer, handle, 10000);
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

	private static final class ServerService extends Service {
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

		@Override
		public <P extends Protocol<?>> void DispatchProtocol(P p, ProtocolFactoryHandle<P> factoryHandle) {
			var handle = factoryHandle.Handle;
			if (handle != null) {
				try {
					handle.handle(p); // 所有协议处理几乎无阻塞,可放心直接跑在IO线程上
				} catch (Throwable e) {
					logger.error("handle protocol exception:", e);
				}
			} else
				logger.warn("DispatchProtocol: Protocol Handle Not Found: {}", p);
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
			}
		}

		if (threadCount < 1)
			threadCount = Runtime.getRuntime().availableProcessors();
		Task.initThreadPool((ThreadPoolExecutor)
						Executors.newFixedThreadPool(threadCount, new ThreadFactoryWithName("ZezeTaskPool")),
				Executors.newSingleThreadScheduledExecutor(new ThreadFactoryWithName("ZezeScheduledPool")));

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
