package Zeze.Arch;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Config;
import Zeze.Util.Random;
import Zeze.Util.ThreadFactoryWithName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ProviderOverload extends ReentrantLock {
	private static final Logger logger = LogManager.getLogger(ProviderOverload.class);
	private static final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
			new ThreadFactoryWithName("ZezeLoadThread", Thread.MAX_PRIORITY));

	private final ArrayList<ThreadPoolMonitor> threadPools = new ArrayList<>();

	public boolean register(@NotNull ExecutorService threadPool, @NotNull Config config) {
		lock();
		try {
			for (var tp : threadPools)
				if (tp.threadPool == threadPool)
					return false;
			threadPools.add(new ThreadPoolMonitor(threadPool, config));
			return true;
		} finally {
			unlock();
		}
	}

	// 由LoadReporter读取报告。
	public int getOverload() {
		var result = BLoad.eWorkFine;
		//noinspection ForLoopReplaceableByForEach
		for (int i = 0, n = threadPools.size(); i < n; i++) {
			var overload = threadPools.get(i).overload();
			if (overload == BLoad.eOverload)
				return overload;
			if (overload == BLoad.eThreshold)
				result = BLoad.eThreshold;
		}
		return result;
	}

	static final class ThreadPoolMonitor extends ReentrantLock {
		private final @NotNull ExecutorService threadPool;
		private final @NotNull Config config;
		private volatile long overload = BLoad.eWorkFine;

		public ThreadPoolMonitor(@NotNull ExecutorService threadPool, @NotNull Config config) {
			this.threadPool = threadPool;
			this.config = config;
			startDetectDelay();
		}

		private void startDetectDelay() {
			lock();
			try {
				scheduledExecutorService.schedule(
						this::detecting, Random.getInstance().nextInt(1000) + 1000, TimeUnit.MILLISECONDS);
			} finally {
				unlock();
			}
		}

		private int calcOverload(long elapse) {
			if (elapse < config.getProviderThreshold())
				return BLoad.eWorkFine;
			if (elapse < config.getProviderOverload())
				return BLoad.eThreshold;
			return BLoad.eOverload;
		}

		@SuppressWarnings("NonAtomicOperationOnVolatileField")
		private void detecting() {
			overload = (overload & 3) | (System.nanoTime() & ~3L); // 保留低2位保存的上次负载状态

			// todo 虚拟线程需要想其他办法检测。比如还是回到任务数量上：同时执行的任务超过多少。
			threadPool.execute(() -> {
				var elapse = (System.nanoTime() - overload) / 1_000_000;
				var o = calcOverload(elapse);
				overload = o;
				if (o != BLoad.eWorkFine)
					logger.warn("detect overload={} elapse={}ms", o, elapse);
				startDetectDelay();
			});
		}

		public int overload() {
			var startTime = overload; // 如果高位是0,则表示当前未开始检测,就取上次的负载结果,否则计算上次和当前的最大值
			return (startTime & ~3L) == 0
					? (int)startTime
					: Math.max((int)startTime & 3, calcOverload((System.nanoTime() - startTime) / 1_000_000));
		}
	}
}
