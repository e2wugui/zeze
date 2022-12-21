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
        var config = Config.load("client.xml");
        createZeze(config);
        createService();
        var c = new OutObject<Connector>();
        ClientService.getConfig().tryGetOrAddConnector(ip, port, true, c);
        Connector = c.value;
        createModules();
        Zeze.start(); // 启动数据库
        startModules(); // 启动模块，装载配置什么的。
        startService(); // 启动网络
    }

    public void Stop() throws Throwable {
        stopService(); // 关闭网络
        stopModules(); // 关闭模块，卸载配置什么的。
        if (Zeze != null)
            Zeze.stop(); // 关闭数据库
        destroyModules();
        destroyServices();
        destroyZeze();
    }

    // ZEZE_FILE_CHUNK {{{ GEN APP @formatter:off
    public Zeze.Application Zeze;
    public final java.util.HashMap<String, Zeze.IModule> modules = new java.util.HashMap<>();

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

    public void createZeze() throws Throwable {
        createZeze(null);
    }

    public synchronized void createZeze(Zeze.Config config) throws Throwable {
        if (Zeze != null)
            throw new RuntimeException("Zeze Has Created!");

        Zeze = new Zeze.Application("client", config);
    }

    public synchronized void createService() throws Throwable {
        ClientService = new ClientGame.ClientService(Zeze);
    }

    public synchronized void createModules() {
        var _modules_ = replaceModuleInstances(new Zeze.IModule[] {
            new Zeze.Builtin.Game.Online.ModuleOnline(this),
            new Zeze.Builtin.Game.Bag.ModuleBag(this),
            new Zeze.Builtin.LinkdBase.ModuleLinkdBase(this),
            new ClientZezex.Linkd.ModuleLinkd(this),
            new ClientGame.Login.ModuleLogin(this),
        });

        Zeze_Builtin_Game_Online = (Zeze.Builtin.Game.Online.ModuleOnline)_modules_[0];
        Zeze_Builtin_Game_Online.Initialize(this);
        if (modules.put(Zeze_Builtin_Game_Online.getFullName(), Zeze_Builtin_Game_Online) != null)
            throw new RuntimeException("duplicate module name: Zeze_Builtin_Game_Online");

        Zeze_Builtin_Game_Bag = (Zeze.Builtin.Game.Bag.ModuleBag)_modules_[1];
        Zeze_Builtin_Game_Bag.Initialize(this);
        if (modules.put(Zeze_Builtin_Game_Bag.getFullName(), Zeze_Builtin_Game_Bag) != null)
            throw new RuntimeException("duplicate module name: Zeze_Builtin_Game_Bag");

        Zeze_Builtin_LinkdBase = (Zeze.Builtin.LinkdBase.ModuleLinkdBase)_modules_[2];
        Zeze_Builtin_LinkdBase.Initialize(this);
        if (modules.put(Zeze_Builtin_LinkdBase.getFullName(), Zeze_Builtin_LinkdBase) != null)
            throw new RuntimeException("duplicate module name: Zeze_Builtin_LinkdBase");

        ClientZezex_Linkd = (ClientZezex.Linkd.ModuleLinkd)_modules_[3];
        ClientZezex_Linkd.Initialize(this);
        if (modules.put(ClientZezex_Linkd.getFullName(), ClientZezex_Linkd) != null)
            throw new RuntimeException("duplicate module name: ClientZezex_Linkd");

        ClientGame_Login = (ClientGame.Login.ModuleLogin)_modules_[4];
        ClientGame_Login.Initialize(this);
        if (modules.put(ClientGame_Login.getFullName(), ClientGame_Login) != null)
            throw new RuntimeException("duplicate module name: ClientGame_Login");

        Zeze.setSchemas(new ClientGame.Schemas());
    }

    public synchronized void destroyModules() {
        ClientGame_Login = null;
        ClientZezex_Linkd = null;
        Zeze_Builtin_LinkdBase = null;
        Zeze_Builtin_Game_Bag = null;
        Zeze_Builtin_Game_Online = null;
        modules.clear();
    }

    public synchronized void destroyServices() {
        ClientService = null;
    }

    public synchronized void destroyZeze() {
        Zeze = null;
    }

    public synchronized void startModules() throws Throwable {
        Zeze_Builtin_Game_Online.Start(this);
        Zeze_Builtin_Game_Bag.Start(this);
        Zeze_Builtin_LinkdBase.Start(this);
        ClientZezex_Linkd.Start(this);
        ClientGame_Login.Start(this);
    }

    public synchronized void stopModules() throws Throwable {
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

    public synchronized void startService() throws Throwable {
        ClientService.Start();
    }

    public synchronized void stopService() throws Throwable {
        if (ClientService != null)
            ClientService.Stop();
    }
    // ZEZE_FILE_CHUNK }}} GEN APP @formatter:on
}
