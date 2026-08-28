package Benchmark;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import UnitTest.Zeze.BMyBean;
import Zeze.Util.Benchmark;
import Zeze.Util.Random;
import demo.App;
import harness.Bench;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * LinkedMap.get 吞吐 A/B 基准。
 *
 * 背景：clear 改为 O(1)（代际号 SerialNo 使旧映射失效）后，所有走索引的读操作
 * （get/getNodeId/getNodeById/remove/move）需要多读一行 root 验代际，从 2 行变 3 行。
 * 本基准度量 get 的事务级吞吐（每次 get 一个 procedure），观察回退幅度。
 *
 * 运行：gradlew :ZezeJavaTest:bench --tests "*BenchLinkedMapGet"
 */
@Bench
public class BenchLinkedMapGet {
	public static final int EntryCount = 3000; // 100个节点(nodeSize=30)，索引/节点行均进表缓存
	public static final int SingleThreadOps = 100_000;
	public static final int MultiThreadCount = 8;
	public static final int MultiThreadOpsPerThread = 20_000;

	@BeforeEach
	public void before() throws Exception {
		demo.App.getInstance().Start();
	}

	private void fill() throws Exception {
		App.Instance.Zeze.newProcedure(() -> {
			var map = App.Instance.LinkedMapModule.open("benchGet", BMyBean.class);
			map.clear();
			for (int i = 0; i < EntryCount; i++) {
				var bean = new BMyBean();
				bean.setI(i);
				map.put(String.valueOf(i), bean);
			}
			return 0;
		}, "bench.fill").call();
	}

	private void doGet(int ops, AtomicInteger counter) {
		var map = App.Instance.LinkedMapModule.open("benchGet", BMyBean.class);
		var random = Random.getInstance();
		for (int i = 0; i < ops; i++) {
			var key = String.valueOf(random.nextInt(EntryCount));
			try {
				App.Instance.Zeze.newProcedure(() -> {
					if (null == map.get(key))
						throw new IllegalStateException("benchGet missing " + key);
					counter.incrementAndGet();
					return 0;
				}, "bench.get").call();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Test
	public void benchGet() throws Exception {
		fill();

		// 单线程
		var counter = new AtomicInteger();
		var b1 = new Benchmark();
		doGet(SingleThreadOps, counter);
		b1.report("LinkedMapGet_SingleThread", counter.get());

		// 多线程
		fill();
		counter.set(0);
		var threads = new ArrayList<Thread>();
		for (int i = 0; i < MultiThreadCount; i++)
			threads.add(new Thread(() -> doGet(MultiThreadOpsPerThread, counter)));
		var b8 = new Benchmark();
		for (var t : threads)
			t.start();
		for (var t : threads)
			t.join();
		b8.report("LinkedMapGet_MultiThread8", counter.get());
	}
}
