package Zeze.Onz;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Transaction.Bean;

/**
 * 开发onz服务器基础
 *
 * 包装网络和onz协议，
 * 允许多个server实例，
 * 不同的server实例功能可以交叉也可以完全不同，
 */
public class OnzServer {
	private final OnzAgent onzAgent = new OnzAgent();
	private final boolean sharedServiceManager;
	private final ConcurrentHashMap<String, AbstractAgent> zezes = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Connector> instances = new ConcurrentHashMap<>();

	/**
	 * 每个zeze集群使用独立的ServiceManager实例时，使用这个方法构造OnzServer。
	 * 建议按这种方式配置，便于解耦。
	 * 此时zezes编码如下：
	 * zeze1=zeze1.xml;zeze2=zeze2.xml;...
	 * zeze1,zeze2是OnzServer自己对每个zeze集群的命名，
	 * zeze1.xml,zeze2.xml是不同zeze集群的配置文件path。
	 * 以后用于Onz分布式事务的调用。
	 * 需要唯一。
	 */
	public OnzServer(String zezeConfigs) throws Exception {
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
			serviceManager.subscribeService(Onz.eServiceName, BSubscribeInfo.SubscribeTypeSimple);
			this.zezes.put(zezeNameAndConfig[0], serviceManager);
		}
		this.sharedServiceManager = false;
	}

	/**
	 * 所有zeze集群共享同一个ServiceManager实例时，使用这个构造函数。
	 * 共享配置时，每个zeze集群需要额外的唯一名配置，并且把它拼接到ServiceManager的注册参数中。
	 *
	 * @param sharedZezeConfig 共享的ServiceManager配置
	 * @param specialZezeNames 共享配置时，已经配置成不同的zeze集群的唯一名字的列表，OnzServer不再自定义命名。
	 */
	public OnzServer(String sharedZezeConfig, String specialZezeNames) throws Exception {
		var config = Config.load(sharedZezeConfig);
		var serviceManager = Application.createServiceManager(config, "OnzServerServiceManager");
		if (serviceManager == null)
			throw new RuntimeException("create ServiceManager fail. " + sharedZezeConfig);
		var zezeArray = specialZezeNames.split(";");
		for (var zeze : zezeArray) {
			if (this.zezes.containsKey(zeze))
				throw new RuntimeException("duplicate zeze=" + zeze+ " zezes=" + specialZezeNames);
			this.zezes.put(zeze, serviceManager);
		}
		this.sharedServiceManager = true;
		serviceManager.subscribeService(Onz.eServiceName, BSubscribeInfo.SubscribeTypeSimple);
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
			connector.start();
			instances.put(zezeName, connector);
			break;
		}
		if (null == connector)
			throw new RuntimeException("create connector fail. " + zezeName);
		return connector.GetReadySocket();
	}

	public void perform(String name, OnzFuncTransaction func) {
		perform(name, func, Onz.eFlushImmediately, 10_000, null, null);
	}

	public void perform(String name, OnzFuncTransaction func, int flushMode) {
		perform(name, func, flushMode, 10_000, null, null);
	}

	public void perform(String name, OnzFuncTransaction func, int flushMode, int flushTimeout) {
		perform(name, func, flushMode, flushTimeout, null, null);
	}

	public void perform(String name, OnzFuncTransaction func, int flushMode, int flushTimeout, Bean argument, Bean result) {
		var t = new OnzTransaction(this, name, func, flushMode, flushTimeout, argument, result);
		try {
			onzAgent.addTransaction(t);
			if (0 == t.perform()) {
				t.waitPendingAsync();
				t.commit();
				t.waitFlushDone();
			} else {
				t.rollback();
			}
		} catch (Throwable ex) {
			t.rollback();
		} finally {
			onzAgent.removeTransaction(t);
		}
	}
}
