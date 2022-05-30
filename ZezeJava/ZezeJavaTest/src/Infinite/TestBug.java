package Infinite;

import Zeze.Transaction.Transaction;
import Zeze.Util.FuncLong;
import Zeze.Util.Task;
import junit.framework.TestCase;
import org.junit.Ignore;

@Ignore
public class TestBug extends TestCase {
	static final App app = new App(1);

	static final class Task1 implements FuncLong {
		private final long k1, k2;
		private final int delta;

		Task1(long k1, long k2, int delta) {
			this.k1 = k1;
			this.k2 = k2;
			this.delta = delta;
		}

		@Override
		public long call() {
			var v1 = app.app.demo_Module1.getTflush().getOrAdd(k1);
			var v2 = app.app.demo_Module1.getTflush().getOrAdd(k2);
			System.out.println(delta + "(1): " + v1.getInt1() + ", " + v2.getInt1());
			System.out.println(delta + "(2): " + v1.getInt1() + ", " + v2.getInt1());
			v1.setInt1(v1.getInt1() - delta);
			v2.setInt1(v2.getInt1() + delta);
			System.out.println(delta + "(3): " + v1.getInt1() + ", " + v2.getInt1());
			//noinspection ConstantConditions
			Transaction.getCurrent().RunWhileCommit(() -> {
				System.out.println(delta + "(4): " + v1.getInt1() + ", " + v2.getInt1());
			});
			return 0L;
		}
	}

	public void test1() throws Throwable {
		System.out.println("begin");
		app.Start();
		app.app.Zeze.NewProcedure(() -> {
			app.app.demo_Module1.getTflush().remove(1L);
			app.app.demo_Module1.getTflush().remove(2L);
			return 0L;
		}, "prepare").Call();

		//Transaction.flag = false;
		var f1 = Task.run(app.app.Zeze.NewProcedure(new Task1(1, 2, 1000), "task1"));
		Thread.sleep(1000);
		//TableX.flag = false;
		var f2 = Task.run(app.app.Zeze.NewProcedure(new Task1(1, 2, 2000), "task2"));
		f1.get();
		f2.get();
		System.out.println("end");
	}

	public static void main(String[] args) throws Throwable {
		new TestBug().test1();
	}
}
