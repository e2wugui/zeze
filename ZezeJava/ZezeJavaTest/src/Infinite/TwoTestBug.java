package Infinite;

public class TwoTestBug {
	public static void main(String args[]) throws Throwable {
		var i = 0;
		try {
			while (true) {
				Simulate.logger.fatal("----------- CBasicSimpleAddConcurrent " + i + " -----------");
				var test1 = new CBasicSimpleAddConcurrent();
				try {
					test1.testBenchmark();
				} catch (Throwable ex) {
					Simulate.logger.fatal("CBasicSimpleAddConcurrent", ex);
				}
				Simulate.logger.fatal("----------- Simulate " + i + " -----------");
				var simulate = new Simulate();
				simulate.Infinite = false;
				simulate.Before();
				try {
					simulate.testMain();
				} catch (Throwable e) {
					Simulate.logger.fatal("main exception:", e);
					throw e;
				} finally {
					simulate.After();
				}
			}
		} finally {
			Simulate.logger.fatal("----------- End " + i + " -----------");
		}
	}
}
