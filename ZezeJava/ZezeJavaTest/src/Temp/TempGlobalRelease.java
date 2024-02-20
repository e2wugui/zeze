package Temp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Future;
import Zeze.Transaction.GlobalAgentBase;
import Zeze.Util.Benchmark;
import Zeze.Util.Task;
import demo.App;

public class TempGlobalRelease {
	public final static int eRecordCountPerTable = 10_0000;

	public static void main(String [] args) throws Exception {
		App.Instance.Start();
		try {
			{
				var b = new Benchmark();
				var tableCount = 1;
				var prepareFutures = new ArrayList<Future<?>>();
				for (var i = 0; i < eRecordCountPerTable; ++i) {
					var key = (long)i;
					prepareFutures.add(Task.runUnsafe(App.Instance.Zeze.newProcedure(() -> {
						App.Instance.demo_Module1.getTable1().getOrAdd(key).setLong2(1L);
						return 0;
					}, "prepare data")));
				}
				for (var future : prepareFutures)
					future.get();
				b.report("prepare data", eRecordCountPerTable * tableCount);
				var gAgent = App.Instance.Zeze.getGlobalAgent();
				assert gAgent != null;
				for (var i = 0; i < gAgent.getAgentCount(); ++i) {
					gAgent.getAgent(i).startRelease(App.Instance.Zeze, null);
				}
				var releaseResults = new GlobalAgentBase.CheckReleaseResult[gAgent.getAgentCount()];
				Arrays.fill(releaseResults, GlobalAgentBase.CheckReleaseResult.Releasing);
				while (true) {
					for (var i = 0; i < releaseResults.length; ++i) {
						if (releaseResults[i] == GlobalAgentBase.CheckReleaseResult.Releasing)
							releaseResults[i] = gAgent.getAgent(i).checkReleaseTimeout(System.currentTimeMillis(), 60_000);
					}
					if (allDone(releaseResults))
						break;
					Thread.sleep(100);
				}
			}
		} finally {
			App.Instance.Stop();
		}
	}

	public static boolean allDone(GlobalAgentBase.CheckReleaseResult[] releaseResults) {
		for (var i = 0; i < releaseResults.length; ++i)
			if (releaseResults[i] == GlobalAgentBase.CheckReleaseResult.Releasing)
				return false;
		return true;
	}
}
