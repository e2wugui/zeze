package Zege;

import java.nio.file.Files;
import java.nio.file.Paths;
import Zeze.Arch.Gen.GenModule;
import Zeze.Arch.LoadConfig;
import Zeze.Arch.Online;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderDirect;
import Zeze.Arch.ProviderModuleBinds;
import Zeze.Arch.ProviderWithOnline;
import Zeze.Collections.DepartmentTree;
import Zeze.Collections.LinkedMap;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Util.JsonReader;
import Zeze.Util.PersistentAtomicLong;
import Zeze.Web.Web;

public class App extends Zeze.AppBase {
    public static final App Instance = new App();
    public static App getInstance() {
        return Instance;
    }

    public ProviderApp ProviderApp;
    public ProviderDirect ProviderDirect;
    public ProviderWithOnline Provider;
    public LinkedMap.Module LinkedMaps;
    public DepartmentTree.Module DepartmentTrees;
    public Web Web;

    private LoadConfig LoadConfig() {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get("linkd.json"));
            return new JsonReader().buf(bytes).parse(LoadConfig.class);
            // return new ObjectMapper().readValue(bytes, LoadConfig.class);
        } catch (Exception e) {
            // e.printStackTrace();
        }
        return new LoadConfig();
    }

    public void Start(String conf) throws Throwable {
        var config = Config.Load(conf);
        CreateZeze(config);
        CreateService();

        Provider = new ProviderWithOnline();
        ProviderDirect = new ProviderDirect();
        ProviderApp = new ProviderApp(Zeze, Provider, Server,
                "Zege.Server.Module#",
                ProviderDirect, ServerDirect, "Zege.Linkd", LoadConfig());
        Provider.Online = GenModule.Instance.ReplaceModuleInstance(this, new Online(this));
        LinkedMaps = new LinkedMap.Module(Zeze);
        DepartmentTrees = new DepartmentTree.Module(Zeze, LinkedMaps);
        Web = new Web(ProviderApp);

        CreateModules();
        Zeze.Start(); // 启动数据库
        StartModules(); // 启动模块，装载配置什么的。
        Provider.Online.Start();
        Web.Start();

        PersistentAtomicLong socketSessionIdGen = PersistentAtomicLong.getOrAdd("Zege.Server." + Zeze.getConfig().getServerId());
        AsyncSocket.setSessionIdGenFunc(socketSessionIdGen::next);
        StartService(); // 启动网络
        ProviderApp.StartLast(ProviderModuleBinds.Load(), Modules);
    }

    public void Stop() throws Throwable {
        if (Provider != null && Provider.Online != null)
            Provider.Online.Stop();
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
