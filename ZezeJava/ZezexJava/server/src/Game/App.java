package Game;

import java.nio.file.Files;
import java.nio.file.Paths;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Util.PersistentAtomicLong;
import com.fasterxml.jackson.databind.ObjectMapper;
import Zeze.Arch.*;

public final class App extends Zeze.AppBase {
	public static App Instance = new App();

	public static App getInstance() {
		return Instance;
	}

	@Override
	public Zeze.IModule ReplaceModuleInstance(Zeze.IModule module) {
		return Zeze.getModuleRedirect().ReplaceModuleInstance(module);
	}

	private MyConfig MyConfig;

	public MyConfig getMyConfig() {
		return MyConfig;
	}

	private final Load Load = new Load();

	public Load getLoad() {
		return Load;
	}

	private void LoadConfig() {
		try {
			byte[] bytes = Files.readAllBytes(Paths.get("Game.json"));
			MyConfig = new ObjectMapper().readValue(bytes, MyConfig.class);
		} catch (Exception e) {
			//MessageBox.Show(ex.ToString());
		}
		if (null == MyConfig) {
			MyConfig = new MyConfig();
		}
	}

	private Provider provider;
	public Provider getProvider() {
		return provider;
	}
	public ProviderApp ProviderApp;
	public ProviderDirectMy ProviderDirectMy;
	public void Start(String[] args) throws Throwable {
		int ServerId = -1;
		for (int i = 0; i < args.length; ++i) {
			//noinspection SwitchStatementWithTooFewBranches
			switch (args[i]) {
			case "-ServerId":
				ServerId = Integer.parseInt(args[++i]);
				break;
			}
		}

		LoadConfig();
		var config = Config.Load("zeze.xml");
		if (ServerId != -1) {
			config.setServerId(ServerId); // replace from args
		}

		// create
		CreateZeze(config);
		Zeze.setModuleRedirect(new ModuleRedirect());
		CreateService();
		provider = new Provider(this);
		ProviderDirectMy = new ProviderDirectMy();
		var kv = ServerDirect.GetOnePassiveAddress(); // need CreateService
		ProviderApp = new ProviderApp(Zeze, provider, Server, "Game.Server.Module#",
				ProviderDirectMy, ServerDirect, kv.getKey(), kv.getValue(), "Game.Linkd");
		CreateModules();
		ProviderApp.initialize(ProviderModuleBinds.Load(), Modules); // need Modules

		// start
		Zeze.Start(); // 启动数据库
		provider.Start(this);
		StartModules(); // 启动模块，装载配置什么的。
		PersistentAtomicLong socketSessionIdGen = PersistentAtomicLong.getOrAdd("Game.Server." + config.getServerId());
		AsyncSocket.setSessionIdGenFunc(socketSessionIdGen::next);
		StartService(); // 启动网络
		getLoad().StartTimerTask();

		// 服务准备好以后才注册和订阅。
		ProviderApp.StartLast();
	}

	public void Stop() throws Throwable {
		StopService(); // 关闭网络
		StopModules(); // 关闭模块，卸载配置什么的。
		provider.Stop(this);
		Zeze.Stop(); // 关闭数据库
		DestroyModules();
		DestroyServices();
		DestroyZeze();
	}

	// ZEZE_FILE_CHUNK {{{ GEN APP @formatter:off
    public Zeze.Application Zeze;
    public final java.util.HashMap<String, Zeze.IModule> Modules = new java.util.HashMap<>();

    public Game.Server Server;
    public Game.ServerDirect ServerDirect;

    public Game.Login.ModuleLogin Game_Login;
    public Game.Bag.ModuleBag Game_Bag;
    public Game.Item.ModuleItem Game_Item;
    public Game.Fight.ModuleFight Game_Fight;
    public Game.Skill.ModuleSkill Game_Skill;
    public Game.Buf.ModuleBuf Game_Buf;
    public Game.Equip.ModuleEquip Game_Equip;
    public Game.Map.ModuleMap Game_Map;
    public Game.Rank.ModuleRank Game_Rank;
    public Game.AutoKey.ModuleAutoKey Game_AutoKey;
    public Game.Timer.ModuleTimer Game_Timer;
    public Game.LongSet.ModuleLongSet Game_LongSet;

    public void CreateZeze() throws Throwable {
        CreateZeze(null);
    }

    public synchronized void CreateZeze(Zeze.Config config) throws Throwable {
        if (Zeze != null)
            throw new RuntimeException("Zeze Has Created!");

        Zeze = new Zeze.Application("Game", config);
    }

    public synchronized void CreateService() throws Throwable {

        Server = new Game.Server(Zeze);
        ServerDirect = new Game.ServerDirect(Zeze);
    }
    public synchronized void CreateModules() {
        Game_Login = new Game.Login.ModuleLogin(this);
        Game_Login.Initialize(this);
        Game_Login = (Game.Login.ModuleLogin)ReplaceModuleInstance(Game_Login);
        if (Modules.put(Game_Login.getFullName(), Game_Login) != null)
            throw new RuntimeException("duplicate module name: Game_Login");

        Game_Bag = new Game.Bag.ModuleBag(this);
        Game_Bag.Initialize(this);
        Game_Bag = (Game.Bag.ModuleBag)ReplaceModuleInstance(Game_Bag);
        if (Modules.put(Game_Bag.getFullName(), Game_Bag) != null)
            throw new RuntimeException("duplicate module name: Game_Bag");

        Game_Item = new Game.Item.ModuleItem(this);
        Game_Item.Initialize(this);
        Game_Item = (Game.Item.ModuleItem)ReplaceModuleInstance(Game_Item);
        if (Modules.put(Game_Item.getFullName(), Game_Item) != null)
            throw new RuntimeException("duplicate module name: Game_Item");

        Game_Fight = new Game.Fight.ModuleFight(this);
        Game_Fight.Initialize(this);
        Game_Fight = (Game.Fight.ModuleFight)ReplaceModuleInstance(Game_Fight);
        if (Modules.put(Game_Fight.getFullName(), Game_Fight) != null)
            throw new RuntimeException("duplicate module name: Game_Fight");

        Game_Skill = new Game.Skill.ModuleSkill(this);
        Game_Skill.Initialize(this);
        Game_Skill = (Game.Skill.ModuleSkill)ReplaceModuleInstance(Game_Skill);
        if (Modules.put(Game_Skill.getFullName(), Game_Skill) != null)
            throw new RuntimeException("duplicate module name: Game_Skill");

        Game_Buf = new Game.Buf.ModuleBuf(this);
        Game_Buf.Initialize(this);
        Game_Buf = (Game.Buf.ModuleBuf)ReplaceModuleInstance(Game_Buf);
        if (Modules.put(Game_Buf.getFullName(), Game_Buf) != null)
            throw new RuntimeException("duplicate module name: Game_Buf");

        Game_Equip = new Game.Equip.ModuleEquip(this);
        Game_Equip.Initialize(this);
        Game_Equip = (Game.Equip.ModuleEquip)ReplaceModuleInstance(Game_Equip);
        if (Modules.put(Game_Equip.getFullName(), Game_Equip) != null)
            throw new RuntimeException("duplicate module name: Game_Equip");

        Game_Map = new Game.Map.ModuleMap(this);
        Game_Map.Initialize(this);
        Game_Map = (Game.Map.ModuleMap)ReplaceModuleInstance(Game_Map);
        if (Modules.put(Game_Map.getFullName(), Game_Map) != null)
            throw new RuntimeException("duplicate module name: Game_Map");

        Game_Rank = new Game.Rank.ModuleRank(this);
        Game_Rank.Initialize(this);
        Game_Rank = (Game.Rank.ModuleRank)ReplaceModuleInstance(Game_Rank);
        if (Modules.put(Game_Rank.getFullName(), Game_Rank) != null)
            throw new RuntimeException("duplicate module name: Game_Rank");

        Game_AutoKey = new Game.AutoKey.ModuleAutoKey(this);
        Game_AutoKey.Initialize(this);
        Game_AutoKey = (Game.AutoKey.ModuleAutoKey)ReplaceModuleInstance(Game_AutoKey);
        if (Modules.put(Game_AutoKey.getFullName(), Game_AutoKey) != null)
            throw new RuntimeException("duplicate module name: Game_AutoKey");

        Game_Timer = new Game.Timer.ModuleTimer(this);
        Game_Timer.Initialize(this);
        Game_Timer = (Game.Timer.ModuleTimer)ReplaceModuleInstance(Game_Timer);
        if (Modules.put(Game_Timer.getFullName(), Game_Timer) != null)
            throw new RuntimeException("duplicate module name: Game_Timer");

        Game_LongSet = new Game.LongSet.ModuleLongSet(this);
        Game_LongSet.Initialize(this);
        Game_LongSet = (Game.LongSet.ModuleLongSet)ReplaceModuleInstance(Game_LongSet);
        if (Modules.put(Game_LongSet.getFullName(), Game_LongSet) != null)
            throw new RuntimeException("duplicate module name: Game_LongSet");

        Zeze.setSchemas(new Game.Schemas());
    }

    public synchronized void DestroyModules() {
        Game_LongSet = null;
        Game_Timer = null;
        Game_AutoKey = null;
        Game_Rank = null;
        Game_Map = null;
        Game_Equip = null;
        Game_Buf = null;
        Game_Skill = null;
        Game_Fight = null;
        Game_Item = null;
        Game_Bag = null;
        Game_Login = null;
        Modules.clear();
    }

    public synchronized void DestroyServices() {
        Server = null;
        ServerDirect = null;
    }

    public synchronized void DestroyZeze() {
        Zeze = null;
    }

    public synchronized void StartModules() throws Throwable {
        Game_Login.Start(this);
        Game_Bag.Start(this);
        Game_Item.Start(this);
        Game_Fight.Start(this);
        Game_Skill.Start(this);
        Game_Buf.Start(this);
        Game_Equip.Start(this);
        Game_Map.Start(this);
        Game_Rank.Start(this);
        Game_AutoKey.Start(this);
        Game_Timer.Start(this);
        Game_LongSet.Start(this);
    }

    public synchronized void StopModules() throws Throwable {
        if (Game_LongSet != null)
            Game_LongSet.Stop(this);
        if (Game_Timer != null)
            Game_Timer.Stop(this);
        if (Game_AutoKey != null)
            Game_AutoKey.Stop(this);
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
        if (Game_Bag != null)
            Game_Bag.Stop(this);
        if (Game_Login != null)
            Game_Login.Stop(this);
    }

    public synchronized void StartService() throws Throwable {
        Server.Start();
        ServerDirect.Start();
    }

    public synchronized void StopService() throws Throwable {
        if (Server != null)
            Server.Stop();
        if (ServerDirect != null)
            ServerDirect.Stop();
    }
    // ZEZE_FILE_CHUNK }}} GEN APP @formatter:on
}
