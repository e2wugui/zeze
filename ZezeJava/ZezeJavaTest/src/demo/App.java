package demo;

import java.util.Objects;
import java.io.File;
import Benchmark.ABasicSimpleAddOneThread;
import Zeze.Application;
import Zeze.Arch.LoadConfig;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderDirectService;
import Zeze.Arch.ProviderModuleBinds;
import Zeze.Arch.ProviderService;
import Zeze.Collections.BoolList;
import Zeze.Collections.LinkedMap;
import Zeze.Config;
import Zeze.Game.Bag;
import Zeze.Game.ProviderDirectWithTransmit;
import Zeze.Game.ProviderWithOnline;
import Zeze.Netty.FreeMarker;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Netty.Thymeleaf;
import Zeze.Services.Daemon;
import Zeze.Services.RocketMQ.Producer;
import Zeze.Util.ShutdownHook;
import org.apache.rocketmq.client.ClientConfig;
import org.jspecify.annotations.Nullable;

public class App extends Zeze.AppBase {


	public static void main(String[] args) throws Exception {
		System.err.println(System.getProperties().get("user.dir"));
		Instance.Start();
		int i = 0;
		if (args.length == 0) {
			//noinspection InfiniteLoopStatement
			while (true) {
				//noinspection BusyWait
				Thread.sleep(1000);
				var result = Instance.getZeze().newProcedure(() ->
				{
					Objects.requireNonNull(Instance.demo_Module1).getTable1().get(1L);
					return 0L;
				}, "Global Access").call();
				++i;
				System.err.println(i + "-" + result);
			}
		}
		//Instance.Stop();
	}

	public static final App Instance = new App();
	public static final Netty netty = new Netty();

	public static App getInstance() {
		return Instance;
	}

	public BoolList.@Nullable Module BoolListModule;
	public LinkedMap.@Nullable Module LinkedMapModule;
	public Bag.@Nullable Module BagModule;
	public @Nullable Producer RocketMQProducer;
	public @Nullable HttpServer HttpServer;

	@Override
	public HttpServer getHttpServer() {
		return Objects.requireNonNull(HttpServer);
	}

	public static class MyHttpServer extends HttpServer {
		private final @Nullable FreeMarker freeMarker;

		public MyHttpServer(Application zeze) throws Exception {
			super(zeze);
			var webDir = new File("web");
			freeMarker = webDir.isDirectory() ? new FreeMarker(webDir) : null;
		}

		@Override
		public @Nullable FreeMarker getFreeMarker() {
			return freeMarker;
		}

		@Override
		public @Nullable Thymeleaf getThymeleaf() {
			return super.getThymeleaf(); //TODO
		}
	}

	public void Start() throws Exception {
		if (started)
			return;
		Start(Config.load("./zeze.xml"));
	}

	private static void adjustTableConf(Config.@Nullable TableConf conf) {
		if (null != conf) {
			if (conf.getRealCacheCapacity() < ABasicSimpleAddOneThread.AddCount) {
				conf.setCacheCapacity(ABasicSimpleAddOneThread.AddCount);
				conf.setCacheFactor(1.0f);
			}
		}
	}

	public @Nullable ProviderApp providerApp;
	private boolean started = false;

	public void Start(Config config) throws Exception {
		if (started)
			return;

		System.setProperty(Daemon.propertyNameClearInUse, "true");
		started = true;
		// 测试本地事务性能需要容量大一点
		adjustTableConf(config.getDefaultTableConf());
		adjustTableConf(config.getTableConfMap().get("demo_Module1_Table1"));

		createZeze(harness.TestDatabaseConfig.apply(config));
		createService();
		var provider = new ProviderWithOnline();
		providerApp = new ProviderApp(getZeze(), provider,
				new ProviderService("Server", getZeze()), "DemoApp#", new ProviderDirectWithTransmit(),
				new ProviderDirectService("ServerDirect", getZeze()), "DemoLinkd", new LoadConfig());
		provider.create(this);
		HttpServer = new MyHttpServer(getZeze());
		createModules();
		HttpServer.enableHttpSession(); // 点亮 Netty.HttpSession Cookie 会话（含表注册）；需在 initialize 之后、start 之前
		LinkedMapModule = new LinkedMap.Module(getZeze());
		BoolListModule = new BoolList.Module(getZeze());
		BagModule = new Bag.Module(providerApp, null);
		RocketMQProducer = new Producer(getZeze(), "testRocketMQ", new ClientConfig());
		getZeze().start(); // 启动数据库
		startModules(); // 启动模块，装载配置什么的
		HttpServer.start(netty, 10000);
		getZeze().endStart();
		startService(); // 启动网络

		providerApp.startLast(ProviderModuleBinds.load(""), modules);
		ShutdownHook.add(this, this::Stop);
	}

	public void Stop() throws Exception {
		if (!started)
			return;
		started = false;
		ShutdownHook.remove(this);
		stopService(); // 关闭网络
		Objects.requireNonNull(HttpServer).close();
		stopModules(); // 关闭模块，卸载配置什么的。
		Objects.requireNonNull(Objects.requireNonNull(providerApp).providerImplement).stop();
		if (Zeze != null) {
			getZeze().stop(); // 关闭数据库
			if (null != BoolListModule) {
				BoolListModule.UnRegister();
				BoolListModule = null;
			}
			if (LinkedMapModule != null) {
				LinkedMapModule.UnRegisterZezeTables(Zeze);
				LinkedMapModule = null;
			}
			if (BagModule != null) {
				BagModule.UnRegisterZezeTables(Zeze);
				BagModule = null;
			}
			if (null != RocketMQProducer) {
				RocketMQProducer.stop();
				RocketMQProducer = null;
			}
		}
		destroyModules();
		destroyServices();
		destroyZeze();
	}

	// ZEZE_FILE_CHUNK {{{ GEN APP @formatter:off
    public Zeze.@org.jspecify.annotations.Nullable Application Zeze;

    public demo.@org.jspecify.annotations.Nullable TestServer TestServer;

    public demo.Game.@org.jspecify.annotations.Nullable ModuleGame demo_Game;
    public demo.web.@org.jspecify.annotations.Nullable ModuleWeb demo_web;
    public demo.Module1.@org.jspecify.annotations.Nullable ModuleModule1 demo_Module1;
    public demo.Module1.Module11.@org.jspecify.annotations.Nullable ModuleModule11 demo_Module1_Module11;
    public demo.M6.@org.jspecify.annotations.Nullable ModuleM6 demo_M6;
    public demo.M6.M7.@org.jspecify.annotations.Nullable ModuleM7 demo_M6_M7;
    public TaskTest.TaskExt.@org.jspecify.annotations.Nullable ModuleTaskExt TaskTest_TaskExt;
    public demo.ModuleGTable.@org.jspecify.annotations.Nullable ModuleModuleGTable demo_ModuleGTable;

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

            Zeze = new Zeze.Application("ZezeJavaTest", config);
        } finally {
            unlock();
        }
    }

    @Override
    public void createService() {
        lock();
        try {
            TestServer = new demo.TestServer(getZeze());
        } finally {
            unlock();
        }
    }

    public static Class<?>[] redirectModuleClasses() {
        return new Class[] {
            demo.Game.ModuleGame.class,
            demo.web.ModuleWeb.class,
            demo.Module1.ModuleModule1.class,
            demo.Module1.Module11.ModuleModule11.class,
            demo.M6.ModuleM6.class,
            demo.M6.M7.ModuleM7.class,
            TaskTest.TaskExt.ModuleTaskExt.class,
            demo.ModuleGTable.ModuleModuleGTable.class,
        };
    }

    @Override
    public void createModules() throws Exception {
        lock();
        try {
            getZeze().initialize(this);
            var _modules_ = createRedirectModules(redirectModuleClasses());

            demo_Game = (demo.Game.ModuleGame)_modules_[0];
            demo_Game.Initialize(this);
            if (modules.put(demo_Game.getFullName(), demo_Game) != null)
                throw new IllegalStateException("duplicate module name: demo_Game");

            demo_web = (demo.web.ModuleWeb)_modules_[1];
            demo_web.Initialize(this);
            if (modules.put(demo_web.getFullName(), demo_web) != null)
                throw new IllegalStateException("duplicate module name: demo_web");

            demo_Module1 = (demo.Module1.ModuleModule1)_modules_[2];
            demo_Module1.Initialize(this);
            if (modules.put(demo_Module1.getFullName(), demo_Module1) != null)
                throw new IllegalStateException("duplicate module name: demo_Module1");

            demo_Module1_Module11 = (demo.Module1.Module11.ModuleModule11)_modules_[3];
            demo_Module1_Module11.Initialize(this);
            if (modules.put(demo_Module1_Module11.getFullName(), demo_Module1_Module11) != null)
                throw new IllegalStateException("duplicate module name: demo_Module1_Module11");

            demo_M6 = (demo.M6.ModuleM6)_modules_[4];
            demo_M6.Initialize(this);
            if (modules.put(demo_M6.getFullName(), demo_M6) != null)
                throw new IllegalStateException("duplicate module name: demo_M6");

            demo_M6_M7 = (demo.M6.M7.ModuleM7)_modules_[5];
            demo_M6_M7.Initialize(this);
            if (modules.put(demo_M6_M7.getFullName(), demo_M6_M7) != null)
                throw new IllegalStateException("duplicate module name: demo_M6_M7");

            TaskTest_TaskExt = (TaskTest.TaskExt.ModuleTaskExt)_modules_[6];
            TaskTest_TaskExt.Initialize(this);
            if (modules.put(TaskTest_TaskExt.getFullName(), TaskTest_TaskExt) != null)
                throw new IllegalStateException("duplicate module name: TaskTest_TaskExt");

            demo_ModuleGTable = (demo.ModuleGTable.ModuleModuleGTable)_modules_[7];
            demo_ModuleGTable.Initialize(this);
            if (modules.put(demo_ModuleGTable.getFullName(), demo_ModuleGTable) != null)
                throw new IllegalStateException("duplicate module name: demo_ModuleGTable");

            getZeze().setSchemas(new demo.Schemas());
        } finally {
            unlock();
        }
    }

    public void destroyModules() throws Exception {
        lock();
        try {
            demo_ModuleGTable = null;
            TaskTest_TaskExt = null;
            demo_M6_M7 = null;
            demo_M6 = null;
            demo_Module1_Module11 = null;
            demo_Module1 = null;
            demo_web = null;
            demo_Game = null;
            modules.clear();
        } finally {
            unlock();
        }
    }

    public void destroyServices() {
        lock();
        try {
            TestServer = null;
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
            java.util.Objects.requireNonNull(demo_Game).Start(this);
            java.util.Objects.requireNonNull(demo_web).Start(this);
            java.util.Objects.requireNonNull(demo_Module1).Start(this);
            java.util.Objects.requireNonNull(demo_Module1_Module11).Start(this);
            java.util.Objects.requireNonNull(demo_M6).Start(this);
            java.util.Objects.requireNonNull(demo_M6_M7).Start(this);
            java.util.Objects.requireNonNull(TaskTest_TaskExt).Start(this);
            java.util.Objects.requireNonNull(demo_ModuleGTable).Start(this);
        } finally {
            unlock();
        }
    }

    @Override
    public void startLastModules() throws Exception {
        lock();
        try {
            java.util.Objects.requireNonNull(demo_Game).StartLast();
            java.util.Objects.requireNonNull(demo_web).StartLast();
            java.util.Objects.requireNonNull(demo_Module1).StartLast();
            java.util.Objects.requireNonNull(demo_Module1_Module11).StartLast();
            java.util.Objects.requireNonNull(demo_M6).StartLast();
            java.util.Objects.requireNonNull(demo_M6_M7).StartLast();
            java.util.Objects.requireNonNull(TaskTest_TaskExt).StartLast();
            java.util.Objects.requireNonNull(demo_ModuleGTable).StartLast();
        } finally {
            unlock();
        }
    }

    public void stopModules() throws Exception {
        lock();
        try {
            if (Zeze == null)
                return;
            if (demo_ModuleGTable != null)
                demo_ModuleGTable.Stop(this);
            if (TaskTest_TaskExt != null)
                TaskTest_TaskExt.Stop(this);
            if (demo_M6_M7 != null)
                demo_M6_M7.Stop(this);
            if (demo_M6 != null)
                demo_M6.Stop(this);
            if (demo_Module1_Module11 != null)
                demo_Module1_Module11.Stop(this);
            if (demo_Module1 != null)
                demo_Module1.Stop(this);
            if (demo_web != null)
                demo_web.Stop(this);
            if (demo_Game != null)
                demo_Game.Stop(this);
        } finally {
            unlock();
        }
    }

    public void stopBeforeModules() throws Exception {
        lock();
        try {
            if (Zeze == null)
                return;
            if (demo_ModuleGTable != null)
                demo_ModuleGTable.StopBefore();
            if (TaskTest_TaskExt != null)
                TaskTest_TaskExt.StopBefore();
            if (demo_M6_M7 != null)
                demo_M6_M7.StopBefore();
            if (demo_M6 != null)
                demo_M6.StopBefore();
            if (demo_Module1_Module11 != null)
                demo_Module1_Module11.StopBefore();
            if (demo_Module1 != null)
                demo_Module1.StopBefore();
            if (demo_web != null)
                demo_web.StopBefore();
            if (demo_Game != null)
                demo_Game.StopBefore();
        } finally {
            unlock();
        }
    }

    public void startService() throws Exception {
        lock();
        try {
            java.util.Objects.requireNonNull(TestServer).start();
        } finally {
            unlock();
        }
    }

    public void stopService() throws Exception {
        lock();
        try {
            if (TestServer != null)
                TestServer.stop();
        } finally {
            unlock();
        }
    }
	// ZEZE_FILE_CHUNK }}} GEN APP @formatter:on
}
