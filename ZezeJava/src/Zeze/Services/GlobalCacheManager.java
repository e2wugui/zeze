package Zeze.Services;

import Zeze.Serialize.*;
import Zeze.Net.*;
import Zeze.Transaction.*;
import Zeze.*;
import java.util.*;

public final class GlobalCacheManager {
	public static final int StateInvalid = 0;
	public static final int StateShare = 1;
	public static final int StateModify = 2;
	public static final int StateRemoved = -1; // 从容器(Cache或Global)中删除后设置的状态，最后一个状态。
	public static final int StateReduceRpcTimeout = -2; // 用来表示 reduce 超时失败。不是状态。
	public static final int StateReduceException = -3; // 用来表示 reduce 异常失败。不是状态。
	public static final int StateReduceNetError = -4; // 用来表示 reduce 网络失败。不是状态。

	public static final int AcquireShareDeadLockFound = 1;
	public static final int AcquireShareAlreadyIsModify = 2;
	public static final int AcquireModifyDeadLockFound = 3;
	public static final int AcquireErrorState = 4;
	public static final int AcquireModifyAlreadyIsModify = 5;
	public static final int AcquireShareFaild = 6;
	public static final int AcquireModifyFaild = 7;

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

	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
	private static GlobalCacheManager Instance = new GlobalCacheManager();
	public static GlobalCacheManager getInstance() {
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
	private Util.HugeConcurrentDictionary<GlobalTableKey, CacheState> global;
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
	private java.util.concurrent.ConcurrentHashMap<Integer, CacheHolder> Sessions;

	private GlobalCacheManager() {
	}

	public static class GCMConfig implements Config.ICustomize {
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
		private long InitialCapacity = 100000000;
		public final long getInitialCapacity() {
			return InitialCapacity;
		}
		public final void setInitialCapacity(long value) {
			InitialCapacity = value;
		}
		private int GCMCount = 16;
		public final int getGCMCount() {
			return GCMCount;
		}
		public final void setGCMCount(int value) {
			GCMCount = value;
		}

		public final void Parse(XmlElement self) {
			String attr;

			attr = self.GetAttribute("ConcurrencyLevel");
			if (attr.length() > 0) {
				setConcurrencyLevel(Integer.parseInt(attr));
			}
			if (getConcurrencyLevel() < Environment.ProcessorCount) {
				setConcurrencyLevel(Environment.ProcessorCount);
			}

			attr = self.GetAttribute("InitialCapacity");
			if (attr.length() > 0) {
				setInitialCapacity(Long.parseLong(attr));
			}
			if (getInitialCapacity() < 31) {
				setInitialCapacity(31);
			}

			attr = self.GetAttribute("GCMCount");
			if (attr.length() > 0) {
				setGCMCount(Integer.parseInt(attr));
			}
			if (getGCMCount() < 1) {
				setGCMCount(1);
			}
		}
	}

	private GCMConfig Config = new GCMConfig();
	public GCMConfig getConfig() {
		return Config;
	}


	public void Start(IPAddress ipaddress, int port) {
		Start(ipaddress, port, null);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void Start(IPAddress ipaddress, int port, Config config = null)
	public void Start(IPAddress ipaddress, int port, Config config) {
		synchronized (this) {
			if (getServer() != null) {
				return;
			}

			if (null == config) {
				config = new Config();
				config.AddCustomize(getConfig());
				config.LoadAndParse();
			}
			Sessions = new java.util.concurrent.ConcurrentHashMap<Integer, CacheHolder>(getConfig().getConcurrencyLevel(), 4096);
			global = new Util.HugeConcurrentDictionary<GlobalTableKey, CacheState> (getConfig().getGCMCount(), getConfig().getConcurrencyLevel(), getConfig().getInitialCapacity());

			setServer(new ServerService(config));

			getServer().AddFactoryHandle((new Acquire()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new Acquire(), Handle = ProcessAcquireRequest});

			getServer().AddFactoryHandle((new Reduce()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new Reduce()});

			getServer().AddFactoryHandle((new Login()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new Login(), Handle = ProcessLogin});

			getServer().AddFactoryHandle((new ReLogin()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new ReLogin(), Handle = ProcessReLogin});

			getServer().AddFactoryHandle((new NormalClose()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new NormalClose(), Handle = ProcessNormalClose});

			// 临时注册到这里，安全起见应该起一个新的Service，并且仅绑定到 localhost。
			getServer().AddFactoryHandle((new Cleanup()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new Cleanup(), Handle = ProcessCleanup});

			ServerSocket = getServer().NewServerSocket(ipaddress, port);
		}
	}

	public void Stop() {
		synchronized (this) {
			if (null == getServer()) {
				return;
			}
			ServerSocket.close();
			ServerSocket = null;
			getServer().Stop();
			setServer(null);
		}
	}

	/** 
	 报告错误的时候带上相关信息（包括GlobalCacheManager和LogicServer等等）
	 手动Cleanup时，连接正确的服务器执行。
	 
	 @param p
	 @return 
	*/
	private int ProcessCleanup(Protocol p) {
		var rpc = p instanceof Cleanup ? (Cleanup)p : null;

		// 安全性以后加强。
		if (false == rpc.getArgument().getSecureKey().equals("Ok! verify secure.")) {
			rpc.SendResultCode(CleanupErrorSecureKey);
			return 0;
		}

		var session = Sessions.putIfAbsent(rpc.getArgument().getAutoKeyLocalId(), (key) -> new CacheHolder(getConfig()));
		if (session.GlobalCacheManagerHashIndex != rpc.getArgument().getGlobalCacheManagerHashIndex()) {
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
		Zeze.Util.Scheduler.getInstance().Schedule((ThisTask) -> {
					for (var e : session.Acquired) {
						// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
						Release(session, e.Key);
					}
					rpc.SendResultCode(0);
		}, 5 * 60 * 1000, -1); // delay 5 mins

		return 0;
	}

	private int ProcessLogin(Protocol p) {
		var rpc = p instanceof Login ? (Login)p : null;
		var session = Sessions.putIfAbsent(rpc.getArgument().getServerId(), (_) -> new CacheHolder(getConfig()));
		if (false == session.TryBindSocket(p.getSender(), rpc.getArgument().getGlobalCacheManagerHashIndex())) {
			rpc.SendResultCode(LoginBindSocketFail);
			return 0;
		}
		// new login, 比如逻辑服务器重启。release old acquired.
		for (var e : session.Acquired) {
			// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
			Release(session, e.Key);
		}
		rpc.SendResultCode(0);
		return 0;
	}

	private int ProcessReLogin(Protocol p) {
		var rpc = p instanceof ReLogin ? (ReLogin)p : null;
		var session = Sessions.putIfAbsent(rpc.getArgument().getServerId(), (_) -> new CacheHolder(getConfig()));
		if (false == session.TryBindSocket(p.getSender(), rpc.getArgument().getGlobalCacheManagerHashIndex())) {
			rpc.SendResultCode(ReLoginBindSocketFail);
			return 0;
		}
		rpc.SendResultCode(0);
		return 0;
	}

	private int ProcessNormalClose(Protocol p) {
		var rpc = p instanceof NormalClose ? (NormalClose)p : null;
		Object tempVar = rpc.getSender().getUserState();
		var session = tempVar instanceof CacheHolder ? (CacheHolder)tempVar : null;
		if (null == session) {
			rpc.SendResultCode(AcquireNotLogin);
			return 0; // not login
		}
		if (false == session.TryUnBindSocket(p.getSender())) {
			rpc.SendResultCode(NormalCloseUnbindFail);
			return 0;
		}
		for (var e : session.getAcquired()) {
			// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
			Release(session, e.Key);
		}
		rpc.SendResultCode(0);
		logger.Debug("After NormalClose global.Count={0}", global.getCount());
		return 0;
	}

	private int ProcessAcquireRequest(Protocol p) {
		Acquire rpc = (Acquire)p;
		if (rpc.getSender().getUserState() == null) {
			rpc.SendResultCode(AcquireNotLogin);
			return 0;
		}
		switch (rpc.getArgument().getState()) {
			case StateInvalid: // realease
				Object tempVar = rpc.getSender().getUserState();
				Release(tempVar instanceof CacheHolder ? (CacheHolder)tempVar : null, rpc.getArgument().getGlobalTableKey());
				rpc.setResult(rpc.getArgument());
				rpc.SendResult();
				return 0;

			case StateShare:
				return AcquireShare(rpc);

			case StateModify:
				return AcquireModify(rpc);

			default:
				rpc.setResult(rpc.getArgument());
				rpc.SendResultCode(AcquireErrorState);
				return 0;
		}
	}

	private void Release(CacheHolder holder, GlobalTableKey gkey) {
		CacheState cs = global.GetOrAdd(gkey, (tabkeKeyNotUsed) -> new CacheState());
		synchronized (cs) {
			if (cs.getModify() == holder) {
				cs.setModify(null);
			}
			cs.getShare().remove(holder); // always try remove

			if (cs.getModify() == null && cs.getShare().isEmpty() && cs.getAcquireStatePending() == StateInvalid) {
				// 安全的从global中删除，没有并发问题。
				cs.setAcquireStatePending(StateRemoved);
				V _;
				tangible.OutObject<CacheState> tempOut__ = new tangible.OutObject<CacheState>();
				global.TryRemove(gkey, tempOut__);
			_ = tempOut__.outArgValue;
			}
			V _;
			tangible.OutObject<Integer> tempOut__2 = new tangible.OutObject<Integer>();
			holder.getAcquired().TryRemove(gkey, tempOut__2);
		_ = tempOut__2.outArgValue;
		}
	}

	private int AcquireShare(Acquire rpc) {
		CacheHolder sender = (CacheHolder)rpc.getSender().getUserState();
		rpc.setResult(rpc.getArgument());
		while (true) {
			CacheState cs = global.GetOrAdd(rpc.getArgument().getGlobalTableKey(), (tabkeKeyNotUsed) -> new CacheState());
			synchronized (cs) {
				if (cs.getAcquireStatePending() == StateRemoved) {
					continue;
				}

				while (cs.getAcquireStatePending() != StateInvalid) {
					switch (cs.getAcquireStatePending()) {
						case StateShare:
							if (cs.getModify() == sender) {
								logger.Debug("1 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
								rpc.getResult().setState(StateInvalid);
								rpc.SendResultCode(AcquireShareDeadLockFound);
								return 0;
							}
							break;
						case StateModify:
							if (cs.getModify() == sender || cs.getShare().contains(sender)) {
								logger.Debug("2 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
								rpc.getResult().setState(StateInvalid);
								rpc.SendResultCode(AcquireShareDeadLockFound);
								return 0;
							}
							break;
					}
					logger.Debug("3 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
					Monitor.Wait(cs);
				}
				cs.setAcquireStatePending(StateShare);

				if (cs.getModify() != null) {
					if (cs.getModify() == sender) {
						cs.setAcquireStatePending(StateInvalid);
						logger.Debug("4 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
						rpc.getResult().setState(StateModify);
						// 已经是Modify又申请，可能是sender异常关闭，
						// 又重启连上。更新一下。应该是不需要的。
						sender.getAcquired().set(rpc.getArgument().getGlobalTableKey(), StateModify);
						rpc.SendResultCode(AcquireShareAlreadyIsModify);
						return 0;
					}

					int stateReduceResult = StateReduceException;
					Zeze.Util.Task.Run(() -> {
								stateReduceResult = cs.getModify().Reduce(rpc.getArgument().getGlobalTableKey(), StateShare);

								synchronized (cs) {
									Monitor.PulseAll(cs);
								}
					}, "GlobalCacheManager.AcquireShare.Reduce");
					logger.Debug("5 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
					Monitor.Wait(cs);

					switch (stateReduceResult) {
						case StateShare:
							cs.getModify().getAcquired().set(rpc.getArgument().getGlobalTableKey(), StateShare);
							cs.getShare().add(cs.getModify());
							// 降级成功，有可能降到 Invalid，此时就不需要加入 Share 了。
							break;

						default:
							// 包含协议返回错误的值的情况。
							// case StateReduceRpcTimeout:
							// case StateReduceException:
							// case StateReduceNetError:
							cs.setAcquireStatePending(StateInvalid);
							Monitor.Pulse(cs);

							logger.Error("XXX 8 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
							rpc.getResult().setState(StateInvalid);
							rpc.SendResultCode(AcquireShareFaild);
							return 0;
					}

					cs.setModify(null);
					sender.getAcquired().set(rpc.getArgument().getGlobalTableKey(), StateShare);
					cs.getShare().add(sender);
					cs.setAcquireStatePending(StateInvalid);
					Monitor.Pulse(cs);
					logger.Debug("6 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
					rpc.SendResult();
					return 0;
				}

				sender.getAcquired().set(rpc.getArgument().getGlobalTableKey(), StateShare);
				cs.getShare().add(sender);
				cs.setAcquireStatePending(StateInvalid);
				logger.Debug("7 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
				rpc.SendResult();
				return 0;
			}
		}
	}

	private int AcquireModify(Acquire rpc) {
		CacheHolder sender = (CacheHolder)rpc.getSender().getUserState();
		rpc.setResult(rpc.getArgument());

		while (true) {
			CacheState cs = global.GetOrAdd(rpc.getArgument().getGlobalTableKey(), (tabkeKeyNotUsed) -> new CacheState());
			synchronized (cs) {
				if (cs.getAcquireStatePending() == StateRemoved) {
					continue;
				}

				while (cs.getAcquireStatePending() != StateInvalid) {
					switch (cs.getAcquireStatePending()) {
						case StateShare:
							if (cs.getModify() == sender) {
								logger.Debug("1 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
								rpc.getResult().setState(StateInvalid);
								rpc.SendResultCode(AcquireModifyDeadLockFound);
								return 0;
							}
							break;
						case StateModify:
							if (cs.getModify() == sender || cs.getShare().contains(sender)) {
								logger.Debug("2 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
								rpc.getResult().setState(StateInvalid);
								rpc.SendResultCode(AcquireModifyDeadLockFound);
								return 0;
							}
							break;
					}
					logger.Debug("3 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
					Monitor.Wait(cs);
				}
				cs.setAcquireStatePending(StateModify);

				if (cs.getModify() != null) {
					if (cs.getModify() == sender) {
						logger.Debug("4 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
						// 已经是Modify又申请，可能是sender异常关闭，又重启连上。
						// 更新一下。应该是不需要的。
						sender.getAcquired().set(rpc.getArgument().getGlobalTableKey(), StateModify);
						rpc.SendResultCode(AcquireModifyAlreadyIsModify);
						cs.setAcquireStatePending(StateInvalid);
						return 0;
					}

					int stateReduceResult = StateReduceException;
					Zeze.Util.Task.Run(() -> {
								stateReduceResult = cs.getModify().Reduce(rpc.getArgument().getGlobalTableKey(), StateInvalid);
								synchronized (cs) {
									Monitor.PulseAll(cs);
								}
					}, "GlobalCacheManager.AcquireModify.Reduce");
					logger.Debug("5 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
					Monitor.Wait(cs);

					switch (stateReduceResult) {
						case StateInvalid:
							V _;
							tangible.OutObject<Integer> tempOut__ = new tangible.OutObject<Integer>();
							cs.getModify().getAcquired().TryRemove(rpc.getArgument().getGlobalTableKey(), tempOut__);
						_ = tempOut__.outArgValue;
							break; // reduce success

						default:
							// case StateReduceRpcTimeout:
							// case StateReduceException:
							// case StateReduceNetError:
							cs.setAcquireStatePending(StateInvalid);
							Monitor.Pulse(cs);

							logger.Error("XXX 9 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
							rpc.getResult().setState(StateInvalid);
							rpc.SendResultCode(AcquireModifyFaild);
							return 0;
					}

					cs.setModify(sender);
					sender.getAcquired().set(rpc.getArgument().getGlobalTableKey(), StateModify);
					cs.setAcquireStatePending(StateInvalid);
					Monitor.Pulse(cs);

					logger.Debug("6 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
					rpc.SendResult();
					return 0;
				}

				ArrayList<Util.KV<CacheHolder, Reduce >> reducePending = new ArrayList<Util.KV<CacheHolder, Reduce>>();
				HashSet<CacheHolder> reduceSuccessed = new HashSet<CacheHolder>();
				boolean senderIsShare = false;
				// 先把降级请求全部发送给出去。
				for (CacheHolder c : cs.getShare()) {
					if (c == sender) {
						senderIsShare = true;
						reduceSuccessed.add(sender);
						continue;
					}
					Reduce reduce = c.ReduceWaitLater(rpc.getArgument().getGlobalTableKey(), StateInvalid);
					if (null != reduce) {
						reducePending.add(Util.KV.Create(c, reduce));
					}
					else {
						// 网络错误不再认为成功。整个降级失败，要中断降级。
						// 已经发出去的降级请求要等待并处理结果。后面处理。
						break;
					}
				}

				Zeze.Util.Task.Run(() -> {
							// 一个个等待是否成功。WaitAll 碰到错误不知道怎么处理的，
							// 应该也会等待所有任务结束（包括错误）。
							for (var reduce : reducePending) {
								try {
									reduce.getValue().getFuture().Task.Wait();
									if (reduce.getValue().getResult().getState() == StateInvalid) {
										// 后面还有个成功的处理循环，但是那里可能包含sender，
										// 在这里更新吧。
										V _;
										tangible.OutObject<Integer> tempOut__ = new tangible.OutObject<Integer>();
										reduce.getKey().getAcquired().TryRemove(rpc.getArgument().getGlobalTableKey(), tempOut__);
										_ = tempOut__.outArgValue;
										reduceSuccessed.add(reduce.getKey());
									}
									else {
										reduce.getKey().SetError();
									}
								}
								catch (RuntimeException ex) {
									reduce.getKey().SetError();
									// 等待失败不再看作成功。
									logger.Error(ex, "Reduce {0} {1} {2} {3}", sender, rpc.getArgument().getState(), cs, reduce.getValue().getArgument());
								}
							}
							synchronized (cs) {
								// 需要唤醒等待任务结束的，但没法指定，只能全部唤醒。
								Monitor.PulseAll(cs);
							}
				}, "GlobalCacheManager.AcquireModify.WaitReduce");
				logger.Debug("7 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
				Monitor.Wait(cs);

				// 移除成功的。
				for (CacheHolder successed : reduceSuccessed) {
					cs.getShare().remove(successed);
				}

				// 如果前面降级发生中断(break)，这里就不会为0。
				if (cs.getShare().isEmpty()) {
					cs.setModify(sender);
					sender.getAcquired().set(rpc.getArgument().getGlobalTableKey(), StateModify);
					cs.setAcquireStatePending(StateInvalid);
					Monitor.Pulse(cs); // Pending 结束，唤醒一个进来就可以了。

					logger.Debug("8 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
					rpc.SendResult();
				}
				else {
					// senderIsShare 在失败的时候，Acquired 没有变化，不需要更新。
					// 失败了，要把原来是share的sender恢复。先这样吧。
					if (senderIsShare) {
						cs.getShare().add(sender);
					}

					cs.setAcquireStatePending(StateInvalid);
					Monitor.Pulse(cs); // Pending 结束，唤醒一个进来就可以了。

					logger.Error("XXX 10 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);

					rpc.getResult().setState(StateInvalid);
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
		private int AcquireStatePending = GlobalCacheManager.StateInvalid;
		public int getAcquireStatePending() {
			return AcquireStatePending;
		}
		public void setAcquireStatePending(int value) {
			AcquireStatePending = value;
		}
		private HashSet<CacheHolder> Share = new HashSet<CacheHolder> ();
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
		private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

		private long SessionId;
		public long getSessionId() {
			return SessionId;
		}
		private void setSessionId(long value) {
			SessionId = value;
		}
		private int GlobalCacheManagerHashIndex;
		public int getGlobalCacheManagerHashIndex() {
			return GlobalCacheManagerHashIndex;
		}
		private void setGlobalCacheManagerHashIndex(int value) {
			GlobalCacheManagerHashIndex = value;
		}

		private Util.HugeConcurrentDictionary<GlobalTableKey, Integer> Acquired;
		public Util.HugeConcurrentDictionary<GlobalTableKey, Integer> getAcquired() {
			return Acquired;
		}

		public CacheHolder(GCMConfig config) {
			Acquired = new Util.HugeConcurrentDictionary<GlobalTableKey, Integer>(config.getGCMCount(), config.getConcurrencyLevel(), config.getInitialCapacity());
		}

		public boolean TryBindSocket(AsyncSocket newSocket, int _GlobalCacheManagerHashIndex) {
			synchronized (this) {
				if (newSocket.getUserState() != null) {
					return false; // 不允许再次绑定。Login Or ReLogin 只能发一次。
				}

				var socket = GlobalCacheManager.getInstance().getServer().GetSocket(getSessionId());
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

				var socket = GlobalCacheManager.getInstance().getServer().GetSocket(getSessionId());
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

		public int Reduce(GlobalTableKey gkey, int state) {
			try {
				Reduce reduce = ReduceWaitLater(gkey, state);
				if (null != reduce) {
					reduce.getFuture().Task.Wait();
					// 如果rpc返回错误的值，外面能处理。
					return reduce.getResult().getState();
				}
				return GlobalCacheManager.StateReduceNetError;
			}
			catch (RpcTimeoutException timeoutex) {
				// 等待超时，应该报告错误。
				logger.Error(timeoutex, "Reduce RpcTimeoutException {0} target={1}", state, getSessionId());
				return GlobalCacheManager.StateReduceRpcTimeout;
			}
			catch (RuntimeException ex) {
				logger.Error(ex, "Reduce Exception {0} target={1}", state, getSessionId());
				return GlobalCacheManager.StateReduceException;
			}
		}

		public static final long ForbitPeriod = 10 * 1000; // 10 seconds
		private long LastErrorTime = 0;

		public void SetError() {
			synchronized (this) {
				long now = Zeze.Util.Time.getNowUnixMillis();
				if (now - LastErrorTime > ForbitPeriod) {
					LastErrorTime = now;
				}
			}
		}
		/** 
		 返回null表示发生了网络错误，或者应用服务器已经关闭。
		 
		 @param gkey
		 @param state
		 @return 
		*/
		public Reduce ReduceWaitLater(GlobalTableKey gkey, int state) {
			try {
				synchronized (this) {
					if (Zeze.Util.Time.getNowUnixMillis() - LastErrorTime < ForbitPeriod) {
						return null;
					}
				}
				AsyncSocket peer = GlobalCacheManager.getInstance().getServer().GetSocket(getSessionId());
				if (null != peer) {
					Reduce reduce = new Reduce(gkey, state);
					reduce.SendForWait(peer, 10000);
					return reduce;
				}
			}
			catch (RuntimeException ex) {
				// 这里的异常只应该是网络发送异常。
				logger.Error(ex, "ReduceWaitLater Exception {0}", gkey);
			}
			SetError();
			return null;
		}
	}

	public final static class Param extends Zeze.Transaction.Bean {
		private GlobalTableKey GlobalTableKey;
		public GlobalTableKey getGlobalTableKey() {
			return GlobalTableKey;
		}
		public void setGlobalTableKey(GlobalTableKey value) {
			GlobalTableKey = value;
		}
		private int State;
		public int getState() {
			return State;
		}
		public void setState(int value) {
			State = value;
		}

		@Override
		public void Decode(ByteBuffer bb) {
			if (null == getGlobalTableKey()) {
				setGlobalTableKey(new GlobalTableKey());
			}
			getGlobalTableKey().Decode(bb);
			setState(bb.ReadInt());
		}

		@Override
		public void Encode(ByteBuffer bb) {
			getGlobalTableKey().Encode(bb);
			bb.WriteInt(getState());
		}

		@Override
		protected void InitChildrenRootInfo(Record.RootInfo root) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return getGlobalTableKey().toString() + ":" + getState();
		}
	}
	public final static class Acquire extends Zeze.Net.Rpc<Param, Param> {
		public final static int ProtocolId_ = Bean.Hash16(Acquire.class.FullName);

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}

		public Acquire() {
		}

		public Acquire(GlobalTableKey gkey, int state) {
			getArgument().setGlobalTableKey(gkey);
			getArgument().setState(state);
		}
	}

	public final static class Reduce extends Zeze.Net.Rpc<Param, Param> {
		public final static int ProtocolId_ = Bean.Hash16(Reduce.class.FullName);

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}

		public Reduce() {
		}

		public Reduce(GlobalTableKey gkey, int state) {
			getArgument().setGlobalTableKey(gkey);
			getArgument().setState(state);
		}
	}

	public final static class LoginParam extends Zeze.Transaction.Bean {
		private int ServerId;
		public int getServerId() {
			return ServerId;
		}
		public void setServerId(int value) {
			ServerId = value;
		}

		// GlobalCacheManager 本身没有编号。
		// 启用多个进程，使用 GlobalTableKey.GetHashCode() 分配负载后，报告错误需要这个来识别哪个进程。
		// 当然识别还可以根据 ServerService 绑定的ip和port。
		// 给每个实例加配置不容易维护。
		private int GlobalCacheManagerHashIndex;
		public int getGlobalCacheManagerHashIndex() {
			return GlobalCacheManagerHashIndex;
		}
		public void setGlobalCacheManagerHashIndex(int value) {
			GlobalCacheManagerHashIndex = value;
		}

		@Override
		public void Decode(ByteBuffer bb) {
			setServerId(bb.ReadInt());
			setGlobalCacheManagerHashIndex(bb.ReadInt());
		}

		@Override
		public void Encode(ByteBuffer bb) {
			bb.WriteInt(getServerId());
			bb.WriteInt(getGlobalCacheManagerHashIndex());
		}

		@Override
		protected void InitChildrenRootInfo(Record.RootInfo root) {
			throw new UnsupportedOperationException();
		}
	}

	public final static class Login extends Zeze.Net.Rpc<LoginParam, Zeze.Transaction.EmptyBean> {
		public final static int ProtocolId_ = Bean.Hash16(Login.class.FullName);

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}

		public Login() {
		}

		public Login(int id) {
			getArgument().setServerId(id);
		}
	}

	public final static class ReLogin extends Zeze.Net.Rpc<LoginParam, Zeze.Transaction.EmptyBean> {
		public final static int ProtocolId_ = Bean.Hash16(ReLogin.class.FullName);

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}

		public ReLogin() {
		}

		public ReLogin(int id) {
			getArgument().setServerId(id);
		}
	}

	public final static class NormalClose extends Zeze.Net.Rpc<Zeze.Transaction.EmptyBean, Zeze.Transaction.EmptyBean> {
		public final static int ProtocolId_ = Bean.Hash16(NormalClose.class.FullName);

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}
	}

	public final static class AchillesHeel extends Zeze.Transaction.Bean {
		private int AutoKeyLocalId;
		public int getAutoKeyLocalId() {
			return AutoKeyLocalId;
		}
		public void setAutoKeyLocalId(int value) {
			AutoKeyLocalId = value;
		}

		private String SecureKey;
		public String getSecureKey() {
			return SecureKey;
		}
		public void setSecureKey(String value) {
			SecureKey = value;
		}
		private int GlobalCacheManagerHashIndex;
		public int getGlobalCacheManagerHashIndex() {
			return GlobalCacheManagerHashIndex;
		}
		public void setGlobalCacheManagerHashIndex(int value) {
			GlobalCacheManagerHashIndex = value;
		}

		@Override
		public void Decode(ByteBuffer bb) {
			setAutoKeyLocalId(bb.ReadInt());
			setSecureKey(bb.ReadString());
			setGlobalCacheManagerHashIndex(bb.ReadInt());
		}

		@Override
		public void Encode(ByteBuffer bb) {
			bb.WriteInt(getAutoKeyLocalId());
			bb.WriteString(getSecureKey());
			bb.WriteInt(getGlobalCacheManagerHashIndex());
		}

		@Override
		protected void InitChildrenRootInfo(Record.RootInfo root) {
			throw new UnsupportedOperationException();
		}
	}

	public final static class Cleanup extends Zeze.Net.Rpc<AchillesHeel, Zeze.Transaction.EmptyBean> {
		public final static int ProtocolId_ = Bean.Hash16(Cleanup.class.FullName);

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}
	}

	public final static class ServerService extends Zeze.Net.Service {
		public ServerService(Config config) {
			super("GlobalCacheManager", config);
		}

		@Override
		public void OnSocketAccept(AsyncSocket so) {
			// so.UserState = new CacheHolder(so.SessionId); // Login ReLogin 的时候初始化。
			super.OnSocketAccept(so);
		}
	}

	public final static class GlobalTableKey implements java.lang.Comparable<GlobalTableKey>, Serializable {
		private String TableName;
		public String getTableName() {
			return TableName;
		}
		private void setTableName(String value) {
			TableName = value;
		}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private byte[] Key;
		private byte[] Key;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public byte[] getKey()
		public byte[] getKey() {
			return Key;
		}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private void setKey(byte[] value)
		private void setKey(byte[] value) {
			Key = value;
		}

		public GlobalTableKey() {
		}

		public GlobalTableKey(String tableName, ByteBuffer key) {
			this(tableName, key.Copy());
		}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public GlobalTableKey(string tableName, byte[] key)
		public GlobalTableKey(String tableName, byte[] key) {
			setTableName(tableName);
			setKey(key);
		}

		public int compareTo(GlobalTableKey other) {
//C# TO JAVA CONVERTER TODO TASK: The following System.String compare method is not converted:
			int c = this.getTableName().CompareTo(other.getTableName());
			if (c != 0) {
				return c;
			}

			return ByteBuffer.Compare(getKey(), other.getKey());
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}

			boolean tempVar = obj instanceof GlobalTableKey;
			GlobalTableKey another = tempVar ? (GlobalTableKey)obj : null;
			if (tempVar) {
				return getTableName().equals(another.getTableName()) && ByteBuffer.Equals(getKey(), another.getKey());
			}

			return false;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 17;
			result = prime * result + ByteBuffer.calc_hashnr(getTableName());
			result = prime * result + ByteBuffer.calc_hashnr(getKey(), 0, getKey().length);
			return result;
		}

		@Override
		public String toString() {
			return String.format("(%1$s,%2$s)", getTableName(), BitConverter.toString(getKey()));
		}

		public void Decode(ByteBuffer bb) {
			setTableName(bb.ReadString());
			setKey(bb.ReadBytes());
		}

		public void Encode(ByteBuffer bb) {
			bb.WriteString(getTableName());
			bb.WriteBytes(getKey());
		}
	}
}