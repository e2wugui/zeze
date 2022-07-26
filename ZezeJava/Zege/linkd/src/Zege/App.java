package Zege;

import java.nio.file.Files;
import java.nio.file.Paths;
import Zege.Linkd.Auth;
import Zeze.Arch.LinkdApp;
import Zeze.Arch.LinkdProvider;
import Zeze.Arch.LoadConfig;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.JsonReader;
import Zeze.Util.PersistentAtomicLong;

public class App extends Zeze.AppBase {
    public static final App Instance = new App();
    public static App getInstance() {
        return Instance;
    }

    public Zeze.Arch.LinkdApp LinkdApp;
    public Zeze.Arch.LinkdProvider LinkdProvider;

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
        LinkdProvider = new LinkdProvider();
        LinkdApp = new LinkdApp("Zege.Linkd", Zeze, LinkdProvider, ProviderService, LinkdService, LoadConfig());
        CreateModules();
        Zeze.Start(); // 启动数据库
        StartModules(); // 启动模块，装载配置什么的。
        var factoryHandle = new Service.ProtocolFactoryHandle<>();
        factoryHandle.Factory = Auth::new;
        factoryHandle.Level = TransactionLevel.None;
        ProviderService.AddFactoryHandle(Auth.TypeId_, factoryHandle);
        AsyncSocket.setSessionIdGenFunc(PersistentAtomicLong.getOrAdd(LinkdApp.GetName())::next);
        StartService(); // 启动网络
        LinkdApp.RegisterService(null);
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

    public Zege.LinkdService LinkdService;
    public Zege.ProviderService ProviderService;

    public Zege.Friend.ModuleFriend Zege_Friend;
    public Zege.Message.ModuleMessage Zege_Message;
    public Zege.Linkd.ModuleLinkd Zege_Linkd;

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

        LinkdService = new Zege.LinkdService(Zeze);
        ProviderService = new Zege.ProviderService(Zeze);
    }
    public synchronized void CreateModules() {
        Zege_Friend = ReplaceModuleInstance(new Zege.Friend.ModuleFriend(this));
        Zege_Friend.Initialize(this);
        if (Modules.put(Zege_Friend.getFullName(), Zege_Friend) != null)
            throw new RuntimeException("duplicate module name: Zege_Friend");

        Zege_Message = ReplaceModuleInstance(new Zege.Message.ModuleMessage(this));
        Zege_Message.Initialize(this);
        if (Modules.put(Zege_Message.getFullName(), Zege_Message) != null)
            throw new RuntimeException("duplicate module name: Zege_Message");

        Zege_Linkd = ReplaceModuleInstance(new Zege.Linkd.ModuleLinkd(this));
        Zege_Linkd.Initialize(this);
        if (Modules.put(Zege_Linkd.getFullName(), Zege_Linkd) != null)
            throw new RuntimeException("duplicate module name: Zege_Linkd");

        Zeze.setSchemas(new Zege.Schemas());
    }

    public synchronized void DestroyModules() {
        Zege_Linkd = null;
        Zege_Message = null;
        Zege_Friend = null;
        Modules.clear();
    }

    public synchronized void DestroyServices() {
        LinkdService = null;
        ProviderService = null;
    }

    public synchronized void DestroyZeze() {
        Zeze = null;
    }

    public synchronized void StartModules() throws Throwable {
        Zege_Friend.Start(this);
        Zege_Message.Start(this);
        Zege_Linkd.Start(this);
    }

    public synchronized void StopModules() throws Throwable {
        if (Zege_Linkd != null)
            Zege_Linkd.Stop(this);
        if (Zege_Message != null)
            Zege_Message.Stop(this);
        if (Zege_Friend != null)
            Zege_Friend.Stop(this);
    }

    public synchronized void StartService() throws Throwable {
        LinkdService.Start();
        ProviderService.Start();
    }

    public synchronized void StopService() throws Throwable {
        if (LinkdService != null)
            LinkdService.Stop();
        if (ProviderService != null)
            ProviderService.Stop();
    }
    // ZEZE_FILE_CHUNK }}} GEN APP @formatter:on
}
