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
import Zeze.Util.Action3;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND19 GA-D04回归：客户端建表异步重试+错误码白名单+总预算（拍板方案A）。
 * eTableNotFound/eTooFewManager是master/manager空窗的暂时性失败（master侧createTable
 * 幂等），1s起指数退避封顶30s重试；其他错误码（配置类）立即失败。
 * bug时一次失败即setException，openTable的waitReady直接抛，master重启窗口内启动的
 * 应用崩溃循环。钉住两个契约：白名单码前两次失败第三次成功→waitReady成功；
 * 非白名单码立即失败且不重试。
 * 形态：纯桩直构——MasterAgent.createTableAsync脚本化返回码序列（成功时返回空分桶表，
 * 不触发openBucket），Dbh2AgentManager只覆写openDatabase返回该桩。
 */
@Fast
public class TestGAD04CreateTableRetryWhitelist {

	// 建表结果脚本化的master：按序返回rc，rc==0返回空分桶表（isNew=true）。
	// 调用序列：首次在测试线程（Dbh2Table构造内同步发起），重试在Task调度线程；
	// 串行推进（下一次重试在上一次回调后才调度），ArrayDeque无并发访问。
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
				if (rc == 0)
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
		conf.setDatabaseUrl("dbh2://127.0.0.1:11000/dbh2d04");
		conf.setName("dbh2");
		var database = new Database(null, manager, conf);
		return (Database.Dbh2Table)database.openTable("t1", 1);
	}

	@Timeout(60) // 重试链1s+2s约3s收敛；挂死（如重试不再发起/成功不置位）转成超时失败
	@Test
	public void testWhitelistRetriedUntilSuccess(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var master = new ScriptedMasterAgent(MasterAgent.eTableNotFound, MasterAgent.eTooFewManager, 0);
		var manager = new Dbh2AgentManager(new Fnd19GADStubSupport.NullServiceAgent(),
				Config.load(Fnd19GADStubSupport.writeRemoteCommitConfig(tempDir).toString())) {
			@Override
			public MasterAgent openDatabase(String masterName, String databaseName) {
				return master;
			}
		};
		try {
			var table = openTable(manager);
			table.waitReady(); // bug时第一次失败即抛
			Assertions.assertEquals(3, master.attempts.get(),
					"白名单码必须重试：前两次失败（1s+2s退避）第三次成功");
			Assertions.assertTrue(table.isNew(), "成功回调的isNew必须传播");
		} finally {
			manager.stop();
		}
	}

	@Test
	public void testOtherCodeFailsImmediately(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var master = new ScriptedMasterAgent(MasterAgent.eDatabaseNotFound);
		var manager = new Dbh2AgentManager(new Fnd19GADStubSupport.NullServiceAgent(),
				Config.load(Fnd19GADStubSupport.writeRemoteCommitConfig(tempDir).toString())) {
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
