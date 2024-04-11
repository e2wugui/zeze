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

    public void Start(String ip, int port) throws Exception {
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
        Connector.GetReadySocket();
    }

    public void Stop() throws Exception {
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

    public ClientGame.ClientService ClientService;

    public Zeze.Builtin.Game.Online.ModuleOnline Zeze_Builtin_Game_Online;
    public Zeze.Builtin.Game.Bag.ModuleBag Zeze_Builtin_Game_Bag;
    public Zeze.Builtin.LinkdBase.ModuleLinkdBase Zeze_Builtin_LinkdBase;
    public ClientZezex.Linkd.ModuleLinkd ClientZezex_Linkd;
    public ClientGame.Login.ModuleLogin ClientGame_Login;
    public ClientGame.Fight.ModuleFight ClientGame_Fight;
    public ClientGame.Equip.ModuleEquip ClientGame_Equip;

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

            Zeze = new Zeze.Application("client", config);
        } finally {
            unlock();
        }
    }

    @Override
    public void createService() {
        lock();
        try {
            ClientService = new ClientGame.ClientService(Zeze);
        } finally {
            unlock();
        }
    }

    @Override
    public void createModules() throws Exception {
        lock();
        try {
            Zeze.initialize(this);
            var _modules_ = createRedirectModules(new Class[] {
                Zeze.Builtin.Game.Online.ModuleOnline.class,
                Zeze.Builtin.Game.Bag.ModuleBag.class,
                Zeze.Builtin.LinkdBase.ModuleLinkdBase.class,
                ClientZezex.Linkd.ModuleLinkd.class,
                ClientGame.Login.ModuleLogin.class,
                ClientGame.Fight.ModuleFight.class,
                ClientGame.Equip.ModuleEquip.class,
            });
            if (_modules_ == null)
                return;

            Zeze_Builtin_Game_Online = (Zeze.Builtin.Game.Online.ModuleOnline)_modules_[0];
            Zeze_Builtin_Game_Online.Initialize(this);
            if (modules.put(Zeze_Builtin_Game_Online.getFullName(), Zeze_Builtin_Game_Online) != null)
                throw new IllegalStateException("duplicate module name: Zeze_Builtin_Game_Online");

            Zeze_Builtin_Game_Bag = (Zeze.Builtin.Game.Bag.ModuleBag)_modules_[1];
            Zeze_Builtin_Game_Bag.Initialize(this);
            if (modules.put(Zeze_Builtin_Game_Bag.getFullName(), Zeze_Builtin_Game_Bag) != null)
                throw new IllegalStateException("duplicate module name: Zeze_Builtin_Game_Bag");

            Zeze_Builtin_LinkdBase = (Zeze.Builtin.LinkdBase.ModuleLinkdBase)_modules_[2];
            Zeze_Builtin_LinkdBase.Initialize(this);
            if (modules.put(Zeze_Builtin_LinkdBase.getFullName(), Zeze_Builtin_LinkdBase) != null)
                throw new IllegalStateException("duplicate module name: Zeze_Builtin_LinkdBase");

            ClientZezex_Linkd = (ClientZezex.Linkd.ModuleLinkd)_modules_[3];
            ClientZezex_Linkd.Initialize(this);
            if (modules.put(ClientZezex_Linkd.getFullName(), ClientZezex_Linkd) != null)
                throw new IllegalStateException("duplicate module name: ClientZezex_Linkd");

            ClientGame_Login = (ClientGame.Login.ModuleLogin)_modules_[4];
            ClientGame_Login.Initialize(this);
            if (modules.put(ClientGame_Login.getFullName(), ClientGame_Login) != null)
                throw new IllegalStateException("duplicate module name: ClientGame_Login");

            ClientGame_Fight = (ClientGame.Fight.ModuleFight)_modules_[5];
            ClientGame_Fight.Initialize(this);
            if (modules.put(ClientGame_Fight.getFullName(), ClientGame_Fight) != null)
                throw new IllegalStateException("duplicate module name: ClientGame_Fight");

            ClientGame_Equip = (ClientGame.Equip.ModuleEquip)_modules_[6];
            ClientGame_Equip.Initialize(this);
            if (modules.put(ClientGame_Equip.getFullName(), ClientGame_Equip) != null)
                throw new IllegalStateException("duplicate module name: ClientGame_Equip");

            Zeze.setSchemas(new ClientGame.Schemas());
        } finally {
            unlock();
        }
    }

    public void destroyModules() throws Exception {
        lock();
        try {
            ClientGame_Equip = null;
            ClientGame_Fight = null;
            ClientGame_Login = null;
            ClientZezex_Linkd = null;
            Zeze_Builtin_LinkdBase = null;
            Zeze_Builtin_Game_Bag = null;
            Zeze_Builtin_Game_Online = null;
            modules.clear();
        } finally {
            unlock();
        }
    }

    public void destroyServices() {
        lock();
        try {
            ClientService = null;
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
            Zeze_Builtin_Game_Online.Start(this);
            Zeze_Builtin_Game_Bag.Start(this);
            Zeze_Builtin_LinkdBase.Start(this);
            ClientZezex_Linkd.Start(this);
            ClientGame_Login.Start(this);
            ClientGame_Fight.Start(this);
            ClientGame_Equip.Start(this);
        } finally {
            unlock();
        }
    }

    @Override
    public void startLastModules() throws Exception {
        lock();
        try {
            Zeze_Builtin_Game_Online.StartLast();
            Zeze_Builtin_Game_Bag.StartLast();
            Zeze_Builtin_LinkdBase.StartLast();
            ClientZezex_Linkd.StartLast();
            ClientGame_Login.StartLast();
            ClientGame_Fight.StartLast();
            ClientGame_Equip.StartLast();
        } finally {
            unlock();
        }
    }

    public void stopModules() throws Exception {
        lock();
        try {
            if (ClientGame_Equip != null)
                ClientGame_Equip.Stop(this);
            if (ClientGame_Fight != null)
                ClientGame_Fight.Stop(this);
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
        } finally {
            unlock();
        }
    }

    public void stopBeforeModules() throws Exception {
        lock();
        try {
            if (ClientGame_Equip != null)
                ClientGame_Equip.StopBefore();
            if (ClientGame_Fight != null)
                ClientGame_Fight.StopBefore();
            if (ClientGame_Login != null)
                ClientGame_Login.StopBefore();
            if (ClientZezex_Linkd != null)
                ClientZezex_Linkd.StopBefore();
            if (Zeze_Builtin_LinkdBase != null)
                Zeze_Builtin_LinkdBase.StopBefore();
            if (Zeze_Builtin_Game_Bag != null)
                Zeze_Builtin_Game_Bag.StopBefore();
            if (Zeze_Builtin_Game_Online != null)
                Zeze_Builtin_Game_Online.StopBefore();
        } finally {
            unlock();
        }
    }

    public void startService() throws Exception {
        lock();
        try {
            ClientService.start();
        } finally {
            unlock();
        }
    }

    public void stopService() throws Exception {
        lock();
        try {
            if (ClientService != null)
                ClientService.stop();
        } finally {
            unlock();
        }
    }
    // ZEZE_FILE_CHUNK }}} GEN APP @formatter:on
}
