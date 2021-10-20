package Game;

import jason.JasonReader;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import Zeze.Services.ServiceManager.SubscribeInfo;
import Zeze.Config;

//ZEZE_FILE_CHUNK {{{ IMPORT GEN

//ZEZE_FILE_CHUNK }}} IMPORT GEN

public final class App extends Zeze.AppBase {
    public static App Instance = new App();
    public static App getInstance() {
        return Instance;
    }

	private HashMap<Integer, Zezex.Provider.BModule> StaticBinds = new HashMap<Integer, Zezex.Provider.BModule> ();
	public HashMap<Integer, Zezex.Provider.BModule> getStaticBinds() {
		return StaticBinds;
	}
	private Zezex.ProviderModuleBinds ProviderModuleBinds;
	public Zezex.ProviderModuleBinds getProviderModuleBinds() {
		return ProviderModuleBinds;
	}
	private void setProviderModuleBinds(Zezex.ProviderModuleBinds value) {
		ProviderModuleBinds = value;
	}

	public Zeze.IModule ReplaceModuleInstance(Zeze.IModule module) {
		return Zezex.ModuleRedirect.Instance.ReplaceModuleInstance(module);
	}

	private MyConfig MyConfig;
	public MyConfig getMyConfig() {
		return MyConfig;
	}

	private Load Load = new Load();
	public Load getLoad() {
		return Load;
	}

	public static final String ServerServiceNamePrefix = "Game.Server.Module#";
	public static final String LinkdServiceName = "Game.Linkd";

	private void LoadConfig() {
		try {
            byte [] bytes = Files.readAllBytes(Paths.get("Game.json"));
            MyConfig = new JasonReader().buf(bytes).parse(MyConfig.class);
		}
		catch (Exception e) {
			//MessageBox.Show(ex.ToString());
		}
		if (null == MyConfig) {
			MyConfig = new MyConfig();
		}
	}


	public void Start(String[] args) {
		int ServerId = -1;
		for (int i = 0; i < args.length; ++i) {
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
		Create(config);

		setProviderModuleBinds(Zezex.ProviderModuleBinds.Load());
		getProviderModuleBinds().BuildStaticBinds(Modules, Zeze.getConfig().getServerId(), StaticBinds);

		Zeze.getServiceManagerAgent().setOnChanged((subscribeState) -> {
				Server.ApplyLinksChanged(subscribeState.getServiceInfos());
		});

		Zeze.Start(); // 启动数据库
		StartModules(); // 启动模块，装载配置什么的。
		StartService(); // 启动网络
		getLoad().StartTimerTask();

		// 服务准备好以后才注册和订阅。
		for (var staticBind : getStaticBinds().entrySet()) {
			Zeze.getServiceManagerAgent().RegisterService(
                    String.format("%1$s%2$s", ServerServiceNamePrefix, staticBind.getKey()),
                    String.valueOf(config.getServerId()),
                    null,
                    0,
                    null);
		}
		Zeze.getServiceManagerAgent().SubscribeService(LinkdServiceName, SubscribeInfo.SubscribeTypeSimple, null);
	}

	public void Stop() {
		StopService(); // 关闭网络
		StopModules(); // 关闭模块,，卸载配置什么的。
		Zeze.Stop(); // 关闭数据库
		Destroy();
	}

	// ZEZE_FILE_CHUNK {{{ GEN APP
    public Zeze.Application Zeze;
    public HashMap<String, Zeze.IModule> Modules = new HashMap<>();

    public Game.Bag.ModuleBag Game_Bag;

    public Game.Buf.ModuleBuf Game_Buf;

    public Game.Equip.ModuleEquip Game_Equip;

    public Game.Fight.ModuleFight Game_Fight;

    public Game.Item.ModuleItem Game_Item;

    public Game.Login.ModuleLogin Game_Login;

    public Game.Map.ModuleMap Game_Map;

    public Game.Rank.ModuleRank Game_Rank;

    public Game.Skill.ModuleSkill Game_Skill;

    public Zezex.Provider.ModuleProvider Zezex_Provider;

    public Game.Server Server;

    public void Create() {
        Create(null);
    }

    public void Create(Config config) {
        synchronized (this) {
            if (null != Zeze)
                return;

            Zeze = new Zeze.Application("Game", config);

            Server = new Game.Server(Zeze);

            Game_Bag = new Game.Bag.ModuleBag(this);
            Game_Bag = (Game.Bag.ModuleBag)ReplaceModuleInstance(Game_Bag);
            if (null != Modules.put(Game_Bag.getName(), Game_Bag)) {
                throw new RuntimeException("duplicate module name: Game_Bag");
            }
            Game_Buf = new Game.Buf.ModuleBuf(this);
            Game_Buf = (Game.Buf.ModuleBuf)ReplaceModuleInstance(Game_Buf);
            if (null != Modules.put(Game_Buf.getName(), Game_Buf)) {
                throw new RuntimeException("duplicate module name: Game_Buf");
            }
            Game_Equip = new Game.Equip.ModuleEquip(this);
            Game_Equip = (Game.Equip.ModuleEquip)ReplaceModuleInstance(Game_Equip);
            if (null != Modules.put(Game_Equip.getName(), Game_Equip)) {
                throw new RuntimeException("duplicate module name: Game_Equip");
            }
            Game_Fight = new Game.Fight.ModuleFight(this);
            Game_Fight = (Game.Fight.ModuleFight)ReplaceModuleInstance(Game_Fight);
            if (null != Modules.put(Game_Fight.getName(), Game_Fight)) {
                throw new RuntimeException("duplicate module name: Game_Fight");
            }
            Game_Item = new Game.Item.ModuleItem(this);
            Game_Item = (Game.Item.ModuleItem)ReplaceModuleInstance(Game_Item);
            if (null != Modules.put(Game_Item.getName(), Game_Item)) {
                throw new RuntimeException("duplicate module name: Game_Item");
            }
            Game_Login = new Game.Login.ModuleLogin(this);
            Game_Login = (Game.Login.ModuleLogin)ReplaceModuleInstance(Game_Login);
            if (null != Modules.put(Game_Login.getName(), Game_Login)) {
                throw new RuntimeException("duplicate module name: Game_Login");
            }
            Game_Map = new Game.Map.ModuleMap(this);
            Game_Map = (Game.Map.ModuleMap)ReplaceModuleInstance(Game_Map);
            if (null != Modules.put(Game_Map.getName(), Game_Map)) {
                throw new RuntimeException("duplicate module name: Game_Map");
            }
            Game_Rank = new Game.Rank.ModuleRank(this);
            Game_Rank = (Game.Rank.ModuleRank)ReplaceModuleInstance(Game_Rank);
            if (null != Modules.put(Game_Rank.getName(), Game_Rank)) {
                throw new RuntimeException("duplicate module name: Game_Rank");
            }
            Game_Skill = new Game.Skill.ModuleSkill(this);
            Game_Skill = (Game.Skill.ModuleSkill)ReplaceModuleInstance(Game_Skill);
            if (null != Modules.put(Game_Skill.getName(), Game_Skill)) {
                throw new RuntimeException("duplicate module name: Game_Skill");
            }
            Zezex_Provider = new Zezex.Provider.ModuleProvider(this);
            Zezex_Provider = (Zezex.Provider.ModuleProvider)ReplaceModuleInstance(Zezex_Provider);
            if (null != Modules.put(Zezex_Provider.getName(), Zezex_Provider)) {
                throw new RuntimeException("duplicate module name: Zezex_Provider");
            }

            Zeze.setSchemas(new Game.Schemas());
        }
    }

    public void Destroy() {
        synchronized(this) {
            Game_Bag = null;
            Game_Buf = null;
            Game_Equip = null;
            Game_Fight = null;
            Game_Item = null;
            Game_Login = null;
            Game_Map = null;
            Game_Rank = null;
            Game_Skill = null;
            Zezex_Provider = null;
            Modules.clear();
            Server = null;
            Zeze = null;
        }
    }

    public void StartModules() {
        synchronized(this) {
            Game_Bag.Start(this);
            Game_Buf.Start(this);
            Game_Equip.Start(this);
            Game_Fight.Start(this);
            Game_Item.Start(this);
            Game_Login.Start(this);
            Game_Map.Start(this);
            Game_Rank.Start(this);
            Game_Skill.Start(this);
            Zezex_Provider.Start(this);

        }
    }

    public void StopModules() {
        synchronized(this) {
            Game_Bag.Stop(this);
            Game_Buf.Stop(this);
            Game_Equip.Stop(this);
            Game_Fight.Stop(this);
            Game_Item.Stop(this);
            Game_Login.Stop(this);
            Game_Map.Stop(this);
            Game_Rank.Stop(this);
            Game_Skill.Stop(this);
            Zezex_Provider.Stop(this);
        }
    }

    public void StartService() {
        synchronized(this) {
            Server.Start();
        }
    }

    public void StopService() {
        synchronized(this) {
            Server.Stop();
        }
    }
    // ZEZE_FILE_CHUNK }}} GEN APP
}
