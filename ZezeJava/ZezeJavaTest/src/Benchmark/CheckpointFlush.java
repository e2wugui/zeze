package Benchmark;

import java.util.ArrayList;
import java.util.concurrent.Future;
import Zeze.Config;
import Zeze.Transaction.CheckpointFlushMode;
import Zeze.Util.PerfCounter;
import Zeze.Util.Task;
import demo.App;
import org.junit.Ignore;
import org.junit.Test;

public class CheckpointFlush {
	@Test
	public void benchFlushSingleThread() throws Exception {
		bench(CheckpointFlushMode.SingleThread);
		System.out.println(PerfCounter.instance.getLogAndReset());
	}

	@Test
	public void benchFlushMultiThread() throws Exception {
		bench(CheckpointFlushMode.MultiThread);
		System.out.println(PerfCounter.instance.getLogAndReset());
	}

	@Test
	public void benchFlushSingleMerge() throws Exception {
		bench(CheckpointFlushMode.SingleThreadMerge);
		System.out.println(PerfCounter.instance.getLogAndReset());
	}

	@Test
	public void benchFlushMultiMerge() throws Exception {
		bench(CheckpointFlushMode.MultiThreadMerge);
		System.out.println(PerfCounter.instance.getLogAndReset());
	}

	private static void bench(CheckpointFlushMode mode) throws Exception {
		var cfg = Config.load("zeze.xml");
		cfg.setCheckpointPeriod(Integer.MAX_VALUE); // disable auto flush
		cfg.setCheckpointFlushMode(mode);
		App.Instance.Start(cfg);
		try {
			var count = 2_0000;
			var futures = new ArrayList<Future<?>>();
			for (int i = 0; i < count; ++i) {
				long key = i;
				futures.add(Task.runUnsafe(App.Instance.Zeze.newProcedure(() -> {
					App.Instance.demo_Module1.getTable1().getOrAdd(key).setLong2(key);
					return 0;
				}, "modify")));
			}
			for (var future : futures)
				future.get();
			var b = new Zeze.Util.Benchmark();
			App.Instance.Zeze.checkpointRun();
			var name = "flush " + mode;
			b.report(name, count);
		} finally {
			//App.Instance.Stop();
		}
	}
}
