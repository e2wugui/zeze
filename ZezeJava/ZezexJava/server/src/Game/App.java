package Game;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
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
import static Zeze.Util.Args.requireInt;
import static Zeze.Util.Args.requireValue;

public final class App extends Zeze.AppBase {
	public static final App Instance = new App();

	public static App getInstance() {
		return Instance;
	}

	public @Nullable ProviderWithOnline Provider;
	public @Nullable ProviderApp ProviderApp;
	public @Nullable ProviderDirectWithTransmit ProviderDirect;
	public LinkedMap.@Nullable Module LinkedMapModule;
	public DepartmentTree.@Nullable Module DepartmentTreeModule;

	private boolean started = false;

	public ProviderWithOnline getProvider() {
		return Objects.requireNonNull(Provider);
	}

	private static @Nullable LoadConfig LoadConfig() {
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
	public @Nullable String Start(String[] args) throws Exception {
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

	public void Start(int serverId, int providerDirectPort, @Nullable String genFileSrcRoot) throws Exception {
		if (started)
			return;

		// 生成模式：模块类清单是静态的，不构造Application（不建库、不开服务、不碰server.xml），
		// 只生成Redirect源码后返回——main据Start返回值不进入wait，进程自然退出。
		if (genFileSrcRoot != null) {
			started = true;
			GenModule.instance.generateRedirectSources(genFileSrcRoot, this, redirectModuleClasses(),
					Boolean.getBoolean("GenFileTryCompile"));
			return; // 不得带空Zeze继续启动
		}

		Start(Config.load("server.xml"), serverId, providerDirectPort);
	}

	/** 使用调用方提供的配置启动，serverId 与监听端口仍可由参数覆盖。 */
	public void Start(Config config, int serverId, int providerDirectPort) throws Exception {
		if (started)
			return;
		started = true;

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
		ProviderApp = new ProviderApp(getZeze(), Provider, Objects.requireNonNull(Server),
				"Game.Server.Module#",
				ProviderDirect, Objects.requireNonNull(ServerDirect), "Game.Linkd", Objects.requireNonNull(LoadConfig()));
		Provider.create(this);

		createModules();
		LinkedMapModule = new LinkedMap.Module(getZeze());
		DepartmentTreeModule = new DepartmentTree.Module(getZeze(), LinkedMapModule);

		getZeze().getTimer().initializeOnlineTimer(ProviderApp);

		// start
		getZeze().start(); // 启动数据库
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
		TaskSpec.ofProcedure(getZeze().newProcedure(() -> {
			coldTimerId = getZeze().getTimer().schedule(TimerSpec.ofDelay(2000).period(2000), ColdTimer.class, new BKick());
			//logger.info("XYZ Schedule={}", coldTimerId);
			return 0;
		}, "coldTimer")).call();
	}

	@Nullable String coldTimerId;
	public int counterColdTimer;

	public static class ColdTimer implements TimerHandle {
		@Override
		public void onTimer(TimerContext context) throws Exception {
			var app = (Game.App)Objects.requireNonNull(context.timer.zeze).getAppBase();
			var buf = (BKick)context.customData;
			//logger.info("XYZ timer={} app={} buf={} counter={}",
			//		context.timerId, app.getZeze().getConfig().getServerId(), buf.getCode(), app.counterColdTimer);
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

		TaskSpec.ofProcedure(getZeze().newProcedure(() -> {
			//logger.info("XYZ Stop cancel={}", coldTimerId);
			getZeze().getTimer().cancel(coldTimerId);
			return 0;
		}, "cancelColdTimer")).call();

		if (Provider != null)
			Provider.stop();
		stopModules(); // 关闭模块，卸载配置什么的。
		if (Zeze != null) {
			getZeze().stop(); // 关闭数据库
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
    public Zeze.@org.jspecify.annotations.Nullable Application Zeze;

    public Game.@org.jspecify.annotations.Nullable Server Server;
    public Game.@org.jspecify.annotations.Nullable ServerDirect ServerDirect;

    public Game.Login.@org.jspecify.annotations.Nullable ModuleLogin Game_Login;
    public Game.Item.@org.jspecify.annotations.Nullable ModuleItem Game_Item;
    public Game.Fight.@org.jspecify.annotations.Nullable ModuleFight Game_Fight;
    public Game.Skill.@org.jspecify.annotations.Nullable ModuleSkill Game_Skill;
    public Game.Buf.@org.jspecify.annotations.Nullable ModuleBuf Game_Buf;
    public Game.Equip.@org.jspecify.annotations.Nullable ModuleEquip Game_Equip;
    public Game.Map.@org.jspecify.annotations.Nullable ModuleMap Game_Map;
    public Game.Rank.@org.jspecify.annotations.Nullable ModuleRank Game_Rank;
    public Game.Timer.@org.jspecify.annotations.Nullable ModuleTimer Game_Timer;
    public Game.LongSet.@org.jspecify.annotations.Nullable ModuleLongSet Game_LongSet;

    @Override
    public Zeze.Application getZeze() {
        return java.util.Objects.requireNonNull(Zeze, "Zeze has not been created");
    }

    public void createZeze() throws Exception {
        createZeze(null);
    }

    @Override
    public void createZeze(Zeze.@org.jspecify.annotations.Nullable Config config) throws Exception {
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
            Server = new Game.Server(getZeze());
            ServerDirect = new Game.ServerDirect(getZeze());
        } finally {
            unlock();
        }
    }

    public static Class<?>[] redirectModuleClasses() {
        return new Class[] {
            Game.Login.ModuleLogin.class,
            Game.Item.ModuleItem.class,
            Game.Fight.ModuleFight.class,
            Game.Skill.ModuleSkill.class,
            Game.Buf.ModuleBuf.class,
            Game.Equip.ModuleEquip.class,
            Game.Map.ModuleMap.class,
            Game.Rank.ModuleRank.class,
            Game.Timer.ModuleTimer.class,
            Game.LongSet.ModuleLongSet.class,
        };
    }

    @Override
    public void createModules() throws Exception {
        lock();
        try {
            getZeze().initialize(this);
            var _modules_ = createRedirectModules(redirectModuleClasses());

            Game_Login = (Game.Login.ModuleLogin)_modules_[0];
            Game_Login.Initialize(this);
            if (modules.put(Game_Login.getFullName(), Game_Login) != null)
                throw new IllegalStateException("duplicate module name: Game_Login");

            Game_Item = (Game.Item.ModuleItem)_modules_[1];
            Game_Item.Initialize(this);
            if (modules.put(Game_Item.getFullName(), Game_Item) != null)
                throw new IllegalStateException("duplicate module name: Game_Item");

            Game_Fight = (Game.Fight.ModuleFight)_modules_[2];
            Game_Fight.Initialize(this);
            if (modules.put(Game_Fight.getFullName(), Game_Fight) != null)
                throw new IllegalStateException("duplicate module name: Game_Fight");

            Game_Skill = (Game.Skill.ModuleSkill)_modules_[3];
            Game_Skill.Initialize(this);
            if (modules.put(Game_Skill.getFullName(), Game_Skill) != null)
                throw new IllegalStateException("duplicate module name: Game_Skill");

            Game_Buf = (Game.Buf.ModuleBuf)_modules_[4];
            Game_Buf.Initialize(this);
            if (modules.put(Game_Buf.getFullName(), Game_Buf) != null)
                throw new IllegalStateException("duplicate module name: Game_Buf");

            Game_Equip = (Game.Equip.ModuleEquip)_modules_[5];
            Game_Equip.Initialize(this);
            if (modules.put(Game_Equip.getFullName(), Game_Equip) != null)
                throw new IllegalStateException("duplicate module name: Game_Equip");

            Game_Map = (Game.Map.ModuleMap)_modules_[6];
            Game_Map.Initialize(this);
            if (modules.put(Game_Map.getFullName(), Game_Map) != null)
                throw new IllegalStateException("duplicate module name: Game_Map");

            Game_Rank = (Game.Rank.ModuleRank)_modules_[7];
            Game_Rank.Initialize(this);
            if (modules.put(Game_Rank.getFullName(), Game_Rank) != null)
                throw new IllegalStateException("duplicate module name: Game_Rank");

            Game_Timer = (Game.Timer.ModuleTimer)_modules_[8];
            Game_Timer.Initialize(this);
            if (modules.put(Game_Timer.getFullName(), Game_Timer) != null)
                throw new IllegalStateException("duplicate module name: Game_Timer");

            Game_LongSet = (Game.LongSet.ModuleLongSet)_modules_[9];
            Game_LongSet.Initialize(this);
            if (modules.put(Game_LongSet.getFullName(), Game_LongSet) != null)
                throw new IllegalStateException("duplicate module name: Game_LongSet");

            getZeze().setSchemas(new Game.Schemas());
        } finally {
            unlock();
        }
    }

    public void destroyModules() throws Exception {
        lock();
        try {
            Game_LongSet = null;
            Game_Timer = null;
            Game_Rank = null;
            Game_Map = null;
            Game_Equip = null;
            Game_Buf = null;
            Game_Skill = null;
            Game_Fight = null;
            Game_Item = null;
            Game_Login = null;
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
            java.util.Objects.requireNonNull(Game_Login).Start(this);
            java.util.Objects.requireNonNull(Game_Item).Start(this);
            java.util.Objects.requireNonNull(Game_Fight).Start(this);
            java.util.Objects.requireNonNull(Game_Skill).Start(this);
            java.util.Objects.requireNonNull(Game_Buf).Start(this);
            java.util.Objects.requireNonNull(Game_Equip).Start(this);
            java.util.Objects.requireNonNull(Game_Map).Start(this);
            java.util.Objects.requireNonNull(Game_Rank).Start(this);
            java.util.Objects.requireNonNull(Game_Timer).Start(this);
            java.util.Objects.requireNonNull(Game_LongSet).Start(this);
        } finally {
            unlock();
        }
    }

    @Override
    public void startLastModules() throws Exception {
        lock();
        try {
            java.util.Objects.requireNonNull(Game_Login).StartLast();
            java.util.Objects.requireNonNull(Game_Item).StartLast();
            java.util.Objects.requireNonNull(Game_Fight).StartLast();
            java.util.Objects.requireNonNull(Game_Skill).StartLast();
            java.util.Objects.requireNonNull(Game_Buf).StartLast();
            java.util.Objects.requireNonNull(Game_Equip).StartLast();
            java.util.Objects.requireNonNull(Game_Map).StartLast();
            java.util.Objects.requireNonNull(Game_Rank).StartLast();
            java.util.Objects.requireNonNull(Game_Timer).StartLast();
            java.util.Objects.requireNonNull(Game_LongSet).StartLast();
        } finally {
            unlock();
        }
    }

    public void stopModules() throws Exception {
        lock();
        try {
            if (Zeze == null)
                return;
            if (Game_LongSet != null)
                Game_LongSet.Stop(this);
            if (Game_Timer != null)
                Game_Timer.Stop(this);
            if (Game_Rank != null)
                Game_Rank.Stop(this);
            if (Game_Map != null)
                Game_Map.Stop(this);
            if (Game_Equip != null)
                Game_Equip.Stop(this);
            if (Game_Buf != null)
                Game_Buf.Stop(this);
            if (Game_Skill != null)
                Game_Skill.Stop(this);
            if (Game_Fight != null)
                Game_Fight.Stop(this);
            if (Game_Item != null)
                Game_Item.Stop(this);
            if (Game_Login != null)
                Game_Login.Stop(this);
        } finally {
            unlock();
        }
    }

    public void stopBeforeModules() throws Exception {
        lock();
        try {
            if (Zeze == null)
                return;
            if (Game_LongSet != null)
                Game_LongSet.StopBefore();
            if (Game_Timer != null)
                Game_Timer.StopBefore();
            if (Game_Rank != null)
                Game_Rank.StopBefore();
            if (Game_Map != null)
                Game_Map.StopBefore();
            if (Game_Equip != null)
                Game_Equip.StopBefore();
            if (Game_Buf != null)
                Game_Buf.StopBefore();
            if (Game_Skill != null)
                Game_Skill.StopBefore();
            if (Game_Fight != null)
                Game_Fight.StopBefore();
            if (Game_Item != null)
                Game_Item.StopBefore();
            if (Game_Login != null)
                Game_Login.StopBefore();
        } finally {
            unlock();
        }
    }

    public void startService() throws Exception {
        lock();
        try {
            java.util.Objects.requireNonNull(Server).start();
            java.util.Objects.requireNonNull(ServerDirect).start();
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
    // ZEZE_FILE_CHUNK }}} GEN APP @formatter:on
}
