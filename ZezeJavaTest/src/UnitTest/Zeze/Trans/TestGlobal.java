package UnitTest.Zeze.Trans;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Log1;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.Task;
import junit.framework.TestCase;

public class TestGlobal extends TestCase{
	public static class PrintLog extends Log1<demo.Module1.Value, demo.Module1.Value> {
		private static final Logger logger = LogManager.getLogger(TestGlobal.class);

		private static volatile int lastInt = -1;
		private int oldInt;
		private int appId;
		private boolean eq = false;
		public PrintLog(Bean bean, demo.Module1.Value value, int appId) {
			super(bean, value);
			int last = lastInt;
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
			}else {
				//logger.debug("xxx " + oldInt + " " + appId);
			}

			lastInt = oldInt;
		}
	}

	//[TestMethod]
	public final void test2AppSameLocalId() {
		demo.App app1 = demo.App.getInstance();
		demo.App app2 = new demo.App();
		var config1 = Zeze.Config.Load("zeze.xml");
		var config2 = Zeze.Config.Load("zeze.xml");
		try {
			app1.Start(config1);
			app2.Start(config2);
		}
		finally {
			app1.Stop();
			app2.Stop();
		}
	}

	public final void test2App() {
		demo.App app1 = demo.App.getInstance();
		demo.App app2 = new demo.App();
		var config1 = Zeze.Config.Load("zeze.xml");
		var config2 = Zeze.Config.Load("zeze.xml");
		config2.setServerId( config1.getServerId() + 1);

		app1.Start(config1);
		app2.Start(config2);
		try {
			// 只删除一个app里面的记录就够了。
			assert Procedure.Success == app1.Zeze.NewProcedure(() -> {
					app1.demo_Module1.getTable1().remove(6785L);
					return Procedure.Success;
			}, "RemoveClean", null).Call();

			Zeze.Util.Task[] task2 = new Task[2];
			int count = 2000;
			task2[0] = Zeze.Util.Task.Run(() -> ConcurrentAdd(app1, count, 1), "TestGlobal.ConcurrentAdd1");
			task2[1] = Zeze.Util.Task.Run(() -> ConcurrentAdd(app2, count, 2), "TestGlobal.ConcurrentAdd2");
			try {
				task2[0].get();
				task2[1].get();
			} catch (Exception e) {
				e.printStackTrace();
			}
			int countall = count * 2;
			assert Procedure.Success == app1.Zeze.NewProcedure(() -> {
					int last1 = app1.demo_Module1.getTable1().get(6785L).getInt1();
					assert countall == last1;
					//Console.WriteLine("app1 " + last1);
					return Procedure.Success;
			}, "CheckResult1", null).Call();
			assert Procedure.Success == app2.Zeze.NewProcedure(() -> {
					int last2 = app2.demo_Module1.getTable1().get(6785L).getInt1();
					assert countall == last2;
					//Console.WriteLine("app1 " + last2);
					return Procedure.Success;
			}, "CheckResult2", null).Call();
		}
		finally {
			app1.Stop();
			app2.Stop();
		}
	}

	private void ConcurrentAdd(demo.App app, int count, int appId) {
		Task[] tasks = new Task[count];
		for (int i = 0; i < tasks.length; ++i) {
			tasks[i] = Zeze.Util.Task.Run(app.Zeze.NewProcedure(()-> {
					demo.Module1.Value b = app.demo_Module1.getTable1().getOrAdd(6785l);
					b.setInt1(b.getInt1()+1);
					PrintLog log = new PrintLog(b, b, appId);
					Transaction.getCurrent().PutLog(log);
					return Procedure.Success;
			}, "ConcurrentAdd" + appId, null), null, null);
		}
		for (int i = 0; i < tasks.length; ++i) {
			try {
				tasks[i].get();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}