package UnitTest.Zeze.Trans;

import Zeze.Transaction.*;
import UnitTest.*;

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestClass] public class TestGlobal
public class TestGlobal {
	public static class PrintLog extends Log<demo.Module1.Value, demo.Module1.Value> {
		private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

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
			return this.Bean.ObjectId + 100;
		}

		@Override
		public void Commit() {
			if (eq) {
				logger.Debug("xxxeq " + oldInt + " " + appId);
			}
			else {
				//logger.Debug("xxx " + oldInt + " " + appId);
			}

			lastInt = oldInt;
		}
	}

	//[TestMethod]
	public final void Test2AppSameLocalId() {
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

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestMethod] public void Test2App()
	public final void Test2App() {
		demo.App app1 = demo.App.getInstance();
		demo.App app2 = new demo.App();
		var config1 = Zeze.Config.Load("zeze.xml");
		var config2 = Zeze.Config.Load("zeze.xml");
		config2.ServerId = config1.ServerId + 1;

		app1.Start(config1);
		app2.Start(config2);
		try {
			// 只删除一个app里面的记录就够了。
			assert Procedure.Success == app1.getZeze().NewProcedure(() -> {
					app1.getDemoModule1().getTable1().Remove(6785);
					return Procedure.Success;
			}, "RemoveClean", null).Call();

			Task[] task2 = new Task[2];
			int count = 2000;
			task2[0] = Zeze.Util.Task.Run(() -> ConcurrentAdd(app1, count, 1), "TestGlobal.ConcurrentAdd1");
			task2[1] = Zeze.Util.Task.Run(() -> ConcurrentAdd(app2, count, 2), "TestGlobal.ConcurrentAdd2");
			Task.WaitAll(task2);
			int countall = count * 2;
			assert Procedure.Success == app1.getZeze().NewProcedure(() -> {
					int last1 = app1.getDemoModule1().getTable1().Get(6785).getInt1();
					assert countall == last1;
					//Console.WriteLine("app1 " + last1);
					return Procedure.Success;
			}, "CheckResult1", null).Call();
			assert Procedure.Success == app2.getZeze().NewProcedure(() -> {
					int last2 = app2.getDemoModule1().getTable1().Get(6785).getInt1();
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
			tasks[i] = Zeze.Util.Task.Run(app.getZeze().NewProcedure(()-> {
					demo.Module1.Value b = app.getDemoModule1().getTable1().GetOrAdd(6785);
					b.Int1 += 1;
					PrintLog log = new PrintLog(b, b, appId);
					Transaction.Current.PutLog(log);
					return Procedure.Success;
			}, "ConcurrentAdd" + appId, null), null, null);
		}
		Task.WaitAll(tasks);
	}
}