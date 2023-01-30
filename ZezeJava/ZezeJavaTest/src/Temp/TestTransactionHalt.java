package Temp;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;
import Zeze.Config;
import Zeze.Transaction.Transaction;
import Zeze.Util.OutLong;
import Zeze.Util.Task;
import demo.App;

public class TestTransactionHalt {
	private static final int KEY_COUNT = 100;
	private static final int PROC_CONC = 100;
	private static final int CHECKPOINT_PERIOD = 0;
	private static final LongAdder counter = new LongAdder();

	private static long add() {
		var rand = ThreadLocalRandom.current();
		var r1 = App.Instance.demo_Module1.getTable1().getOrAdd(rand.nextLong(KEY_COUNT));
		var r3 = App.Instance.demo_Module1.getTable3().getOrAdd(rand.nextLong(KEY_COUNT));
		r1.setLong2(r1.getLong2() + 1);
		r3.setLong2(r3.getLong2() + 1);
		counter.increment();
		Transaction.whileCommit(() -> Task.run(App.Instance.Zeze.newProcedure(TestTransactionHalt::add, "add")));
		return 0L;
	}

	public static void main(String[] args) throws Exception {
		var cfg = Config.load("zeze.xml");
		cfg.setCheckpointPeriod(CHECKPOINT_PERIOD);
		demo.App.getInstance().Start(cfg);

		var total1 = new OutLong();
		var total3 = new OutLong();
		for (int i = 0; i < KEY_COUNT; i++) {
			long k = i;
			App.Instance.Zeze.newProcedure(() -> {
				total1.value += App.Instance.demo_Module1.getTable1().getOrAdd(k).getLong2();
				total3.value += App.Instance.demo_Module1.getTable3().getOrAdd(k).getLong2();
				return 0L;
			}, "init").call();
		}
		if (total1.value != total3.value && args.length == 0)
			throw new AssertionError("check failed: " + total1.value + " != " + total3.value);

		for (int i = 0; i < KEY_COUNT; i++) {
			long k = i;
			App.Instance.Zeze.newProcedure(() -> {
				App.Instance.demo_Module1.getTable1().getOrAdd(k).setLong2(0);
				App.Instance.demo_Module1.getTable3().getOrAdd(k).setLong2(0);
				return 0L;
			}, "init").call();
		}

		while (!App.Instance.Zeze.getCheckpoint().debugOnlyRelativeRecordSetMap().isEmpty()) {
			Thread.sleep(10);
		}

		for (int i = 0; i < PROC_CONC; i++)
			Task.run(App.Instance.Zeze.newProcedure(TestTransactionHalt::add, "add"));

		Task.scheduleUnsafe(50, () -> {
			System.out.println("transactions: " + counter.sum());
			Runtime.getRuntime().halt(0);
		});

		Thread.sleep(Integer.MAX_VALUE);
	}
}
