package Zeze.Services;

import Zeze.Serialize.*;
import Zeze.Net.*;
import Zeze.Raft.*;
import Zeze.*;
import java.util.*;

public final class GlobalCacheManagerWithRaft {
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
	private static GlobalCacheManagerWithRaft Instance = new GlobalCacheManagerWithRaft();
	public static GlobalCacheManagerWithRaft getInstance() {
		return Instance;
	}
	private Raft Raft;
	public Raft getRaft() {
		return Raft;
	}
	private void setRaft(Raft value) {
		Raft = value;
	}
	public RaftDatas getRaftData() {
		return (RaftDatas)getRaft().getStateMachine();
	}

	private GlobalCacheManagerWithRaft() {
	}

	private GlobalCacheManager.GCMConfig Config = new GlobalCacheManager.GCMConfig();
	public GlobalCacheManager.GCMConfig getConfig() {
		return Config;
	}


	public void Start(Zeze.Raft.RaftConfig raftconfig) {
		Start(raftconfig, null);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void Start(Zeze.Raft.RaftConfig raftconfig, Config config = null)
	public void Start(RaftConfig raftconfig, Config config) {
		synchronized (this) {
			if (getRaft() != null) {
				return;
			}

			if (null == config) {
				config = new Config();
				config.AddCustomize(getConfig());
				config.LoadAndParse();
			}

			setRaft(new Raft(new RaftDatas(getConfig()), raftconfig.getName(), raftconfig, config));

			getRaft().getServer().AddFactoryHandle((new GlobalCacheManager.Acquire()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new GlobalCacheManager.Acquire(), Handle = ProcessAcquireRequest});

			getRaft().getServer().AddFactoryHandle((new GlobalCacheManager.Reduce()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new GlobalCacheManager.Reduce()});

			getRaft().getServer().AddFactoryHandle((new GlobalCacheManager.Login()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new GlobalCacheManager.Login(), Handle = ProcessLogin});

			getRaft().getServer().AddFactoryHandle((new GlobalCacheManager.ReLogin()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new GlobalCacheManager.ReLogin(), Handle = ProcessReLogin});

			getRaft().getServer().AddFactoryHandle((new GlobalCacheManager.NormalClose()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new GlobalCacheManager.NormalClose(), Handle = ProcessNormalClose});

			// 临时注册到这里，安全起见应该起一个新的Service，并且仅绑定到 localhost。
			getRaft().getServer().AddFactoryHandle((new GlobalCacheManager.Cleanup()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new GlobalCacheManager.Cleanup(), Handle = ProcessCleanup});

			getOperateFactory().put((new RemoveCacheState()).getTypeId(), () -> new RemoveCacheState());

			getOperateFactory().put((new SetCacheStateAcquireStatePending()).getTypeId(), () -> new SetCacheStateAcquireStatePending());
			getOperateFactory().put((new SetCacheStateModify()).getTypeId(), () -> new SetCacheStateModify());

			getOperateFactory().put((new AddCacheStateShare()).getTypeId(), () -> new AddCacheStateShare());
			getOperateFactory().put((new RemoveCacheStateShare()).getTypeId(), () -> new RemoveCacheStateShare());

			getOperateFactory().put((new PutCacheHolderAcquired()).getTypeId(), () -> new PutCacheHolderAcquired());
			getOperateFactory().put((new RemoveCacheHolderAcquired()).getTypeId(), () -> new RemoveCacheHolderAcquired());

			getRaft().getServer().Start();
		}
	}

	public void Stop() {
		synchronized (this) {
			if (null == getRaft()) {
				return;
			}
			getRaft().getServer().Stop();
			setRaft(null);
		}
	}

	private CacheHolder GetSession(int id) {
		return getRaftData().getSessions().GetOrAdd(id, (k) -> new CacheHolder(k));
	}

	/** 
	 报告错误的时候把相关信息（包括GlobalCacheManager和LogicServer等等）编码，手动Cleanup时，
	 解码并连接正确的服务器执行。降低手动风险。
	 
	 @param p
	 @return 
	*/
	private int ProcessCleanup(Protocol p) {
		var rpc = p instanceof GlobalCacheManager.Cleanup ? (GlobalCacheManager.Cleanup)p : null;

		// 安全性以后加强。
		if (false == rpc.getArgument().getSecureKey().equals("Ok! verify secure.")) {
			rpc.SendResultCode(GlobalCacheManager.CleanupErrorSecureKey);
			return 0;
		}

		var session = GetSession(rpc.getArgument().getAutoKeyLocalId());
		if (session.getGlobalCacheManagerHashIndex() != rpc.getArgument().getGlobalCacheManagerHashIndex()) {
			// 多点验证
			rpc.SendResultCode(GlobalCacheManager.CleanupErrorGlobalCacheManagerHashIndex);
			return 0;
		}

		if (this.getRaft().getServer().GetSocket(session.getSessionId()) != null) {
			// 连接存在，禁止cleanup。
			rpc.SendResultCode(GlobalCacheManager.CleanupErrorHasConnection);
			return 0;
		}

		// 还有更多的防止出错的手段吗？

		// XXX verify danger
		Zeze.Util.Scheduler.getInstance().Schedule((ThisTask) -> {
					for (var gkey : session.getAcquired().keySet()) {
						// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
						Release(rpc.getSender().getRemoteAddress(), rpc.getUniqueRequestId(), session, gkey);
					}
					rpc.SendResultCode(0);
		}, 5 * 60 * 1000, -1); // delay 5 mins

		return 0;
	}

	private int ProcessLogin(Protocol p) {
		var rpc = p instanceof GlobalCacheManager.Login ? (GlobalCacheManager.Login)p : null;
		var session = GetSession(rpc.getArgument().getServerId());
		if (false == session.TryBindSocket(p.getSender(), rpc.getArgument().getGlobalCacheManagerHashIndex())) {
			rpc.SendResultCode(GlobalCacheManager.LoginBindSocketFail);
			return 0;
		}
		// new login, 比如逻辑服务器重启。release old acquired.
		for (var gkey : session.getAcquired().keySet()) {
			// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
			Release(rpc.getSender().getRemoteAddress(), rpc.getUniqueRequestId(), session, gkey);
		}
		rpc.SendResultCode(0);
		return 0;
	}

	private int ProcessReLogin(Protocol p) {
		var rpc = p instanceof GlobalCacheManager.ReLogin ? (GlobalCacheManager.ReLogin)p : null;
		var session = GetSession(rpc.getArgument().getServerId());
		if (false == session.TryBindSocket(p.getSender(), rpc.getArgument().getGlobalCacheManagerHashIndex())) {
			rpc.SendResultCode(GlobalCacheManager.ReLoginBindSocketFail);
			return 0;
		}
		rpc.SendResultCode(0);
		return 0;
	}

	private int ProcessNormalClose(Protocol p) {
		var rpc = p instanceof GlobalCacheManager.NormalClose ? (GlobalCacheManager.NormalClose)p : null;
		Object tempVar = rpc.getSender().getUserState();
		var session = tempVar instanceof CacheHolder ? (CacheHolder)tempVar : null;
		if (null == session) {
			rpc.SendResultCode(GlobalCacheManager.AcquireNotLogin);
			return 0; // not login
		}
		if (false == session.TryUnBindSocket(p.getSender())) {
			rpc.SendResultCode(GlobalCacheManager.NormalCloseUnbindFail);
			return 0;
		}
		for (var gkey : session.getAcquired().keySet()) {
			// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
			Release(rpc.getSender().getRemoteAddress(), rpc.getUniqueRequestId(), session, gkey);
		}
		rpc.SendResultCode(0);
		logger.Debug("After NormalClose global.Count={0}", getRaftData().getGlobal().getCount());
		return 0;
	}

	private int ProcessAcquireRequest(Protocol p) {
		var rpc = p instanceof GlobalCacheManager.Acquire ? (GlobalCacheManager.Acquire)p : null;
		if (rpc.getSender().getUserState() == null) {
			rpc.SendResultCode(GlobalCacheManager.AcquireNotLogin);
			return 0;
		}
		switch (rpc.getArgument().getState()) {
			case GlobalCacheManager.StateInvalid: // realease
				Object tempVar = rpc.getSender().getUserState();
				var session = tempVar instanceof CacheHolder ? (CacheHolder)tempVar : null;
				Release(rpc.getSender().getRemoteAddress(), rpc.getUniqueRequestId(), session, rpc.getArgument().getGlobalTableKey());
				rpc.setResult(rpc.getArgument());
				rpc.SendResult();
				return 0;

			case GlobalCacheManager.StateShare:
				return AcquireShare(rpc);

			case GlobalCacheManager.StateModify:
				return AcquireModify(rpc);

			default:
				rpc.setResult(rpc.getArgument());
				rpc.SendResultCode(GlobalCacheManager.AcquireErrorState);
				return 0;
		}
	}

	private void Release(String appInstance, long requestId, CacheHolder holder, GlobalCacheManager.GlobalTableKey gkey) {
		var cs = getRaftData().getGlobal().GetOrAdd(gkey);
		synchronized (cs) {
			var step0 = new OperatesLog(appInstance, requestId);
			if (cs.getModify() == holder.getId()) {
				step0.SetCacheStateModify(gkey, -1);
			}
			// always try remove
			step0.RemoveCacheStateShare(gkey, holder.getId());

			getRaft().AppendLog(step0);

			var step1 = new OperatesLog(appInstance, requestId);
			if (cs.getModify() == -1 && cs.getShare().isEmpty() && cs.getAcquireStatePending() == GlobalCacheManager.StateInvalid) {
				// 安全的从global中删除，没有并发问题。
				step1.SetCacheStateAcquireStatePending(gkey, GlobalCacheManager.StateRemoved);
				step1.RemoveCacheState(gkey);
			}
			step1.RemoveCacheHolderAcquired(holder.getId(), gkey);
			getRaft().AppendLog(step1);
		}
	}

	private int AcquireShare(GlobalCacheManager.Acquire rpc) {
		Object tempVar = rpc.getSender().getUserState();
		var sender = tempVar instanceof CacheHolder ? (CacheHolder)tempVar : null;
		rpc.setResult(rpc.getArgument());
		var gkey = rpc.getArgument().getGlobalTableKey();
		while (true) {
			var cs = getRaftData().getGlobal().GetOrAdd(gkey);
			synchronized (cs) {
				if (cs.getAcquireStatePending() == GlobalCacheManager.StateRemoved) {
					continue;
				}

				while (cs.getAcquireStatePending() != GlobalCacheManager.StateInvalid) {
					switch (cs.getAcquireStatePending()) {
						case GlobalCacheManager.StateShare:
							if (cs.getModify() == sender.getId()) {
								logger.Debug("1 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
								rpc.getResult().setState(GlobalCacheManager.StateInvalid);
								rpc.SendResultCode(GlobalCacheManager.AcquireShareDeadLockFound);
								return 0;
							}
							break;
						case GlobalCacheManager.StateModify:
							if (cs.getModify() == sender.getId() || cs.getShare().contains(sender.getId())) {
								logger.Debug("2 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
								rpc.getResult().setState(GlobalCacheManager.StateInvalid);
								rpc.SendResultCode(GlobalCacheManager.AcquireShareDeadLockFound);
								return 0;
							}
							break;
					}
					logger.Debug("3 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
					Monitor.Wait(cs);
				}
				var step0 = new OperatesLog(rpc.getSender().getRemoteAddress(), rpc.getUniqueRequestId());
				step0.SetCacheStateAcquireStatePending(gkey, GlobalCacheManager.StateShare);
				getRaft().AppendLog(step0);
				// cs.AcquireStatePending = GlobalCacheManager.StateShare;

				if (cs.getModify() != -1) {
					if (cs.getModify() == sender.getId()) {
						logger.Debug("4 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
						rpc.getResult().setState(GlobalCacheManager.StateModify);
						// 已经是Modify又申请，可能是sender异常关闭，又重启连上。更新一下。应该是不需要的。
						var setp1 = new OperatesLog(rpc.getSender().getRemoteAddress(), rpc.getUniqueRequestId());
						setp1.PutCacheHolderAcquired(sender.getId(), gkey, GlobalCacheManager.StateModify);
						setp1.SetCacheStateAcquireStatePending(gkey, GlobalCacheManager.StateInvalid);
						// cs.AcquireStatePending = GlobalCacheManager.StateInvalid;
						getRaft().AppendLog(setp1);
						rpc.SendResultCode(GlobalCacheManager.AcquireShareAlreadyIsModify);
						return 0;
					}

					int stateReduceResult = GlobalCacheManager.StateReduceException;
					var modifyHolder = GetSession(cs.getModify());
					Zeze.Util.Task.Run(() -> {
								stateReduceResult = modifyHolder.Reduce(gkey, GlobalCacheManager.StateShare);

								synchronized (cs) {
									Monitor.PulseAll(cs);
								}
					}, "GlobalCacheManager.AcquireShare.Reduce");
					logger.Debug("5 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
					Monitor.Wait(cs);

					var step2 = new OperatesLog(rpc.getSender().getRemoteAddress(), rpc.getUniqueRequestId());
					switch (stateReduceResult) {
						case GlobalCacheManager.StateShare:
							step2.PutCacheHolderAcquired(cs.getModify(), gkey, GlobalCacheManager.StateShare);
							//cs.Modify.Acquired[rpc.Argument.GlobalTableKey] = GlobalCacheManager.StateShare;
							// 降级成功，有可能降到 Invalid，此时就不需要加入 Share 了。
							step2.AddCacheStateShare(rpc.getArgument().getGlobalTableKey(), cs.getModify());
							//cs.Share.Add(cs.Modify); 
							break;

						default:
							// 包含协议返回错误的值的情况。
							// case StateReduceRpcTimeout:
							// case StateReduceException:
							// case StateReduceNetError:
							step2.SetCacheStateAcquireStatePending(rpc.getArgument().getGlobalTableKey(), GlobalCacheManager.StateInvalid);
							//cs.AcquireStatePending = GlobalCacheManager.StateInvalid;
							getRaft().AppendLog(step2);

							Monitor.Pulse(cs);
							logger.Error("XXX 8 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
							rpc.getResult().setState(GlobalCacheManager.StateInvalid);
							rpc.SendResultCode(GlobalCacheManager.AcquireShareFaild);
							return 0;
					}

					step2.SetCacheStateModify(gkey, -1);
					//cs.Modify = null;
					step2.PutCacheHolderAcquired(sender.getId(), gkey, GlobalCacheManager.StateShare);
					//sender.Acquired[gkey] = GlobalCacheManager.StateShare;
					step2.AddCacheStateShare(gkey, sender.getId());
					//cs.Share.Add(sender);
					step2.SetCacheStateAcquireStatePending(gkey, GlobalCacheManager.StateInvalid);
					//cs.AcquireStatePending = GlobalCacheManager.StateInvalid;
					getRaft().AppendLog(step2);

					Monitor.Pulse(cs);
					logger.Debug("6 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
					rpc.SendResult();
					return 0;
				}

				var step3 = new OperatesLog(rpc.getSender().getRemoteAddress(), rpc.getUniqueRequestId());
				step3.PutCacheHolderAcquired(sender.getId(), gkey, GlobalCacheManager.StateShare);
				//sender.Acquired[gkey] = GlobalCacheManager.StateShare;
				step3.AddCacheStateShare(gkey, sender.getId());
				//cs.Share.Add(sender);
				step3.SetCacheStateAcquireStatePending(gkey, GlobalCacheManager.StateInvalid);
				//cs.AcquireStatePending = GlobalCacheManager.StateInvalid;
				logger.Debug("7 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
				getRaft().AppendLog(step3);
				rpc.SendResult();
				return 0;
			}
		}
	}

	private int AcquireModify(GlobalCacheManager.Acquire rpc) {
		Object tempVar = rpc.getSender().getUserState();
		var sender = tempVar instanceof CacheHolder ? (CacheHolder)tempVar : null;
		rpc.setResult(rpc.getArgument());
		var gkey = rpc.getArgument().getGlobalTableKey();
		while (true) {
			var cs = getRaftData().getGlobal().GetOrAdd(gkey);
			synchronized (cs) {
				if (cs.getAcquireStatePending() == GlobalCacheManager.StateRemoved) {
					continue;
				}

				while (cs.getAcquireStatePending() != GlobalCacheManager.StateInvalid) {
					switch (cs.getAcquireStatePending()) {
						case GlobalCacheManager.StateShare:
							if (cs.getModify() == sender.getId()) {
								logger.Debug("1 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
								rpc.getResult().setState(GlobalCacheManager.StateInvalid);
								rpc.SendResultCode(GlobalCacheManager.AcquireModifyDeadLockFound);
								return 0;
							}
							break;
						case GlobalCacheManager.StateModify:
							if (cs.getModify() == sender.getId() || cs.getShare().contains(sender.getId())) {
								logger.Debug("2 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
								rpc.getResult().setState(GlobalCacheManager.StateInvalid);
								rpc.SendResultCode(GlobalCacheManager.AcquireModifyDeadLockFound);
								return 0;
							}
							break;
					}
					logger.Debug("3 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
					Monitor.Wait(cs);
				}
				var step0 = new OperatesLog(rpc.getSender().getRemoteAddress(), rpc.getUniqueRequestId());
				step0.SetCacheStateAcquireStatePending(gkey, GlobalCacheManager.StateModify);
				//cs.AcquireStatePending = GlobalCacheManager.StateModify;
				getRaft().AppendLog(step0);

				if (cs.getModify() != -1) {
					if (cs.getModify() == sender.getId()) {
						logger.Debug("4 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
						// 已经是Modify又申请，可能是sender异常关闭，又重启连上。更新一下。应该是不需要的。
						var step1 = new OperatesLog(rpc.getSender().getRemoteAddress(), rpc.getUniqueRequestId());
						step1.PutCacheHolderAcquired(sender.getId(), gkey, GlobalCacheManager.StateModify);
						//sender.Acquired[rpc.Argument.GlobalTableKey] = GlobalCacheManager.StateModify;
						step1.SetCacheStateAcquireStatePending(gkey, GlobalCacheManager.StateInvalid);
						//cs.AcquireStatePending = GlobalCacheManager.StateInvalid;
						getRaft().AppendLog(step1);
						rpc.SendResultCode(GlobalCacheManager.AcquireModifyAlreadyIsModify);
						return 0;
					}

					int stateReduceResult = GlobalCacheManager.StateReduceException;
					var modifyHolder = GetSession(cs.getModify());
					Zeze.Util.Task.Run(() -> {
								stateReduceResult = modifyHolder.Reduce(gkey, GlobalCacheManager.StateInvalid);
								synchronized (cs) {
									Monitor.PulseAll(cs);
								}
					}, "GlobalCacheManager.AcquireModify.Reduce");
					logger.Debug("5 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
					Monitor.Wait(cs);

					var step2 = new OperatesLog(rpc.getSender().getRemoteAddress(), rpc.getUniqueRequestId());

					switch (stateReduceResult) {
						case GlobalCacheManager.StateInvalid:
							step2.RemoveCacheHolderAcquired(cs.getModify(), gkey);
							//cs.Modify.Acquired.TryRemove(rpc.Argument.GlobalTableKey, out var _);
							break; // reduce success

						default:
							// case StateReduceRpcTimeout:
							// case StateReduceException:
							// case StateReduceNetError:
							step2.SetCacheStateAcquireStatePending(gkey, GlobalCacheManager.StateInvalid);
							// cs.AcquireStatePending = GlobalCacheManager.StateInvalid;
							getRaft().AppendLog(step2);

							Monitor.Pulse(cs);

							logger.Error("XXX 9 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
							rpc.getResult().setState(GlobalCacheManager.StateInvalid);
							rpc.SendResultCode(GlobalCacheManager.AcquireModifyFaild);
							return 0;
					}

					step2.SetCacheStateModify(gkey, sender.getId());
					//cs.Modify = sender.Id;
					step2.PutCacheHolderAcquired(sender.getId(), gkey, GlobalCacheManager.StateModify);
					//sender.Acquired[rpc.Argument.GlobalTableKey] = GlobalCacheManager.StateModify;
					step2.SetCacheStateAcquireStatePending(gkey, GlobalCacheManager.StateInvalid);
					// cs.AcquireStatePending = GlobalCacheManager.StateInvalid;
					getRaft().AppendLog(step2);

					Monitor.Pulse(cs);

					logger.Debug("6 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
					rpc.SendResult();
					return 0;
				}

				ArrayList<Util.KV<CacheHolder, GlobalCacheManager.Reduce >> reducePending = new ArrayList<Util.KV<CacheHolder, GlobalCacheManager.Reduce>>();
				HashSet<CacheHolder> reduceSuccessed = new HashSet<CacheHolder>();
				boolean senderIsShare = false;
				// 先把降级请求全部发送给出去。
				for (var c : cs.getShare()) {
					if (c == sender.getId()) {
						senderIsShare = true;
						reduceSuccessed.add(sender);
						continue;
					}
					var shareHolder = GetSession(c);
					var reduce = shareHolder.ReduceWaitLater(gkey, GlobalCacheManager.StateInvalid);
					if (null != reduce) {
						reducePending.add(Util.KV.Create(shareHolder, reduce));
					}
					else {
						// 网络错误不再认为成功。整个降级失败，要中断降级。
						// 已经发出去的降级请求要等待并处理结果。后面处理。
						break;
					}
				}

				var step3 = new OperatesLog(rpc.getSender().getRemoteAddress(), rpc.getUniqueRequestId());
				Zeze.Util.Task.Run(() -> {
							// 一个个等待是否成功。WaitAll 碰到错误不知道怎么处理的，
							// 应该也会等待所有任务结束（包括错误）。
							for (var reduce : reducePending) {
								try {
									reduce.getValue().getFuture().Task.Wait();
									if (reduce.getValue().getResult().getState() == GlobalCacheManager.StateInvalid) {
										// 后面还有个成功的处理循环，但是那里可能包含sender，在这里更新吧。
										step3.RemoveCacheHolderAcquired(reduce.getKey().getId(), gkey);
										//reduce.Key.Acquired.TryRemove(rpc.Argument.GlobalTableKey, out var _);
										reduceSuccessed.add(reduce.getKey());
									}
									else {
										reduce.getKey().SetError();
									}
								}
								catch (RuntimeException ex) {
									reduce.getKey().SetError();
									// 等待失败不再看作成功。这个以前已经处理了，但这个注释没有更新。
									logger.Error(ex, "Reduce {0} {1} {2} {3}", sender, rpc.getArgument().getState(), cs, reduce.getValue().getArgument());
								}
							}
							synchronized (cs) {
								Monitor.PulseAll(cs); // 需要唤醒等待任务结束的，但没法指定，只能全部唤醒。
							}
				}, "GlobalCacheManager.AcquireModify.WaitReduce");
				logger.Debug("7 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
				Monitor.Wait(cs);

				// 移除成功的。
				for (CacheHolder successed : reduceSuccessed) {
					step3.RemoveCacheStateShare(gkey, successed.getId());
					//cs.Share.Remove(successed.Id);
				}

				getRaft().AppendLog(step3);

				// 如果前面降级发生中断(break)，这里不会为0。
				if (cs.getShare().isEmpty()) {
					var step4 = new OperatesLog(rpc.getSender().getRemoteAddress(), rpc.getUniqueRequestId());
					step4.SetCacheStateModify(gkey, sender.getId());
					//cs.Modify = sender.Id;
					step4.PutCacheHolderAcquired(sender.getId(), gkey, GlobalCacheManager.StateModify);
					//sender.Acquired[gkey] = GlobalCacheManager.StateModify;
					step4.SetCacheStateAcquireStatePending(gkey, GlobalCacheManager.StateInvalid);
					//cs.AcquireStatePending = GlobalCacheManager.StateInvalid;

					getRaft().AppendLog(step4);

					Monitor.Pulse(cs); // Pending 结束，唤醒一个进来就可以了。

					logger.Debug("8 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);
					rpc.SendResult();
				}
				else {
					var step5 = new OperatesLog(rpc.getSender().getRemoteAddress(), rpc.getUniqueRequestId());
					// senderIsShare 在失败的时候，Acquired 没有变化，不需要更新。
					if (senderIsShare) {
						step5.AddCacheStateShare(gkey, sender.getId());
						//cs.Share.Add(sender);
						// 失败了，要把原来是share的sender恢复。先这样吧。
					}

					step5.SetCacheStateAcquireStatePending(gkey, GlobalCacheManager.StateInvalid);
					//cs.AcquireStatePending = GlobalCacheManager.StateInvalid;

					getRaft().AppendLog(step5);

					Monitor.Pulse(cs); // Pending 结束，唤醒一个进来就可以了。

					logger.Error("XXX 10 {0} {1} {2}", sender, rpc.getArgument().getState(), cs);

					rpc.getResult().setState(GlobalCacheManager.StateInvalid);
					rpc.SendResultCode(GlobalCacheManager.AcquireModifyFaild);
				}
				// 很好，网络失败不再看成成功，发现除了加break，
				// 其他处理已经能包容这个改动，都不用动。
				return 0;
			}
		}
	}

	public static class RaftDatas extends Zeze.Raft.StateMachine {
		private ConcurrentMap<GlobalCacheManager.GlobalTableKey, CacheState> Global;
		public final ConcurrentMap<GlobalCacheManager.GlobalTableKey, CacheState> getGlobal() {
			return Global;
		}

		private ConcurrentMap<Integer, CacheHolder> Sessions;
		public final ConcurrentMap<Integer, CacheHolder> getSessions() {
			return Sessions;
		}

		public RaftDatas(GlobalCacheManager.GCMConfig config) {
			Global = new ConcurrentMap<GlobalCacheManager.GlobalTableKey, CacheState> (config.getConcurrencyLevel(), (int)config.getInitialCapacity());
			Sessions = new ConcurrentMap<Integer, CacheHolder>(config.getConcurrencyLevel(), 4096);
			AddFactory((new OperatesLog("", 0)).TypeId, () -> new OperatesLog("", 0));
		}

		@Override
		public void LoadFromSnapshot(String path) {
			synchronized (getRaft()) {
				try (var file = new System.IO.FileStream(path, System.IO.FileMode.Open)) {
					getGlobal().UnSerializeFrom(file);
					getSessions().UnSerializeFrom(file);
				}
			}
		}

		@Override
		public boolean Snapshot(String path, tangible.OutObject<Long> LastIncludedIndex, tangible.OutObject<Long> LastIncludedTerm) {
			try (var file = new System.IO.FileStream(path, System.IO.FileMode.Create)) {
				synchronized (getRaft()) {
					var lastAppliedLog = getRaft().getLogSequence().LastAppliedLog();
					LastIncludedIndex.outArgValue = lastAppliedLog.getIndex();
					LastIncludedTerm.outArgValue = lastAppliedLog.getTerm();
					if (!getGlobal().StartSerialize()) {
						return false;
					}
					if (!getSessions().StartSerialize()) {
						return false;
					}
				}
				getGlobal().SerializeTo(file);
				getSessions().SerializeTo(file);
				long oldFirstIndex = 0;
				synchronized (getRaft()) {
					getGlobal().EndSerialize();
					getSessions().EndSerialize();
					// 先关闭文件，结束Snapshot。
					// 马上调整FirstIndex允许请求在新的状态上工作。
					// 然后在锁外，慢慢删除旧的日志。
					file.Close();
					oldFirstIndex = getRaft().getLogSequence().GetAndSetFirstIndex(LastIncludedIndex.outArgValue);
				}
				getRaft().getLogSequence().RemoveLogBeforeLastApplied(oldFirstIndex);
				return true;
			}
		}
	}
	/** 
	 【优化】按顺序记录多个修改数据的操作，减少提交给Raft的日志数量。
	*/
	public final static class OperatesLog extends Log {
		private ArrayList<Operate> Operates = new ArrayList<Operate> ();
		private ArrayList<Operate> getOperates() {
			return Operates;
		}

		public OperatesLog(String appInstance, long requestId) {
			super(appInstance, requestId);

		}

		public void RemoveCacheState(GlobalCacheManager.GlobalTableKey key) {
			RemoveCacheState tempVar = new RemoveCacheState();
			tempVar.setKey(key);
			getOperates().add(tempVar);
		}

		public void SetCacheStateAcquireStatePending(GlobalCacheManager.GlobalTableKey key, int state) {
			SetCacheStateAcquireStatePending tempVar = new SetCacheStateAcquireStatePending();
			tempVar.setKey(key);
			tempVar.setAcquireStatePending(state);
			getOperates().add(tempVar);
		}

		public void SetCacheStateModify(GlobalCacheManager.GlobalTableKey key, int id) {
			SetCacheStateModify tempVar = new SetCacheStateModify();
			tempVar.setKey(key);
			tempVar.setModify(id);
			getOperates().add(tempVar);
		}

		public void AddCacheStateShare(GlobalCacheManager.GlobalTableKey key, int id) {
			AddCacheStateShare tempVar = new AddCacheStateShare();
			tempVar.setKey(key);
			tempVar.setShare(id);
			getOperates().add(tempVar);
		}

		public void RemoveCacheStateShare(GlobalCacheManager.GlobalTableKey key, int id) {
			RemoveCacheStateShare tempVar = new RemoveCacheStateShare();
			tempVar.setKey(key);
			tempVar.setShare(id);
			getOperates().add(tempVar);
		}

		public void PutCacheHolderAcquired(int id, GlobalCacheManager.GlobalTableKey key, int state) {
			PutCacheHolderAcquired tempVar = new PutCacheHolderAcquired();
			tempVar.setId(id);
			tempVar.setKey(key);
			tempVar.setAcquireState(state);
			getOperates().add(tempVar);
		}

		public void RemoveCacheHolderAcquired(int id, GlobalCacheManager.GlobalTableKey key) {
			RemoveCacheHolderAcquired tempVar = new RemoveCacheHolderAcquired();
			tempVar.setId(id);
			tempVar.setKey(key);
			getOperates().add(tempVar);
		}

		@Override
		public void Apply(StateMachine stateMachine) {
			var sm = stateMachine instanceof RaftDatas ? (RaftDatas)stateMachine : null;
			for (var op : getOperates()) {
				op.Apply(sm);
			}
		}

		@Override
		public void Decode(ByteBuffer bb) {
			super.Decode(bb);
			for (int count = bb.ReadInt(); count > 0; --count) {
				var opid = bb.ReadInt();
				if (getInstance().getOperateFactory().containsKey(opid) && (var factory = getInstance().getOperateFactory().get(opid)) == var factory) {
					Operate op = factory();
					op.Decode(bb);
				}
				else {
					throw new RuntimeException(String.format("Unknown Operate With Id=%1$s", opid));
				}
			}
		}

		@Override
		public void Encode(ByteBuffer bb) {
			super.Encode(bb);
			bb.WriteInt(getOperates().size());
			for (var op : getOperates()) {
				bb.WriteInt(op.getTypeId());
				op.Encode(bb);
			}
		}
	}

	private HashMap<Integer, tangible.Func0Param<Operate>> OperateFactory = new HashMap<Integer, tangible.Func0Param<Operate>> ();
	private HashMap<Integer, tangible.Func0Param<Operate>> getOperateFactory() {
		return OperateFactory;
	}

	public abstract static class Operate implements Serializable {
		public int getTypeId() {
			return (int)Zeze.Transaction.Bean.Hash32(this.getClass().getName());
		}
		// 这里不管多线程，由调用者决定并发。
		public abstract void Apply(RaftDatas sm);
		public abstract void Encode(ByteBuffer bb);
		public abstract void Decode(ByteBuffer bb);
	}

	public final static class PutCacheHolderAcquired extends Operate {
		private int Id;
		public int getId() {
			return Id;
		}
		public void setId(int value) {
			Id = value;
		}
		private GlobalCacheManager.GlobalTableKey Key;
		public GlobalCacheManager.GlobalTableKey getKey() {
			return Key;
		}
		public void setKey(GlobalCacheManager.GlobalTableKey value) {
			Key = value;
		}
		private int AcquireState;
		public int getAcquireState() {
			return AcquireState;
		}
		public void setAcquireState(int value) {
			AcquireState = value;
		}

		@Override
		public void Apply(RaftDatas sm) {
			sm.getSessions().Update(getId(), (v) -> v.Acquired[getKey()] = getAcquireState());
		}

		@Override
		public void Decode(ByteBuffer bb) {
			setId(bb.ReadInt());
			setKey(new Zeze.Services.GlobalCacheManager.GlobalTableKey());
			getKey().Decode(bb);
			setAcquireState(bb.ReadInt());
		}

		@Override
		public void Encode(ByteBuffer bb) {
			bb.WriteInt(getId());
			getKey().Encode(bb);
			bb.WriteInt(getAcquireState());
		}
	}

	public final static class RemoveCacheHolderAcquired extends Operate {
		private int Id;
		public int getId() {
			return Id;
		}
		public void setId(int value) {
			Id = value;
		}
		private GlobalCacheManager.GlobalTableKey Key;
		public GlobalCacheManager.GlobalTableKey getKey() {
			return Key;
		}
		public void setKey(GlobalCacheManager.GlobalTableKey value) {
			Key = value;
		}

		@Override
		public void Apply(RaftDatas sm) {
//C# TO JAVA CONVERTER TODO TASK: The following method call contained an unresolved 'out' keyword - these cannot be converted using the 'OutObject' helper class unless the method is within the code being modified:
			sm.getSessions().Update(getId(), (v) -> v.Acquired.TryRemove(getKey(), out var _));
		}

		@Override
		public void Decode(ByteBuffer bb) {
			setId(bb.ReadInt());
			setKey(new Zeze.Services.GlobalCacheManager.GlobalTableKey());
			getKey().Decode(bb);
		}

		@Override
		public void Encode(ByteBuffer bb) {
			bb.WriteInt(getId());
			getKey().Encode(bb);
		}
	}

	public final static class RemoveCacheState extends Operate {
		private GlobalCacheManager.GlobalTableKey Key;
		public GlobalCacheManager.GlobalTableKey getKey() {
			return Key;
		}
		public void setKey(GlobalCacheManager.GlobalTableKey value) {
			Key = value;
		}

		@Override
		public void Apply(RaftDatas sm) {
			sm.getGlobal().Remove(getKey());
		}

		@Override
		public void Decode(ByteBuffer bb) {
			setKey(new Zeze.Services.GlobalCacheManager.GlobalTableKey());
			getKey().Decode(bb);
		}

		@Override
		public void Encode(ByteBuffer bb) {
			getKey().Encode(bb);
		}
	}

	public final static class SetCacheStateAcquireStatePending extends Operate {
		private GlobalCacheManager.GlobalTableKey Key;
		public GlobalCacheManager.GlobalTableKey getKey() {
			return Key;
		}
		public void setKey(GlobalCacheManager.GlobalTableKey value) {
			Key = value;
		}
		private int AcquireStatePending;
		public int getAcquireStatePending() {
			return AcquireStatePending;
		}
		public void setAcquireStatePending(int value) {
			AcquireStatePending = value;
		}

		@Override
		public void Apply(RaftDatas sm) {
			sm.getGlobal().Update(getKey(), (v) -> v.AcquireStatePending = getAcquireStatePending());
		}

		@Override
		public void Decode(ByteBuffer bb) {
			setKey(new Zeze.Services.GlobalCacheManager.GlobalTableKey());
			getKey().Decode(bb);
			setAcquireStatePending(bb.ReadInt());
		}

		@Override
		public void Encode(ByteBuffer bb) {
			getKey().Encode(bb);
			bb.WriteInt(getAcquireStatePending());
		}
	}

	public final static class SetCacheStateModify extends Operate {
		private GlobalCacheManager.GlobalTableKey Key;
		public GlobalCacheManager.GlobalTableKey getKey() {
			return Key;
		}
		public void setKey(GlobalCacheManager.GlobalTableKey value) {
			Key = value;
		}
		private int Modify;
		public int getModify() {
			return Modify;
		}
		public void setModify(int value) {
			Modify = value;
		}

		@Override
		public void Apply(RaftDatas sm) {
			sm.getGlobal().Update(getKey(), (v) -> v.Modify = getModify());
		}

		@Override
		public void Decode(ByteBuffer bb) {
			setKey(new Zeze.Services.GlobalCacheManager.GlobalTableKey());
			getKey().Decode(bb);
			setModify(bb.ReadInt());
		}

		@Override
		public void Encode(ByteBuffer bb) {
			getKey().Encode(bb);
			bb.WriteInt(getModify());
		}
	}

	public final static class AddCacheStateShare extends Operate {
		private Zeze.Services.GlobalCacheManager.GlobalTableKey Key;
		public Zeze.Services.GlobalCacheManager.GlobalTableKey getKey() {
			return Key;
		}
		public void setKey(Zeze.Services.GlobalCacheManager.GlobalTableKey value) {
			Key = value;
		}
		private int Share;
		public int getShare() {
			return Share;
		}
		public void setShare(int value) {
			Share = value;
		}

		@Override
		public void Apply(RaftDatas sm) {
			sm.getGlobal().Update(getKey(), (v) -> v.Share.Add(getShare()));
		}

		@Override
		public void Decode(ByteBuffer bb) {
			setKey(new Zeze.Services.GlobalCacheManager.GlobalTableKey());
			getKey().Decode(bb);
			setShare(bb.ReadInt());
		}

		@Override
		public void Encode(ByteBuffer bb) {
			getKey().Encode(bb);
			bb.WriteInt(getShare());
		}
	}

	public final static class RemoveCacheStateShare extends Operate {
		private Zeze.Services.GlobalCacheManager.GlobalTableKey Key;
		public Zeze.Services.GlobalCacheManager.GlobalTableKey getKey() {
			return Key;
		}
		public void setKey(Zeze.Services.GlobalCacheManager.GlobalTableKey value) {
			Key = value;
		}
		private int Share;
		public int getShare() {
			return Share;
		}
		public void setShare(int value) {
			Share = value;
		}

		@Override
		public void Apply(RaftDatas sm) {
			sm.getGlobal().Update(getKey(), (v) -> v.Share.Remove(getShare()));
		}

		@Override
		public void Decode(ByteBuffer bb) {
			setKey(new Zeze.Services.GlobalCacheManager.GlobalTableKey());
			getKey().Decode(bb);
			setShare(bb.ReadInt());
		}

		@Override
		public void Encode(ByteBuffer bb) {
			getKey().Encode(bb);
			bb.WriteInt(getShare());
		}
	}

	public final static class CacheState implements Copyable<CacheState> {
		private int AcquireStatePending = Services.GlobalCacheManager.StateInvalid;
		public int getAcquireStatePending() {
			return AcquireStatePending;
		}
		public void setAcquireStatePending(int value) {
			AcquireStatePending = value;
		}
		private int Modify;
		public int getModify() {
			return Modify;
		}
		public void setModify(int value) {
			Modify = value;
		}
		private HashSet<Integer> Share = new HashSet<Integer> ();
		public HashSet<Integer> getShare() {
			return Share;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			ByteBuffer.BuildString(sb, getShare());
			return String.format("P%1$s M%2$s S%3$s", getAcquireStatePending(), getModify(), sb);
		}

		public CacheState() {
		}

		private CacheState(CacheState other) {
			setAcquireStatePending(other.getAcquireStatePending());
			setModify(other.getModify());
			for (var e : other.getShare()) {
				getShare().add(e);
			}
		}

		public CacheState Copy() {
			return new CacheState(this);
		}

		public void Decode(ByteBuffer bb) {
			setAcquireStatePending(bb.ReadInt());
			setModify(bb.ReadInt());
			getShare().clear();
			for (int count = bb.ReadInt(); count > 0; --count) {
				getShare().add(bb.ReadInt());
			}
		}

		public void Encode(ByteBuffer bb) {
			bb.WriteInt(getAcquireStatePending());
			bb.WriteInt(getModify());
			bb.WriteInt(getShare().size());
			for (var e : getShare()) {
				bb.WriteInt(e);
			}
		}
	}

	public final static class CacheHolder implements Copyable<CacheHolder> {
		private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

		// local only. 每一个Raft服务器独立设置。【不会系列化】。
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

		// 已分配给这个cache的记录。【需要系列化】。
		private java.util.concurrent.ConcurrentHashMap<GlobalCacheManager.GlobalTableKey, Integer> Acquired;
		public java.util.concurrent.ConcurrentHashMap<GlobalCacheManager.GlobalTableKey, Integer> getAcquired() {
			return Acquired;
		}

		public CacheHolder(GlobalCacheManager.GCMConfig config) {
			Acquired = new java.util.concurrent.ConcurrentHashMap<GlobalCacheManager.GlobalTableKey, Integer> (config.getConcurrencyLevel(), (int)config.getInitialCapacity());
		}
		// 【需要系列化】。
		private int Id;
		public int getId() {
			return Id;
		}
		private void setId(int value) {
			Id = value;
		}

		public CacheHolder(int key) {
			setId(key);
		}

		public boolean TryBindSocket(AsyncSocket newSocket, int _GlobalCacheManagerHashIndex) {
			synchronized (this) {
				if (newSocket.getUserState() != null) {
					return false; // 不允许再次绑定。Login Or ReLogin 只能发一次。
				}

				var socket = Services.GlobalCacheManagerWithRaft.getInstance().getRaft().getServer().GetSocket(getSessionId());
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

				var socket = Services.GlobalCacheManagerWithRaft.getInstance().getRaft().getServer().GetSocket(getSessionId());
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

		public int Reduce(GlobalCacheManager.GlobalTableKey gkey, int state) {
			try {
				var reduce = ReduceWaitLater(gkey, state);
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
				return Services.GlobalCacheManager.StateReduceRpcTimeout;
			}
			catch (RuntimeException ex) {
				logger.Error(ex, "Reduce Exception {0} target={1}", state, getSessionId());
				return Services.GlobalCacheManager.StateReduceException;
			}
		}

		public static final long ForbitPeriod = 10 * 1000; // 10 seconds
		private long LastErrorTime = 0; // 本地变量，【不会系列化】。

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
		public GlobalCacheManager.Reduce ReduceWaitLater(GlobalCacheManager.GlobalTableKey gkey, int state) {
			try {
				synchronized (this) {
					if (Zeze.Util.Time.getNowUnixMillis() - LastErrorTime < ForbitPeriod) {
						return null;
					}
				}
				AsyncSocket peer = Services.GlobalCacheManagerWithRaft.getInstance().getRaft().getServer().GetSocket(getSessionId());
				if (null != peer) {
					var reduce = new Zeze.Services.GlobalCacheManager.Reduce(gkey, state);
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

		public CacheHolder() {
		}

		private CacheHolder(CacheHolder other) {
			setSessionId(other.getSessionId());
			setGlobalCacheManagerHashIndex(other.getGlobalCacheManagerHashIndex());
			for (var e : other.getAcquired()) {
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
				getAcquired().TryAdd(e.Key, e.Value);
			}
		}

		public CacheHolder Copy() {
			return new CacheHolder(this);
		}

		public void Decode(ByteBuffer bb) {
			setId(bb.ReadInt());
			getAcquired().clear();
			for (int count = bb.ReadInt(); count > 0; --count) {
				var key = new Zeze.Services.GlobalCacheManager.GlobalTableKey();
				key.Decode(bb);
				int value = bb.ReadInt();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
				getAcquired().TryAdd(key, value);
			}
		}

		public void Encode(ByteBuffer bb) {
			bb.WriteInt(getId());
			bb.WriteInt(getAcquired().size());
			for (var e : getAcquired()) {
				e.Key.Encode(bb);
				bb.WriteInt(e.Value);
			}
		}
	}
}