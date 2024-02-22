package Game;

import java.nio.file.Files;
import java.nio.file.Paths;
import Zeze.Arch.Gen.GenModule;
import Zeze.Arch.LoadConfig;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderModuleBinds;
import Zeze.Builtin.Provider.BKick;
import Zeze.Collections.DepartmentTree;
import Zeze.Collections.LinkedMap;
import Zeze.Component.TimerContext;
import Zeze.Component.TimerHandle;
import Zeze.Config;
import Zeze.Game.ProviderDirectWithTransmit;
import Zeze.Game.ProviderWithOnline;
import Zeze.Net.AsyncSocket;
import Zeze.Transaction.Transaction;
import Zeze.Util.JsonReader;
import Zeze.Util.PersistentAtomicLong;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public final class App extends Zeze.AppBase {
	private static final Logger logger = LogManager.getLogger(App.class);

	public static final App Instance = new App();

	public static App getInstance() {
		return Instance;
	}

	public ProviderWithOnline Provider;
	public ProviderApp ProviderApp;
	public ProviderDirectWithTransmit ProviderDirect;
	public LinkedMap.Module LinkedMapModule;
	public DepartmentTree.Module DepartmentTreeModule;


	public ProviderWithOnline getProvider() {
		return Provider;
	}

	private static LoadConfig LoadConfig() {
		try {
			byte[] bytes = Files.readAllBytes(Paths.get("linkd.json"));
			return new JsonReader().buf(bytes).parse(LoadConfig.class);
			// return new ObjectMapper().readValue(bytes, LoadConfig.class);
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return new LoadConfig();
	}

	public void Start(String[] args) throws Exception {
		int serverId = -1;
		int providerDirectPort = -1;
		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-ServerId":
				serverId = Integer.parseInt(args[++i]);
				break;
			case "-GenFileSrcRoot":
				GenModule.instance.genFileSrcRoot = args[++i];
				break;
			case "-ProviderDirectPort":
				providerDirectPort = Integer.parseInt(args[++i]);
				break;
			}
		}
		Start(serverId, providerDirectPort);
	}

	public void Start(int serverId, int providerDirectPort) throws Exception {

		var config = Config.load("server.xml");
		if (serverId != -1) {
			config.setServerId(serverId); // replace from args
		}
		var commitService = config.getServiceConf("Zeze.Dbh2.Commit");
		if (null != commitService) {
			commitService.forEachAcceptor((a) -> {
				a.setPort(a.getPort() + config.getServerId());
			});
		}
		if (providerDirectPort != -1) {
			final int port = providerDirectPort;
			config.getServiceConfMap().get("ServerDirect").forEachAcceptor((a) -> a.setPort(port));
		}
		// create
		createZeze(config);
		createService();
		Provider = new ProviderWithOnline();
		Provider.setControlKick(BKick.eControlReportClient);

		ProviderDirect = new ProviderDirectWithTransmit();
		ProviderApp = new ProviderApp(Zeze, Provider, Server,
				"Game.Server.Module#",
				ProviderDirect, ServerDirect, "Game.Linkd", LoadConfig());
		Provider.create(this);

		createModules();
		if (GenModule.instance.genFileSrcRoot != null) {
			System.out.println("---------------");
			System.out.println("New Source File Has Generate. Re-Compile Need.");
			System.exit(0);
		}
		LinkedMapModule = new LinkedMap.Module(Zeze);
		DepartmentTreeModule = new DepartmentTree.Module(Zeze, LinkedMapModule);

		Zeze.getTimer().initializeOnlineTimer(ProviderApp);

		// start
		Zeze.start(); // 启动数据库
		startModules(); // 启动模块，装载配置什么的。
		Provider.start();

		PersistentAtomicLong socketSessionIdGen = PersistentAtomicLong.getOrAdd("Game.Server." + config.getServerId());
		AsyncSocket.setSessionIdGenFunc(socketSessionIdGen::next);
		startService(); // 启动网络
		// 服务准备好以后才注册和订阅。
		ProviderApp.startLast(ProviderModuleBinds.load(), modules);

		counterColdTimer = 0;
		Task.call(Zeze.newProcedure(() -> {
			coldTimerId = Zeze.getTimer().schedule(2000, 2000, ColdTimer.class, new BKick());
			//logger.info("XYZ Schedule={}", coldTimerId);
			return 0;
		}, "coldTimer"));
	}

	String coldTimerId;
	public int counterColdTimer;

	public static class ColdTimer implements TimerHandle {
		@Override
		public void onTimer(@NotNull TimerContext context) throws Exception {
			var app = (Game.App)context.timer.zeze.getAppBase();
			var buf = (BKick)context.customData;
			//logger.info("XYZ timer={} app={} buf={} counter={}",
			//		context.timerId, app.Zeze.getConfig().getServerId(), buf.getCode(), app.counterColdTimer);
			if (buf.getCode() != app.counterColdTimer)
				throw new RuntimeException("XYZ verify cold timer error." + buf.getCode() + " counter=" + app.counterColdTimer);
			buf.setCode(app.counterColdTimer + 1);
			Transaction.whileCommit(() -> app.counterColdTimer += 1);
		}

		@Override
		public void onTimerCancel() throws Exception {

		}
	}

	public void Stop() throws Exception {
		Task.call(Zeze.newProcedure(() -> {
			//logger.info("XYZ Stop cancel={}", coldTimerId);
			Zeze.getTimer().cancel(coldTimerId);
			return 0;
		}, "cancelColdTimer"));

		if (Provider != null)
			Provider.stop();
		stopModules(); // 关闭模块，卸载配置什么的。
		if (Zeze != null) {
			Zeze.stop(); // 关闭数据库
			if (DepartmentTreeModule != null) {
				DepartmentTreeModule.UnRegisterZezeTables(Zeze);
				DepartmentTreeModule = null;
			}
			if (LinkedMapModule != null) {
				LinkedMapModule.UnRegisterZezeTables(Zeze);
				LinkedMapModule = null;
			}
		}
		stopService(); // 关闭网络
		destroyModules();
		destroyServices();
		destroyZeze();
	}

	// ZEZE_FILE_CHUNK {{{ GEN APP @formatter:off
    public Zeze.Application Zeze;

    public Game.Server Server;
    public Game.ServerDirect ServerDirect;

    public Game.Map.ModuleMap Game_Map;
    public Game.Rank.ModuleRank Game_Rank;

    @Override
    public Zeze.Application getZeze() {
        return Zeze;
    }

    public void createZeze() throws Exception {
        createZeze(null);
    }

    @Override
    public synchronized void createZeze(Zeze.Config config) throws Exception {
        if (Zeze != null)
            throw new IllegalStateException("Zeze Has Created!");

        Zeze = new Zeze.Application("server", config);
    }

    @Override
    public synchronized void createService() {
        Server = new Game.Server(Zeze);
        ServerDirect = new Game.ServerDirect(Zeze);
    }

    @Override
    public synchronized void createModules() throws Exception {
        Zeze.setHotManager(new Zeze.Hot.HotManager(this, Zeze.getConfig().getHotWorkingDir(), Zeze.getConfig().getHotDistributeDir()));
        Zeze.initialize(this);
        Zeze.getHotManager().initialize(modules);
        var _modules_ = createRedirectModules(new Class[] {
            Game.Map.ModuleMap.class,
            Game.Rank.ModuleRank.class,
        });
        if (_modules_ == null)
            return;

        Game_Map = (Game.Map.ModuleMap)_modules_[0];
        Game_Map.Initialize(this);
        if (modules.put(Game_Map.getFullName(), Game_Map) != null)
            throw new IllegalStateException("duplicate module name: Game_Map");

        Game_Rank = (Game.Rank.ModuleRank)_modules_[1];
        Game_Rank.Initialize(this);
        if (modules.put(Game_Rank.getFullName(), Game_Rank) != null)
            throw new IllegalStateException("duplicate module name: Game_Rank");

        Zeze.setSchemas(new Game.Schemas());
    }

    public synchronized void destroyModules() throws Exception {
        Game_Rank = null;
        Game_Map = null;
        if (null != Zeze.getHotManager()) {
            Zeze.getHotManager().destroyModules();
            Zeze.setHotManager(null);
        }
        modules.clear();
    }

    public synchronized void destroyServices() {
        Server = null;
        ServerDirect = null;
    }

    public synchronized void destroyZeze() {
        Zeze = null;
    }

    public synchronized void startModules() throws Exception {
        Game_Map.Start(this);
        Game_Rank.Start(this);
        if (null != Zeze.getHotManager()) {
            var definedOrder = new java.util.HashSet<String>();
            Zeze.getHotManager().startModulesExcept(definedOrder);
        }
    }

    @Override
    public synchronized void startLastModules() throws Exception {
        Game_Map.StartLast();
        Game_Rank.StartLast();
        if (null != Zeze.getHotManager()) {
            var definedOrder = new java.util.HashSet<String>();
            Zeze.getHotManager().startLastModulesExcept(definedOrder);
        }
    }

    public synchronized void stopModules() throws Exception {
        if (Game_Rank != null)
            Game_Rank.Stop(this);
        if (Game_Map != null)
            Game_Map.Stop(this);
        if (null != Zeze.getHotManager()) {
            var definedOrder = new java.util.HashSet<String>();
            Zeze.getHotManager().stopModulesExcept(definedOrder);
        }
    }

    public synchronized void stopBeforeModules() throws Exception {
        if (Game_Rank != null)
            Game_Rank.StopBefore();
        if (Game_Map != null)
            Game_Map.StopBefore();
        if (null != Zeze.getHotManager()) {
            var definedOrder = new java.util.HashSet<String>();
            Zeze.getHotManager().stopBeforeModulesExcept(definedOrder);
        }
    }

    public synchronized void startService() throws Exception {
        Server.start();
        ServerDirect.start();
    }

    public synchronized void stopService() throws Exception {
        if (Server != null)
            Server.stop();
        if (ServerDirect != null)
            ServerDirect.stop();
    }

    public static void distributeHot(Zeze.Hot.Distribute distribute) throws Exception {
        var hotModules = new java.util.HashSet<String>();
        hotModules.add("Game.Login");
        hotModules.add("Game.Item");
        hotModules.add("Game.Fight");
        hotModules.add("Game.Skill");
        hotModules.add("Game.Buf");
        hotModules.add("Game.Equip");
        hotModules.add("Game.Timer");
        hotModules.add("Game.LongSet");
        distribute.pack(hotModules, "server", "Game");
    }
    // ZEZE_FILE_CHUNK }}} GEN APP @formatter:on
}
