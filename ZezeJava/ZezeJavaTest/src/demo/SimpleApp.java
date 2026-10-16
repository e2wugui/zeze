package demo;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import java.util.HashMap;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.LoadConfig;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderDirectService;
import Zeze.Arch.ProviderModuleBinds;
import Zeze.Arch.ProviderService;
import Zeze.Config;
import Zeze.Game.ProviderDirectWithTransmit;
import Zeze.Game.ProviderWithOnline;
import Zeze.Game.Rank;
import Zeze.IModule;

// 简单的无需读配置文件的App
public class SimpleApp extends AppBase {
	private @Nullable Application zeze;
	private @Nullable ProviderApp providerApp;

	// public Bag.Module bag;
	public @Nullable Rank rank;

	public SimpleApp(int serverId) throws Exception {
		this(serverId, 20000 + serverId + 1, 20000);
	}

	public SimpleApp(int serverId, int directPort, int cacheSize) throws Exception {
		var config = Config.load("server.xml");
		var directConf = config.getServiceConfMap().get("ServerDirect");
		directConf.forEachAcceptor2((a) -> {
			a.setPort(directPort);
			a.setIp("127.0.0.1");
			return false;
		});
		var tableConf = new Config.TableConf();
		tableConf.setCacheCapacity(cacheSize);
		config.setDefaultTableConf(tableConf);
		config.setServerId(serverId); // 设置Provider服务器ID
		zeze = new Application("SimpleApp", config);
	}

	@Override
	public Application getZeze() {
		return Objects.requireNonNull(zeze);
	}

	public void start() throws Exception {
		var provider = new ProviderWithOnline();
		providerApp = new ProviderApp(Objects.requireNonNull(zeze), provider,
				new ProviderService("Server", zeze), "SimpleApp#", new ProviderDirectWithTransmit(),
				new ProviderDirectService("ServerDirect", zeze), "SimpleLinkd", new LoadConfig());
		provider.create(this);
		zeze.initialize(this);

		//var modules = new HashMap<String, IModule>();

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

		zeze.setSchemas(new demo.Schemas());
		zeze.start();
		Objects.requireNonNull(((ProviderWithOnline)providerApp.providerImplement)).start();
		providerApp.providerService.start();
		Objects.requireNonNull(providerApp.providerDirectService).start();
		beforeStartLast();
		providerApp.startLast(ProviderModuleBinds.load(""), modules);
	}

	public ProviderApp getProviderApp() {
		return Objects.requireNonNull(providerApp);
	}

	/** startLast 前的注入点（测试预置 LoginQueueAgent 等场景用）。 */
	protected void beforeStartLast() throws Exception {
	}

	public void stop() throws Exception {
		if (providerApp != null) {
			Objects.requireNonNull(providerApp.providerImplement).stop();
			Objects.requireNonNull(providerApp.providerDirectService).stop();
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
