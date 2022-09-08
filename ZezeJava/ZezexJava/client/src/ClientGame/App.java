package ClientGame;

import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Util.OutObject;

public class App extends Zeze.AppBase {
    public static final App Instance = new App();
    public static App getInstance() {
        return Instance;
    }
    public Connector Connector;

    public void Start(String ip, int port) throws Throwable {
        var config = Config.Load("client.xml");
        CreateZeze(config);
        CreateService();
        var c = new OutObject<Connector>();
        ClientService.getConfig().TryGetOrAddConnector(ip, port, true, c);
        Connector = c.Value;
        CreateModules();
        Zeze.Start(); // 启动数据库
        StartModules(); // 启动模块，装载配置什么的。
        StartService(); // 启动网络
    }

    public void Stop() throws Throwable {
        StopService(); // 关闭网络
        StopModules(); // 关闭模块，卸载配置什么的。
        if (Zeze != null)
            Zeze.Stop(); // 关闭数据库
        DestroyModules();
        DestroyServices();
        DestroyZeze();
    }

    // ZEZE_FILE_CHUNK {{{ GEN APP @formatter:off
    public Zeze.Application Zeze;
    public final java.util.HashMap<String, Zeze.IModule> Modules = new java.util.HashMap<>();

    public ClientGame.ClientService ClientService;

    public Zeze.Builtin.Game.Online.ModuleOnline Zeze_Builtin_Game_Online;
    public Zeze.Builtin.Game.Bag.ModuleBag Zeze_Builtin_Game_Bag;
    public Zeze.Builtin.LinkdBase.ModuleLinkdBase Zeze_Builtin_LinkdBase;
    public ClientZezex.Linkd.ModuleLinkd ClientZezex_Linkd;
    public ClientGame.Login.ModuleLogin ClientGame_Login;

    @Override
    public Zeze.Application getZeze() {
        return Zeze;
    }

    public void CreateZeze() throws Throwable {
        CreateZeze(null);
    }

    public synchronized void CreateZeze(Zeze.Config config) throws Throwable {
        if (Zeze != null)
            throw new RuntimeException("Zeze Has Created!");

        Zeze = new Zeze.Application("ClientGame", config);
    }

    public synchronized void CreateService() throws Throwable {

        ClientService = new ClientGame.ClientService(Zeze);
    }
    public synchronized void CreateModules() {
        Zeze_Builtin_Game_Online = ReplaceModuleInstance(new Zeze.Builtin.Game.Online.ModuleOnline(this));
        Zeze_Builtin_Game_Online.Initialize(this);
        if (Modules.put(Zeze_Builtin_Game_Online.getFullName(), Zeze_Builtin_Game_Online) != null)
            throw new RuntimeException("duplicate module name: Zeze_Builtin_Game_Online");

        Zeze_Builtin_Game_Bag = ReplaceModuleInstance(new Zeze.Builtin.Game.Bag.ModuleBag(this));
        Zeze_Builtin_Game_Bag.Initialize(this);
        if (Modules.put(Zeze_Builtin_Game_Bag.getFullName(), Zeze_Builtin_Game_Bag) != null)
            throw new RuntimeException("duplicate module name: Zeze_Builtin_Game_Bag");

        Zeze_Builtin_LinkdBase = ReplaceModuleInstance(new Zeze.Builtin.LinkdBase.ModuleLinkdBase(this));
        Zeze_Builtin_LinkdBase.Initialize(this);
        if (Modules.put(Zeze_Builtin_LinkdBase.getFullName(), Zeze_Builtin_LinkdBase) != null)
            throw new RuntimeException("duplicate module name: Zeze_Builtin_LinkdBase");

        ClientZezex_Linkd = ReplaceModuleInstance(new ClientZezex.Linkd.ModuleLinkd(this));
        ClientZezex_Linkd.Initialize(this);
        if (Modules.put(ClientZezex_Linkd.getFullName(), ClientZezex_Linkd) != null)
            throw new RuntimeException("duplicate module name: ClientZezex_Linkd");

        ClientGame_Login = ReplaceModuleInstance(new ClientGame.Login.ModuleLogin(this));
        ClientGame_Login.Initialize(this);
        if (Modules.put(ClientGame_Login.getFullName(), ClientGame_Login) != null)
            throw new RuntimeException("duplicate module name: ClientGame_Login");

        Zeze.setSchemas(new ClientGame.Schemas());
    }

    public synchronized void DestroyModules() {
        ClientGame_Login = null;
        ClientZezex_Linkd = null;
        Zeze_Builtin_LinkdBase = null;
        Zeze_Builtin_Game_Bag = null;
        Zeze_Builtin_Game_Online = null;
        Modules.clear();
    }

    public synchronized void DestroyServices() {
        ClientService = null;
    }

    public synchronized void DestroyZeze() {
        Zeze = null;
    }

    public synchronized void StartModules() throws Throwable {
        Zeze_Builtin_Game_Online.Start(this);
        Zeze_Builtin_Game_Bag.Start(this);
        Zeze_Builtin_LinkdBase.Start(this);
        ClientZezex_Linkd.Start(this);
        ClientGame_Login.Start(this);
    }

    public synchronized void StopModules() throws Throwable {
        if (ClientGame_Login != null)
            ClientGame_Login.Stop(this);
        if (ClientZezex_Linkd != null)
            ClientZezex_Linkd.Stop(this);
        if (Zeze_Builtin_LinkdBase != null)
            Zeze_Builtin_LinkdBase.Stop(this);
        if (Zeze_Builtin_Game_Bag != null)
            Zeze_Builtin_Game_Bag.Stop(this);
        if (Zeze_Builtin_Game_Online != null)
            Zeze_Builtin_Game_Online.Stop(this);
    }

    public synchronized void StartService() throws Throwable {
        ClientService.Start();
    }

    public synchronized void StopService() throws Throwable {
        if (ClientService != null)
            ClientService.Stop();
    }
    // ZEZE_FILE_CHUNK }}} GEN APP @formatter:on
}
