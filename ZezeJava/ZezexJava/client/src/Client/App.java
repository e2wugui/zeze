package Client;

import Zeze.Config;

public class App extends Zeze.AppBase {
    public static App Instance = new App();
    public static App getInstance() {
        return Instance;
    }

    public void Start(String ip, int port) throws Throwable {
        var config = Config.Load("client.xml");
        CreateZeze(config);
        CreateService();
        ClientService.getConfig().TryGetOrAddConnector(ip, port, true, null);

        CreateModules();
        Zeze.Start(); // 启动数据库
        StartModules(); // 启动模块，装载配置什么的。
        StartService(); // 启动网络
    }

    public void Stop() throws Throwable {
        StopService(); // 关闭网络
        StopModules(); // 关闭模块，卸载配置什么的。
        Zeze.Stop(); // 关闭数据库
        DestroyModules();
        DestroyServices();
        DestroyZeze();
    }

    // ZEZE_FILE_CHUNK {{{ GEN APP @formatter:off
    public Zeze.Application Zeze;
    public final java.util.HashMap<String, Zeze.IModule> Modules = new java.util.HashMap<>();

    public Client.ClientService ClientService;

    public Client.Builtin.Game.Online.ModuleOnline Client_Builtin_Game_Online;
    public Client.Builtin.Game.Bag.ModuleBag Client_Builtin_Game_Bag;
    public Client.Builtin.LinkdBase.ModuleLinkdBase Client_Builtin_LinkdBase;
    public Client.Linkd.ModuleLinkd Client_Linkd;

    public Zeze.Application getZeze() {
        return Zeze;
    }

    public void CreateZeze() throws Throwable {
        CreateZeze(null);
    }

    public synchronized void CreateZeze(Zeze.Config config) throws Throwable {
        if (Zeze != null)
            throw new RuntimeException("Zeze Has Created!");

        Zeze = new Zeze.Application("Client", config);
    }

    public synchronized void CreateService() throws Throwable {

        ClientService = new Client.ClientService(Zeze);
    }
    public synchronized void CreateModules() {
        Client_Builtin_Game_Online = (Client.Builtin.Game.Online.ModuleOnline)ReplaceModuleInstance(new Client.Builtin.Game.Online.ModuleOnline(this));
        Client_Builtin_Game_Online.Initialize(this);
        if (Modules.put(Client_Builtin_Game_Online.getFullName(), Client_Builtin_Game_Online) != null)
            throw new RuntimeException("duplicate module name: Client_Builtin_Game_Online");

        Client_Builtin_Game_Bag = (Client.Builtin.Game.Bag.ModuleBag)ReplaceModuleInstance(new Client.Builtin.Game.Bag.ModuleBag(this));
        Client_Builtin_Game_Bag.Initialize(this);
        if (Modules.put(Client_Builtin_Game_Bag.getFullName(), Client_Builtin_Game_Bag) != null)
            throw new RuntimeException("duplicate module name: Client_Builtin_Game_Bag");

        Client_Builtin_LinkdBase = (Client.Builtin.LinkdBase.ModuleLinkdBase)ReplaceModuleInstance(new Client.Builtin.LinkdBase.ModuleLinkdBase(this));
        Client_Builtin_LinkdBase.Initialize(this);
        if (Modules.put(Client_Builtin_LinkdBase.getFullName(), Client_Builtin_LinkdBase) != null)
            throw new RuntimeException("duplicate module name: Client_Builtin_LinkdBase");

        Client_Linkd = (Client.Linkd.ModuleLinkd)ReplaceModuleInstance(new Client.Linkd.ModuleLinkd(this));
        Client_Linkd.Initialize(this);
        if (Modules.put(Client_Linkd.getFullName(), Client_Linkd) != null)
            throw new RuntimeException("duplicate module name: Client_Linkd");

        Zeze.setSchemas(new Client.Schemas());
    }

    public synchronized void DestroyModules() {
        Client_Linkd = null;
        Client_Builtin_LinkdBase = null;
        Client_Builtin_Game_Bag = null;
        Client_Builtin_Game_Online = null;
        Modules.clear();
    }

    public synchronized void DestroyServices() {
        ClientService = null;
    }

    public synchronized void DestroyZeze() {
        Zeze = null;
    }

    public synchronized void StartModules() throws Throwable {
        Client_Builtin_Game_Online.Start(this);
        Client_Builtin_Game_Bag.Start(this);
        Client_Builtin_LinkdBase.Start(this);
        Client_Linkd.Start(this);
    }

    public synchronized void StopModules() throws Throwable {
        if (Client_Linkd != null)
            Client_Linkd.Stop(this);
        if (Client_Builtin_LinkdBase != null)
            Client_Builtin_LinkdBase.Stop(this);
        if (Client_Builtin_Game_Bag != null)
            Client_Builtin_Game_Bag.Stop(this);
        if (Client_Builtin_Game_Online != null)
            Client_Builtin_Game_Online.Stop(this);
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
