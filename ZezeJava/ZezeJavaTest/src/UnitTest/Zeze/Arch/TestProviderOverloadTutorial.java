package UnitTest.Zeze.Arch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import Zeze.Arch.ProviderOverload;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Config;
import Zeze.Util.ThreadFactoryWithName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import harness.Fast;

/**
 * Arch.ProviderOverload 的单元测试（不组网）。
 * 探针周期为 1~2 秒随机抖动，测试用轮询等待状态迁移而不是固定 sleep，避免抖动导致的脆弱。
 * 阈值设为 50/100ms：阻塞场景（秒级排队）稳定判 eOverload，空闲场景（亚毫秒）稳定判 eWorkFine，
 * 中间档 eThreshold 依赖精确计时，不在此覆盖。
 */
@Fast
public class TestProviderOverloadTutorial {

	private static Config tinyThresholdConfig() {
		var config = new Config();
		config.setProviderThreshold(50);
		config.setProviderOverload(100);
		return config;
	}

	@Test
	public void testRegisterDedupAndClose() {
		var po = new ProviderOverload();
		ExecutorService pool = Executors.newSingleThreadExecutor(new ThreadFactoryWithName("TestOverloadDedup"));
		try {
			Assertions.assertTrue(po.register(pool, tinyThresholdConfig()));
			Assertions.assertFalse(po.register(pool, tinyThresholdConfig())); // 同一池重复注册：幂等拒绝

			po.close();
			// close 摘除全部探针后，无池可看，聚合结果回到 eWorkFine（确定性，无在途探针参与）
			Assertions.assertEquals(BLoad.eWorkFine, po.getOverload());

			// close 后空槽可复用：再次注册同一池成功，探针重新点亮
			Assertions.assertTrue(po.register(pool, tinyThresholdConfig()));
		} finally {
			po.close();
			pool.shutdownNow();
		}
	}

	@Test
	public void testProbeDetectsOverloadAndRecovers() throws Exception {
		var po = new ProviderOverload();
		ExecutorService pool = Executors.newSingleThreadExecutor(new ThreadFactoryWithName("TestOverloadProbe"));
		try {
			Assertions.assertTrue(po.register(pool, tinyThresholdConfig()));

			// 用一个长任务占死唯一的线程：探针入队后无法执行，
			// detecting() 在 overload 字段高位打包的入队时刻与当前时刻的差持续增大，
			// overload() 的在途折叠实时算出 eOverload（不必等探针真正执行）。
			var gate = new CountDownLatch(1);
			pool.execute(() -> {
				try {
					gate.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});

			// 首个探针在注册后 1~2 秒（抖动）才打包时刻，4 秒上限足够覆盖
			Assertions.assertTrue(awaitOverload(po, BLoad.eOverload, 4),
					"阻塞的线程池应被发现为 eOverload");

			// 放行：本周期探针测得秒级延迟记下 eOverload，下一周期（再 1~2 秒）探针测得亚毫秒
			// 延迟，状态恢复 eWorkFine；6 秒上限覆盖两轮抖动
			gate.countDown();
			Assertions.assertTrue(awaitOverload(po, BLoad.eWorkFine, 6),
					"空闲后的线程池应恢复 eWorkFine");
		} finally {
			po.close();
			pool.shutdownNow();
		}
	}

	private static boolean awaitOverload(ProviderOverload po, int expected, int timeoutSeconds)
			throws InterruptedException {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
		while (System.nanoTime() < deadline) {
			if (po.getOverload() == expected)
				return true;
			TimeUnit.MILLISECONDS.sleep(50);
		}
		return po.getOverload() == expected;
	}
}
