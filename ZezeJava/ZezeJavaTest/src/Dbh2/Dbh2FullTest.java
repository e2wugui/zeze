package Dbh2;

import Zeze.Dbh2.Database;
import Zeze.Serialize.ByteBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

// 测试整体结构(Dbh2Manager,Master,Agent)。只保留功能路径：环境拓扑(raft/bucket多manager)+数据正确性断言。
// 吞吐负载部分见 Benchmark.BenchDbh2FullTransaction（@Bench，进 bench 车道）。环境拓扑由 Dbh2TestEnv 脚手架提供。
public class Dbh2FullTest {
	@Test
	public void testFull() throws Exception {
		var env = new Dbh2TestEnv();
		env.prepareNewEnvironment();
		try {
			// testFull();
			var table1 = env.tables.getFirst();
			var table2 = env.tables.get(1);

			var key = ByteBuffer.Wrap(ByteBuffer.Empty);
			var key1 = ByteBuffer.Wrap(new byte[]{1});

			try (var trans = env.database.beginTransaction()) {
				table1.replace(trans, key, env.value);
				table1.replace(trans, key1, env.value);
				table2.replace(trans, key, env.value);
				table2.replace(trans, key1, env.value);
				trans.commit();
			}
			{
				var valueFindKey = table1.find(key);
				Assertions.assertNotNull(valueFindKey);
				Assertions.assertEquals(valueFindKey, env.value);

				var valueFindKey1 = table1.find(key1);
				Assertions.assertNotNull(valueFindKey1);
				Assertions.assertEquals(valueFindKey1, env.value);
			}
			{
				var valueFindKey = table2.find(key);
				Assertions.assertNotNull(valueFindKey);
				Assertions.assertEquals(valueFindKey, env.value);

				var valueFindKey1 = table2.find(key1);
				Assertions.assertNotNull(valueFindKey1);
				Assertions.assertEquals(valueFindKey1, env.value);
			}

			// testCommitServerQueryVerify()
			try (var _trans = env.database.beginTransaction()) {
				var trans = (Database.Dbh2Transaction)_trans;
				table1.replace(trans, key, env.value);
				table1.replace(trans, key1, env.value);
				trans.commitBreakAfterPrepareForDebugOnly();
			}
			// <CustomizeConf Name="Dbh2Config" RpcTimeout="1000" PrepareMaxTime="2000" BucketMaxTime="3000"/>
			// BucketMaxTime
			// 由于raft选举，第一服务可用时间比较长，这个超时需要很长，这个回查测试先不做了。
			// 需要时，去掉这个注释，然后在测试log中查找" query"以及"timeout undo"。验证回查。
			// Thread.sleep(110_000);
		} finally {
			env.stopAll();
		}
	}

	// 回归：同一JVM内第二次环境循环（模拟IDE在同一JVM里重跑测试；也守护tempHome自包含）。
	@Test
	public void testFullSecondCycleProbe() throws Exception {
		var env = new Dbh2TestEnv();
		env.prepareNewEnvironment();
		try {
			var table1 = env.tables.getFirst();
			var key = ByteBuffer.Wrap(ByteBuffer.Empty);
			try (var trans = env.database.beginTransaction()) {
				table1.replace(trans, key, env.value);
				trans.commit();
			}
			Assertions.assertEquals(env.value, table1.find(key));
		} finally {
			env.stopAll();
		}
	}
}
