package Game;

import java.nio.file.Files;
import java.nio.file.Paths;
import Zeze.Arch.Gen.GenModule;
import Zeze.Arch.LoadConfig;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderModuleBinds;
import Zeze.Config;
import Zeze.Game.Online;
import Zeze.Game.ProviderDirectWithTransmit;
import Zeze.Game.ProviderImplementWithOnline;
import Zeze.Game.TaskBase;
import Zeze.Net.AsyncSocket;
import Zeze.Util.JsonReader;
import Zeze.Util.PersistentAtomicLong;

public final class App extends Zeze.AppBase {
	public static final App Instance = new App();

	public static App getInstance() {
		return Instance;
	}

	public ProviderImplementWithOnline Provider;
	public ProviderApp ProviderApp;
	public ProviderDirectWithTransmit ProviderDirect;

	public ProviderImplementWithOnline getProvider() {
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
		if (providerDirectPort != -1) {
			final int port = providerDirectPort;
			config.getServiceConfMap().get("ServerDirect").forEachAcceptor((a) -> a.setPort(port));
		}
		// create
		createZeze(config);
		createService();
		Provider = new ProviderImplementWithOnline();
		ProviderDirect = new ProviderDirectWithTransmit();
		ProviderApp = new ProviderApp(Zeze, Provider, Server,
				"Game.Server.Module#",
				ProviderDirect, ServerDirect, "Game.Linkd", LoadConfig());
		Provider.online = Online.create(this);
		Provider.online.Initialize(this);

		createModules();
		if (GenModule.instance.genFileSrcRoot != null) {
			System.out.println("---------------");
			System.out.println("New Source File Has Generate. Re-Compile Need.");
			System.exit(0);
		}
		taskModule = new TaskBase.Module(getZeze());

		// start
		Zeze.start(); // 启动数据库
		startModules(); // 启动模块，装载配置什么的。
		Provider.online.start();

		PersistentAtomicLong socketSessionIdGen = PersistentAtomicLong.getOrAdd("Game.Server." + config.getServerId());
		AsyncSocket.setSessionIdGenFunc(socketSessionIdGen::next);
		startService(); // 启动网络
		// 服务准备好以后才注册和订阅。
		ProviderApp.startLast(ProviderModuleBinds.load(), modules);
	}

	public void Stop() throws Exception {
		if (Provider != null && Provider.online != null)
			Provider.online.stop();
		stopService(); // 关闭网络
		stopModules(); // 关闭模块，卸载配置什么的。
		if (Zeze != null)
			Zeze.stop(); // 关闭数据库
		destroyModules();
		destroyServices();
		destroyZeze();
	}

	public TaskBase.Module taskModule;

	// ZEZE_FILE_CHUNK {{{ GEN APP @formatter:off
    public Zeze.Application Zeze;
    public final java.util.HashMap<String, Zeze.IModule> modules = new java.util.HashMap<>();

    public Game.Server Server;
    public Game.ServerDirect ServerDirect;

    public Game.Login.ModuleLogin Game_Login;
    public Game.Item.ModuleItem Game_Item;
    public Game.Fight.ModuleFight Game_Fight;
    public Game.Skill.ModuleSkill Game_Skill;
    public Game.Buf.ModuleBuf Game_Buf;
    public Game.Equip.ModuleEquip Game_Equip;
    public Game.Map.ModuleMap Game_Map;
    public Game.Rank.ModuleRank Game_Rank;
    public Game.Timer.ModuleTimer Game_Timer;
    public Game.LongSet.ModuleLongSet Game_LongSet;

    @Override
    public Zeze.Application getZeze() {
        return Zeze;
    }

    public void createZeze() throws Exception {
        createZeze(null);
    }

    public synchronized void createZeze(Zeze.Config config) throws Exception {
        if (Zeze != null)
            throw new RuntimeException("Zeze Has Created!");

        Zeze = new Zeze.Application("server", config);
    }

    public synchronized void createService() {
        Server = new Game.Server(Zeze);
        ServerDirect = new Game.ServerDirect(Zeze);
    }

    public synchronized void createModules() {
        var _modules_ = createRedirectModules(new Class[] {
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
        });
        if (_modules_ == null)
            return;

        Game_Login = (Game.Login.ModuleLogin)_modules_[0];
        Game_Login.Initialize(this);
        if (modules.put(Game_Login.getFullName(), Game_Login) != null)
            throw new RuntimeException("duplicate module name: Game_Login");

        Game_Item = (Game.Item.ModuleItem)_modules_[1];
        Game_Item.Initialize(this);
        if (modules.put(Game_Item.getFullName(), Game_Item) != null)
            throw new RuntimeException("duplicate module name: Game_Item");

        Game_Fight = (Game.Fight.ModuleFight)_modules_[2];
        Game_Fight.Initialize(this);
        if (modules.put(Game_Fight.getFullName(), Game_Fight) != null)
            throw new RuntimeException("duplicate module name: Game_Fight");

        Game_Skill = (Game.Skill.ModuleSkill)_modules_[3];
        Game_Skill.Initialize(this);
        if (modules.put(Game_Skill.getFullName(), Game_Skill) != null)
            throw new RuntimeException("duplicate module name: Game_Skill");

        Game_Buf = (Game.Buf.ModuleBuf)_modules_[4];
        Game_Buf.Initialize(this);
        if (modules.put(Game_Buf.getFullName(), Game_Buf) != null)
            throw new RuntimeException("duplicate module name: Game_Buf");

        Game_Equip = (Game.Equip.ModuleEquip)_modules_[5];
        Game_Equip.Initialize(this);
        if (modules.put(Game_Equip.getFullName(), Game_Equip) != null)
            throw new RuntimeException("duplicate module name: Game_Equip");

        Game_Map = (Game.Map.ModuleMap)_modules_[6];
        Game_Map.Initialize(this);
        if (modules.put(Game_Map.getFullName(), Game_Map) != null)
            throw new RuntimeException("duplicate module name: Game_Map");

        Game_Rank = (Game.Rank.ModuleRank)_modules_[7];
        Game_Rank.Initialize(this);
        if (modules.put(Game_Rank.getFullName(), Game_Rank) != null)
            throw new RuntimeException("duplicate module name: Game_Rank");

        Game_Timer = (Game.Timer.ModuleTimer)_modules_[8];
        Game_Timer.Initialize(this);
        if (modules.put(Game_Timer.getFullName(), Game_Timer) != null)
            throw new RuntimeException("duplicate module name: Game_Timer");

        Game_LongSet = (Game.LongSet.ModuleLongSet)_modules_[9];
        Game_LongSet.Initialize(this);
        if (modules.put(Game_LongSet.getFullName(), Game_LongSet) != null)
            throw new RuntimeException("duplicate module name: Game_LongSet");

        Zeze.setSchemas(new Game.Schemas());
    }

    public synchronized void destroyModules() {
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
    }

    public synchronized void destroyServices() {
        Server = null;
        ServerDirect = null;
    }

    public synchronized void destroyZeze() {
        Zeze = null;
    }

    public synchronized void startModules() throws Exception {
        Game_Login.Start(this);
        Game_Item.Start(this);
        Game_Fight.Start(this);
        Game_Skill.Start(this);
        Game_Buf.Start(this);
        Game_Equip.Start(this);
        Game_Map.Start(this);
        Game_Rank.Start(this);
        Game_Timer.Start(this);
        Game_LongSet.Start(this);
    }

    public synchronized void stopModules() throws Exception {
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
    // ZEZE_FILE_CHUNK }}} GEN APP @formatter:on
}
