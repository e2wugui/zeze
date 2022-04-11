package UnitTest.Zeze.GlobalRaft;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Future;
import Zeze.Config;
import Zeze.Raft.RaftConfig;
import Zeze.Services.GlobalCacheManagerWithRaft;
import Zeze.Services.ServiceManagerServer;
import Zeze.Transaction.Log1;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.Task;
import demo.App;
import demo.Module1.Value;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TestGlobalCacheMgrWithRaft {
	private static final Logger logger = LogManager.getLogger(TestGlobalCacheMgrWithRaft.class);

	private static String configFileName = "global.raft.xml";

	public void Run(String[] args) throws InterruptedException {
		try {
			_Run(args);
		} catch (Throwable ex) {
			logger.error("Run", ex);
		}
		System.out.println("___________________________________________");
		System.out.println("___________________________________________");
		System.out.println("___________________________________________");
		System.out.println("Press [Ctrl+c] Enter To Exit.");
		System.out.println("___________________________________________");
		System.out.println("___________________________________________");
		System.out.println("___________________________________________");
		synchronized (Thread.currentThread()) {
			Thread.currentThread().wait();
		}
	}

	private void _Run(String[] args) throws Throwable {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-Config"))
				configFileName = args[++i];
		}

		logger.debug("Start.");

		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			try {
				logger.error("uncaught fatal exception for thread: " + t.getName(), e);
			} catch (Throwable ex) {
				ex.printStackTrace();
			} finally {
				e.printStackTrace();
			}
		});

		Task.tryInitThreadPool(null, null, null);

//		String ip = null;
//		int port = 5001;
//
//		InetAddress address = (ip != null && !ip.isBlank()) ? InetAddress.getByName(ip) : null;
//
//		var config = new Zeze.Config().AddCustomize(new ServiceManagerServer.Conf()).LoadAndParse();
//
//		var globalRaft1 = new TestGlobal("127.0.0.1:5556", configFileName);
//		var globalRaft2 = new TestGlobal("127.0.0.1:5557", configFileName);
//		var globalRaft3 = new TestGlobal("127.0.0.1:5558", configFileName);

		try {
//			new ServiceManagerServer(address, port, config);
//			globalRaft1.Start();
//			globalRaft2.Start();
//			globalRaft3.Start();
			System.out.println("Started GlobalCacheManagerWithRaft");

			TestApp();
		} catch (Throwable ex) {
			ex.printStackTrace();
		} finally {
//			globalRaft1.Stop();
//			globalRaft2.Stop();
//			globalRaft3.Stop();
		}


	}

	public static class PrintLog extends Log1<Value, Value> {
		private static volatile int lastInt = -1;
		private int oldInt;
		private int appId;
		private boolean eq = false;
		public PrintLog(Zeze.Transaction.Bean bean, demo.Module1.Value value, int appId) {
			super(bean, value);
			oldInt = getValue().getInt1();
			eq = lastInt == oldInt;
			this.appId = appId;
		}

		@Override
		public long getLogKey() {
			return this.getBean().getObjectId() + 100;
		}

		@Override
		public void Commit() {
			if (eq) {
				logger.debug("xxxeq " + oldInt + " " + appId);
			} else {
				//logger.debug("xxx " + oldInt + " " + appId);
			}

			lastInt = oldInt;
		}
	}

	public void TestApp() throws Throwable {
		App app1 = App.getInstance();
		App app2 = new App();

		Config config1 = Config.Load("zeze.xml");
		Config config2 = Config.Load("zeze2.xml");
		config2.setServerId(config1.getServerId() + 1);

		app1.Start(config1);
		app2.Start(config2);

		logger.info("app Started");

		try {
			long rst = app1.Zeze.NewProcedure(() -> {
				app1.demo_Module1.getTable1().remove(1234L);
				return Procedure.Success;
			}, "RemoveData").Call();
			assert rst == Procedure.Success;

			Future<?>[] task2 = new Future[2];

			int count = 100000;
			task2[0] = Zeze.Util.Task.run(() -> doConcurrency(app1, count, 1), "doConcurrency");
			task2[1] = Zeze.Util.Task.run(() -> doConcurrency(app2, count, 2), "doConcurrency");
			try {
				task2[0].get();
				task2[1].get();
			} catch (Exception e) {
				e.printStackTrace();
			}

			int allCount = count * 2;

			long result1 = app1.Zeze.NewProcedure(() -> {
				int last1 = app1.demo_Module1.getTable1().get(1234L).getInt1();
				System.out.println(String.format("last1 %d", last1));
				assert allCount == last1;
				return Procedure.Success;
			}, "CheckResult1").Call();
			assert Procedure.Success == result1;

			long result2 = app2.Zeze.NewProcedure(() -> {
				int last2 = app2.demo_Module1.getTable1().get(1234L).getInt1();
				System.out.println(String.format("last2 %d", last2));
				assert allCount == last2;
				return Procedure.Success;
			}, "CheckResult2").Call();
			assert Procedure.Success == result2;
		} finally {
			app1.Stop();
			app2.Stop();
		}
	}

	private void doConcurrency(App app, int count, int appId) {
		Future<?>[] tasks = new Future[count];
		for (int i = 0; i  < tasks.length; ++i) {
			tasks[i] = Zeze.Util.Task.run(app.Zeze.NewProcedure(() -> {
				Value val = app.demo_Module1.getTable1().getOrAdd(1234L);
				val.setInt1(val.getInt1() + 1);
				PrintLog log = new PrintLog(val, val, appId);
				Transaction.getCurrent().PutLog(log);
//				logger.info("appId={} value={}", appId, val.getInt1());
				System.out.println(String.format("appId %d value %d", appId, val.getInt1()));
				return Procedure.Success;
			}, "Procedure"), null, null);
		}
		for (int i = 0; i < tasks.length; ++i) {
			try {
				tasks[i].get();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static class TestGlobal {
		private Zeze.Services.GlobalCacheManagerWithRaft GlobalCacheManagerWithRaft;
		private final String RaftConfigFileName;
		private final String RaftName;

		public TestGlobal(String raftName, String configFileName) throws Throwable {
			RaftName = raftName;
			RaftConfigFileName = configFileName;
		}

		public void Start() throws Throwable {
			synchronized (this) {
				logger.debug("GlobalCacheManagerWithRaft {} Start ...", RaftName);
				GlobalCacheManagerWithRaft = new GlobalCacheManagerWithRaft(RaftName, RaftConfig.Load(RaftConfigFileName));
			}
		}

		public void Stop() throws IOException {
			synchronized (this) {
				logger.debug("GlobalCacheManagerWithRaft {} Stop ...", RaftName);
				if (GlobalCacheManagerWithRaft != null) {
					GlobalCacheManagerWithRaft.close();
					GlobalCacheManagerWithRaft = null;
				}
			}
		}
	}

	public static void main(String[] args) throws Throwable {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-Config"))
				configFileName = args[++i];
		}
		new TestGlobalCacheMgrWithRaft().Run(args);
	}

}
