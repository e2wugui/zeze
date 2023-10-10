package Zeze.Arch;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Config;
import Zeze.Util.Random;
import Zeze.Util.ThreadDiagnosable;

public class ProviderOverload {
	private final HashSet<ThreadPoolMonitor> threadPools = new HashSet<>();
	private final ScheduledExecutorService scheduledExecutorService;

	public ProviderOverload() {
		scheduledExecutorService = Executors.newScheduledThreadPool(
				1, ThreadDiagnosable.newFactory("ZezeScheduledPool"));
	}

	public void register(ExecutorService threadPool, Config config) {
		threadPools.add(new ThreadPoolMonitor(threadPool, config));
	}

	// 由LoadReporter读取报告。
	public int getOverload() {
		var result = BLoad.eWorkFine;
		for (var threadPool : threadPools) {
			var overload = threadPool.overload();
			if (overload == BLoad.eOverload)
				return overload;
			if (overload == BLoad.eThreshold)
				result = BLoad.eThreshold;
		}
		return result;
	}

	public class ThreadPoolMonitor {
		final ExecutorService threadPool;
		final Config config;
		volatile int overload = BLoad.eWorkFine;

		public ThreadPoolMonitor(ExecutorService threadPool, Config config) {
			this.threadPool = threadPool;
			this.config = config;
			startDetectDelay();
		}

		private synchronized void startDetectDelay() {
			scheduledExecutorService.schedule(
					this::detecting, Random.getInstance().nextLong(1000) + 1000, TimeUnit.MILLISECONDS);
		}

		private void detecting() {
			var time = System.currentTimeMillis();

			// todo 虚拟线程需要想其他办法检测。比如还是回到任务数量上：同时执行的任务超过多少。
			threadPool.execute(() -> {
				synchronized (this) {
					var detectElapse = System.currentTimeMillis() - time;
					if (detectElapse > config.getProviderOverload())
						overload = BLoad.eOverload;
					else if (detectElapse > config.getProviderThreshold())
						overload = BLoad.eThreshold;
					else
						overload = BLoad.eWorkFine;

					startDetectDelay();
				}
			});
		}

		public int overload() {
			return overload;
		}

		@Override
		public int hashCode() {
			return threadPool.hashCode();
		}

		@Override
		public boolean equals(Object other) {
			return other == this; // 直接比较引用即可。
		}
	}
}
