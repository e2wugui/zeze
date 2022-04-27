package Game;

public class App extends Zeze.AppBase {
    public static App Instance = new App();
    public static App getInstance() {
        return Instance;
    }

    public void Start() throws Throwable {
        CreateZeze();
        CreateService();
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

    public Game.Client Client;

    public Zeze.Builtin.Game.Online.ModuleOnline Zeze_Builtin_Game_Online;
    public Zeze.Builtin.Game.Bag.ModuleBag Zeze_Builtin_Game_Bag;
    public Zeze.Builtin.LinkdBase.ModuleLinkdBase Zeze_Builtin_LinkdBase;
    public Zezex.Linkd.ModuleLinkd Zezex_Linkd;

    public Zeze.Application getZeze() {
        return Zeze;
    }

    public void CreateZeze() throws Throwable {
        CreateZeze(null);
    }

    public synchronized void CreateZeze(Zeze.Config config) throws Throwable {
        if (Zeze != null)
            throw new RuntimeException("Zeze Has Created!");

        Zeze = new Zeze.Application("Game", config);
    }

    public synchronized void CreateService() throws Throwable {

        Client = new Game.Client(Zeze);
    }
    public synchronized void CreateModules() {
        Zeze_Builtin_Game_Online = (Zeze.Builtin.Game.Online.ModuleOnline)ReplaceModuleInstance(new Zeze.Builtin.Game.Online.ModuleOnline(this));
        Zeze_Builtin_Game_Online.Initialize(this);
        if (Modules.put(Zeze_Builtin_Game_Online.getFullName(), Zeze_Builtin_Game_Online) != null)
            throw new RuntimeException("duplicate module name: Zeze_Builtin_Game_Online");

        Zeze_Builtin_Game_Bag = (Zeze.Builtin.Game.Bag.ModuleBag)ReplaceModuleInstance(new Zeze.Builtin.Game.Bag.ModuleBag(this));
        Zeze_Builtin_Game_Bag.Initialize(this);
        if (Modules.put(Zeze_Builtin_Game_Bag.getFullName(), Zeze_Builtin_Game_Bag) != null)
            throw new RuntimeException("duplicate module name: Zeze_Builtin_Game_Bag");

        Zeze_Builtin_LinkdBase = (Zeze.Builtin.LinkdBase.ModuleLinkdBase)ReplaceModuleInstance(new Zeze.Builtin.LinkdBase.ModuleLinkdBase(this));
        Zeze_Builtin_LinkdBase.Initialize(this);
        if (Modules.put(Zeze_Builtin_LinkdBase.getFullName(), Zeze_Builtin_LinkdBase) != null)
            throw new RuntimeException("duplicate module name: Zeze_Builtin_LinkdBase");

        Zezex_Linkd = (Zezex.Linkd.ModuleLinkd)ReplaceModuleInstance(new Zezex.Linkd.ModuleLinkd(this));
        Zezex_Linkd.Initialize(this);
        if (Modules.put(Zezex_Linkd.getFullName(), Zezex_Linkd) != null)
            throw new RuntimeException("duplicate module name: Zezex_Linkd");

        Zeze.setSchemas(new Game.Schemas());
    }

    public synchronized void DestroyModules() {
        Zezex_Linkd = null;
        Zeze_Builtin_LinkdBase = null;
        Zeze_Builtin_Game_Bag = null;
        Zeze_Builtin_Game_Online = null;
        Modules.clear();
    }

    public synchronized void DestroyServices() {
        Client = null;
    }

    public synchronized void DestroyZeze() {
        Zeze = null;
    }

    public synchronized void StartModules() throws Throwable {
        Zeze_Builtin_Game_Online.Start(this);
        Zeze_Builtin_Game_Bag.Start(this);
        Zeze_Builtin_LinkdBase.Start(this);
        Zezex_Linkd.Start(this);
    }

    public synchronized void StopModules() throws Throwable {
        if (Zezex_Linkd != null)
            Zezex_Linkd.Stop(this);
        if (Zeze_Builtin_LinkdBase != null)
            Zeze_Builtin_LinkdBase.Stop(this);
        if (Zeze_Builtin_Game_Bag != null)
            Zeze_Builtin_Game_Bag.Stop(this);
        if (Zeze_Builtin_Game_Online != null)
            Zeze_Builtin_Game_Online.Stop(this);
    }

    public synchronized void StartService() throws Throwable {
        Client.Start();
    }

    public synchronized void StopService() throws Throwable {
        if (Client != null)
            Client.Stop();
    }
    // ZEZE_FILE_CHUNK }}} GEN APP @formatter:on
}
