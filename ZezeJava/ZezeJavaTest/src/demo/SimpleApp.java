package demo;

import java.util.HashMap;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.LoadConfig;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderDirectService;
import Zeze.Arch.ProviderModuleBinds;
import Zeze.Arch.ProviderService;
import Zeze.Config;
import Zeze.Game.Bag;
import Zeze.Game.ProviderDirectWithTransmit;
import Zeze.Game.ProviderImplementWithOnline;
import Zeze.Game.Rank;
import Zeze.IModule;
import Zeze.Net.Acceptor;
import Zeze.Net.Connector;
import Zeze.Net.ServiceConf;

// 简单的无需读配置文件的App
public class SimpleApp extends AppBase {
	private final Application zeze;
	private final ProviderApp providerApp;

	public Bag.Module bag;
	public Rank rank;

	public SimpleApp(int serverId) throws Throwable {
		this(serverId, 20000 + serverId, "127.0.0.1", 5001, "127.0.0.1", 5555, 20000);
	}

	public SimpleApp(int serverId, int providerPort, String serviceManagerIp, int serviceManagerPort,
					 String globalServerIp, int globalServerPort, int cacheSize) throws Throwable {
		var config = new Config();
		var serviceConf = new ServiceConf();
		serviceConf.AddConnector(new Connector(serviceManagerIp, serviceManagerPort)); // 连接本地ServiceManager
		config.getServiceConfMap().put("Zeze.Services.ServiceManager.Agent", serviceConf);
		serviceConf = new ServiceConf();
		serviceConf.AddAcceptor(new Acceptor(providerPort, null));
		config.getServiceConfMap().put("ProviderDirectService", serviceConf); // 提供Provider之间直连服务
		config.setGlobalCacheManagerHostNameOrAddress(globalServerIp); // 连接本地GlobalServer
		config.setGlobalCacheManagerPort(globalServerPort);
		config.getDatabaseConfMap().put("", new Config.DatabaseConf()); // 默认内存数据库配置
		var tableConf = new Config.TableConf();
		tableConf.setCacheCapacity(cacheSize);
		config.setDefaultTableConf(tableConf);
		config.setServerId(serverId); // 设置Provider服务器ID
		zeze = new Application("SimpleApp", config);

		providerApp = new ProviderApp(zeze, new ProviderImplementWithOnline(),
				new ProviderService("ProviderService", zeze), "SimpleApp#", new ProviderDirectWithTransmit(),
				new ProviderDirectService("ProviderDirectService", zeze), "Game.Linkd", new LoadConfig());
	}

	@Override
	public Application getZeze() {
		return zeze;
	}

	@Override
	public <T extends IModule> T ReplaceModuleInstance(T in) {
		return zeze.Redirect.ReplaceModuleInstance(this, in);
	}

	public void start() throws Throwable {
		var modules = new HashMap<String, IModule>();

		bag = new Bag.Module(zeze);
		bag.Initialize(this);
		modules.put(bag.getFullName(), bag);

		rank = Rank.create(this);
		rank.Initialize(this);
		modules.put(rank.getFullName(), rank);

//		if (GenModule.Instance.GenFileSrcRoot != null) {
//			System.out.println("---------------");
//			System.out.println("New Source File Has Generate. Re-Compile Need.");
//			System.exit(0);
//		}

		providerApp.initialize(ProviderModuleBinds.Load(""), modules);
		zeze.Start();
		providerApp.ProviderService.Start();
		providerApp.ProviderDirectService.Start();
		providerApp.StartLast();
	}

	public void stop() throws Throwable {
		providerApp.ProviderDirectService.Stop();
		providerApp.ProviderService.Stop();
		zeze.Stop();
		if (rank != null) {
			rank.UnRegister();
			rank = null;
		}
		if (bag != null) {
			bag.UnRegister();
			bag = null;
		}
	}
}
