package Onz;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import Zeze.Config;
import Zeze.Onz.OnzProcedure;
import Zeze.Onz.OnzServer;
import Zeze.Onz.OnzTransaction;
import Zeze.Util.RocksDatabase;
import demo.App;
import demo.Module1.BKuafu;
import demo.Module1.BKuafuResult;
import harness.TestEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * FND4-88 回归：commitPoint/commitIndex 两表同 key 生命周期。
 * 协调者每个事务同 batch 写两表（saveCommitPoint），但清理入口 removeCommitIndex
 * 只删索引不删点——长期运行的 OnzServer 磁盘单调增长。修复：索引删除同 batch
 * 联动删点（索引在则点在，索引删则点删）。
 * 检查方式：perform 一个成功事务并停机后重开 CommitOnzServer 库，两表必须均空
 * （修复前 commitIndex 空、commitPoint 残留 1 条）。启动前清库目录保证计数确定性。
 */
public class TestOnzCommitPointCleanup {
	// 过程名必须全 JVM 唯一：demo.App 单例的 Onz 注册表跨测试类持久（"kuafu"归 TestOnz）。
	private static final String ProcName = "kuafuCpCleanup";

	private final App zeze2 = new App();
	private OnzServer onzServer;
	private String dbHome;

	@BeforeEach
	public void before() throws Exception {
		// 第二对服务 SM(5011)/Global(5012) 由 TestEnvLauncherListener 在进程内自动启动。
		Assumptions.assumeTrue(TestEnv.portReachable("127.0.0.1", 5011) && TestEnv.portReachable("127.0.0.1", 5012),
				"第二对服务(5011/5012)不可用：zeze.test.env=off 时 TestEnvLauncherListener 不在进程内自动启动");

		var myConfig = Config.load("zeze.xml");
		dbHome = "CommitOnzServer" + myConfig.getServerId();
		deleteRecursively(Path.of(dbHome)); // 含修复前版本留下的孤儿commitPoint

		App.Instance.Start();
		var config2 = Config.load("./zeze_cluster_2.xml");
		zeze2.Start(config2);

		Infinite.App.clearDbTable(zeze2.demo_Module1.getKuafu());
		Infinite.App.clearDbTable(App.Instance.demo_Module1.getKuafu());

		App.Instance.Zeze.getOnz().register(ProcName, TestOnzCommitPointCleanup::kuaFu, BKuafu.class, BKuafuResult.class);
		zeze2.Zeze.getOnz().register(ProcName, TestOnzCommitPointCleanup::kuaFu, BKuafu.class, BKuafuResult.class);

		onzServer = new OnzServer("zeze1=zeze.xml;zeze2=zeze_cluster_2.xml", myConfig);
		onzServer.start();
	}

	@AfterEach
	public void after() throws Exception {
		// before() 被 Assumption 跳过时 onzServer 尚未创建；testCommitPointCleanup已stop过则幂等
		if (onzServer != null)
			onzServer.stop();
		zeze2.Stop();
	}

	private static long kuaFu(OnzProcedure onzProcedure, BKuafu argument, BKuafuResult result) {
		var app = (App)onzProcedure.getStub().getOnz().getZeze().getAppBase();
		var account = app.demo_Module1.getKuafu().getOrAdd(argument.getAccount());
		account.setMoney(account.getMoney() + argument.getMoney());
		result.setMoney(account.getMoney());
		return 0;
	}

	private static void deleteRecursively(Path root) throws Exception {
		if (!Files.exists(root))
			return;
		try (var walk = Files.walk(root)) {
			walk.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.delete(p);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

	private static long count(RocksDatabase.Table table) throws Exception {
		long n = 0;
		try (var it = table.iterator()) {
			for (it.seekToFirst(); it.isValid(); it.next())
				n++;
		}
		return n;
	}

	// 同 TestOnz.waitOnzReady：等订阅发现两侧集群并建连（getZezeInstance成功即perform就绪）。
	private void waitOnzReady() throws InterruptedException {
		var deadline = System.currentTimeMillis() + 60_000;
		for (;;) {
			try {
				onzServer.getZezeInstance("zeze1");
				onzServer.getZezeInstance("zeze2");
				return;
			} catch (RuntimeException e) {
				if (System.currentTimeMillis() > deadline)
					throw e;
				Thread.sleep(100);
			}
		}
	}

	// KuafuTransaction 硬编码过程名"kuafu"，这里用唯一名自带事务。
	private static class CleanupTransaction extends OnzTransaction<BKuafu.Data, BKuafuResult.Data> {
		@Override
		protected long perform() throws Exception {
			var arg1 = new BKuafu.Data();
			arg1.setAccount(1);
			arg1.setMoney(1);
			var f1 = super.callProcedureAsync("zeze1", ProcName, arg1, new BKuafuResult.Data());
			var arg2 = new BKuafu.Data();
			arg2.setAccount(2);
			arg2.setMoney(-1);
			var f2 = super.callProcedureAsync("zeze2", ProcName, arg2, new BKuafuResult.Data());
			f1.get();
			f2.get();
			return 0;
		}
	}

	@Test
	@Timeout(120)
	public void testCommitPointCleanup() throws Exception {
		waitOnzReady();
		var txn = new CleanupTransaction();
		txn.setOnzServer(onzServer);
		Assertions.assertEquals(0, onzServer.perform(txn), "事务必须成功（否则测试无效）");

		onzServer.stop(); // 关闭句柄后才能重开库检查
		try (var db = new RocksDatabase(dbHome)) {
			Assertions.assertEquals(0, count(db.getOrAddTable("CommitIndex")),
					"commitIndex 随补发完成清理（既有行为）");
			Assertions.assertEquals(0, count(db.getOrAddTable("CommitPoint")),
					"commitPoint 必须随 commitIndex 联动删除（FND4-88）");
		}
	}
}
