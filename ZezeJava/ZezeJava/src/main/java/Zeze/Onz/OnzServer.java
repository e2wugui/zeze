package Zeze.Onz;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Builtin.Onz.FuncProcedure;
import Zeze.Builtin.Onz.FuncSaga;
import Zeze.Builtin.Onz.FuncSagaEnd;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Connector;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Transaction.Data;
import Zeze.Transaction.Procedure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 开发onz服务器基础
 *
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

	/**
	 * 每个zeze集群使用独立的ServiceManager实例时，使用这个方法构造OnzServer。
	 * 建议按这种方式配置，便于解耦。
	 * 此时zezes编码如下：
	 * zeze1=zeze1.xml;zeze2=zeze2.xml;...
	 * zeze1,zeze2是OnzServer自己对每个zeze集群的命名，以后用于Onz分布式事务的调用。需要唯一。
	 * zeze1.xml,zeze2.xml是不同zeze集群的配置文件path。
	 */
	public OnzServer(String zezeConfigs, Config myConfig) throws Exception {
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
			serviceManager.subscribeService(Onz.eServiceName, BSubscribeInfo.SubscribeTypeSimple);
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
	}

	public void stop() throws Exception {
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
				throw new RuntimeException("duplicate zeze=" + zeze+ " zezes=" + specialZezeNames);
			this.zezes.put(zeze, serviceManager);
		}
		this.sharedServiceManager = true;
		serviceManager.subscribeService(Onz.eServiceName, BSubscribeInfo.SubscribeTypeSimple);
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

		for (var onzService : onzServices.getServiceInfos().getServiceInfoListSortedByIdentity()) {
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
	 *
	 * 1. 自行决定txn的创建和初始化。
	 * 2. 可以不通过A,R结构传递参数和结果，完全自定义实现。
	 * 3. 设置其他onz事务的控制参数。如flushMode,flushTimeout等。
	 */
	public long perform(OnzTransaction<?, ?> txn) {
		try {
			onzAgent.addTransaction(txn);
			var rc = txn.perform();
			if (0 == rc) {
				txn.waitPendingAsync();
				txn.commit();
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
}
