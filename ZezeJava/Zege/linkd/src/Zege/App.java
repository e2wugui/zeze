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

    public Zege.LinkdService LinkdService;
    public Zege.ProviderService ProviderService;

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
        Zege_Linkd = ReplaceModuleInstance(new Zege.Linkd.ModuleLinkd(this));
        Zege_Linkd.Initialize(this);
        if (Modules.put(Zege_Linkd.getFullName(), Zege_Linkd) != null)
            throw new RuntimeException("duplicate module name: Zege_Linkd");

        Zeze.setSchemas(new Zege.Schemas());
    }

    public synchronized void DestroyModules() {
        Zege_Linkd = null;
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
        Zege_Linkd.Start(this);
    }

    public synchronized void StopModules() throws Throwable {
        if (Zege_Linkd != null)
            Zege_Linkd.Stop(this);
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
