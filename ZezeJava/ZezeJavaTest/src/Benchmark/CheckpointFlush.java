package Benchmark;

import Zeze.Config;
import Zeze.Transaction.CheckpointFlushMode;
import demo.App;
import org.junit.Test;

public class CheckpointFlush {
	@Test
	public void benchFlush() throws Exception {
		{
			var cfg = Config.load("zeze.xml");
			cfg.setCheckpointPeriod(Integer.MAX_VALUE); // disable auto flush
			App.Instance.Start(cfg);
		}
		try {
			flush(CheckpointFlushMode.SingleThread);
		} finally {
			App.Instance.Stop();
		}

		{
			var cfg = Config.load("zeze.xml");
			cfg.setCheckpointPeriod(Integer.MAX_VALUE); // disable auto flush
			App.Instance.Start(cfg);
		}
		try {
			flush(CheckpointFlushMode.MultiThread);
		} finally {
			App.Instance.Stop();
		}

		{
			var cfg = Config.load("zeze.xml");
			cfg.setCheckpointPeriod(Integer.MAX_VALUE); // disable auto flush
			App.Instance.Start(cfg);
		}
		try {
			flush(CheckpointFlushMode.SingleThreadMerge);
		} finally {
			App.Instance.Stop();
		}

		{
			var cfg = Config.load("zeze.xml");
			cfg.setCheckpointPeriod(Integer.MAX_VALUE); // disable auto flush
			App.Instance.Start(cfg);
		}
		try {
			flush(CheckpointFlushMode.MultiThreadMerge);
		} finally {
			App.Instance.Stop();
		}
	}

	private void flush(CheckpointFlushMode mode) {
		App.Instance.Zeze.getConfig().setCheckpointFlushMode(mode);
		var count = 10_0000;
		for (int i = 0; i < count; ++i) {
			long key = i;
			App.Instance.Zeze.newProcedure(() -> {
				App.Instance.demo_Module1.getTable1().getOrAdd(key).setLong2(key);
				return 0;
			}, "modify").call();
		}
		var b = new Zeze.Util.Benchmark();
		App.Instance.Zeze.checkpointRun();
		b.report("flush " + mode, count);
	}
}
