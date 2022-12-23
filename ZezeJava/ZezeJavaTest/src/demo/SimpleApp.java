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
import Zeze.Game.Online;
import Zeze.Game.ProviderDirectWithTransmit;
import Zeze.Game.ProviderImplementWithOnline;
import Zeze.Game.Rank;
import Zeze.IModule;
import Zeze.Net.Acceptor;
import Zeze.Net.Connector;
import Zeze.Net.ServiceConf;

// 简单的无需读配置文件的App
public class SimpleApp extends AppBase {
	private Application zeze;
	private ProviderApp providerApp;

	// public Bag.Module bag;
	public Rank rank;

	public SimpleApp(int serverId) throws Throwable {
		this(serverId, 20000 + serverId, "127.0.0.1", 5001, "127.0.0.1", 5555, 20000);
	}

	public SimpleApp(int serverId, int providerPort, String serviceManagerIp, int serviceManagerPort,
					 String globalServerIp, int globalServerPort, int cacheSize) throws Throwable {
		var config = new Config();
		var serviceConf = new ServiceConf();
		serviceConf.addConnector(new Connector(serviceManagerIp, serviceManagerPort)); // 连接本地ServiceManager
		config.getServiceConfMap().put("Zeze.Services.ServiceManager.Agent", serviceConf);
		serviceConf = new ServiceConf();
		serviceConf.addAcceptor(new Acceptor(providerPort, null));
		config.getServiceConfMap().put("ServerDirect", serviceConf); // 提供Provider之间直连服务
		config.setGlobalCacheManagerHostNameOrAddress(globalServerIp); // 连接本地GlobalServer
		config.setGlobalCacheManagerPort(globalServerPort);
		config.getDatabaseConfMap().put("", new Config.DatabaseConf()); // 默认内存数据库配置
		var tableConf = new Config.TableConf();
		tableConf.setCacheCapacity(cacheSize);
		config.setDefaultTableConf(tableConf);
		config.setServerId(serverId); // 设置Provider服务器ID
		zeze = new Application("SimpleApp", config);
	}

	@Override
	public Application getZeze() {
		return zeze;
	}

	public void start() throws Throwable {
		var provider = new ProviderImplementWithOnline();
		providerApp = new ProviderApp(zeze, provider,
				new ProviderService("Server", zeze), "SimpleApp#", new ProviderDirectWithTransmit(),
				new ProviderDirectService("ServerDirect", zeze), "SimpleLinkd", new LoadConfig());
		provider.online = Online.create(this);
		provider.online.Initialize(this);

		var modules = new HashMap<String, IModule>();

//		bag = new Bag.Module(zeze);
//		bag.Initialize(this);
//		modules.put(bag.getFullName(), bag);

		rank = Rank.create(this);
		rank.Initialize(this);
		modules.put(rank.getFullName(), rank);

//		if (GenModule.Instance.GenFileSrcRoot != null) {
//			System.out.println("---------------");
//			System.out.println("New Source File Has Generate. Re-Compile Need.");
//			System.exit(0);
//		}

		zeze.start();
		((ProviderImplementWithOnline)providerApp.providerImplement).online.start();
		providerApp.providerService.Start();
		providerApp.providerDirectService.Start();
		providerApp.startLast(ProviderModuleBinds.load(""), modules);
	}

	public void stop() throws Throwable {
		if (providerApp != null) {
			if (providerApp.providerImplement != null) {
				var online = ((ProviderImplementWithOnline)providerApp.providerImplement).online;
				if (online != null)
					online.stop();
			}
			providerApp.providerDirectService.Stop();
			providerApp.providerService.Stop();
			providerApp = null;
		}
		if (rank != null) {
			rank.UnRegister();
			rank = null;
		}
//		if (bag != null) {
//			bag.UnRegister();
//			bag = null;
//		}
		if (zeze != null) {
			zeze.stop();
			zeze = null;
		}
	}
}
