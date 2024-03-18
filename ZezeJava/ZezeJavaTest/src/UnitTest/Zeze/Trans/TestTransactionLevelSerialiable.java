package UnitTest.Zeze.Trans;

import java.util.concurrent.Future;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Transaction;
import demo.App;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestTransactionLevelSerialiable {
	@Before
	public final void testInit() throws Exception {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Exception {
		//demo.App.getInstance().Stop();
	}

	private volatile boolean InTest = true;

	@Test
	public final void Test2() throws Exception {
		App.Instance.Zeze.newProcedure(TestTransactionLevelSerialiable::init, "test_init").call();
		Zeze.Util.Task.run(this::verify_task, "verify_task", DispatchMode.Normal);
		try {
			Future<?>[] tasks = new Future[20000];
			for (int i = 0; i < tasks.length; ++i) {
				tasks[i] = Zeze.Util.Task.runUnsafe(
						App.Instance.Zeze.newProcedure(TestTransactionLevelSerialiable::trade, "test_trade"),
						DispatchMode.Normal);
			}
			Zeze.Util.Task.waitAll(tasks);
		} finally {
			InTest = false;
		}
	}

	private void verify_task() throws Exception {
		while (InTest) {
			App.Instance.Zeze.newProcedure(TestTransactionLevelSerialiable::verify, "test_verify").call();
		}
	}

	private static long verify() {
		var v1 = App.Instance.demo_Module1.getTable1().getOrAdd(1L);
		var v2 = App.Instance.demo_Module1.getTable1().getOrAdd(2L);
		final var total = v1.getInt_1() + v2.getInt_1();
		// 必须在事务成功时verify，执行过程中是可能失败的。
		Transaction.whileCommit(() -> Assert.assertEquals(total, 100_000));
		return 0;
	}

	private static long init() {
		var v1 = App.Instance.demo_Module1.getTable1().getOrAdd(1L);
		var v2 = App.Instance.demo_Module1.getTable1().getOrAdd(2L);
		v1.setInt_1(100_000);
		v2.setInt_1(0);
		return 0;
	}

	private static long trade() {
		var v1 = App.Instance.demo_Module1.getTable1().getOrAdd(1L);
		var v2 = App.Instance.demo_Module1.getTable1().getOrAdd(2L);
		var money = Zeze.Util.Random.getInstance().nextInt(1000);
		if (Zeze.Util.Random.getInstance().nextBoolean()) {
			// random swap
			var tmp = v1;
			v1 = v2;
			v2 = tmp;
		}
		v1.setInt_1(v1.getInt_1() - money);
		v2.setInt_1(v2.getInt_1() + money);
		return 0;
	}
}
