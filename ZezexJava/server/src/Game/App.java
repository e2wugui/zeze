package Game;

import java.util.*;

// auto-generated


public final class App {
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
		return Game.ModuleRedirect.Instance.ReplaceModuleInstance(module);
	}

	private Config Config;
	public Config getConfig() {
		return Config;
	}
	private void setConfig(Config value) {
		Config = value;
	}
	private Load Load = new Load();
	public Load getLoad() {
		return Load;
	}

	public static final String ServerServiceNamePrefix = "Game.Server.Module#";
	public static final String LinkdServiceName = "Game.Linkd";

	private void LoadConfig() {
		try {
			String json = Encoding.UTF8.GetString(System.IO.File.ReadAllBytes("Game.json"));
			setConfig(JsonSerializer.<Config>Deserialize(json));
		}
		catch (RuntimeException e) {
			//MessageBox.Show(ex.ToString());
		}
		if (null == getConfig()) {
			setConfig(new Config());
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
		var config = Zeze.Config.Load("zeze.xml");
		if (ServerId != -1) {
			config.ServerId = ServerId; // replace from args
		}
		Create(config);

		setProviderModuleBinds(Zezex.ProviderModuleBinds.Load());
		getProviderModuleBinds().BuildStaticBinds(getModules(), getZeze().Config.ServerId, getStaticBinds());

		getZeze().ServiceManagerAgent.OnChanged = (subscribeState) -> {
				getServer().ApplyLinksChanged(subscribeState.ServiceInfos);
		};
		getZeze().ServiceManagerAgent.OnChanged = (getZeze().Services.ServiceManager.Agent.SubscribeState subscribeState) -> TangibleLambdaToken32;

		getZeze().Start(); // 启动数据库
		StartModules(); // 启动模块，装载配置什么的。
		StartService(); // 启动网络
		getLoad().StartTimerTask();

		// 服务准备好以后才注册和订阅。
		for (var staticBind : getStaticBinds().entrySet()) {
			getZeze().ServiceManagerAgent.RegisterService(String.format("%1$s%2$s", ServerServiceNamePrefix, staticBind.getKey()), config.ServerId.toString(), null, 0, null);
		}
		getZeze().ServiceManagerAgent.SubscribeService(LinkdServiceName, Zeze.Services.ServiceManager.SubscribeInfo.SubscribeTypeSimple, null);
	}

	public void Stop() {
		StopService(); // 关闭网络
		StopModules(); // 关闭模块,，卸载配置什么的。
		getZeze().Stop(); // 关闭数据库
		Destroy();
	}


	private static App Instance = new App();
	public static App getInstance() {
		return Instance;
	}

	private Zeze.Application Zeze;
	public Zeze.Application getZeze() {
		return Zeze;
	}
	public void setZeze(Zeze.Application value) {
		Zeze = value;
	}

	private HashMap<String, Zeze.IModule> Modules = new HashMap < String, getZeze().IModule> ();
	public HashMap<String, Zeze.IModule> getModules() {
		return Modules;
	}

	private Game.Bag.ModuleBag Game_Bag;
	public Game.Bag.ModuleBag getGameBag() {
		return Game_Bag;
	}
	public void setGameBag(Game.Bag.ModuleBag value) {
		Game_Bag = value;
	}

	private Game.Buf.ModuleBuf Game_Buf;
	public Game.Buf.ModuleBuf getGameBuf() {
		return Game_Buf;
	}
	public void setGameBuf(Game.Buf.ModuleBuf value) {
		Game_Buf = value;
	}

	private Game.Equip.ModuleEquip Game_Equip;
	public Game.Equip.ModuleEquip getGameEquip() {
		return Game_Equip;
	}
	public void setGameEquip(Game.Equip.ModuleEquip value) {
		Game_Equip = value;
	}

	private Game.Fight.ModuleFight Game_Fight;
	public Game.Fight.ModuleFight getGameFight() {
		return Game_Fight;
	}
	public void setGameFight(Game.Fight.ModuleFight value) {
		Game_Fight = value;
	}

	private Game.Item.ModuleItem Game_Item;
	public Game.Item.ModuleItem getGameItem() {
		return Game_Item;
	}
	public void setGameItem(Game.Item.ModuleItem value) {
		Game_Item = value;
	}

	private Game.Login.ModuleLogin Game_Login;
	public Game.Login.ModuleLogin getGameLogin() {
		return Game_Login;
	}
	public void setGameLogin(Game.Login.ModuleLogin value) {
		Game_Login = value;
	}

	private Game.Map.ModuleMap Game_Map;
	public Game.Map.ModuleMap getGameMap() {
		return Game_Map;
	}
	public void setGameMap(Game.Map.ModuleMap value) {
		Game_Map = value;
	}

	private Game.Rank.ModuleRank Game_Rank;
	public Game.Rank.ModuleRank getGameRank() {
		return Game_Rank;
	}
	public void setGameRank(Game.Rank.ModuleRank value) {
		Game_Rank = value;
	}

	private Game.Skill.ModuleSkill Game_Skill;
	public Game.Skill.ModuleSkill getGameSkill() {
		return Game_Skill;
	}
	public void setGameSkill(Game.Skill.ModuleSkill value) {
		Game_Skill = value;
	}

	private Zezex.Provider.ModuleProvider Zezex_Provider;
	public Zezex.Provider.ModuleProvider getZezexProvider() {
		return Zezex_Provider;
	}
	public void setZezexProvider(Zezex.Provider.ModuleProvider value) {
		Zezex_Provider = value;
	}

	private Game.Server Server;
	public Game.Server getServer() {
		return Server;
	}
	public void setServer(Game.Server value) {
		Server = value;
	}


	public void Create() {
		Create(null);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void Create(Zeze.Config config = null)
	public void Create(Zeze.Config config) {
		synchronized (this) {
			if (null != getZeze()) {
				return;
			}

			setZeze(new Zeze.Application("Game", config));

			setServer(new Game.Server(getZeze()));

			setGameBag(new Game.Bag.ModuleBag(this));
			setGameBag((Game.Bag.ModuleBag)ReplaceModuleInstance(getGameBag()));
			getModules().put(getGameBag().getName(), getGameBag());
			setGameBuf(new Game.Buf.ModuleBuf(this));
			setGameBuf((Game.Buf.ModuleBuf)ReplaceModuleInstance(getGameBuf()));
			getModules().put(getGameBuf().getName(), getGameBuf());
			setGameEquip(new Game.Equip.ModuleEquip(this));
			setGameEquip((Game.Equip.ModuleEquip)ReplaceModuleInstance(getGameEquip()));
			getModules().put(getGameEquip().getName(), getGameEquip());
			setGameFight(new Game.Fight.ModuleFight(this));
			setGameFight((Game.Fight.ModuleFight)ReplaceModuleInstance(getGameFight()));
			getModules().put(getGameFight().getName(), getGameFight());
			setGameItem(new Game.Item.ModuleItem(this));
			setGameItem((Game.Item.ModuleItem)ReplaceModuleInstance(getGameItem()));
			getModules().put(getGameItem().getName(), getGameItem());
			setGameLogin(new Game.Login.ModuleLogin(this));
			setGameLogin((Game.Login.ModuleLogin)ReplaceModuleInstance(getGameLogin()));
			getModules().put(getGameLogin().getName(), getGameLogin());
			setGameMap(new Game.Map.ModuleMap(this));
			setGameMap((Game.Map.ModuleMap)ReplaceModuleInstance(getGameMap()));
			getModules().put(getGameMap().getName(), getGameMap());
			setGameRank(new Game.Rank.ModuleRank(this));
			setGameRank((Game.Rank.ModuleRank)ReplaceModuleInstance(getGameRank()));
			getModules().put(getGameRank().getName(), getGameRank());
			setGameSkill(new Game.Skill.ModuleSkill(this));
			setGameSkill((Game.Skill.ModuleSkill)ReplaceModuleInstance(getGameSkill()));
			getModules().put(getGameSkill().getName(), getGameSkill());
			setZezexProvider(new Zezex.Provider.ModuleProvider(this));
			setZezexProvider((Zezex.Provider.ModuleProvider)ReplaceModuleInstance(getZezexProvider()));
			getModules().put(getZezexProvider().getName(), getZezexProvider());

			getZeze().Schemas = new Game.Schemas();
		}
	}

	public void Destroy() {
		synchronized (this) {
			setGameBag(null);
			setGameBuf(null);
			setGameEquip(null);
			setGameFight(null);
			setGameItem(null);
			setGameLogin(null);
			setGameMap(null);
			setGameRank(null);
			setGameSkill(null);
			setZezexProvider(null);
			getModules().clear();
			setServer(null);
			setZeze(null);
		}
	}

	public void StartModules() {
		synchronized (this) {
			getGameBag().Start(this);
			getGameBuf().Start(this);
			getGameEquip().Start(this);
			getGameFight().Start(this);
			getGameItem().Start(this);
			getGameLogin().Start(this);
			getGameMap().Start(this);
			getGameRank().Start(this);
			getGameSkill().Start(this);
			getZezexProvider().Start(this);

		}
	}

	public void StopModules() {
		synchronized (this) {
			getGameBag().Stop(this);
			getGameBuf().Stop(this);
			getGameEquip().Stop(this);
			getGameFight().Stop(this);
			getGameItem().Stop(this);
			getGameLogin().Stop(this);
			getGameMap().Stop(this);
			getGameRank().Stop(this);
			getGameSkill().Stop(this);
			getZezexProvider().Stop(this);
		}
	}

	public void StartService() {
		synchronized (this) {
			getServer().Start();
		}
	}

	public void StopService() {
		synchronized (this) {
			getServer().Stop();
		}
	}
}