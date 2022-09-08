package demo;

import Benchmark.ABasicSimpleAddOneThread;
import Benchmark.CBasicSimpleAddConcurrent;
import Zeze.Collections.LinkedMap;
import Zeze.Config;
import Zeze.Game.Bag;

public class App extends Zeze.AppBase {
	public static void main(String[] args) throws Throwable {
		System.err.println(System.getProperties().get("user.dir"));
		Instance.Start();
		int i = 0;
		if (args.length == 0) {
			//noinspection InfiniteLoopStatement
			while (true) {
				//noinspection BusyWait
				Thread.sleep(1000);
				var result = Instance.Zeze.NewProcedure(() ->
				{
					Instance.demo_Module1.getTable1().get(1L);
					return 0L;
				}, "Global Access").Call();
				++i;
				System.err.println("" + i + "-" + result);
			}
		}
		Instance.Stop();
	}

	public static final App Instance = new App();

	public static App getInstance() {
		return Instance;
	}

	public LinkedMap.Module LinkedMapModule;
	public Bag.Module BagModule;

	public void Start() throws Throwable {
		Start(Config.Load("./zeze.xml"));
	}

	private static void adjustTableConf(Config.TableConf conf) {
		if (null != conf) {
			if (conf.getRealCacheCapacity() < ABasicSimpleAddOneThread.AddCount) {
				conf.setCacheCapacity(ABasicSimpleAddOneThread.AddCount);
				conf.setCacheFactor(1.0f);
			}
			if (conf.getCacheConcurrencyLevel() < CBasicSimpleAddConcurrent.ConcurrentLevel)
				conf.setCacheConcurrencyLevel(CBasicSimpleAddConcurrent.ConcurrentLevel);
		}
	}

	public void Start(Config config) throws Throwable {
		System.setProperty("log4j.configurationFile", "log4j2.xml");
		// 测试本地事务性能需要容量大一点
		adjustTableConf(config.getDefaultTableConf());
		adjustTableConf(config.getTableConfMap().get("demo_Module1_Table1"));

		CreateZeze(config);
		CreateService();
		CreateModules();
		LinkedMapModule = new LinkedMap.Module(Zeze);
		BagModule = new Bag.Module(Zeze);
		Zeze.Start(); // 启动数据库
		StartModules(); // 启动模块，装载配置什么的。
		StartService(); // 启动网络
	}

	public void Stop() throws Throwable {
		StopService(); // 关闭网络
		StopModules(); // 关闭模块，卸载配置什么的。
		if (Zeze != null) {
			Zeze.Stop(); // 关闭数据库
			if (LinkedMapModule != null) {
				LinkedMapModule.UnRegisterZezeTables(Zeze);
				LinkedMapModule = null;
			}
			if (BagModule != null) {
				BagModule.UnRegisterZezeTables(Zeze);
				BagModule = null;
			}
		}
		DestroyModules();
		DestroyServices();
		DestroyZeze();
	}

	// ZEZE_FILE_CHUNK {{{ GEN APP @formatter:off
    public Zeze.Application Zeze;
    public final java.util.HashMap<String, Zeze.IModule> Modules = new java.util.HashMap<>();

    public demo.Server Server;

    public demo.Module1.ModuleModule1 demo_Module1;
    public demo.Module1.Module11.ModuleModule11 demo_Module1_Module11;
    public demo.M6.ModuleM6 demo_M6;
    public demo.M6.M7.ModuleM7 demo_M6_M7;

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

        Zeze = new Zeze.Application("demo", config);
    }

    public synchronized void CreateService() throws Throwable {
        Server = new demo.Server(Zeze);
    }

    public synchronized void CreateModules() {
        demo_Module1 = ReplaceModuleInstance(new demo.Module1.ModuleModule1(this));
        demo_Module1.Initialize(this);
        if (Modules.put(demo_Module1.getFullName(), demo_Module1) != null)
            throw new RuntimeException("duplicate module name: demo_Module1");

        demo_Module1_Module11 = ReplaceModuleInstance(new demo.Module1.Module11.ModuleModule11(this));
        demo_Module1_Module11.Initialize(this);
        if (Modules.put(demo_Module1_Module11.getFullName(), demo_Module1_Module11) != null)
            throw new RuntimeException("duplicate module name: demo_Module1_Module11");

        demo_M6 = ReplaceModuleInstance(new demo.M6.ModuleM6(this));
        demo_M6.Initialize(this);
        if (Modules.put(demo_M6.getFullName(), demo_M6) != null)
            throw new RuntimeException("duplicate module name: demo_M6");

        demo_M6_M7 = ReplaceModuleInstance(new demo.M6.M7.ModuleM7(this));
        demo_M6_M7.Initialize(this);
        if (Modules.put(demo_M6_M7.getFullName(), demo_M6_M7) != null)
            throw new RuntimeException("duplicate module name: demo_M6_M7");

        Zeze.setSchemas(new demo.Schemas());
    }

    public synchronized void DestroyModules() {
        demo_M6_M7 = null;
        demo_M6 = null;
        demo_Module1_Module11 = null;
        demo_Module1 = null;
        Modules.clear();
    }

    public synchronized void DestroyServices() {
        Server = null;
    }

    public synchronized void DestroyZeze() {
        Zeze = null;
    }

    public synchronized void StartModules() throws Throwable {
        demo_Module1.Start(this);
        demo_Module1_Module11.Start(this);
        demo_M6.Start(this);
        demo_M6_M7.Start(this);
    }

    public synchronized void StopModules() throws Throwable {
        if (demo_M6_M7 != null)
            demo_M6_M7.Stop(this);
        if (demo_M6 != null)
            demo_M6.Stop(this);
        if (demo_Module1_Module11 != null)
            demo_Module1_Module11.Stop(this);
        if (demo_Module1 != null)
            demo_Module1.Stop(this);
    }

    public synchronized void StartService() throws Throwable {
        Server.Start();
    }

    public synchronized void StopService() throws Throwable {
        if (Server != null)
            Server.Stop();
    }
	// ZEZE_FILE_CHUNK }}} GEN APP @formatter:on
}
