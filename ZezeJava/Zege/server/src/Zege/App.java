package Zege;

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

    public Zege.Server Server;
    public Zege.ServerDirect ServerDirect;

    public Zege.User.ModuleUser Zege_User;
    public Zege.Friend.ModuleFriend Zege_Friend;
    public Zege.Message.ModuleMessage Zege_Message;

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

        Server = new Zege.Server(Zeze);
        ServerDirect = new Zege.ServerDirect(Zeze);
    }
    public synchronized void CreateModules() {
        Zege_User = ReplaceModuleInstance(new Zege.User.ModuleUser(this));
        Zege_User.Initialize(this);
        if (Modules.put(Zege_User.getFullName(), Zege_User) != null)
            throw new RuntimeException("duplicate module name: Zege_User");

        Zege_Friend = ReplaceModuleInstance(new Zege.Friend.ModuleFriend(this));
        Zege_Friend.Initialize(this);
        if (Modules.put(Zege_Friend.getFullName(), Zege_Friend) != null)
            throw new RuntimeException("duplicate module name: Zege_Friend");

        Zege_Message = ReplaceModuleInstance(new Zege.Message.ModuleMessage(this));
        Zege_Message.Initialize(this);
        if (Modules.put(Zege_Message.getFullName(), Zege_Message) != null)
            throw new RuntimeException("duplicate module name: Zege_Message");

        Zeze.setSchemas(new Zege.Schemas());
    }

    public synchronized void DestroyModules() {
        Zege_Message = null;
        Zege_Friend = null;
        Zege_User = null;
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
        Zege_User.Start(this);
        Zege_Friend.Start(this);
        Zege_Message.Start(this);
    }

    public synchronized void StopModules() throws Throwable {
        if (Zege_Message != null)
            Zege_Message.Stop(this);
        if (Zege_Friend != null)
            Zege_Friend.Stop(this);
        if (Zege_User != null)
            Zege_User.Stop(this);
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
