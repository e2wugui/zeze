package Benchmark;

import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import org.junit.Test;

public class TaskOneByOne {
	public final static int TaskCount = 100_0000;
	@Test
	public void testBenchmark() throws InterruptedException {
		Task.tryInitThreadPool(null,null, null);
		var oo = new TaskOneByOneByKey();
		var b = new Zeze.Util.Benchmark();
		for (int i = 0; i < TaskCount; ++i)
			oo.Execute(1, () -> {});
		oo.Execute(1, () -> { synchronized (this) { this.notify(); }});
		synchronized (this) {
			this.wait();
		}
		b.report(this.getClass().getName(), TaskCount);
	}
}
