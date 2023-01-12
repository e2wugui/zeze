package Zege;

import java.nio.file.Files;
import java.nio.file.Paths;
import Zege.User.Create;
import Zege.User.CreateWithCert;
import Zege.User.Prepare;
import Zege.User.VerifyChallengeResult;
import Zeze.Arch.LinkdApp;
import Zeze.Arch.LinkdProvider;
import Zeze.Arch.LoadConfig;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.CommandConsole;
import Zeze.Util.JsonReader;
import Zeze.Util.PersistentAtomicLong;

public class App extends Zeze.AppBase {
    public static final App Instance = new App();
    public static App getInstance() {
        return Instance;
    }

    public Zeze.Arch.LinkdApp LinkdApp;
    public Zeze.Arch.LinkdProvider LinkdProvider;

    private LoadConfig loadConfig() {
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
        var config = Config.load(conf);
        createZeze(config);
        createService();
        LinkdProvider = new LinkdProvider();
        LinkdApp = new LinkdApp("Zege.Linkd", Zeze, LinkdProvider, ProviderService, LinkdService, loadConfig());
        createModules();
        Zeze.start(); // 启动数据库
        startModules(); // 启动模块，装载配置什么的。

        // 直接发送Server-Provider支持的Rpc，但这个Rpc没有通过普通的Service注册。
        // 需要特别处理，由于Server接受任何支持的Rpc，但Rpc-Result要特别注册。
        {
            var factoryHandle = new Service.ProtocolFactoryHandle<>();
            factoryHandle.Factory = Prepare::new;
            factoryHandle.Level = TransactionLevel.None;
            ProviderService.AddFactoryHandle(Prepare.TypeId_, factoryHandle);
        }
        {
            var factoryHandle = new Service.ProtocolFactoryHandle<>();
            factoryHandle.Factory = Create::new;
            factoryHandle.Level = TransactionLevel.None;
            ProviderService.AddFactoryHandle(Create.TypeId_, factoryHandle);
        }
        {
            var factoryHandle = new Service.ProtocolFactoryHandle<>();
            factoryHandle.Factory = CreateWithCert::new;
            factoryHandle.Level = TransactionLevel.None;
            ProviderService.AddFactoryHandle(CreateWithCert.TypeId_, factoryHandle);
        }
        {
            var factoryHandle = new Service.ProtocolFactoryHandle<>();
            factoryHandle.Factory = VerifyChallengeResult::new;
            factoryHandle.Level = TransactionLevel.None;
            ProviderService.AddFactoryHandle(VerifyChallengeResult.TypeId_, factoryHandle);
        }
        AsyncSocket.setSessionIdGenFunc(PersistentAtomicLong.getOrAdd(LinkdApp.getName())::next);
        startService(); // 启动网络
        {
            var cc = new CommandConsole();
            cc.register("echo", (sender, args) -> sender.Send(args.toString() + "\r\n"));
            cc.register("options", (sender, args) -> sender.Send(CommandConsole.Options.parseJvm(args) + "\r\n"));
            LinkdApp.commandConsoleService.setCommandConsole(cc);
        }
        LinkdApp.registerService(null);

        // 基于linkd转发的Web服务，考虑移除，需要在Server实现Web请使用基于Netty-Http的Web。
        /*
        LinkdApp.HttpService = new HttpService(LinkdApp, 80, Task.getThreadPool());
        // 如果需要拦截验证请求在linkd处理。在这里注册，
        // HttpService.interceptAuthContext("/myapp/myauth", new MyHttpAuth(HttpService));
        LinkdApp.HttpService.start();
        */
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

    public Zege.LinkdService LinkdService;
    public Zege.ProviderService ProviderService;

    public Zege.Friend.ModuleFriend Zege_Friend;
    public Zege.Message.ModuleMessage Zege_Message;
    public Zege.Linkd.ModuleLinkd Zege_Linkd;
    public Zege.User.ModuleUser Zege_User;

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

        Zeze = new Zeze.Application("linkd", config);
    }

    public synchronized void createService() throws Throwable {
        LinkdService = new Zege.LinkdService(Zeze);
        ProviderService = new Zege.ProviderService(Zeze);
    }

    public synchronized void createModules() {
        var _modules_ = createRedirectModules(new Class[] {
            Zege.Friend.ModuleFriend.class,
            Zege.Message.ModuleMessage.class,
            Zege.Linkd.ModuleLinkd.class,
            Zege.User.ModuleUser.class,
        });
        if (_modules_ == null)
            return;

        Zege_Friend = (Zege.Friend.ModuleFriend)_modules_[0];
        Zege_Friend.Initialize(this);
        if (modules.put(Zege_Friend.getFullName(), Zege_Friend) != null)
            throw new RuntimeException("duplicate module name: Zege_Friend");

        Zege_Message = (Zege.Message.ModuleMessage)_modules_[1];
        Zege_Message.Initialize(this);
        if (modules.put(Zege_Message.getFullName(), Zege_Message) != null)
            throw new RuntimeException("duplicate module name: Zege_Message");

        Zege_Linkd = (Zege.Linkd.ModuleLinkd)_modules_[2];
        Zege_Linkd.Initialize(this);
        if (modules.put(Zege_Linkd.getFullName(), Zege_Linkd) != null)
            throw new RuntimeException("duplicate module name: Zege_Linkd");

        Zege_User = (Zege.User.ModuleUser)_modules_[3];
        Zege_User.Initialize(this);
        if (modules.put(Zege_User.getFullName(), Zege_User) != null)
            throw new RuntimeException("duplicate module name: Zege_User");

        Zeze.setSchemas(new Zege.Schemas());
    }

    public synchronized void destroyModules() {
        Zege_User = null;
        Zege_Linkd = null;
        Zege_Message = null;
        Zege_Friend = null;
        modules.clear();
    }

    public synchronized void destroyServices() {
        LinkdService = null;
        ProviderService = null;
    }

    public synchronized void destroyZeze() {
        Zeze = null;
    }

    public synchronized void startModules() throws Throwable {
        Zege_Friend.Start(this);
        Zege_Message.Start(this);
        Zege_Linkd.Start(this);
        Zege_User.Start(this);
    }

    public synchronized void stopModules() throws Throwable {
        if (Zege_User != null)
            Zege_User.Stop(this);
        if (Zege_Linkd != null)
            Zege_Linkd.Stop(this);
        if (Zege_Message != null)
            Zege_Message.Stop(this);
        if (Zege_Friend != null)
            Zege_Friend.Stop(this);
    }

    public synchronized void startService() throws Throwable {
        LinkdService.start();
        ProviderService.start();
    }

    public synchronized void stopService() throws Throwable {
        if (LinkdService != null)
            LinkdService.stop();
        if (ProviderService != null)
            ProviderService.stop();
    }
    // ZEZE_FILE_CHUNK }}} GEN APP @formatter:on
}
