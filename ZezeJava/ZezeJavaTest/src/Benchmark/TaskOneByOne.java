package Benchmark;

import java.util.concurrent.atomic.AtomicLong;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import org.junit.Test;

public class TaskOneByOne {
	public final static int TaskCount = 5000_0000;

	private final AtomicLong counter = new AtomicLong();
	@Test
	public void testBenchmark() throws InterruptedException {
		Task.tryInitThreadPool(null,null, null);
		var oo = new TaskOneByOneByKey();
		var b = new Zeze.Util.Benchmark();
		for (int i = 0; i < TaskCount; ++i)
			oo.Execute(1, () -> { counter.incrementAndGet(); });
		oo.Execute(1, () -> { synchronized (this) { counter.incrementAndGet(); this.notify(); }});
		synchronized (this) {
			this.wait();
		}
		b.report(this.getClass().getName(), TaskCount);
		System.out.println(counter.get());
	}
}
