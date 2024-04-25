package demo;

import Benchmark.ABasicSimpleAddOneThread;
import Zeze.Arch.LoadConfig;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderDirectService;
import Zeze.Arch.ProviderService;
import Zeze.Collections.BoolList;
import Zeze.Collections.LinkedMap;
import Zeze.Config;
import Zeze.Game.Bag;
import Zeze.Game.ProviderDirectWithTransmit;
import Zeze.Game.ProviderWithOnline;
import Zeze.Services.Daemon;
import Zeze.Services.RocketMQ.Producer;
import Zeze.Util.ShutdownHook;

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
				var result = Instance.Zeze.newProcedure(() ->
				{
					Instance.demo_Module1.getTable1().get(1L);
					return 0L;
				}, "Global Access").call();
				++i;
				System.err.println(i + "-" + result);
			}
		}
		//Instance.Stop();
	}

	public static final App Instance = new App();

	public static App getInstance() {
		return Instance;
	}

	public BoolList.Module BoolListModule;
	public LinkedMap.Module LinkedMapModule;
	public Bag.Module BagModule;
	public Producer RocketMQProducer;

	public void Start() throws Exception {
		Start(Config.load("./zeze.xml"));
	}

	private static void adjustTableConf(Config.TableConf conf) {
		if (null != conf) {
			if (conf.getRealCacheCapacity() < ABasicSimpleAddOneThread.AddCount) {
				conf.setCacheCapacity(ABasicSimpleAddOneThread.AddCount);
				conf.setCacheFactor(1.0f);
			}
		}
	}

	public ProviderApp providerApp;
	private boolean started = false;
	public void Start(Config config) throws Exception {
		if (started)
			return;

		System.setProperty(Daemon.propertyNameClearInUse, "true");
		started = true;
		// 测试本地事务性能需要容量大一点
		adjustTableConf(config.getDefaultTableConf());
		adjustTableConf(config.getTableConfMap().get("demo_Module1_Table1"));

		createZeze(config);
		createService();
		var provider = new ProviderWithOnline();
		providerApp = new ProviderApp(Zeze, provider,
				new ProviderService("Server", Zeze), "DemoApp#", new ProviderDirectWithTransmit(),
				new ProviderDirectService("ServerDirect", Zeze), "DemoLinkd", new LoadConfig());
		provider.create(this);
		createModules();
		LinkedMapModule = new LinkedMap.Module(Zeze);
		BoolListModule = new BoolList.Module(Zeze);
		BagModule = new Bag.Module(providerApp, null);
		RocketMQProducer = new Producer(Zeze);
		Zeze.start(); // 启动数据库
		startModules(); // 启动模块，装载配置什么的
		Zeze.endStart();
		startService(); // 启动网络

		ShutdownHook.add(this, this::Stop);
	}

	public void Stop() throws Exception {
		if (!started)
			return;
		started = false;
		stopService(); // 关闭网络
		stopModules(); // 关闭模块，卸载配置什么的。
		if (Zeze != null) {
			Zeze.stop(); // 关闭数据库
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
    public Zeze.Application Zeze;

    public demo.TestServer TestServer;

    public demo.Module1.ModuleModule1 demo_Module1;
    public demo.Module1.Module11.ModuleModule11 demo_Module1_Module11;
    public demo.M6.ModuleM6 demo_M6;
    public demo.M6.M7.ModuleM7 demo_M6_M7;
    public TaskTest.TaskExt.ModuleTaskExt TaskTest_TaskExt;

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

            Zeze = new Zeze.Application("ZezeJavaTest", config);
        } finally {
            unlock();
        }
    }

    @Override
    public void createService() {
        lock();
        try {
            TestServer = new demo.TestServer(Zeze);
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
                demo.Module1.ModuleModule1.class,
                demo.Module1.Module11.ModuleModule11.class,
                demo.M6.ModuleM6.class,
                demo.M6.M7.ModuleM7.class,
                TaskTest.TaskExt.ModuleTaskExt.class,
            });
            if (_modules_ == null)
                return;

            demo_Module1 = (demo.Module1.ModuleModule1)_modules_[0];
            demo_Module1.Initialize(this);
            if (modules.put(demo_Module1.getFullName(), demo_Module1) != null)
                throw new IllegalStateException("duplicate module name: demo_Module1");

            demo_Module1_Module11 = (demo.Module1.Module11.ModuleModule11)_modules_[1];
            demo_Module1_Module11.Initialize(this);
            if (modules.put(demo_Module1_Module11.getFullName(), demo_Module1_Module11) != null)
                throw new IllegalStateException("duplicate module name: demo_Module1_Module11");

            demo_M6 = (demo.M6.ModuleM6)_modules_[2];
            demo_M6.Initialize(this);
            if (modules.put(demo_M6.getFullName(), demo_M6) != null)
                throw new IllegalStateException("duplicate module name: demo_M6");

            demo_M6_M7 = (demo.M6.M7.ModuleM7)_modules_[3];
            demo_M6_M7.Initialize(this);
            if (modules.put(demo_M6_M7.getFullName(), demo_M6_M7) != null)
                throw new IllegalStateException("duplicate module name: demo_M6_M7");

            TaskTest_TaskExt = (TaskTest.TaskExt.ModuleTaskExt)_modules_[4];
            TaskTest_TaskExt.Initialize(this);
            if (modules.put(TaskTest_TaskExt.getFullName(), TaskTest_TaskExt) != null)
                throw new IllegalStateException("duplicate module name: TaskTest_TaskExt");

            Zeze.setSchemas(new demo.Schemas());
        } finally {
            unlock();
        }
    }

    public void destroyModules() throws Exception {
        lock();
        try {
            TaskTest_TaskExt = null;
            demo_M6_M7 = null;
            demo_M6 = null;
            demo_Module1_Module11 = null;
            demo_Module1 = null;
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
            demo_Module1.Start(this);
            demo_Module1_Module11.Start(this);
            demo_M6.Start(this);
            demo_M6_M7.Start(this);
            TaskTest_TaskExt.Start(this);
        } finally {
            unlock();
        }
    }

    @Override
    public void startLastModules() throws Exception {
        lock();
        try {
            demo_Module1.StartLast();
            demo_Module1_Module11.StartLast();
            demo_M6.StartLast();
            demo_M6_M7.StartLast();
            TaskTest_TaskExt.StartLast();
        } finally {
            unlock();
        }
    }

    public void stopModules() throws Exception {
        lock();
        try {
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
        } finally {
            unlock();
        }
    }

    public void stopBeforeModules() throws Exception {
        lock();
        try {
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
        } finally {
            unlock();
        }
    }

    public void startService() throws Exception {
        lock();
        try {
            TestServer.start();
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
