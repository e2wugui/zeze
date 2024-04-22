package UnitTest.Zeze.Trans;

import java.util.Objects;
import java.util.concurrent.Future;
import Zeze.Config;
import Zeze.Services.ServiceManager.EditService;
import Zeze.Transaction.DispatchMode;
import demo.Module1.BValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Log1;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import junit.framework.TestCase;
import org.junit.Assert;

public class TestGlobal extends TestCase {
	public static class PrintLog extends Log1<BValue, BValue> {
		private static final Logger logger = LogManager.getLogger(TestGlobal.class);

		private static volatile int lastInt = -1;
		private final int oldInt;
		private final int appId;
		private final boolean eq;

		public PrintLog(Bean bean, BValue value, int appId) {
			super(bean, 0, value);
			assert getValue() != null;
			oldInt = getValue().getInt_1();
			eq = lastInt == oldInt;
			this.appId = appId;
		}

		@Override
		public int getTypeId() {
			return 0; // 现在Log1仅用于特殊目的，不支持相关日志系列化。
		}

		@Override
		public long getLogKey() {
			return this.getBean().objectId() + 100;
		}

		@Override
		public void commit() {
			if (eq) {
				logger.debug("xxxeq {} {}", oldInt, appId);
			} else {
				//logger.debug("xxx {} {}", oldInt, appId);
			}

			lastInt = oldInt;
		}
	}

	public final void testNone() {
		var rname = EditService.class.getTypeName();
		System.out.println(rname);
		var x = Zeze.Transaction.Bean.hash32(rname);
		System.out.println(x);
		var i = x & 0xffff;
		System.out.println(i);
	}

	public final void test2App() throws Exception {
		demo.App app1 = new demo.App();
		demo.App app2 = new demo.App();
		var config1 = Config.load("zeze.xml");
		config1.setServerId(1);
		var config2 = Config.load("zeze.xml");
		config2.setServerId(2);
		var commitService = config2.getServiceConf("Zeze.Dbh2.Commit");
		if (commitService != null)
			commitService.forEachAcceptor(a -> a.setPort(a.getPort() + config2.getServerId()));
		config1.getServiceConfMap().remove("TestServer");
		config2.getServiceConfMap().remove("TestServer");
		config1.getServiceConfMap().remove("Zeze.Onz.Server");
		config2.getServiceConfMap().remove("Zeze.Onz.Server");

		app1.Start(config1);
		app2.Start(config2);
		try {
			Assert.assertEquals(Procedure.Success, app1.Zeze.newProcedure(() -> {
				app1.demo_Module1.getTable1().getOrAdd(6785L);
				return Procedure.Success;
			}, "RemoveClean").call());
			Assert.assertEquals(Procedure.Success, app2.Zeze.newProcedure(() -> {
				app2.demo_Module1.getTable1().getOrAdd(6785L);
				return Procedure.Success;
			}, "RemoveClean").call());

			// 只删除一个app里面的记录就够了。
			Assert.assertEquals(Procedure.Success, app1.Zeze.newProcedure(() -> {
				app1.demo_Module1.getTable1().remove(6785L);
				return Procedure.Success;
			}, "RemoveClean").call());

			@SuppressWarnings("unchecked")
			Future<Long>[] task2 = new Future[2];
			int count = 2000;
			task2[0] = Zeze.Util.Task.runUnsafe(() -> (long)ConcurrentAdd(app1, count, 1), null);
			task2[1] = Zeze.Util.Task.runUnsafe(() -> (long)ConcurrentAdd(app2, count, 2), null);
			long success0 = 0;
			long success1 = 0;
			try {
				success0 = task2[0].get();
				success1 = task2[1].get();
			} catch (Exception e) {
				e.printStackTrace();
			}
			long countall = success0 + success1;
			if (countall != count * 2)
				Thread.sleep(5000); // wait for globalForbidPeriod

			Assert.assertEquals(Procedure.Success, app1.Zeze.newProcedure(() -> {
				int last1 = Objects.requireNonNull(app1.demo_Module1.getTable1().get(6785L)).getInt_1();
				System.out.println("app1 " + last1);
				Assert.assertEquals(countall, last1);
				return Procedure.Success;
			}, "CheckResult1").call());
			Assert.assertEquals(Procedure.Success, app2.Zeze.newProcedure(() -> {
				int last2 = Objects.requireNonNull(app2.demo_Module1.getTable1().get(6785L)).getInt_1();
				System.out.println("app2 " + last2);
				Assert.assertEquals(countall, last2);
				return Procedure.Success;
			}, "CheckResult2").call());
		} finally {
			//app1.Stop();
			//app2.Stop();
		}
	}

	private static int ConcurrentAdd(demo.App app, int count, int appId) {
		@SuppressWarnings("unchecked")
		Future<Long>[] tasks = new Future[count];
		for (int i = 0; i < tasks.length; ++i) {
			tasks[i] = Zeze.Util.Task.runUnsafe(app.Zeze.newProcedure(() -> {
				BValue b = app.demo_Module1.getTable1().getOrAdd(6785L);
				b.setInt_1(b.getInt_1() + 1);
				PrintLog log = new PrintLog(b, b, appId);
				//noinspection DataFlowIssue
				Transaction.getCurrent().putLog(log);
				return Procedure.Success;
			}, "ConcurrentAdd" + appId), DispatchMode.Normal);
		}
		int success = 0;
		for (Future<Long> task : tasks) {
			try {
				var r = task.get();
				if (r == Procedure.Success)
					success++;
				else
					Assert.assertEquals(Procedure.AbortException, r.longValue());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return success;
	}
}
