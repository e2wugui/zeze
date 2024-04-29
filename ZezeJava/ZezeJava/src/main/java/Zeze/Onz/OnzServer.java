package Zeze.Onz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import Zeze.Application;
import Zeze.Builtin.Onz.BSavedCommits;
import Zeze.Builtin.Onz.Checkpoint;
import Zeze.Builtin.Onz.Commit;
import Zeze.Builtin.Onz.FuncProcedure;
import Zeze.Builtin.Onz.FuncSaga;
import Zeze.Builtin.Onz.FuncSagaEnd;
import Zeze.Builtin.Onz.Rollback;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Connector;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.AutoKey;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Transaction.Data;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Util.Func2;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;

/**
 * 开发onz服务器基础
 * <p>
 * 包装网络和onz协议，
 * 允许多个server实例，
 * 不同的server实例功能可以交叉也可以完全不同，
 */
public class OnzServer extends AbstractOnz {
	private static final Logger logger = LogManager.getLogger();

	private final OnzAgent onzAgent;
	private final boolean sharedServiceManager;
	private final ConcurrentHashMap<String, AbstractAgent> zezes = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Connector> instances = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, OnzTransactionStub<?, ?>> remoteStubs = new ConcurrentHashMap<>();
	private final OnzServerService service;

	private final RocksDatabase database;
	private final RocksDatabase.Table commitPoint;
	private final RocksDatabase.Table commitIndex;
	private WriteOptions writeOptions = RocksDatabase.getDefaultWriteOptions();
	private Future<?> redoTimer;
	private final AbstractAgent myServiceManager;
	private final AutoKey onzTidAutoKey;

	public long nextOnzTid() {
		return onzTidAutoKey.next();
	}

	public void setWriteOptions(WriteOptions writeOptions) {
		this.writeOptions = writeOptions;
	}

	public WriteOptions getWriteOptions() {
		return writeOptions;
	}

	/**
	 * 每个zeze集群使用独立的ServiceManager实例时，使用这个方法构造OnzServer。
	 * 建议按这种方式配置，便于解耦。
	 * 此时zezes编码如下：
	 * zeze1=zeze1.xml;zeze2=zeze2.xml;...
	 * zeze1,zeze2是OnzServer自己对每个zeze集群的命名，以后用于Onz分布式事务的调用。需要唯一。
	 * zeze1.xml,zeze2.xml是不同zeze集群的配置文件path。
	 */
	public OnzServer(String zezeConfigs, Config myConfig) throws Exception {
		myServiceManager = Application.createServiceManager(myConfig, "OnzServerMyServiceManager");
		if (myServiceManager == null)
			throw new RuntimeException("My ServiceManager not found");
		myServiceManager.start();
		try {
			myServiceManager.waitReady();
		} catch (Exception ignored) {
			// raft 版第一次等待由于选择leader原因肯定会失败一次。
			myServiceManager.waitReady();
		}
		onzTidAutoKey = myServiceManager.getAutoKey("OnzServerTidAutoKey");

		database = new RocksDatabase("CommitOnzServer" + myConfig.getServerId());
		commitPoint = database.getOrAddTable("CommitPoint");
		commitIndex = database.getOrAddTable("CommitIndex");

		var zezesArray = zezeConfigs.split(";");
		for (var zeze : zezesArray) {
			var zezeNameAndConfig = zeze.split("=");
			if (zezeNameAndConfig.length != 2)
				throw new RuntimeException("error zezes=" + zezeConfigs);
			if (this.zezes.containsKey(zezeNameAndConfig[0]))
				throw new RuntimeException("duplicate zeze=" + zezeNameAndConfig[0] + " zezes=" + zezeConfigs);
			var zezeConfig = Config.load(zezeNameAndConfig[1]);
			var serviceManager = Application.createServiceManager(zezeConfig, "OnzServerServiceManager");
			if (serviceManager == null)
				throw new RuntimeException("serviceManager not found for " + zezeNameAndConfig[0] + " zezes=" + zezeConfigs);
			serviceManager.start();
			try {
				serviceManager.waitReady();
			} catch (Exception ignored) {
				// raft 版第一次等待由于选择leader原因肯定会失败一次。
				serviceManager.waitReady();
			}
			serviceManager.subscribeService(new BSubscribeInfo(Onz.eServiceName));
			this.zezes.put(zezeNameAndConfig[0], serviceManager);
		}
		this.sharedServiceManager = false;

		service = new OnzServerService(myConfig);
		onzAgent = new OnzAgent();
		RegisterProtocols(service);
	}

	public void start() throws Exception {
		service.start();
		onzAgent.start();

		try {
			redoTimer();
		} catch (Exception ex) {
			logger.error("first try.", ex);
		}
		// 1 minute?
		redoTimer = Task.scheduleUnsafe(60000, 60000, this::redoTimer);
	}

	private void redoTimer() throws RocksDBException {
		try (var it = commitIndex.iterator()) {
			for (it.seekToFirst(); it.isValid(); it.next()) {
				var value = it.value();
				var state = ByteBuffer.Wrap(value).ReadInt();
				switch (state) {
				case eCommitting:
					redo(it.key(), OnzServer::commit);
					break;
				case ePreparing:
					redo(it.key(), OnzServer::rollback);
					break;
				}
			}
		}
	}

	private static TaskCompletionSource<EmptyBean.Data> commit(Connector conn, long tid) {
		var r = new Commit();
		r.Argument.setOnzTid(tid);
		return r.SendForWait(conn.GetReadySocket());
	}

	private static TaskCompletionSource<EmptyBean.Data> rollback(Connector conn, long tid) {
		var r = new Rollback();
		r.Argument.setOnzTid(tid);
		return r.SendForWait(conn.GetReadySocket());
	}

	private void redo(byte[] key, Func2<Connector, Long, TaskCompletionSource<EmptyBean.Data>> func) throws RocksDBException {

		var value = Objects.requireNonNull(commitPoint.get(key));
		var state = new BSavedCommits.Data();
		state.decode(ByteBuffer.Wrap(value));

		var zezeOnzs = new HashMap<String, Connector>();
		var tid = ByteBuffer.ToLongBE(key, 0);
		try {
			var futures = new ArrayList<TaskCompletionSource<?>>();
			for (var e : state.getOnzs()) {
				futures.add(func.call(openRedoConnection(zezeOnzs, e), tid));
			}
			for (var e : futures)
				e.await();
			removeCommitIndex(key);
		} catch (Throwable ex) {
			// timer will redo
			logger.error("", ex);
		} finally {
			for (var zeze : zezeOnzs.values())
				zeze.stop();
		}
	}

	void saveCommitPoint(byte[] tidBytes, BSavedCommits.Data bState, int state) throws RocksDBException {
		bState.setState(state);
		var bb = ByteBuffer.Allocate();
		bState.encode(bb);
		var bbIndex = ByteBuffer.Allocate(5);
		bbIndex.WriteInt(state);
		try (var batch = database.borrowBatch()) {
			// putIfAbsent ？？？ 报错！
			commitPoint.put(batch, tidBytes, tidBytes.length, bb.Bytes, bb.WriteIndex);
			commitIndex.put(batch, tidBytes, tidBytes.length, bbIndex.Bytes, bbIndex.WriteIndex);
			batch.commit(writeOptions);
		}
	}

	void removeCommitIndex(byte[] tidBytes) {
		try {
			commitIndex.delete(tidBytes);
		} catch (RocksDBException e) {
			// 这个错误仅仅记录日志，所有没有删除的index，以后重启和Timer会尝试重做。
			logger.error("", e);
		}
	}

	private static Connector openRedoConnection(HashMap<String, Connector> conns, String ip_port) {
		var conn = conns.computeIfAbsent(ip_port, __ -> {
			var newConn = new Connector(ip_port, false);
			newConn.start();
			return newConn;
		});
		conn.GetReadySocket();
		return conn;
	}

	public void stop() throws Exception {
		if (null != redoTimer)
			redoTimer.cancel(false);
		database.close();

		onzAgent.stop();
		service.stop();
	}

	/**
	 * 所有zeze集群共享同一个ServiceManager实例时，使用这个构造函数。
	 * 共享配置时，每个zeze集群需要额外的唯一名配置，并且把它拼接到ServiceManager的注册参数中。
	 *
	 * @param sharedZezeConfig 共享的ServiceManager配置
	 * @param specialZezeNames 共享配置时，已经配置成不同的zeze集群的唯一名字的列表，OnzServer不再自定义命名。
	 */
	public OnzServer(String sharedZezeConfig, String specialZezeNames, Config myConfig) throws Exception {
		myServiceManager = Application.createServiceManager(myConfig, "OnzServerMyServiceManager");
		if (myServiceManager == null)
			throw new RuntimeException("My ServiceManager not found");
		myServiceManager.start();
		try {
			myServiceManager.waitReady();
		} catch (Exception ignored) {
			// raft 版第一次等待由于选择leader原因肯定会失败一次。
			myServiceManager.waitReady();
		}
		onzTidAutoKey = myServiceManager.getAutoKey("OnzServerTidAutoKey");

		database = new RocksDatabase("CommitOnzServer" + myConfig.getServerId());
		commitPoint = database.getOrAddTable("CommitPoint");
		commitIndex = database.getOrAddTable("CommitIndex");

		var config = Config.load(sharedZezeConfig);
		var serviceManager = Application.createServiceManager(config, "OnzServerServiceManager");
		if (serviceManager == null)
			throw new RuntimeException("create ServiceManager fail. " + sharedZezeConfig);
		serviceManager.start();
		try {
			serviceManager.waitReady();
		} catch (Exception ignored) {
			// raft 版第一次等待由于选择leader原因肯定会失败一次。
			serviceManager.waitReady();
		}
		var zezeArray = specialZezeNames.split(";");
		for (var zeze : zezeArray) {
			if (this.zezes.containsKey(zeze))
				throw new RuntimeException("duplicate zeze=" + zeze + " zezes=" + specialZezeNames);
			this.zezes.put(zeze, serviceManager);
		}
		this.sharedServiceManager = true;
		serviceManager.subscribeService(new BSubscribeInfo(Onz.eServiceName));
		service = new OnzServerService(myConfig);
		onzAgent = new OnzAgent();
		RegisterProtocols(service);
	}

	public OnzAgent getOnzAgent() {
		return onzAgent;
	}

	public AsyncSocket getZezeInstance(String zezeName) {
		// find connected
		var connector = instances.get(zezeName);
		if (null != connector) {
			var socket = connector.TryGetReadySocket();
			if (null != socket)
				return socket;
		}

		// find from serviceManager
		var zeze = zezes.get(zezeName);
		if (null == zeze)
			throw new RuntimeException("unknown zeze=" + zezeName);

		var onzSmName = sharedServiceManager ? zezeName : Onz.eServiceName;
		var onzServices = zeze.getSubscribeStates().get(onzSmName);
		if (null == onzServices)
			throw new RuntimeException("serviceManager subscribe not found. " + zezeName);

		var serviceInfos = onzServices.getServiceInfos(0);
		if (serviceInfos != null) {
			for (var onzService : serviceInfos.getSortedIdentities()) {
				var ip = onzService.getPassiveIp();
				var port = onzService.getPassivePort();
				if (null != connector && connector.getName().equals(ip + "_" + port))
					continue; // 跳过当前的

				connector = new Connector(ip, port);
				connector.SetService(onzAgent.getService());
				connector.start();
				instances.put(zezeName, connector);
				break;
			}
		}
		if (null == connector)
			throw new RuntimeException("create connector fail. " + zezeName);
		return connector.GetReadySocket();
	}

	/**
	 * 独立进程运行OnzServer时需要注册。
	 * 嵌入时不用注册。
	 */
	public <A extends Data, R extends Data> void register(Class<OnzTransaction<?, ?>> txnClass,
														  Class<A> argumentClass, Class<R> resultClass) {
		if (null != remoteStubs.putIfAbsent(txnClass.getName(),
				new OnzTransactionStub<>(this, argumentClass, resultClass)))
			throw new RuntimeException("duplicate OnzTransaction Name=" + txnClass.getName());
	}

	/**
	 * Class.forName & set ; 主动创建并且控制txn的初始化。
	 * 由于嵌入时，本地是知道className的，可以直接new出来，
	 * 以获得更大灵活度。
	 */
	public static <A extends Data, R extends Data> OnzTransaction<A, R> createTransaction(
			String name, OnzServer onzServer, A argument, R result) throws Exception {

		@SuppressWarnings("unchecked")
		var cls = (Class<OnzTransaction<A, R>>)Class.forName(name);
		var txn = cls.getConstructor((Class<?>[])null).newInstance((Object[])null);
		txn.setOnzServer(onzServer);
		txn.setArgument(argument);
		txn.setResult(result);
		return txn;
	}

	/**
	 * 执行onz分布式事务。
	 * <p>
	 * 1. 自行决定txn的创建和初始化。
	 * 2. 可以不通过A,R结构传递参数和结果，完全自定义实现。
	 * 3. 设置其他onz事务的控制参数。如flushMode,flushTimeout等。
	 */
	public long perform(OnzTransaction<?, ?> txn) {
		try {
			onzAgent.addTransaction(txn);
			var rc = txn.perform();
			var state = txn.buildSavedCommits();
			var tidBytes = new byte[8];
			ByteBuffer.longBeHandler.set(tidBytes, 0, txn.getOnzTid());
			saveCommitPoint(tidBytes, state, ePreparing);
			// 这里和下面的txn.Commit分成两步saveCommitPoint，
			// 实际上这中间没有做太多额外的事情，可以考虑合并成异步，
			// 但为了明确两个事务状态，仍然分开。原因如下：
			// 参考Dbh2的两步：由于Dbh2一开始就知道所有的服务器，所以可以一开始就保存一次ePreparing，
			// 而这上面的perform是便执行边产生服务器地址，无法一开始保存事务状态。
			// 最严格的做法是每产生一个服务器地址，就写一次ePreparing（包含所有的服务器地址）。
			// 现在先简单处理为：等待perform完成。
			if (0 == rc) {
				txn.waitPendingAsync();
				txn.commit(tidBytes, state);
				txn.waitFlushDone();
				return 0;
			}
			txn.rollback();
			return rc;

		} catch (Throwable ex) {
			txn.rollback();
			logger.error("", ex);
			return Procedure.Exception;

		} finally {
			onzAgent.removeTransaction(txn);
		}
	}

	@Override
	protected long ProcessCheckpointRequest(Checkpoint r) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	protected long ProcessCommitRequest(Commit r) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	protected long ProcessFuncProcedureRequest(FuncProcedure r) throws Exception {
		// 这个本来是嵌入zeze的组件Onz的处理协议，直接拿来作为OnzServer的远程调用，够用，还超了一点。
		var stub = remoteStubs.get(r.Argument.getFuncName());
		if (stub == null)
			return errorCode(eProcedureNotFound);

		var buffer = ByteBuffer.Wrap(r.Argument.getFuncArgument().bytesUnsafe());
		var txn = stub.createTransaction(r.Argument.getFuncName(), buffer);
		var rc = perform(txn);
		if (0 != rc)
			return rc;

		var bbResult = ByteBuffer.Allocate();
		txn.getResult().encode(bbResult);
		r.Result.setFuncResult(new Binary(bbResult));
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessFuncSagaRequest(FuncSaga r) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	protected long ProcessFuncSagaEndRequest(FuncSagaEnd r) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	protected long ProcessRollbackRequest(Rollback r) throws Exception {
		throw new UnsupportedOperationException();
	}
}
