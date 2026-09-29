package Zeze.Dbh2;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Walk;
import Zeze.Config;
import Zeze.Dbh2.Dbh2Agent;
import Zeze.Dbh2.Dbh2AgentManager;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.Net.Binary;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND19 GA-D01回归：walkPage对refused重定向设上限（对齐Dbh2Table.find先例，拍板方案A；
 * 上限值后经bf8923edc自2放宽、ed89f2d51把判定收敛为refused总数恰256，本测试随新契约对齐）。
 * 场景：master侧表长期陈旧（分桶发布失败等），客户端每轮reload都拿到同一张陈旧分桶表，
 * 桶服务端持续bucketRefuse。bug时无限循环——每轮2次rpc无声占用线程与配额，调用方永不返回。
 * 钉住：连续refused总数达256（前255次各reload后重试、第256次）抛RuntimeException，消息带
 * master/database/table与计数上下文；fetch成功即清零计数不在此测试范围。
 * 形态：纯桩直构——MasterAgent.getBuckets恒返同一张陈旧表，Dbh2Agent.walk恒返
 * bucketRefuse（真实walk入口的fetcher负责映射isBucketRefuse为REFUSED，一并覆盖）。
 */
@Fast
public class TestGAD01WalkPageRedirectLimit {

	// 恒返bucketRefuse的桶代理。super会启动raft-client指向死端口（后台重连，不影响本测试，
	// walk被覆写不再触网），close()统一回收。
	private static final class RefusedAgent extends Dbh2Agent {
		final AtomicInteger walkCount = new AtomicInteger();

		RefusedAgent() throws Exception {
			super("""
					<?xml version="1.0" encoding="utf-8"?>
					<raft Name=""><node Host="127.0.0.1" Port="19199"/></raft>
					""");
		}

		@Override
		public Walk walk(Binary exclusiveStartKey, int proposeLimit, boolean desc, byte[] prefix) {
			walkCount.incrementAndGet();
			var r = new Walk();
			r.Result.setBucketRefuse(true);
			return r;
		}
	}

	// master恒返陈旧表：getBuckets每次返回同一个MasterTable.Data实例（reload不收敛）。
	private static final class StaleMasterAgent extends MasterAgent {
		private final MasterTable.Data stale;

		StaleMasterAgent(MasterTable.Data stale) {
			super(new Config());
			this.stale = stale;
		}

		@Override
		public MasterTable.Data getBuckets(String database, String table) {
			return stale;
		}
	}

	private static MasterTable.Data oneBucketTable() {
		var meta = new BBucketMeta.Data();
		meta.setDatabaseName("dbh2d01");
		meta.setTableName("t1");
		meta.setRaftConfig("deadBucketRaft");
		meta.setKeyFirst(Binary.Empty);
		meta.setKeyLast(Binary.Empty);
		var table = new MasterTable.Data();
		table.getBuckets().put(Binary.Empty, meta);
		return table;
	}

	@Timeout(60) // bug回归时walkPage无限循环，用超时兜底转成失败而不是挂死车道
	@Test
	public void testConsecutiveRefusedBeyondLimitThrowsWithContext(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var agent = new RefusedAgent();
		var manager = new Dbh2AgentManager(new Fnd19GADStubSupport.NullServiceAgent(),
				Config.load(Fnd19GADStubSupport.writeRemoteCommitConfig(tempDir).toString())) {
			@Override
			public Dbh2Agent openBucket(String raftString) {
				return agent; // 陈旧表的raftConfig统一路由到恒拒桩桶
			}
		};
		try {
			var master = new StaleMasterAgent(oneBucketTable());
			var ex = Assertions.assertThrows(RuntimeException.class,
					() -> manager.walk(master, "127.0.0.1_11000", "dbh2d01", "t1",
							(key, value) -> false, false, null));
			Assertions.assertTrue(ex.getMessage().contains("walkPage bucket refused too many redirect"),
					"消息必须指明拒绝重定向超限: " + ex.getMessage());
			Assertions.assertTrue(ex.getMessage().contains("master=127.0.0.1_11000"), ex.getMessage());
			Assertions.assertTrue(ex.getMessage().contains("database=dbh2d01"), ex.getMessage());
			Assertions.assertTrue(ex.getMessage().contains("table=t1"), ex.getMessage());
			Assertions.assertEquals(256, agent.walkCount.get(),
					"前255次refused各reload后重试，第256次必须抛出（refused总数上限256）");
		} finally {
			manager.stop();
			agent.close();
		}
	}
}
