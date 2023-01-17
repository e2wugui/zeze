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

	public SimpleApp(int serverId) throws Exception {
		this(serverId, 20000 + serverId + 1, 20000);
	}

	public SimpleApp(int serverId, int directPort, int cacheSize) throws Exception {
		var config = Config.load("server.xml");
		var directConf = config.getServiceConfMap().get("ServerDirect");
		directConf.forEachAcceptor2((a) -> { a.setPort(directPort); a.setIp("127.0.0.1"); return false; });
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

	public void start() throws Exception {
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
		providerApp.providerService.start();
		providerApp.providerDirectService.start();
		providerApp.startLast(ProviderModuleBinds.load(""), modules);
	}

	public void stop() throws Exception {
		if (providerApp != null) {
			if (providerApp.providerImplement != null) {
				var online = ((ProviderImplementWithOnline)providerApp.providerImplement).online;
				if (online != null)
					online.stop();
			}
			providerApp.providerDirectService.stop();
			providerApp.providerService.stop();
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
