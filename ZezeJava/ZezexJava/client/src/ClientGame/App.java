package ClientGame;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import Zeze.Builtin.LoginQueue.BLoginToken;
import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Services.LoginQueueClient;
import Zeze.Util.OutObject;
import Zeze.Util.TaskCompletionSource;

public class App extends Zeze.AppBase {
    public static final App Instance = new App();
    public static App getInstance() {
        return Instance;
    }
    public @Nullable Connector Connector;
    public LoginQueueClient loginQueueClient = new LoginQueueClient();
    public @Nullable TaskCompletionSource<BLoginToken.Data> onLinkConnectedFuture;

    public void Start(String ip, int port) throws Exception {
        var config = Config.load("client.xml");
        // 客户端纯RPC零表：不建库。client.xml的默认Memory空桶无谓；MySQL重定向下250客户端
        // 各起一物理池瞬时需求>151连接上限（BenchRoleTimer风暴根源，2026-10-24终案）。
        config.setNoDatabase(true);
        createZeze(config);
        createService();
        createModules();
        getZeze().start(); // 启动数据库
        startModules(); // 启动模块，装载配置什么的。
        startService(); // 启动网络

		onLinkConnectedFuture = new TaskCompletionSource<>();
        loginQueueClient.setLoginToken((loginToken) -> {
            var c = new OutObject<Connector>();
            Objects.requireNonNull(ClientService).getConfig().tryGetOrAddConnector(loginToken.getLinkIp(), loginToken.getLinkPort(), true, c);
            Connector = Objects.requireNonNull(c.value);
            Connector.start();
            Connector.WaitReady();
            Objects.requireNonNull(onLinkConnectedFuture).setResult(loginToken);
        });
        loginQueueClient.connect("127.0.0.1", 5020);
    }

    public void Start2(String wsUrl) throws Exception {
        var config = Config.load("client.xml");
        config.setNoDatabase(true); // 同Start：客户端纯RPC零表不建库（见Start处注释）
        createZeze(config);
        createService();
        createModules();
        getZeze().start(); // 启动数据库
        startModules(); // 启动模块，装载配置什么的。
        startService(); // 启动网络

		onLinkConnectedFuture = new TaskCompletionSource<>();
        loginQueueClient.setLoginToken((loginToken) -> {
            var url = "ws://" + loginToken.getLinkIp()
                    + ":" + (loginToken.getLinkPort() + 10000) + "/websocket";
            Connector = new Connector(true, url);
            System.out.println("wsUrl =========== " + url);
            Objects.requireNonNull(ClientService).getConfig().addConnector(Connector);
            Connector.start();
            Connector.WaitReady();
            Objects.requireNonNull(onLinkConnectedFuture).setResult(loginToken);
        });
        loginQueueClient.connect("127.0.0.1", 5020);
    }

    public void Stop() throws Exception {
        stopService(); // 关闭网络
        stopModules(); // 关闭模块，卸载配置什么的。
        if (Zeze != null)
            getZeze().stop(); // 关闭数据库
        destroyModules();
        destroyServices();
        destroyZeze();
    }

    // ZEZE_FILE_CHUNK {{{ GEN APP @formatter:off
    public Zeze.@org.jspecify.annotations.Nullable Application Zeze;

    public ClientGame.@org.jspecify.annotations.Nullable ClientService ClientService;

    public Zeze.Builtin.Game.Online.@org.jspecify.annotations.Nullable ModuleOnline Zeze_Builtin_Game_Online;
    public Zeze.Builtin.Game.Bag.@org.jspecify.annotations.Nullable ModuleBag Zeze_Builtin_Game_Bag;
    public Zeze.Builtin.LinkdBase.@org.jspecify.annotations.Nullable ModuleLinkdBase Zeze_Builtin_LinkdBase;
    public ClientZezex.Linkd.@org.jspecify.annotations.Nullable ModuleLinkd ClientZezex_Linkd;
    public ClientGame.Login.@org.jspecify.annotations.Nullable ModuleLogin ClientGame_Login;
    public ClientGame.Fight.@org.jspecify.annotations.Nullable ModuleFight ClientGame_Fight;
    public ClientGame.Equip.@org.jspecify.annotations.Nullable ModuleEquip ClientGame_Equip;

    @Override
    public Zeze.Application getZeze() {
        return java.util.Objects.requireNonNull(Zeze, "Zeze has not been created");
    }

    public void createZeze() throws Exception {
        createZeze(null);
    }

    @Override
    public void createZeze(Zeze.@org.jspecify.annotations.Nullable Config config) throws Exception {
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
            ClientService = new ClientGame.ClientService(getZeze());
        } finally {
            unlock();
        }
    }

    public static Class<?>[] redirectModuleClasses() {
        return new Class[] {
            Zeze.Builtin.Game.Online.ModuleOnline.class,
            Zeze.Builtin.Game.Bag.ModuleBag.class,
            Zeze.Builtin.LinkdBase.ModuleLinkdBase.class,
            ClientZezex.Linkd.ModuleLinkd.class,
            ClientGame.Login.ModuleLogin.class,
            ClientGame.Fight.ModuleFight.class,
            ClientGame.Equip.ModuleEquip.class,
        };
    }

    @Override
    public void createModules() throws Exception {
        lock();
        try {
            getZeze().initialize(this);
            var _modules_ = createRedirectModules(redirectModuleClasses());

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

            getZeze().setSchemas(new ClientGame.Schemas());
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
            java.util.Objects.requireNonNull(Zeze_Builtin_Game_Online).Start(this);
            java.util.Objects.requireNonNull(Zeze_Builtin_Game_Bag).Start(this);
            java.util.Objects.requireNonNull(Zeze_Builtin_LinkdBase).Start(this);
            java.util.Objects.requireNonNull(ClientZezex_Linkd).Start(this);
            java.util.Objects.requireNonNull(ClientGame_Login).Start(this);
            java.util.Objects.requireNonNull(ClientGame_Fight).Start(this);
            java.util.Objects.requireNonNull(ClientGame_Equip).Start(this);
        } finally {
            unlock();
        }
    }

    @Override
    public void startLastModules() throws Exception {
        lock();
        try {
            java.util.Objects.requireNonNull(Zeze_Builtin_Game_Online).StartLast();
            java.util.Objects.requireNonNull(Zeze_Builtin_Game_Bag).StartLast();
            java.util.Objects.requireNonNull(Zeze_Builtin_LinkdBase).StartLast();
            java.util.Objects.requireNonNull(ClientZezex_Linkd).StartLast();
            java.util.Objects.requireNonNull(ClientGame_Login).StartLast();
            java.util.Objects.requireNonNull(ClientGame_Fight).StartLast();
            java.util.Objects.requireNonNull(ClientGame_Equip).StartLast();
        } finally {
            unlock();
        }
    }

    public void stopModules() throws Exception {
        lock();
        try {
            if (Zeze == null)
                return;
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
            if (Zeze == null)
                return;
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
            java.util.Objects.requireNonNull(ClientService).start();
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
