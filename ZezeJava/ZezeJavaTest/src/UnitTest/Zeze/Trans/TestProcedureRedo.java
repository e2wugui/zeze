package UnitTest.Zeze.Trans;

import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.OutLong;
import Zeze.Util.Task;
import demo.App;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by zyao on 2021/11/24 11:15
 */
public class TestProcedureRedo {

	@Before
	public final void testInit() throws Exception {
		App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Exception {
		//App.getInstance().Stop();
	}

	private static int counter = 0;

	@SuppressWarnings("DataFlowIssue")
	@Test
	public final void testProcedureRedo() throws Exception {
		App.getInstance().Zeze.newProcedure(() -> {

			var v = App.getInstance().demo_Module1.getTable1().getOrAdd(6785L);

			v.setLong2(1L);
			v.getMap15().put(1L, 100L);
			return Procedure.Success;

		}, "TestProcedureRedoFirst").call();

		var outLong2 = new OutLong();
		var ftask1 = Task.runUnsafe(App.getInstance().Zeze.newProcedure(() -> {

			counter += 1;
			System.out.println("task1 counter " + counter);
			var v = App.getInstance().demo_Module1.getTable1().getOrAdd(6785L);
			long vLong = v.getLong2();

			Thread.sleep(1000);
			outLong2.value = v.getMap15().get(vLong);

			Transaction.whileCommit(() -> {
				System.out.println("value=" + outLong2.value);
				System.out.println("task1 suss");
			});
			return Procedure.Success;

		}, "TestProcedureRedoTask1"), DispatchMode.Normal);

		var ftask2 = Task.runUnsafe(App.getInstance().Zeze.newProcedure(() -> {

			Thread.sleep(100);
			var v = App.getInstance().demo_Module1.getTable1().getOrAdd(6785L);
			long vInt = v.getLong2();

			long vlong = v.getMap15().get(vInt);
			v.getMap15().remove(vlong);
			v.setLong2(2L);
			v.getMap15().put(2L, 200L);

			Transaction.whileCommit(() -> System.out.println("task2 suss"));
			return Procedure.Success;

		}, "TestProcedureRedoTask2"), DispatchMode.Normal);

		ftask2.get();
		ftask1.get();
		Assert.assertEquals(200, outLong2.value);
	}
}
