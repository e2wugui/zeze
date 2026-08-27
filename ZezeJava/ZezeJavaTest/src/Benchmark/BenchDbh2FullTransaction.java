package Benchmark;

import java.util.ArrayList;
import java.util.concurrent.Future;
import Dbh2.Dbh2TestEnv;
import Zeze.Dbh2.Database;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Database.AbstractKVTable;
import Zeze.Util.TaskSpec;
import harness.Bench;
import org.junit.jupiter.api.Test;

// Dbh2 全事务吞吐基准（master + 3个Dbh2Manager 多bucket拓扑，与 Dbh2.Dbh2FullTest.testFull 的环境一致）。
// 从 Dbh2FullTest 拆出：integrationTest 只保留功能路径（testFull），负载形态整体归 bench 车道。环境拓扑由 Dbh2.Dbh2TestEnv 脚手架提供。
@SuppressWarnings("NewClassNamingConvention")
@Bench
public class BenchDbh2FullTransaction {
	private static Future<?> startBench(int keyStart, int keyEnd, Database database, AbstractKVTable table, ByteBuffer value) {
		return TaskSpec.ofAction(() -> {
			for (int i = keyStart; i < keyEnd; ++i) {
				try (var trans = database.beginTransaction()) {
					var key = ByteBuffer.Allocate();
					key.WriteInt(i);
					table.replace(trans, key, value);
					trans.commit();
				}
			}
		}).name("").submitNow();
	}

	@Test
	public void testBenchmark() throws Exception {
		var env = new Dbh2TestEnv();
		env.prepareNewEnvironment();
		try {
			var count = 3000;
			var threads = 2;
			var futures = new ArrayList<Future<?>>();
			var b = new Zeze.Util.Benchmark();
			for (var t = 0; t < threads; ++t) {
				var keyStart = t * count;
				var keyEnd = keyStart + count;
				futures.add(startBench(keyStart, keyEnd, env.database, env.tables.get(t % env.tables.size()), env.value));
			}
			Thread.sleep(1000); // 等待agent都连上，然后dump出来。此时任务在并发执行。
			env.dbh2AgentManager.dumpAgents();
			for (var future : futures)
				future.get();
			b.report("Bench Dbh2 Full Transaction", count * threads);
		} finally {
			env.stopAll();
		}
	}
}
