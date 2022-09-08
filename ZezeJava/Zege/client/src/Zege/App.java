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

    public void Start(String ip, int port) throws Throwable {
        var config = Config.Load("client.xml");
        CreateZeze(config);
        CreateService();
        if (null != ip && !ip.isEmpty() && port != 0) {
            var c = new OutObject<Connector>();
            ClientService.getConfig().TryGetOrAddConnector(ip, port, false, c);
            Connector = c.Value;
        } else {
            ClientService.getConfig().forEachConnector2((c) -> { Connector = c; return false; });
        }
        if (null == Connector)
            throw new RuntimeException("miss Connector!");

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

    public void CreateZeze() throws Throwable {
        CreateZeze(null);
    }

    public synchronized void CreateZeze(Zeze.Config config) throws Throwable {
        if (Zeze != null)
            throw new RuntimeException("Zeze Has Created!");

        Zeze = new Zeze.Application("Zege", config);
    }

    public synchronized void CreateService() throws Throwable {
        ClientService = new Zege.ClientService(Zeze);
    }

    public synchronized void CreateModules() {
        Zeze_Builtin_Online = ReplaceModuleInstance(new Zeze.Builtin.Online.ModuleOnline(this));
        Zeze_Builtin_Online.Initialize(this);
        if (Modules.put(Zeze_Builtin_Online.getFullName(), Zeze_Builtin_Online) != null)
            throw new RuntimeException("duplicate module name: Zeze_Builtin_Online");

        Zeze_Builtin_LinkdBase = ReplaceModuleInstance(new Zeze.Builtin.LinkdBase.ModuleLinkdBase(this));
        Zeze_Builtin_LinkdBase.Initialize(this);
        if (Modules.put(Zeze_Builtin_LinkdBase.getFullName(), Zeze_Builtin_LinkdBase) != null)
            throw new RuntimeException("duplicate module name: Zeze_Builtin_LinkdBase");

        Zege_Linkd = ReplaceModuleInstance(new Zege.Linkd.ModuleLinkd(this));
        Zege_Linkd.Initialize(this);
        if (Modules.put(Zege_Linkd.getFullName(), Zege_Linkd) != null)
            throw new RuntimeException("duplicate module name: Zege_Linkd");

        Zege_Friend = ReplaceModuleInstance(new Zege.Friend.ModuleFriend(this));
        Zege_Friend.Initialize(this);
        if (Modules.put(Zege_Friend.getFullName(), Zege_Friend) != null)
            throw new RuntimeException("duplicate module name: Zege_Friend");

        Zege_Message = ReplaceModuleInstance(new Zege.Message.ModuleMessage(this));
        Zege_Message.Initialize(this);
        if (Modules.put(Zege_Message.getFullName(), Zege_Message) != null)
            throw new RuntimeException("duplicate module name: Zege_Message");

        Zege_User = ReplaceModuleInstance(new Zege.User.ModuleUser(this));
        Zege_User.Initialize(this);
        if (Modules.put(Zege_User.getFullName(), Zege_User) != null)
            throw new RuntimeException("duplicate module name: Zege_User");

        Zeze.setSchemas(new Zege.Schemas());
    }

    public synchronized void DestroyModules() {
        Zege_User = null;
        Zege_Message = null;
        Zege_Friend = null;
        Zege_Linkd = null;
        Zeze_Builtin_LinkdBase = null;
        Zeze_Builtin_Online = null;
        Modules.clear();
    }

    public synchronized void DestroyServices() {
        ClientService = null;
    }

    public synchronized void DestroyZeze() {
        Zeze = null;
    }

    public synchronized void StartModules() throws Throwable {
        Zeze_Builtin_Online.Start(this);
        Zeze_Builtin_LinkdBase.Start(this);
        Zege_Linkd.Start(this);
        Zege_Friend.Start(this);
        Zege_Message.Start(this);
        Zege_User.Start(this);
    }

    public synchronized void StopModules() throws Throwable {
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

    public synchronized void StartService() throws Throwable {
        ClientService.Start();
    }

    public synchronized void StopService() throws Throwable {
        if (ClientService != null)
            ClientService.Stop();
    }
    // ZEZE_FILE_CHUNK }}} GEN APP @formatter:on
}
