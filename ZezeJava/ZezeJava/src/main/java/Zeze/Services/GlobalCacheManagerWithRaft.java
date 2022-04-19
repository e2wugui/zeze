package Zeze.Services;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import Zeze.Beans.GlobalCacheManagerWithRaft.Acquire;
import Zeze.Beans.GlobalCacheManagerWithRaft.AcquiredState;
import Zeze.Beans.GlobalCacheManagerWithRaft.CacheState;
import Zeze.Beans.GlobalCacheManagerWithRaft.Cleanup;
import Zeze.Beans.GlobalCacheManagerWithRaft.GlobalTableKey;
import Zeze.Beans.GlobalCacheManagerWithRaft.KeepAlive;
import Zeze.Beans.GlobalCacheManagerWithRaft.Login;
import Zeze.Beans.GlobalCacheManagerWithRaft.NormalClose;
import Zeze.Beans.GlobalCacheManagerWithRaft.ReLogin;
import Zeze.Beans.GlobalCacheManagerWithRaft.Reduce;
import Zeze.Beans.GlobalCacheManagerWithRaft.ReduceParam;
import Zeze.Net.AsyncSocket;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.Procedure;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Raft.RocksRaft.Table;
import Zeze.Raft.RocksRaft.TableTemplate;
import Zeze.Raft.RocksRaft.Transaction;
import Zeze.Util.KV;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GlobalCacheManagerWithRaft
		extends AbstractGlobalCacheManagerWithRaft implements Closeable, GlobalCacheManagerConst {
	private static final Logger logger = LogManager.getLogger(GlobalCacheManagerWithRaft.class);
	public static final int GlobalSerialIdAtomicLongIndex = 0;

	private final Rocks Rocks;
	private final GlobalLocks Locks = new GlobalLocks();
	/**
	 * 全局记录分配状态。
	 */
	private final Table<GlobalTableKey, CacheState> GlobalStates;
	/**
	 * 每个服务器已分配记录。
	 * 这是个Table模板，使用的时候根据ServerId打开真正的存储表。
	 */
	private final TableTemplate<GlobalTableKey, AcquiredState> ServerAcquiredTemplate;
	/*
	 * 会话。
	 * key是 LogicServer.Id，现在的实现就是Zeze.Config.ServerId。
	 * 在连接建立后收到的Login Or ReLogin 中设置。
	 * 每个会话还需要记录该会话的Socket.SessionId。在连接重新建立时更新。
	 * 总是GetOrAdd，不删除。按现在的cache-sync设计，
	 * ServerId 是及其有限的。不会一直增长。
	 * 简化实现。
	 */
	private final LongConcurrentHashMap<CacheHolder> Sessions = new LongConcurrentHashMap<>();

	public GlobalCacheManagerWithRaft(String raftName) throws Throwable {
		this(raftName, null, null, false);
	}

	public GlobalCacheManagerWithRaft(String raftName, RaftConfig raftConf) throws Throwable {
		this(raftName, raftConf, null, false);
	}

	public GlobalCacheManagerWithRaft(String raftName, RaftConfig raftConf, Zeze.Config config)
			throws Throwable {
		this(raftName, raftConf, config, false);
	}

	public GlobalCacheManagerWithRaft(String raftName, RaftConfig raftConf, Zeze.Config config,
									  boolean RocksDbWriteOptionSync) throws Throwable {
		Rocks = new Rocks(raftName, RocksMode.Pessimism, raftConf, config, RocksDbWriteOptionSync);

		RegisterRocksTables(Rocks);
		RegisterProtocols(Rocks.getRaft().getServer());

		GlobalStates = Rocks.<GlobalTableKey, CacheState>GetTableTemplate("Global").OpenTable(0);
		ServerAcquiredTemplate = Rocks.GetTableTemplate("Session");

		Rocks.getRaft().getServer().Start();
	}

	public Rocks getRocks() {
		return Rocks;
	}

	private static AcquiredState newAcquiredState(int state) {
		AcquiredState acquiredState = new AcquiredState();
		acquiredState.setState(state);
		return acquiredState;
	}

	@Override
	protected long ProcessAcquireRequest(Acquire rpc) throws Throwable {
		rpc.Result.setGlobalTableKey(rpc.Argument.getGlobalTableKey());
		rpc.Result.setState(rpc.Argument.getState()); // default success
		rpc.setResultCode(0);

		if (rpc.getSender().getUserState() == null) {
			rpc.Result.setState(StateInvalid);
			// 没有登录重做。登录是Agent自动流程的一部分，应该稍后重试。
			rpc.SendResultCode(Zeze.Transaction.Procedure.RaftRetry);
			return 0L;
		}

		var proc = new Procedure(Rocks, () -> {
			switch (rpc.Argument.getState()) {
			case StateInvalid: // release
				rpc.Result.setState(_Release((CacheHolder)rpc.getSender().getUserState(),
						rpc.Argument.getGlobalTableKey(), true));
				return 0L;

			case StateShare:
				return AcquireShare(rpc);

			case StateModify:
				return AcquireModify(rpc);

			default:
				rpc.Result.setState(StateInvalid);
				return AcquireErrorState;
			}
		});
		proc.AutoResponse = rpc; // 启用自动发送rpc结果，但不做唯一检查。
		proc.Call();
		return 0; // has handle all error.
	}

	private long AcquireShare(Acquire rpc) throws InterruptedException {
		CacheHolder sender = (CacheHolder)rpc.getSender().getUserState();
		GlobalTableKey globalTableKey = rpc.Argument.getGlobalTableKey();
		int acquireState = rpc.Argument.getState();

		while (true) {
			var lockey = Transaction.getCurrent().AddPessimismLock(Locks.Get(globalTableKey));

			CacheState cs = GlobalStates.GetOrAdd(globalTableKey);
			if (cs.getAcquireStatePending() == StateRemoved)
				continue;

			if (cs.getModify() != -1 && cs.getShare().size() != 0)
				throw new IllegalStateException("CacheState state error");

			while (cs.getAcquireStatePending() != StateInvalid && cs.getAcquireStatePending() != StateRemoved) {
				switch (cs.getAcquireStatePending()) {
				case StateShare:
					if (cs.getModify() == -1)
						throw new IllegalStateException("CacheState state error");

					if (cs.getModify() == sender.getServerId()) {
						logger.debug("1 {} {} {}", sender, acquireState, cs);
						rpc.Result.setState(StateInvalid);
						rpc.Result.setGlobalSerialId(cs.getGlobalSerialId());
						return AcquireShareDeadLockFound;
					}
					break;
				case StateModify:
					if (cs.getModify() == sender.getServerId() || cs.getShare().Contains(sender.getServerId())) {
						logger.debug("2 {} {} {}", sender, acquireState, cs);
						rpc.Result.setState(StateInvalid);
						rpc.Result.setGlobalSerialId(cs.getGlobalSerialId());
						return AcquireShareDeadLockFound;
					}
					break;
				case StateRemoving:
					break;
				}
				logger.debug("3 {} {} {}", sender, acquireState, cs);
				lockey.Wait();
				if (cs.getModify() != -1 && cs.getShare().size() != 0)
					throw new IllegalStateException("CacheState state error");
			}

			if (cs.getAcquireStatePending() == StateRemoved)
				continue; // concurrent release.

			cs.setAcquireStatePending(StateShare);
			cs.setGlobalSerialId(Rocks.AtomicLongIncrementAndGet(GlobalSerialIdAtomicLongIndex));
			var SenderAcquired = ServerAcquiredTemplate.OpenTable(sender.getServerId());
			if (cs.getModify() != -1) {
				if (cs.getModify() == sender.getServerId()) {
					// 已经是Modify又申请，可能是sender异常关闭，
					// 又重启连上。更新一下。应该是不需要的。
					SenderAcquired.Put(globalTableKey, newAcquiredState(StateModify));
					cs.setAcquireStatePending(StateInvalid);
					logger.debug("4 {} {} {}", sender, acquireState, cs);
					rpc.Result.setState(StateModify);
					rpc.Result.setGlobalSerialId(cs.getGlobalSerialId());
					return AcquireShareAlreadyIsModify;
				}

				var reduceResultState = new OutObject<>(StateReduceNetError); // 默认网络错误。
				if (CacheHolder.Reduce(Sessions, cs.getModify(), globalTableKey, cs.getGlobalSerialId(), p -> {
					reduceResultState.Value = p.isTimeout() ? StateReduceRpcTimeout : p.Result.getState();
					lockey.Enter();
					try {
						lockey.PulseAll();
					} finally {
						lockey.Exit();
					}
					return 0;
				})) {
					logger.debug("5 {} {} {}", sender, acquireState, cs);
					lockey.Wait();
				}

				var ModifyAcquired = ServerAcquiredTemplate.OpenTable(cs.getModify());
				switch (reduceResultState.Value) {
				case StateShare:
					ModifyAcquired.Put(globalTableKey, newAcquiredState(StateShare));
					cs.getShare().add(cs.getModify()); // 降级成功。
					break;

				case StateInvalid:
					// 降到了 Invalid，此时就不需要加入 Share 了。
					ModifyAcquired.Remove(globalTableKey);
					break;

				default:
					// 包含协议返回错误的值的情况。
					// case StateReduceRpcTimeout: // 11
					// case StateReduceException: // 12
					// case StateReduceNetError: // 13
					cs.setAcquireStatePending(StateInvalid);
					logger.error("XXX 8 state={} {} {} {}", reduceResultState.Value, sender, acquireState, cs);
					rpc.Result.setState(StateInvalid);
					rpc.Result.setGlobalSerialId(cs.getGlobalSerialId());
					lockey.PulseAll();
					return AcquireShareFailed;
				}

				SenderAcquired.Put(globalTableKey, newAcquiredState(StateShare));
				cs.setModify(-1);
				cs.getShare().add(sender.getServerId());
				cs.setAcquireStatePending(StateInvalid);
				logger.debug("6 {} {} {}", sender, acquireState, cs);
				rpc.Result.setGlobalSerialId(cs.getGlobalSerialId());
				lockey.PulseAll();
				return 0; // 成功也会自动发送结果.
			}

			SenderAcquired.Put(globalTableKey, newAcquiredState(StateShare));
			cs.getShare().add(sender.getServerId());
			cs.setAcquireStatePending(StateInvalid);
			logger.debug("7 {} {} {}", sender, acquireState, cs);
			rpc.Result.setGlobalSerialId(cs.getGlobalSerialId());
			lockey.PulseAll();
			return 0; // 成功也会自动发送结果.
		}
	}

	private long AcquireModify(Acquire rpc) throws InterruptedException {
		CacheHolder sender = (CacheHolder)rpc.getSender().getUserState();
		GlobalTableKey globalTableKey = rpc.Argument.getGlobalTableKey();
		int acquireState = rpc.Argument.getState();

		while (true) {
			var lockey = Transaction.getCurrent().AddPessimismLock(Locks.Get(globalTableKey));

			CacheState cs = GlobalStates.GetOrAdd(globalTableKey);
			if (cs.getAcquireStatePending() == StateRemoved)
				continue;

			if (cs.getModify() != -1 && cs.getShare().size() != 0)
				throw new IllegalStateException("CacheState state error");

			while (cs.getAcquireStatePending() != StateInvalid && cs.getAcquireStatePending() != StateRemoved) {
				switch (cs.getAcquireStatePending()) {
				case StateShare:
					if (cs.getModify() == -1) {
						logger.error("cs state must be modify");
						throw new IllegalStateException("CacheState state error");
					}
					if (cs.getModify() == sender.getServerId()) {
						logger.debug("1 {} {} {}", sender, acquireState, cs);
						rpc.Result.setState(StateInvalid);
						rpc.Result.setGlobalSerialId(cs.getGlobalSerialId());
						return AcquireModifyDeadLockFound;
					}
					break;
				case StateModify:
					if (cs.getModify() == sender.getServerId() || cs.getShare().Contains(sender.getServerId())) {
						logger.debug("2 {} {} {}", sender, acquireState, cs);
						rpc.Result.setState(StateInvalid);
						rpc.Result.setGlobalSerialId(cs.getGlobalSerialId());
						return AcquireModifyDeadLockFound;
					}
					break;
				case StateRemoving:
					break;
				}
				logger.debug("3 {} {} {}", sender, acquireState, cs);
				lockey.Wait();
				if (cs.getModify() != -1 && cs.getShare().size() != 0)
					throw new IllegalStateException("CacheState state error");
			}
			if (cs.getAcquireStatePending() == StateRemoved)
				continue; // concurrent release

			cs.setAcquireStatePending(StateModify);
			cs.setGlobalSerialId(Rocks.AtomicLongIncrementAndGet(GlobalSerialIdAtomicLongIndex));
			var SenderAcquired = ServerAcquiredTemplate.OpenTable(sender.getServerId());
			if (cs.getModify() != -1) {
				if (cs.getModify() == sender.getServerId()) {
					// 已经是Modify又申请，可能是sender异常关闭，又重启连上。
					// 更新一下。应该是不需要的。
					SenderAcquired.Put(globalTableKey, newAcquiredState(StateModify));
					cs.setAcquireStatePending(StateInvalid);
					logger.debug("4 {} {} {}", sender, acquireState, cs);
					rpc.Result.setGlobalSerialId(cs.getGlobalSerialId());
					lockey.PulseAll();
					return AcquireModifyAlreadyIsModify;
				}

				var reduceResultState = new OutObject<>(StateReduceNetError); // 默认网络错误。
				if (CacheHolder.Reduce(Sessions, cs.getModify(), globalTableKey, cs.getGlobalSerialId(), p -> {
					reduceResultState.Value = p.isTimeout() ? StateReduceRpcTimeout : p.Result.getState();
					lockey.Enter();
					try {
						lockey.PulseAll();
					} finally {
						lockey.Exit();
					}
					return 0;
				})) {
					logger.debug("5 {} {} {}", sender, acquireState, cs);
					lockey.Wait();
				}

				var ModifyAcquired = ServerAcquiredTemplate.OpenTable(cs.getModify());
				//noinspection SwitchStatementWithTooFewBranches
				switch (reduceResultState.Value) {
				case StateInvalid:
					ModifyAcquired.Remove(globalTableKey);
					break; // reduce success

				default:
					// case StateReduceRpcTimeout: // 11
					// case StateReduceException: // 12
					// case StateReduceNetError: // 13
					cs.setAcquireStatePending(StateInvalid);
					logger.error("XXX 9 {} {} {}", sender, acquireState, cs);
					rpc.Result.setState(StateInvalid);
					rpc.Result.setGlobalSerialId(cs.getGlobalSerialId());
					lockey.PulseAll();
					return AcquireModifyFailed;
				}

				cs.setModify(sender.getServerId());
				cs.getShare().remove(sender.getServerId());
				SenderAcquired.Put(globalTableKey, newAcquiredState(StateModify));
				cs.setAcquireStatePending(StateInvalid);
				lockey.PulseAll();

				logger.debug("6 {} {} {}", sender, acquireState, cs);
				rpc.Result.setGlobalSerialId(cs.getGlobalSerialId());
				return 0;
			}

			ArrayList<KV<CacheHolder, Reduce>> reducePending = new ArrayList<>();
			HashSet<CacheHolder> reduceSucceed = new HashSet<>();
			boolean senderIsShare = false;
			// 先把降级请求全部发送给出去。
			for (var c : cs.getShare()) {
				if (c == sender.getServerId()) {
					senderIsShare = true;
					reduceSucceed.add(sender);
					continue;
				}
				var kv = CacheHolder.ReduceWaitLater(Sessions, c, globalTableKey, cs.getGlobalSerialId());
				if (kv == null) {
					// 网络错误不再认为成功。整个降级失败，要中断降级。
					// 已经发出去的降级请求要等待并处理结果。后面处理。
					break;
				}
				reducePending.add(kv);
			}
			// 两种情况不需要发reduce
			// 1. share是空的, 可以直接升为Modify
			// 2. sender是share, 而且reducePending是空的
			if (cs.getShare().size() != 0 && (!senderIsShare || !reducePending.isEmpty())) {
				Task.run(() -> {
					// 一个个等待是否成功。WaitAll 碰到错误不知道怎么处理的，
					// 应该也会等待所有任务结束（包括错误）。
					for (var kv : reducePending) {
						CacheHolder session = kv.getKey();
						Reduce reduce = kv.getValue();
						try {
							reduce.getFuture().await();
							if (reduce.Result.getState() == StateInvalid)
								reduceSucceed.add(session);
							else {
								session.SetError();
								logger.warn("Reduce {} AcquireState={} CacheState={} res={}",
										sender, acquireState, cs, reduce.Result);
							}
						} catch (RuntimeException ex) {
							session.SetError();
							// 等待失败不再看作成功。
							logger.error(String.format("Reduce %s AcquireState=%d CacheState=%s arg=%s",
									sender, acquireState, cs, reduce.Argument), ex);
						}
					}
					lockey.Enter();
					try {
						lockey.PulseAll();
					} finally {
						lockey.Exit();
					}
				}, "GlobalCacheManagerWithRaft.AcquireModify.WaitReduce");
				logger.debug("7 {} {} {}", sender, acquireState, cs);
				lockey.Wait();
			}

			// 移除成功的。
			for (CacheHolder succeed : reduceSucceed) {
				if (succeed.getServerId() != sender.getServerId()) {
					// sender 不移除：
					// 1. 如果申请成功，后面会更新到Modify状态。
					// 2. 如果申请不成功，恢复 cs.Share，保持 Acquired 不变。
					var KeyAcquired = ServerAcquiredTemplate.OpenTable(succeed.getServerId());
					KeyAcquired.Remove(globalTableKey);
				}
				cs.getShare().remove(succeed.getServerId());
			}

			// 如果前面降级发生中断(break)，这里就不会为0。
			if (cs.getShare().size() != 0) {
				// senderIsShare 在失败的时候，Acquired 没有变化，不需要更新。
				// 失败了，要把原来是share的sender恢复。先这样吧。
				if (senderIsShare)
					cs.getShare().add(sender.getServerId());

				cs.setAcquireStatePending(StateInvalid);
				logger.error("XXX 10 {} {} {}", sender, acquireState, cs);
				rpc.Result.setState(StateInvalid);
				rpc.Result.setGlobalSerialId(cs.getGlobalSerialId());
				lockey.PulseAll();
				return AcquireModifyFailed;
			}

			SenderAcquired.Put(globalTableKey, newAcquiredState(StateModify));
			cs.setModify(sender.getServerId());
			cs.setAcquireStatePending(StateInvalid);
			logger.debug("8 {} {} {}", sender, acquireState, cs);
			rpc.Result.setGlobalSerialId(cs.getGlobalSerialId());
			lockey.PulseAll();
			return 0; // 成功也会自动发送结果.
		}
	}

	private void Release(CacheHolder sender, GlobalTableKey gkey) throws Throwable {
		Rocks.NewProcedure(() -> {
			_Release(sender, gkey, false);
			return 0L;
		}).Call();
	}

	private int _Release(CacheHolder sender, GlobalTableKey gkey, boolean noWait) throws InterruptedException {
		while (true) {
			var lockey = Transaction.getCurrent().AddPessimismLock(Locks.Get(gkey));

			CacheState cs = GlobalStates.GetOrAdd(gkey);
			if (cs.getAcquireStatePending() == StateRemoved)
				continue; // 这个是不可能的，因为有Release请求进来意味着肯定有拥有者(share or modify)，此时不可能进入StateRemoved。

			while (cs.getAcquireStatePending() != StateInvalid && cs.getAcquireStatePending() != StateRemoved) {
				switch (cs.getAcquireStatePending()) {
				case StateShare:
				case StateModify:
					logger.debug("Release 0 {} {} {}", sender, gkey, cs);
					if (noWait)
						return GetSenderCacheState(cs, sender);
					break;
				case StateRemoving:
					// release 不会导致死锁，等待即可。
					break;
				}
				lockey.Wait();
			}
			if (cs.getAcquireStatePending() == StateRemoved)
				continue;
			cs.setAcquireStatePending(StateRemoving);

			if (cs.getModify() == sender.getServerId())
				cs.setModify(-1);
			cs.getShare().remove(sender.getServerId()); // always try remove

			if (cs.getModify() == -1 && cs.getShare().size() == 0 && cs.getAcquireStatePending() == StateInvalid) {
				// 安全的从global中删除，没有并发问题。
				cs.setAcquireStatePending(StateRemoved);
				GlobalStates.Remove(gkey);
			} else
				cs.setAcquireStatePending(StateInvalid);
			var SenderAcquired = ServerAcquiredTemplate.OpenTable(sender.getServerId());
			SenderAcquired.Remove(gkey);
			lockey.PulseAll();
			return GetSenderCacheState(cs, sender);
		}
	}

	private int GetSenderCacheState(CacheState cs, CacheHolder sender) {
		if (cs.getModify() == sender.getServerId())
			return StateModify;
		if (cs.getShare().Contains(sender.getServerId()))
			return StateShare;
		return StateInvalid;
	}

	@Override
	protected long ProcessLoginRequest(Login rpc) throws Throwable {
		var session = Sessions.computeIfAbsent(rpc.Argument.getServerId(), serverId -> {
			CacheHolder tempVar = new CacheHolder();
			tempVar.setGlobalInstance(this);
			tempVar.setServerId((int)serverId);
			return tempVar;
		});

		synchronized (session) { // 同一个节点互斥。不同节点Bind不需要互斥，Release由Raft-Leader唯一性提供保护。
			if (!session.TryBindSocket(rpc.getSender(), rpc.Argument.getGlobalCacheManagerHashIndex())) {
				rpc.SendResultCode(LoginBindSocketFail);
				return 0;
			}
			// new login, 比如逻辑服务器重启。release old acquired.
			var SenderAcquired = ServerAcquiredTemplate.OpenTable(session.ServerId);
			SenderAcquired.Walk((key, value) -> {
				Release(session, key);
				return true; // continue walk
			});
			rpc.SendResultCode(0);
			logger.info("Login {} {}.", Rocks.getRaft().getName(), rpc.getSender());
			return 0;
		}
	}

	@Override
	protected long ProcessReLoginRequest(ReLogin rpc) {
		var session = Sessions.computeIfAbsent(rpc.Argument.getServerId(), serverId -> {
			CacheHolder tempVar = new CacheHolder();
			tempVar.setGlobalInstance(this);
			tempVar.setServerId((int)serverId);
			return tempVar;
		});

		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (session) { // 同一个节点互斥。
			if (!session.TryBindSocket(rpc.getSender(), rpc.Argument.getGlobalCacheManagerHashIndex())) {
				rpc.SendResultCode(ReLoginBindSocketFail);
				return 0;
			}
			rpc.SendResultCode(0);
			logger.info("ReLogin {} {}.", Rocks.getRaft().getName(), rpc.getSender());
			return 0;
		}
	}

	@Override
	protected long ProcessNormalCloseRequest(NormalClose rpc) throws Throwable {
		Object userState = rpc.getSender().getUserState();
		if (!(userState instanceof CacheHolder)) {
			rpc.SendResultCode(AcquireNotLogin);
			return 0; // not login
		}
		CacheHolder session = (CacheHolder)userState;
		synchronized (session) { // 同一个节点互斥。不同节点Bind不需要互斥，Release由Raft-Leader唯一性提供保护。
			if (!session.TryUnBindSocket(rpc.getSender())) {
				rpc.SendResultCode(NormalCloseUnbindFail);
				return 0;
			}
			// TODO 确认Walk中删除记录是否有问题。
			var SenderAcquired = ServerAcquiredTemplate.OpenTable(session.ServerId);
			SenderAcquired.Walk((key, value) -> {
				Release(session, key);
				return true; // continue walk
			});
			rpc.SendResultCode(0);
			logger.info("NormalClose {} {}", Rocks.getRaft().getName(), rpc.getSender());
			return 0;
		}
	}

	@Override
	protected long ProcessCleanupRequest(Cleanup rpc) {
		// 安全性以后加强。
		if (!rpc.Argument.getSecureKey().equals("Ok! verify secure.")) {
			rpc.SendResultCode(CleanupErrorSecureKey);
			return 0;
		}

		var session = Sessions.computeIfAbsent(rpc.Argument.getServerId(), __ -> {
			CacheHolder tempVar = new CacheHolder();
			tempVar.setGlobalInstance(this);
			return tempVar;
		});
		if (session.GlobalCacheManagerHashIndex != rpc.Argument.getGlobalCacheManagerHashIndex()) {
			// 多点验证
			rpc.SendResultCode(CleanupErrorGlobalCacheManagerHashIndex);
			return 0;
		}

		if (Rocks.getRaft().getServer().GetSocket(session.SessionId) != null) {
			// 连接存在，禁止cleanup。
			rpc.SendResultCode(CleanupErrorHasConnection);
			return 0;
		}

		// 还有更多的防止出错的手段吗？

		// XXX verify danger
		Task.schedule(5 * 60 * 1000, () -> { // delay 5 mins
			var SenderAcquired = ServerAcquiredTemplate.OpenTable(session.ServerId);
			SenderAcquired.Walk((key, value) -> {
				Release(session, key);
				return true; // continue release;
			});
			rpc.SendResultCode(0);
		});

		return 0;
	}

	@Override
	protected long ProcessKeepAliveRequest(KeepAlive rpc) {
		rpc.SendResultCode(Zeze.Transaction.Procedure.NotImplement);
		return 0;
	}

	@Override
	public void close() throws IOException {
		try {
			Rocks.close();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	static final class CacheHolder {
		private static final long ForbidPeriod = 10 * 1000; // 10 seconds

		private long SessionId;
		private int GlobalCacheManagerHashIndex;
		private int ServerId;
		private GlobalCacheManagerWithRaft GlobalInstance;
		private long LastErrorTime;

		int getServerId() {
			return ServerId;
		}

		void setServerId(int value) {
			ServerId = value;
		}

		void setGlobalInstance(GlobalCacheManagerWithRaft value) {
			GlobalInstance = value;
		}

		boolean TryBindSocket(AsyncSocket newSocket, int globalCacheManagerHashIndex) {
			if (newSocket.getUserState() != null && newSocket.getUserState() != this)
				return false; // 允许重复login|relogin，但不允许切换ServerId。

			var socket = GlobalInstance.getRocks().getRaft().getServer().GetSocket(SessionId);
			if (socket == null || socket == newSocket) {
				// old socket not exist or has lost.
				SessionId = newSocket.getSessionId();
				newSocket.setUserState(this);
				GlobalCacheManagerHashIndex = globalCacheManagerHashIndex;
				return true;
			}
			// 每个ServerId只允许一个实例，已经存在了以后，旧的实例上有状态，阻止新的实例登录成功。
			return false;
		}

		boolean TryUnBindSocket(AsyncSocket oldSocket) {
			// 这里检查比较严格，但是这些检查应该都不会出现。

			if (oldSocket.getUserState() != this)
				return false; // not bind to this

			var socket = GlobalInstance.getRocks().getRaft().getServer().GetSocket(SessionId);
			if (socket != oldSocket)
				return false; // not same socket

			SessionId = 0;
			return true;
		}

		synchronized void SetError() {
			long now = System.currentTimeMillis();
			if (now - LastErrorTime > ForbidPeriod)
				LastErrorTime = now;
		}

		static boolean Reduce(LongConcurrentHashMap<CacheHolder> sessions, int serverId,
							  GlobalTableKey gkey, long globalSerialId,
							  ProtocolHandle<Rpc<ReduceParam, ReduceParam>> response) {
			var session = sessions.get(serverId);
			if (session == null) {
				logger.error("Reduce invalid serverId={}", serverId);
				return false;
			}
			return session.Reduce(gkey, globalSerialId, response);
		}

		static KV<CacheHolder, Reduce> ReduceWaitLater(LongConcurrentHashMap<CacheHolder> sessions, int serverId,
													   GlobalTableKey gkey, long globalSerialId) {
			CacheHolder session = sessions.get(serverId);
			if (session == null)
				return null;
			Reduce reduce = session.ReduceWaitLater(gkey, globalSerialId);
			return reduce != null ? KV.Create(session, reduce) : null;
		}

		boolean Reduce(GlobalTableKey gkey, long globalSerialId,
					   ProtocolHandle<Rpc<ReduceParam, ReduceParam>> response) {
			try {
				synchronized (this) {
					if (System.currentTimeMillis() - LastErrorTime < ForbidPeriod)
						return false;
				}
				AsyncSocket peer = GlobalInstance.getRocks().getRaft().getServer().GetSocket(SessionId);
				if (peer != null) {
					var reduce = new Reduce();
					reduce.Argument.setGlobalTableKey(gkey);
					reduce.Argument.setState(StateInvalid);
					reduce.Argument.setGlobalSerialId(globalSerialId);
					if (reduce.Send(peer, response, 10000))
						return true;
					logger.warn("Reduce send failed. SessionId={}, peer={}, gkey={}, globalSerialId={}",
							SessionId, peer, gkey, globalSerialId);
				} else
					logger.error("Reduce invalid SessionId={}. gkey={}, globalSerialId={}",
							SessionId, gkey, globalSerialId);
			} catch (RuntimeException ex) {
				// 这里的异常只应该是网络发送异常。
				logger.error("Reduce Exception: " + gkey, ex);
			}
			SetError();
			return false;
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
				AsyncSocket peer = GlobalInstance.getRocks().getRaft().getServer().GetSocket(SessionId);
				if (peer != null) {
					var reduce = new Reduce();
					reduce.Argument.setGlobalTableKey(gkey);
					reduce.Argument.setState(StateInvalid);
					reduce.Argument.setGlobalSerialId(globalSerialId);
					reduce.SendForWait(peer, 10000);
					return reduce;
				} else
					logger.error("ReduceWaitLater invalid sessionId={}", SessionId);
			} catch (RuntimeException ex) {
				// 这里的异常只应该是网络发送异常。
				logger.error("ReduceWaitLater Exception: " + gkey, ex);
			}
			SetError();
			return null;
		}

		@Override
		public String toString() {
			return String.format("%s@%s", SessionId, ServerId);
		}
	}
}
