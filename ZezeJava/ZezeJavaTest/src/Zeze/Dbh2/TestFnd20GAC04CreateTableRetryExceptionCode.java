package Zeze.Dbh2;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Config;
import Zeze.Dbh2.Database;
import Zeze.Dbh2.Dbh2AgentManager;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action3;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND20 GA-C04回归：master侧建表的暂时性异常失败必须落在GA-D04重试白名单内。
 * createBucketRafts超时/建桶rpc失败等异常从ProcessCreateTableRequest逃逸，master侧
 * MasterService是noProcedure+None级，派发层把异常统一翻成Procedure.Exception(-1)回客户端
 * （Task.callFuncCore的通用翻译）。bug时白名单只认eTableNotFound/eTooFewManager，
 * rc==-1立即setException：扩容滚动窗口内应用启动直接终止。修复=白名单追加-1，
 * 重试并以GA-D04总预算兜底（码面无法区分暂时/永久）。
 * 形态：纯桩直构（对齐TestFnd19GAD04）：脚本化MasterAgent按序回码。
 */
@Fast
public class TestFnd20GAC04CreateTableRetryExceptionCode {

	// 建表结果脚本化的master：按序返回rc，rc==0返回空分桶表（isNew=true），脚本耗尽回0。
	private static final class ScriptedMasterAgent extends MasterAgent {
		private final ArrayDeque<Integer> script = new ArrayDeque<>();
		final AtomicInteger attempts = new AtomicInteger();

		ScriptedMasterAgent(Integer... rcs) {
			super(new Config());
			script.addAll(List.of(rcs));
		}

		@Override
		public void createTableAsync(String database, String table,
									 Action3<Integer, Boolean, MasterTable.Data> callback) {
			attempts.incrementAndGet();
			var rc = script.poll();
			try {
				if (rc == null || rc == 0)
					callback.run(0, true, new MasterTable.Data());
				else
					callback.run(rc, false, null);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static Database.Dbh2Table openTable(Dbh2AgentManager manager) {
		var conf = new Config.DatabaseConf();
		conf.setDatabaseType(Config.DbType.Dbh2);
		conf.setDatabaseUrl("dbh2://127.0.0.1:11000/dbh2Fnd20C04");
		conf.setName("dbh2");
		var database = new Database(null, manager, conf);
		return (Database.Dbh2Table)database.openTable("t1", 1);
	}

	@Timeout(60) // 重试链1s退避约1-2s收敛；挂死转超时失败
	@Test
	public void testProcedureExceptionRetriedUntilSuccess(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		// 模拟一次createBucketRafts超时（派发层翻-1）后扩容完成、建表成功。
		var master = new ScriptedMasterAgent((int)Procedure.Exception, 0);
		var manager = new Dbh2AgentManager(new Fnd19GADStubSupport.NullServiceAgent(),
				Config.load(Fnd19GADStubSupport.writeRemoteCommitConfig(tempDir).toString()), 831) {
			@Override
			public MasterAgent openDatabase(String masterName, String databaseName) {
				return master;
			}
		};
		try {
			var table = openTable(manager);
			table.waitReady(); // bug时：rc==-1不在白名单，第一次失败即抛
			Assertions.assertEquals(2, master.attempts.get(),
					"Procedure.Exception（master侧异常翻码）必须重试：失败一次（1s退避）后成功");
			Assertions.assertTrue(table.isNew());
		} finally {
			manager.stop();
		}
	}

	@Test
	public void testOtherCodeStillFailsImmediately(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var master = new ScriptedMasterAgent(MasterAgent.eDatabaseNotFound);
		var manager = new Dbh2AgentManager(new Fnd19GADStubSupport.NullServiceAgent(),
				Config.load(Fnd19GADStubSupport.writeRemoteCommitConfig(tempDir).toString()), 832) {
			@Override
			public MasterAgent openDatabase(String masterName, String databaseName) {
				return master;
			}
		};
		try {
			var table = openTable(manager);
			var ex = Assertions.assertThrows(RuntimeException.class, table::waitReady);
			Assertions.assertTrue(ex.getMessage().contains("rc=" + MasterAgent.eDatabaseNotFound),
					"失败必须携带原始错误码: " + ex.getMessage());
			Assertions.assertEquals(1, master.attempts.get(), "非白名单码（配置类错误）不得重试");
		} finally {
			manager.stop();
		}
	}
}
