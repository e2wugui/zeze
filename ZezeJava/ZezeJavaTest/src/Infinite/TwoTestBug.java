package Infinite;

import Benchmark.CBasicSimpleAddConcurrent;

public class TwoTestBug {
	public static void main(String[] args) throws Exception {
		int i = 0;
		try {
			//noinspection InfiniteLoopStatement
			for (; ; i++) {
				Simulate.logger.fatal("----------- CBasicSimpleAddConcurrent {} -----------", i);
				var test1 = new CBasicSimpleAddConcurrent();
				try {
					test1.testBenchmark();
				} catch (Throwable ex) { // print stacktrace.
					Simulate.logger.fatal("CBasicSimpleAddConcurrent", ex);
				}
				Simulate.logger.fatal("----------- Simulate {} -----------", i);
				Tasks.clearAllCounters();
				var simulate = new Simulate();
				simulate.Infinite = false;
				simulate.Before();
				try {
					simulate.testMain();
				} catch (Throwable e) { // print stacktrace. rethrow
					Simulate.logger.fatal("main exception:", e);
					throw e;
				} finally {
					simulate.After();
				}
			}
		} finally {
			Simulate.logger.fatal("----------- End {} -----------", i);
		}
	}
}
