package Zege;

import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Util.OutObject;

public class App extends Zeze.AppBase {
    public static final App Instance = new App();
    public static App getInstance() {
        return Instance;
    }
    public Zeze.Net.Connector Connector;

    public void Start(String ip, int port) throws Exception {
        var config = Config.load("client.xml");
        createZeze(config);
        createService();
        if (null != ip && !ip.isEmpty() && port != 0) {
            var c = new OutObject<Connector>();
            ClientService.getConfig().tryGetOrAddConnector(ip, port, false, c);
            Connector = c.value;
        } else {
            ClientService.getConfig().forEachConnector2((c) -> { Connector = c; return false; });
        }
        if (null == Connector)
            throw new RuntimeException("miss Connector!");

        createModules();
        Zeze.start(); // 启动数据库
        startModules(); // 启动模块，装载配置什么的。
        startService(); // 启动网络
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
    public final java.util.HashMap<String, Zeze.IModule> modules = new java.util.HashMap<>();

    public Zege.ClientService ClientService;

    public Zeze.Builtin.Online.ModuleOnline Zeze_Builtin_Online;
    public Zeze.Builtin.LinkdBase.ModuleLinkdBase Zeze_Builtin_LinkdBase;
    public Zege.Linkd.ModuleLinkd Zege_Linkd;
    public Zege.Friend.ModuleFriend Zege_Friend;
    public Zege.Message.ModuleMessage Zege_Message;
    public Zege.User.ModuleUser Zege_User;

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

        Zeze = new Zeze.Application("client", config);
    }

    public synchronized void createService() throws Exception {
        ClientService = new Zege.ClientService(Zeze);
    }

    public synchronized void createModules() {
        var _modules_ = createRedirectModules(new Class[] {
            Zeze.Builtin.Online.ModuleOnline.class,
            Zeze.Builtin.LinkdBase.ModuleLinkdBase.class,
            Zege.Linkd.ModuleLinkd.class,
            Zege.Friend.ModuleFriend.class,
            Zege.Message.ModuleMessage.class,
            Zege.User.ModuleUser.class,
        });
        if (_modules_ == null)
            return;

        Zeze_Builtin_Online = (Zeze.Builtin.Online.ModuleOnline)_modules_[0];
        Zeze_Builtin_Online.Initialize(this);
        if (modules.put(Zeze_Builtin_Online.getFullName(), Zeze_Builtin_Online) != null)
            throw new RuntimeException("duplicate module name: Zeze_Builtin_Online");

        Zeze_Builtin_LinkdBase = (Zeze.Builtin.LinkdBase.ModuleLinkdBase)_modules_[1];
        Zeze_Builtin_LinkdBase.Initialize(this);
        if (modules.put(Zeze_Builtin_LinkdBase.getFullName(), Zeze_Builtin_LinkdBase) != null)
            throw new RuntimeException("duplicate module name: Zeze_Builtin_LinkdBase");

        Zege_Linkd = (Zege.Linkd.ModuleLinkd)_modules_[2];
        Zege_Linkd.Initialize(this);
        if (modules.put(Zege_Linkd.getFullName(), Zege_Linkd) != null)
            throw new RuntimeException("duplicate module name: Zege_Linkd");

        Zege_Friend = (Zege.Friend.ModuleFriend)_modules_[3];
        Zege_Friend.Initialize(this);
        if (modules.put(Zege_Friend.getFullName(), Zege_Friend) != null)
            throw new RuntimeException("duplicate module name: Zege_Friend");

        Zege_Message = (Zege.Message.ModuleMessage)_modules_[4];
        Zege_Message.Initialize(this);
        if (modules.put(Zege_Message.getFullName(), Zege_Message) != null)
            throw new RuntimeException("duplicate module name: Zege_Message");

        Zege_User = (Zege.User.ModuleUser)_modules_[5];
        Zege_User.Initialize(this);
        if (modules.put(Zege_User.getFullName(), Zege_User) != null)
            throw new RuntimeException("duplicate module name: Zege_User");

        Zeze.setSchemas(new Zege.Schemas());
    }

    public synchronized void destroyModules() {
        Zege_User = null;
        Zege_Message = null;
        Zege_Friend = null;
        Zege_Linkd = null;
        Zeze_Builtin_LinkdBase = null;
        Zeze_Builtin_Online = null;
        modules.clear();
    }

    public synchronized void destroyServices() {
        ClientService = null;
    }

    public synchronized void destroyZeze() {
        Zeze = null;
    }

    public synchronized void startModules() throws Exception {
        Zeze_Builtin_Online.Start(this);
        Zeze_Builtin_LinkdBase.Start(this);
        Zege_Linkd.Start(this);
        Zege_Friend.Start(this);
        Zege_Message.Start(this);
        Zege_User.Start(this);
    }

    public synchronized void stopModules() throws Exception {
        if (Zege_User != null)
            Zege_User.Stop(this);
        if (Zege_Message != null)
            Zege_Message.Stop(this);
        if (Zege_Friend != null)
            Zege_Friend.Stop(this);
        if (Zege_Linkd != null)
            Zege_Linkd.Stop(this);
        if (Zeze_Builtin_LinkdBase != null)
            Zeze_Builtin_LinkdBase.Stop(this);
        if (Zeze_Builtin_Online != null)
            Zeze_Builtin_Online.Stop(this);
    }

    public synchronized void startService() throws Exception {
        ClientService.start();
    }

    public synchronized void stopService() throws Exception {
        if (ClientService != null)
            ClientService.stop();
    }
    // ZEZE_FILE_CHUNK }}} GEN APP @formatter:on
}
