package Benchmark;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

// 派发路径微基准：拆解 submitNow 全路径的每任务成本构成。
// 三个分量对比：full（TaskSpec.ofAction().submitNow().get()）vs raw（同线程数裸池 submit().get()）
// vs direct（载荷直调）。full - raw = TaskSpec/Task 层开销（timeout/hotGuard/统计/日志），
// raw - direct = 执行器交接成本（lambda/FutureTask/队列）。
// 线程池形态对齐 Task 默认（固定数量虚拟线程）。
@State(Scope.Benchmark)
@org.openjdk.jmh.annotations.BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class DispatchBench {
	private ExecutorService rawPool;
	private Runnable noop = () -> {};

	@Setup(Level.Trial)
	public void setup() {
		Task.tryInitThreadPool();
		// 对齐 Task.newFixedThreadPool 的虚拟线程工厂（JMH JVM 非 JUnit，固定数量虚拟线程）
		rawPool = Executors.newFixedThreadPool(
				Runtime.getRuntime().availableProcessors() * 30,
				new Zeze.Util.ThreadFactoryWithName("JmhDispatchRaw"));
	}

	@TearDown(Level.Trial)
	public void tearDown() {
		rawPool.shutdown();
	}

	@Benchmark
	public void fullSubmitNow(Blackhole bh) throws Exception {
		var f = TaskSpec.ofAction(() -> {}).name("noop").submitNow();
		bh.consume(f.get());
	}

	@Benchmark
	public void rawSubmit(Blackhole bh) throws Exception {
		Future<?> f = rawPool.submit(noop);
		bh.consume(f.get());
	}

	@Benchmark
	public void directCall(Blackhole bh) {
		noop.run();
		bh.consume(this);
	}
}
