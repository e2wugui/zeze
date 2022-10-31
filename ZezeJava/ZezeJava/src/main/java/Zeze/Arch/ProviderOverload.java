package Zeze.Arch;

import java.util.HashSet;
import java.util.concurrent.ThreadPoolExecutor;
import Zeze.Builtin.Provider.BLoad;

public class ProviderOverload {
	private final HashSet<ThreadPoolMonitor> threadPools = new HashSet<>();

	public void register(ThreadPoolExecutor threadPool, int threshold, int overload) {
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
		ThreadPoolExecutor threadPool;
		int threshold;
		int overload;

		public ThreadPoolMonitor(ThreadPoolExecutor threadPool, int threshold, int overload) {
			this.threadPool = threadPool;
			this.threshold = threshold;
			this.overload = overload;
		}

		public int overload() {
			var elementSize = threadPool.getQueue().size();
			if (elementSize > overload)
				return BLoad.eOverload;
			if (elementSize > threshold)
				return BLoad.eThreshold;
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
			if (other instanceof ThreadPoolMonitor config) {
				return threadPool.equals(config.threadPool);
			}
			return false;
		}
	}
}
