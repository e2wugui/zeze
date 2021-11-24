package Zeze.Services;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import Zeze.Serialize.*;
import Zeze.Net.*;
import Zeze.Services.GlobalCacheManager.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class GlobalCacheManagerServer {
	public static final int StateInvalid = 0;
	public static final int StateShare = 1;
	public static final int StateModify = 2;
	public static final int StateRemoved = -1; // 从容器(Cache或Global)中删除后设置的状态，最后一个状态。
	public static final int StateReduceRpcTimeout = -2; // 用来表示 reduce 超时失败。不是状态。
	public static final int StateReduceException = -3; // 用来表示 reduce 异常失败。不是状态。
	public static final int StateReduceNetError = -4; // 用来表示 reduce 网络失败。不是状态。
	public static final int StateReduceDuplicate = -5; // 用来表示重复的 reduce。错误报告，不是状态。

	public static final int AcquireShareDeadLockFound = 1;
	public static final int AcquireShareAlreadyIsModify = 2;
	public static final int AcquireModifyDeadLockFound = 3;
	public static final int AcquireErrorState = 4;
	public static final int AcquireModifyAlreadyIsModify = 5;
	public static final int AcquireShareFaild = 6;
	public static final int AcquireModifyFaild = 7;
	public static final int AcquireException = 8;

	public static final int ReduceErrorState = 11;
	public static final int ReduceShareAlreadyIsInvalid = 12;
	public static final int ReduceShareAlreadyIsShare = 13;
	public static final int ReduceInvalidAlreadyIsInvalid = 14;

	public static final int AcquireNotLogin = 20;

	public static final int CleanupErrorSecureKey = 30;
	public static final int CleanupErrorGlobalCacheManagerHashIndex = 31;
	public static final int CleanupErrorHasConnection = 32;

	public static final int ReLoginBindSocketFail = 40;

	public static final int NormalCloseUnbindFail = 50;

	public static final int LoginBindSocketFail = 60;

	static{
		System.setProperty("log4j.configurationFile", "log4j2.xml");
	}

	static final Logger logger = LogManager.getLogger(GlobalCacheManagerServer.class);
	private final static GlobalCacheManagerServer Instance = new GlobalCacheManagerServer();
	public static GlobalCacheManagerServer getInstance() {
		return Instance;
	}
	private ServerService Server;
	public ServerService getServer() {
		return Server;
	}
	private void setServer(ServerService value) {
		Server = value;
	}
	private AsyncSocket ServerSocket;
	public AsyncSocket getServerSocket() {
		return ServerSocket;
	}
	private void setServerSocket(AsyncSocket value) {
		ServerSocket = value;
	}
	private ConcurrentHashMap<GlobalTableKey, CacheState> global;
	private AtomicLong SerialIdGenerator = new AtomicLong();


	/*
	 * 会话。
	 * key是 LogicServer.Id，现在的实现就是Zeze.Config.ServerId。
	 * 在连接建立后收到的Login Or Relogin 中设置。
	 * 每个会话记住分配给自己的GlobalTableKey，用来在正常退出的时候释放。
	 * 每个会话还需要记录该会话的Socket.SessionId。在连接重新建立时更新。
	 * 总是GetOrAdd，不删除。按现在的cache-sync设计，
	 * ServerId 是及其有限的。不会一直增长。
	 * 简化实现。
	 */
	private ConcurrentHashMap<Integer, CacheHolder> Sessions;

	private GlobalCacheManagerServer() {
	}

	public static class GCMConfig implements Zeze.Config.ICustomize {
		public final String getName() {
			return "GlobalCacheManager";
		}

		private int ConcurrencyLevel = 1024;
		public final int getConcurrencyLevel() {
			return ConcurrencyLevel;
		}
		public final void setConcurrencyLevel(int value) {
			ConcurrencyLevel = value;
		}
		// 设置了这么大，开始使用后，大概会占用700M的内存，作为全局服务器，先这么大吧。
		// 尽量不重新调整ConcurrentDictionary。
		private int InitialCapacity = 100000000;
		public final int getInitialCapacity() {
			return InitialCapacity;
		}
		public final void setInitialCapacity(int value) {
			InitialCapacity = value;
		}

		public final void Parse(Element self) {
			String attr;

			attr = self.getAttribute("ConcurrencyLevel");
			if (attr.length() > 0) {
				setConcurrencyLevel(Integer.parseInt(attr));
			}
			if (getConcurrencyLevel() < Runtime.getRuntime().availableProcessors()) {
				setConcurrencyLevel(Runtime.getRuntime().availableProcessors());
			}

			attr = self.getAttribute("InitialCapacity");
			if (attr.length() > 0) {
				setInitialCapacity(Integer.parseInt(attr));
			}
			if (getInitialCapacity() < 31) {
				setInitialCapacity(31);
			}
		}
	}

	private final GCMConfig Config = new GCMConfig();
	public GCMConfig getConfig() {
		return Config;
	}


	public void Start(InetAddress ipaddress, int port) throws Throwable {
		Start(ipaddress, port, null);
	}

	public void Start(InetAddress ipaddress, int port, Zeze.Config config) throws Throwable {
		synchronized (this) {
			if (getServer() != null) {
				return;
			}

			if (null == config) {
				config = new Zeze.Config();
				config.AddCustomize(getConfig());
				config.LoadAndParse();
			}
			Sessions = new ConcurrentHashMap<>(4096, 0.75f, getConfig().getConcurrencyLevel());
			global = new ConcurrentHashMap<> (getConfig().getInitialCapacity(), 0.75f, getConfig().getConcurrencyLevel());

			setServer(new ServerService(config));

			getServer().AddFactoryHandle((new Acquire()).getTypeId(),
					new Service.ProtocolFactoryHandle(Acquire::new, this::ProcessAcquireRequest));

			getServer().AddFactoryHandle((new Reduce()).getTypeId(),
					new Service.ProtocolFactoryHandle(Reduce::new));

			getServer().AddFactoryHandle((new Login()).getTypeId(),
					new Service.ProtocolFactoryHandle(Login::new, this::ProcessLogin));

			getServer().AddFactoryHandle((new ReLogin()).getTypeId(),
					new Service.ProtocolFactoryHandle(ReLogin::new, this::ProcessReLogin));

			getServer().AddFactoryHandle((new NormalClose()).getTypeId(),
					new Service.ProtocolFactoryHandle(NormalClose::new, this::ProcessNormalClose));

			// 临时注册到这里，安全起见应该起一个新的Service，并且仅绑定到 localhost。
			getServer().AddFactoryHandle((new Cleanup()).getTypeId(),
					new Service.ProtocolFactoryHandle(Cleanup::new, this::ProcessCleanup));

			setServerSocket(getServer().NewServerSocket(ipaddress, port, null));
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
	}

	public void Stop() throws Throwable {
		synchronized (this) {
			if (null == getServer()) {
				return;
			}
			getServerSocket().close();
			setServerSocket(null);
			getServer().Stop();
			setServer(null);
		}
	}

	/**
	 报告错误的时候带上相关信息（包括GlobalCacheManager和LogicServer等等）
	 手动Cleanup时，连接正确的服务器执行。
	 */
	private long ProcessCleanup(Zeze.Net.Protocol p) throws Throwable {
		var rpc = (Cleanup)p;

		// 安全性以后加强。
		if (!rpc.Argument.SecureKey.equals("Ok! verify secure.")) {
			rpc.SendResultCode(CleanupErrorSecureKey);
			return 0;
		}

		final var session = Sessions.computeIfAbsent(
				rpc.Argument.AutoKeyLocalId, (key) -> new CacheHolder(getConfig()));
		if (session.GlobalCacheManagerHashIndex != rpc.Argument.GlobalCacheManagerHashIndex) {
			// 多点验证
			rpc.SendResultCode(CleanupErrorGlobalCacheManagerHashIndex);
			return 0;
		}

		if (this.getServer().GetSocket(session.SessionId) != null) {
			// 连接存在，禁止cleanup。
			rpc.SendResultCode(CleanupErrorHasConnection);
			return 0;
		}

		// 还有更多的防止出错的手段吗？

		// XXX verify danger
		Zeze.Util.Task.schedule((ThisTask) -> {
					for (var e : session.getAcquired().entrySet()) {
						// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
						Release(session, e.getKey());
					}
					rpc.SendResultCode(0);
		}, 5 * 60 * 1000); // delay 5 mins

		return 0;
	}

	private long ProcessLogin(Zeze.Net.Protocol p) throws Throwable {
		var rpc = (Login)p;
		var session = Sessions.computeIfAbsent(
				rpc.Argument.ServerId, (key) -> new CacheHolder(getConfig()));
		if (!session.TryBindSocket(p.getSender(), rpc.Argument.GlobalCacheManagerHashIndex)) {
			rpc.SendResultCode(LoginBindSocketFail);
			return 0;
		}
		// new login, 比如逻辑服务器重启。release old acquired.
		for (var e : session.getAcquired().entrySet()) {
			// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
			Release(session, e.getKey());
		}
		rpc.SendResultCode(0);
		return 0;
	}

	private long ProcessReLogin(Zeze.Net.Protocol p) throws Throwable {
		var rpc = (ReLogin)p;
		var session = Sessions.computeIfAbsent(
				rpc.Argument.ServerId, (key) -> new CacheHolder(getConfig()));
		if (!session.TryBindSocket(p.getSender(), rpc.Argument.GlobalCacheManagerHashIndex)) {
			rpc.SendResultCode(ReLoginBindSocketFail);
			return 0;
		}
		rpc.SendResultCode(0);
		return 0;
	}

	private long ProcessNormalClose(Zeze.Net.Protocol p) throws Throwable {
		var rpc = (NormalClose)p;
		var session = (CacheHolder)rpc.getSender().getUserState();
		if (null == session) {
			rpc.SendResultCode(AcquireNotLogin);
			return 0; // not login
		}
		if (!session.TryUnBindSocket(p.getSender())) {
			rpc.SendResultCode(NormalCloseUnbindFail);
			return 0;
		}
		for (var e : session.getAcquired().entrySet()) {
			// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
			Release(session, e.getKey());
		}
		rpc.SendResultCode(0);
		logger.debug("After NormalClose global.Count={}", global.size());
		return 0;
	}

	private long ProcessAcquireRequest(Zeze.Net.Protocol p) throws Throwable {
		Acquire rpc = (Acquire)p;

		rpc.Result.GlobalTableKey = rpc.Argument.GlobalTableKey;
		rpc.Result.State = rpc.Argument.State; // default success

		if (rpc.getSender().getUserState() == null) {
			rpc.Result.State = StateInvalid;
			rpc.SendResultCode(AcquireNotLogin);
			return 0;
		}
		try {
			switch (rpc.Argument.State) {
				case StateInvalid: // realease
					Release((CacheHolder) rpc.getSender().getUserState(), rpc.Argument.GlobalTableKey);
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
			logger.error(ex);
			rpc.Result.State = StateInvalid;
			rpc.SendResultCode(AcquireException);
		}
		return 0;
	}

	private void Release(CacheHolder holder, GlobalTableKey gkey) {
		final CacheState cs = global.computeIfAbsent(gkey, (tabkeKeyNotUsed) -> new CacheState());
		synchronized (cs) {
			if (cs.getModify() == holder) {
				cs.setModify(null);
			}
			cs.getShare().remove(holder); // always try remove

			if (cs.getModify() == null && cs.getShare().isEmpty() && cs.getAcquireStatePending() == StateInvalid) {
				// 安全的从global中删除，没有并发问题。
				cs.setAcquireStatePending(StateRemoved);
				global.remove(gkey);
			}
			holder.getAcquired().remove(gkey);
		}
	}

	private int AcquireShare(Acquire rpc) throws InterruptedException {
		CacheHolder sender = (CacheHolder)rpc.getSender().getUserState();

		while (true) {
			CacheState cs = global.computeIfAbsent(rpc.Argument.GlobalTableKey, (tabkeKeyNotUsed) -> new CacheState());
			synchronized (cs) {

				if (cs.getAcquireStatePending() == StateRemoved) {
					continue;
				}

				if (cs.getModify() != null && cs.getShare().size() > 0) {
					throw new RuntimeException("CacheState state error");
				}

				while (cs.getAcquireStatePending() != StateInvalid) {
					switch (cs.getAcquireStatePending()) {
						case StateShare:
							if (cs.getModify() == null) {
								throw new RuntimeException("CacheState state error");
							}
							if (cs.getModify() == sender) {
								logger.debug("1 {} {} {}", sender, rpc.Argument.State, cs);
								rpc.Result.State = StateInvalid;
								rpc.Result.GlobalSerialId = cs.GlobalSerialId;
								rpc.SendResultCode(AcquireShareDeadLockFound);
								return 0;
							}
							break;
						case StateModify:
							if (cs.getModify() == sender || cs.getShare().contains(sender)) {
								logger.debug("2 {} {} {}", sender, rpc.Argument.State, cs);
								rpc.Result.State = StateInvalid;
								rpc.Result.GlobalSerialId = cs.GlobalSerialId;
								rpc.SendResultCode(AcquireShareDeadLockFound);
								return 0;
							}
							break;
					}
					logger.debug("3 {} {} {}", sender, rpc.Argument.State, cs);
					cs.wait();
					if (cs.getModify() != null && cs.getShare().size() > 0) {
						throw new RuntimeException("CacheState state error");
					}
				}
				cs.setAcquireStatePending(StateShare);
				cs.GlobalSerialId = SerialIdGenerator.incrementAndGet();

				if (cs.getModify() != null) {
					if (cs.getModify() == sender) {
						cs.setAcquireStatePending(StateInvalid);
						logger.debug("4 {} {} {}", sender, rpc.Argument.State, cs);
						rpc.Result.State = StateModify;
						// 已经是Modify又申请，可能是sender异常关闭，
						// 又重启连上。更新一下。应该是不需要的。
						sender.getAcquired().put(rpc.Argument.GlobalTableKey, StateModify);
						rpc.Result.GlobalSerialId = cs.GlobalSerialId;
						rpc.SendResultCode(AcquireShareAlreadyIsModify);
						return 0;
					}

					final var out = new Zeze.Util.OutObject<Reduce>();
					Zeze.Util.Task.Run(() -> {
						out.Value = cs.getModify().Reduce(rpc.Argument.GlobalTableKey, StateShare, cs.GlobalSerialId);
						synchronized (cs) {
							cs.notifyAll();
						}
					}, "GlobalCacheManager.AcquireShare.Reduce");
					logger.debug("5 {} {} {}", sender, rpc.Argument.State, cs);
					cs.wait();
					int stateReduceResult = out.Value.Result.State;
					switch (stateReduceResult) {
						case StateShare:
							cs.getModify().getAcquired().put(rpc.Argument.GlobalTableKey, StateShare);
							cs.getShare().add(cs.getModify()); // 降级成功。
							break;

						case StateInvalid:
							// 降到了 Invalid，此时就不需要加入 Share 了。
							cs.getModify().getAcquired().remove(rpc.Argument.GlobalTableKey);
							break;

						default:
							// 包含协议返回错误的值的情况。
							// case StateReduceRpcTimeout:
							// case StateReduceException:
							// case StateReduceNetError:
							cs.setAcquireStatePending(StateInvalid);
							cs.notify();

							logger.error("XXX 8 {} {} {}", sender, rpc.Argument.State, cs);
							rpc.Result.State = StateInvalid;
							rpc.Result.GlobalSerialId = cs.GlobalSerialId;
							rpc.SendResultCode(AcquireShareFaild);
							return 0;
					}

					cs.setModify(null);
					sender.getAcquired().put(rpc.Argument.GlobalTableKey, StateShare);
					cs.getShare().add(sender);
					cs.setAcquireStatePending(StateInvalid);
					cs.notify();
					logger.debug("6 {} {} {}", sender, rpc.Argument.State, cs);
					rpc.Result.GlobalSerialId = cs.GlobalSerialId;
					rpc.SendResult();
					return 0;
				}

				sender.getAcquired().put(rpc.Argument.GlobalTableKey, StateShare);
				cs.getShare().add(sender);
				cs.setAcquireStatePending(StateInvalid);
				cs.notify();
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
			CacheState cs = global.computeIfAbsent(rpc.Argument.GlobalTableKey, (tabkeKeyNotUsed) -> new CacheState());
			synchronized (cs) {
				if (cs.getAcquireStatePending() == StateRemoved) {
					continue;
				}

				if (cs.getModify() != null && cs.getShare().size() > 0) {
					throw new RuntimeException("CacheState state error");
				}

				while (cs.getAcquireStatePending() != StateInvalid) {
					switch (cs.getAcquireStatePending()) {
						case StateShare:
							if (cs.getModify() == null) {
								throw new RuntimeException("CacheState state error");
							}

							if (cs.getModify() == sender) {
								logger.debug("1 {} {} {}", sender, rpc.Argument.State, cs);
								rpc.Result.State = StateInvalid;
								rpc.Result.GlobalSerialId = cs.GlobalSerialId;
								rpc.SendResultCode(AcquireModifyDeadLockFound);
								return 0;
							}
							break;
						case StateModify:
							if (cs.getModify() == sender || cs.getShare().contains(sender)) {
								logger.debug("2 {} {} {}", sender, rpc.Argument.State, cs);
								rpc.Result.State = StateInvalid;
								rpc.Result.GlobalSerialId = cs.GlobalSerialId;
								rpc.SendResultCode(AcquireModifyDeadLockFound);
								return 0;
							}
							break;
					}
					logger.debug("3 {} {} {}", sender, rpc.Argument.State, cs);
					cs.wait();
					if (cs.getModify() != null && cs.getShare().size() > 0) {
						throw new RuntimeException("CacheState state error");
					}
				}
				cs.setAcquireStatePending(StateModify);
				cs.GlobalSerialId = SerialIdGenerator.incrementAndGet();

				if (cs.getModify() != null) {
					if (cs.getModify() == sender) {
						logger.debug("4 {} {} {}", sender, rpc.Argument.State, cs);
						// 已经是Modify又申请，可能是sender异常关闭，又重启连上。
						// 更新一下。应该是不需要的。
						sender.getAcquired().put(rpc.Argument.GlobalTableKey, StateModify);
						rpc.Result.GlobalSerialId = cs.GlobalSerialId;
						rpc.SendResultCode(AcquireModifyAlreadyIsModify);
						cs.setAcquireStatePending(StateInvalid);
						cs.notify();
						return 0;
					}

					final var out = new Zeze.Util.OutObject<Reduce>();
					Zeze.Util.Task.Run(() -> {
								out.Value = cs.getModify().Reduce(rpc.Argument.GlobalTableKey, StateInvalid, cs.GlobalSerialId);
								synchronized (cs) {
									cs.notifyAll();
								}
					}, "GlobalCacheManager.AcquireModify.Reduce");
					logger.debug("5 {} {} {}", sender, rpc.Argument.State, cs);
					cs.wait();

					int stateReduceResult = out.Value.Result.State;
					switch (stateReduceResult) {
						case StateInvalid:
							cs.getModify().getAcquired().remove(rpc.Argument.GlobalTableKey);
							break; // reduce success

						default:
							// case StateReduceRpcTimeout:
							// case StateReduceException:
							// case StateReduceNetError:
							cs.setAcquireStatePending(StateInvalid);
							cs.notify();

							logger.error("XXX 9 {} {} {}", sender, rpc.Argument.State, cs);
							rpc.Result.State = StateInvalid;
							rpc.Result.GlobalSerialId = cs.GlobalSerialId;
							rpc.SendResultCode(AcquireModifyFaild);
							return 0;
					}

					cs.setModify(sender);
					cs.getShare().remove(sender);
					sender.getAcquired().put(rpc.Argument.GlobalTableKey, StateModify);
					cs.setAcquireStatePending(StateInvalid);
					cs.notify();

					logger.debug("6 {} {} {}", sender, rpc.Argument.State, cs);
					rpc.Result.GlobalSerialId = cs.GlobalSerialId;
					rpc.SendResult();
					return 0;
				}

				ArrayList<Zeze.Util.KV<CacheHolder, Reduce >> reducePending = new ArrayList<>();
				HashSet<CacheHolder> reduceSuccessed = new HashSet<>();
				boolean senderIsShare = false;
				// 先把降级请求全部发送给出去。
				for (CacheHolder c : cs.getShare()) {
					if (c == sender) {
						senderIsShare = true;
						reduceSuccessed.add(sender);
						continue;
					}
					Reduce reduce = c.ReduceWaitLater(rpc.Argument.GlobalTableKey, StateInvalid, cs.GlobalSerialId);
					if (null != reduce) {
						reducePending.add(Zeze.Util.KV.Create(c, reduce));
					}
					else {
						// 网络错误不再认为成功。整个降级失败，要中断降级。
						// 已经发出去的降级请求要等待并处理结果。后面处理。
						break;
					}
				}

				// 两种情况不需要发reduce
				// 1. share是空的, 可以直接升为Modify
				// 2. sender是share, 而且reducePending的size是0
				if (!cs.getShare().isEmpty() && (!senderIsShare || reducePending.size() > 0)) {
					Zeze.Util.Task.Run(() -> {
						// 一个个等待是否成功。WaitAll 碰到错误不知道怎么处理的，
						// 应该也会等待所有任务结束（包括错误）。
						for (var reduce : reducePending) {
							try {
								reduce.getValue().getFuture().Wait();
								if (reduce.getValue().Result.State == StateInvalid) {
									// 后面还有个成功的处理循环，但是那里可能包含sender，
									// 在这里更新吧。
									reduce.getKey().getAcquired().remove(rpc.Argument.GlobalTableKey);
									reduceSuccessed.add(reduce.getKey());
								}
								else {
									reduce.getKey().SetError();
								}
							}
							catch (Throwable ex) {
								reduce.getKey().SetError();
								// 等待失败不再看作成功。
								logger.error("Reduce {} {} {} {}", sender, rpc.Argument.State, cs, reduce.getValue().Argument, ex);
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
				for (CacheHolder successed : reduceSuccessed) {
					cs.getShare().remove(successed);
				}

				// 如果前面降级发生中断(break)，这里就不会为0。
				if (cs.getShare().isEmpty()) {
					cs.setModify(sender);
					sender.getAcquired().put(rpc.Argument.GlobalTableKey, StateModify);
					cs.setAcquireStatePending(StateInvalid);
					cs.notify(); // Pending 结束，唤醒一个进来就可以了。

					logger.debug("8 {} {} {}", sender, rpc.Argument.State, cs);
					rpc.Result.GlobalSerialId = cs.GlobalSerialId;
					rpc.SendResult();
				}
				else {
					// senderIsShare 在失败的时候，Acquired 没有变化，不需要更新。
					// 失败了，要把原来是share的sender恢复。先这样吧。
					if (senderIsShare) {
						cs.getShare().add(sender);
					}

					cs.setAcquireStatePending(StateInvalid);
					cs.notify(); // Pending 结束，唤醒一个进来就可以了。

					logger.error("XXX 10 {} {} {}", sender, rpc.Argument.State, cs);

					rpc.Result.State = StateInvalid;
					rpc.Result.GlobalSerialId = cs.GlobalSerialId;
					rpc.SendResultCode(AcquireModifyFaild);
				}
				// 很好，网络失败不再看成成功，发现除了加break，
				// 其他处理已经能包容这个改动，都不用动。
				return 0;
			}
		}
	}

	public final static class CacheState {
		private CacheHolder Modify;
		public CacheHolder getModify() {
			return Modify;
		}
		public void setModify(CacheHolder value) {
			Modify = value;
		}
		private int AcquireStatePending = StateInvalid;
		public long GlobalSerialId;
		public int getAcquireStatePending() {
			return AcquireStatePending;
		}
		public void setAcquireStatePending(int value) {
			AcquireStatePending = value;
		}
		private final HashSet<CacheHolder> Share = new HashSet<CacheHolder> ();
		public HashSet<CacheHolder> getShare() {
			return Share;
		}
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			ByteBuffer.BuildString(sb, getShare());
			return String.format("P%1$s M%2$s S%3$s", getAcquireStatePending(), getModify(), sb);
		}
	}

	public final static class CacheHolder {
		private long SessionId;
		public long getSessionId() {
			return SessionId;
		}
		private void setSessionId(long value) {
			SessionId = value;
		}
		private int GlobalCacheManagerHashIndex;
		private void setGlobalCacheManagerHashIndex(int value) {
			GlobalCacheManagerHashIndex = value;
		}

		private final ConcurrentHashMap<GlobalTableKey, Integer> Acquired;
		public ConcurrentHashMap<GlobalTableKey, Integer> getAcquired() {
			return Acquired;
		}

		public CacheHolder(GCMConfig config) {
			Acquired = new ConcurrentHashMap<>(config.getInitialCapacity(), 0.75f, config.getConcurrencyLevel());
		}

		public boolean TryBindSocket(AsyncSocket newSocket, int _GlobalCacheManagerHashIndex) {
			synchronized (this) {
				if (newSocket.getUserState() != null) {
					return false; // 不允许再次绑定。Login Or ReLogin 只能发一次。
				}

				var socket = GlobalCacheManagerServer.getInstance().getServer().GetSocket(getSessionId());
				if (null == socket) {
					// old socket not exist or has lost.
					setSessionId(newSocket.getSessionId());
					newSocket.setUserState(this);
					setGlobalCacheManagerHashIndex(_GlobalCacheManagerHashIndex);
					return true;
				}
				// 每个AutoKeyLocalId只允许一个实例，已经存在了以后，旧的实例上有状态，阻止新的实例登录成功。
				return false;
			}
		}

		public boolean TryUnBindSocket(AsyncSocket oldSocket) {
			synchronized (this) {
				// 这里检查比较严格，但是这些检查应该都不会出现。

				if (oldSocket.getUserState() != this) {
					return false; // not bind to this
				}

				var socket = GlobalCacheManagerServer.getInstance().getServer().GetSocket(getSessionId());
				if (socket != oldSocket) {
					return false; // not same socket
				}

				setSessionId(0);
				return true;
			}
		}
		@Override
		public String toString() {
			return "" + getSessionId();
		}

		public Reduce Reduce(GlobalTableKey gkey, int state, long globalSerialId) {
			Reduce reduce = ReduceWaitLater(gkey, state, globalSerialId);
			try {
				if (null != reduce) {
					reduce.getFuture().Wait();
					// 如果rpc返回错误的值，外面能处理。
					return reduce;
				}
				reduce.Result.State = StateReduceNetError;
				return reduce;
			}
			catch (RpcTimeoutException timeoutex) {
				// 等待超时，应该报告错误。
				logger.error( "Reduce RpcTimeoutException {} target={} '{}'", state, getSessionId(), gkey, timeoutex);
				reduce.Result.State = StateReduceRpcTimeout;
				return reduce;
			}
			catch (Throwable ex) {
				logger.error("Reduce Exception {} target={} '{}'", state, getSessionId(), gkey, ex);
				reduce.Result.State = StateReduceException;
				return reduce;
			}
		}

		public static final long ForbitPeriod = 10 * 1000; // 10 seconds
		private long LastErrorTime = 0;

		public void SetError() {
			synchronized (this) {
				long now = System.currentTimeMillis();
				if (now - LastErrorTime > ForbitPeriod) {
					LastErrorTime = now;
				}
			}
		}
		/**
		 返回null表示发生了网络错误，或者应用服务器已经关闭。
		 */
		public Reduce ReduceWaitLater(GlobalTableKey gkey, int state, long globalSerialId) {
			try {
				synchronized (this) {
					if (System.currentTimeMillis() - LastErrorTime < ForbitPeriod) {
						return null;
					}
				}
				AsyncSocket peer = GlobalCacheManagerServer.getInstance().getServer().GetSocket(getSessionId());
				if (null != peer) {
					Reduce reduce = new Reduce(gkey, state, globalSerialId);
					reduce.SendForWait(peer, 10000);
					return reduce;
				}
			}
			catch (Throwable ex) {
				// 这里的异常只应该是网络发送异常。
				logger.error("ReduceWaitLater Exception {}", gkey, ex);
			}
			SetError();
			return null;
		}
	}

	public final static class ServerService extends Zeze.Net.Service {

		public ServerService(Zeze.Config config) throws Throwable {
			super("GlobalCacheManager", config);
		}

		@Override
		public void OnSocketAccept(AsyncSocket so) throws Throwable {
			// so.UserState = new CacheHolder(so.SessionId); // Login ReLogin 的时候初始化。
			super.OnSocketAccept(so);
		}
	}

	public static void main(String[] args) throws Throwable {
		System.setProperty("log4j.configurationFile", "log4j2.xml");
		String ip = null;
		int port = 5555;

		Zeze.Util.Task.tryInitThreadPool(null, null, null);

		for (int i = 0; i < args.length; ++i)
		{
			switch (args[i])
			{
				case "-ip": ip = args[++i]; break;
				case "-port": port = Integer.parseInt(args[++i]); break;

			}
		}
		InetAddress address = (null == ip || ip.isEmpty()) ?
				new InetSocketAddress(0).getAddress() : InetAddress.getByName(ip);

		var GlobalServer = Zeze.Services.GlobalCacheManagerServer.Instance;
		GlobalServer.Start(address, port);
		logger.info("Start .");
		while (true) {
			Thread.sleep(10000);
		}
	}
}
