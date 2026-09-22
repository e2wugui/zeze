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
import Zeze.Component.TimerSpec;
import Zeze.Config;
import Zeze.Game.ProviderDirectWithTransmit;
import Zeze.Game.ProviderWithOnline;
import Zeze.Transaction.Transaction;
import Zeze.Util.JsonReader;
import Zeze.Util.TaskSpec;
import org.jetbrains.annotations.NotNull;
import static Zeze.Util.Args.requireInt;
import static Zeze.Util.Args.requireValue;

public final class App extends Zeze.AppBase {
	public static final App Instance = new App();

	public static App getInstance() {
		return Instance;
	}

	public ProviderWithOnline Provider;
	public ProviderApp ProviderApp;
	public ProviderDirectWithTransmit ProviderDirect;
	public LinkedMap.Module LinkedMapModule;
	public DepartmentTree.Module DepartmentTreeModule;

	private boolean started = false;

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

	/**
	 * @return 生成模式的源码根（已生成Redirect源码，不进入正常启动）；null=正常启动。
	 */
	public String Start(String[] args) throws Exception {
		int serverId = -1;
		int providerDirectPort = -1;
		String genFileSrcRoot = System.getProperty("GenFileSrcRoot");
		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-ServerId":
				serverId = requireInt(args, ++i, "-ServerId");
				break;
			case "-GenFileSrcRoot":
				genFileSrcRoot = requireValue(args, ++i, "-GenFileSrcRoot");
				break;
			case "-ProviderDirectPort":
				providerDirectPort = requireInt(args, ++i, "-ProviderDirectPort");
				break;
			}
		}
		Start(serverId, providerDirectPort, genFileSrcRoot);
		return genFileSrcRoot;
	}

	public void Start(int serverId, int providerDirectPort) throws Exception {
		Start(serverId, providerDirectPort, System.getProperty("GenFileSrcRoot"));
	}

	public void Start(int serverId, int providerDirectPort, String genFileSrcRoot) throws Exception {
		if (started)
			return;
		started = true;

		// 生成模式：模块类清单是静态的，不构造Application（不建库、不开服务、不碰server.xml），
		// 只生成Redirect源码后返回——main据Start返回值不进入wait，进程自然退出。
		if (genFileSrcRoot != null) {
			GenModule.instance.generateRedirectSources(genFileSrcRoot, this, redirectModuleClasses(),
					Boolean.getBoolean("GenFileTryCompile"));
			return; // 不得带空Zeze继续启动
		}

		var config = Config.load("server.xml");
		if (serverId != -1) {
			config.setServerId(serverId); // replace from args
		}
		var commitService = config.getServiceConf("Zeze.Dbh2.Commit");
		if (commitService != null) {
			commitService.forEachAcceptor(a -> a.setPort(a.getPort() + config.getServerId()));
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
		LinkedMapModule = new LinkedMap.Module(Zeze);
		DepartmentTreeModule = new DepartmentTree.Module(Zeze, LinkedMapModule);

		Zeze.getTimer().initializeOnlineTimer(ProviderApp);

		// start
		Zeze.start(); // 启动数据库
		startModules(); // 启动模块，装载配置什么的。
		Provider.start();

		// FND7-19/R3：不再全局安装PersistentAtomicLong发号——多App同JVM（linkd+Game.Server
		// 拓扑）值域重叠必撞号（SM服务端socket表按sessionId索引）。Service实例级随机63位
		// 基址发号（FND7-19）已保证跨JVM/跨App唯一；如需可读小号请用Service实例级
		// setSessionIdGenFunc且保证进程内全局值域不重叠。
		startService(); // 启动网络
		// 服务准备好以后才注册和订阅。
		ProviderApp.startLast(ProviderModuleBinds.load(), modules);

		counterColdTimer = 0;
		TaskSpec.ofProcedure(Zeze.newProcedure(() -> {
			coldTimerId = Zeze.getTimer().schedule(TimerSpec.ofDelay(2000).period(2000), ColdTimer.class, new BKick());
			//logger.info("XYZ Schedule={}", coldTimerId);
			return 0;
		}, "coldTimer")).call();
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
			//noinspection DataFlowIssue
			if (buf.getCode() != app.counterColdTimer)
				throw new RuntimeException("XYZ verify cold timer error." + buf.getCode() + " counter=" + app.counterColdTimer);
			buf.setCode(app.counterColdTimer + 1);
			Transaction.whileCommit(() -> app.counterColdTimer += 1);
		}
	}

	public void Stop() throws Exception {
		if (!started)
			return;
		started = false;
		if (Zeze == null) // 半启动（Start 在 createZeze 前/中失败）：尚无任何资源可停，直接返回，避免 NPE 掩盖真正的失败原因
			return;

		TaskSpec.ofProcedure(Zeze.newProcedure(() -> {
			//logger.info("XYZ Stop cancel={}", coldTimerId);
			Zeze.getTimer().cancel(coldTimerId);
			return 0;
		}, "cancelColdTimer")).call();

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
    public void createZeze(Zeze.Config config) throws Exception {
        lock();
        try {
            if (Zeze != null)
                throw new IllegalStateException("Zeze Has Created!");

            Zeze = new Zeze.Application("server", config);
        } finally {
            unlock();
        }
    }

    @Override
    public void createService() {
        lock();
        try {
            Server = new Game.Server(Zeze);
            ServerDirect = new Game.ServerDirect(Zeze);
        } finally {
            unlock();
        }
    }

    public static Class<?>[] redirectModuleClasses() {
        return new Class[] {
            Game.Map.ModuleMap.class,
            Game.Rank.ModuleRank.class,
        };
    }

    @Override
    public void createModules() throws Exception {
        lock();
        try {
            Zeze.setHotManager(new Zeze.Hot.HotManager(this, Zeze.getConfig().getHotWorkingDir(), Zeze.getConfig().getHotDistributeDir()));
            Zeze.initialize(this);
            Zeze.getHotManager().initialize(modules);
            var _modules_ = createRedirectModules(redirectModuleClasses());

            Game_Map = (Game.Map.ModuleMap)_modules_[0];
            Game_Map.Initialize(this);
            if (modules.put(Game_Map.getFullName(), Game_Map) != null)
                throw new IllegalStateException("duplicate module name: Game_Map");

            Game_Rank = (Game.Rank.ModuleRank)_modules_[1];
            Game_Rank.Initialize(this);
            if (modules.put(Game_Rank.getFullName(), Game_Rank) != null)
                throw new IllegalStateException("duplicate module name: Game_Rank");

            Zeze.setSchemas(new Game.Schemas());
        } finally {
            unlock();
        }
    }

    public void destroyModules() throws Exception {
        lock();
        try {
            Game_Rank = null;
            Game_Map = null;
            if (null != Zeze.getHotManager()) {
                Zeze.getHotManager().destroyModules();
                Zeze.setHotManager(null);
            }
            modules.clear();
        } finally {
            unlock();
        }
    }

    public void destroyServices() {
        lock();
        try {
            Server = null;
            ServerDirect = null;
        } finally {
            unlock();
        }
    }

    public void destroyZeze() {
        lock();
        try {
            Zeze = null;
        } finally {
            unlock();
        }
    }

    public void startModules() throws Exception {
        lock();
        try {
            Game_Map.Start(this);
            Game_Rank.Start(this);
            if (null != Zeze.getHotManager()) {
                var definedOrder = new java.util.HashSet<String>();
                Zeze.getHotManager().startModulesExcept(definedOrder);
            }
        } finally {
            unlock();
        }
    }

    @Override
    public void startLastModules() throws Exception {
        lock();
        try {
            Game_Map.StartLast();
            Game_Rank.StartLast();
            if (null != Zeze.getHotManager()) {
                var definedOrder = new java.util.HashSet<String>();
                Zeze.getHotManager().startLastModulesExcept(definedOrder);
            }
        } finally {
            unlock();
        }
    }

    public void stopModules() throws Exception {
        lock();
        try {
            if (Zeze == null)
                return;
            if (Game_Rank != null)
                Game_Rank.Stop(this);
            if (Game_Map != null)
                Game_Map.Stop(this);
            if (null != Zeze.getHotManager()) {
                var definedOrder = new java.util.HashSet<String>();
                Zeze.getHotManager().stopModulesExcept(definedOrder);
            }
        } finally {
            unlock();
        }
    }

    public void stopBeforeModules() throws Exception {
        lock();
        try {
            if (Zeze == null)
                return;
            if (Game_Rank != null)
                Game_Rank.StopBefore();
            if (Game_Map != null)
                Game_Map.StopBefore();
            if (null != Zeze.getHotManager()) {
                var definedOrder = new java.util.HashSet<String>();
                Zeze.getHotManager().stopBeforeModulesExcept(definedOrder);
            }
        } finally {
            unlock();
        }
    }

    public void startService() throws Exception {
        lock();
        try {
            Server.start();
            ServerDirect.start();
        } finally {
            unlock();
        }
    }

    public void stopService() throws Exception {
        lock();
        try {
            if (Server != null)
                Server.stop();
            if (ServerDirect != null)
                ServerDirect.stop();
        } finally {
            unlock();
        }
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
