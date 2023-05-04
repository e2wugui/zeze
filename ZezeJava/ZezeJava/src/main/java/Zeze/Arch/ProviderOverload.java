package Zeze.Arch;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import Zeze.Builtin.Provider.BLoad;

public class ProviderOverload {
	private final HashSet<ThreadPoolMonitor> threadPools = new HashSet<>();

	public void register(ExecutorService threadPool, int threshold, int overload) {
		threadPools.add(new ThreadPoolMonitor(threadPool, threshold, overload));
	}

	// 由LoadReporter读取报告。
	public int getOverload() {
		for (var threadPool : threadPools) {
			var overload = threadPool.overload();
			if (overload != BLoad.eWorkFine)
				return overload;
		}
		return BLoad.eWorkFine;
	}

	public static class ThreadPoolMonitor {
		final ExecutorService threadPool;
		final int threshold;
		final int overload;

		public ThreadPoolMonitor(ExecutorService threadPool, int threshold, int overload) {
			this.threadPool = threadPool;
			this.threshold = threshold;
			this.overload = overload;
		}

		public int overload() {
			if (threadPool instanceof ThreadPoolExecutor) {
				var pool = (ThreadPoolExecutor)threadPool;
				var elementSize = pool.getQueue().size();
				if (elementSize > overload)
					return BLoad.eOverload;
				if (elementSize > threshold)
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
