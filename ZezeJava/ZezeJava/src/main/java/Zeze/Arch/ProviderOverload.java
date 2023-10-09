package Zeze.Arch;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Config;

public class ProviderOverload {
	private final HashSet<ThreadPoolMonitor> threadPools = new HashSet<>();

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

	public static class ThreadPoolMonitor {
		final ExecutorService threadPool;
		final Config config;

		public ThreadPoolMonitor(ExecutorService threadPool, Config config) {
			this.threadPool = threadPool;
			this.config = config;
		}

		public int overload() {
			if (threadPool instanceof ThreadPoolExecutor) {
				var pool = (ThreadPoolExecutor)threadPool;
				var elementSize = pool.getQueue().size();
				if (elementSize > config.getProviderOverload())
					return BLoad.eOverload;
				if (elementSize > config.getProviderThreshold())
					return BLoad.eThreshold;
				return BLoad.eWorkFine;
			}
			// todo 虚拟线程需要使用TaskCount/n，再来比较。
			return BLoad.eWorkFine;
		}

		@Override
		public int hashCode() {
			return threadPool.hashCode();
		}

		@Override
		public boolean equals(Object other) {
			if (other == this)
				return true;
			if (other instanceof ThreadPoolMonitor) {
				return threadPool.equals(((ThreadPoolMonitor)other).threadPool);
			}
			return false;
		}
	}
}
