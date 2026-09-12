package Onz;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import Zeze.Config;
import Zeze.Onz.OnzServer;
import Zeze.Util.RocksDatabase;
import harness.TestEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * FND4-89 回归：OnzServer构造多步资源获取无回滚。
 * 构造顺序获取myServiceManager→RocksDatabase→各zeze的SM，中途任一步失败时
 * 已启动资源不回收（stop不可达——对象未构造完成）：泄漏网络线程、端口与
 * RocksDB目录锁，阻碍同进程重试。修复：构造的全有或全无，失败逆序释放。
 * 注入：zeze2用ServiceManager="disable"的配置——createServiceManager返回null，
 * 构造在第二个集群处失败（此时mySM/库/zeze1的SM均已获取）。
 * 检查点：RocksDB目录锁必须随失败释放（重开同一目录必须成功）。
 */
public class TestOnzServerCtorRollback {
	private static final String NoSmConfig = "onz_zeze_no_sm.xml";

	private Path dbHome;

	@BeforeEach
	public void before() throws Exception {
		// 只需第一对服务 SM(5001)（cluster2在失败点之前不会被触达）。
		Assumptions.assumeTrue(TestEnv.portReachable("127.0.0.1", 5001),
				"SM(5001)不可用：zeze.test.env=off 时 TestEnvLauncherListener 不在进程内自动启动");

		Files.writeString(Path.of(NoSmConfig), "<zeze ServiceManager=\"disable\"></zeze>\n");
		dbHome = Path.of("CommitOnzServer0");
		deleteRecursively(dbHome); // 清历史运行残留，保证锁语义确定
	}

	@AfterEach
	public void after() throws Exception {
		Files.deleteIfExists(Path.of(NoSmConfig));
	}

	@Test
	@Timeout(120)
	public void testCtorRollbackReleasesDbLock() throws Exception {
		var myConfig = Config.load("zeze.xml");
		Assertions.assertThrows(Exception.class,
				() -> {
					var unused = new OnzServer("zeze1=zeze.xml;zeze2=" + NoSmConfig, myConfig);
				},
				"第二个集群无ServiceManager必须构造失败");

		// 半途失败不得泄漏RocksDB目录锁：重开同一目录必须成功
		//（修复前句柄仍被半成品构造持有，重开报lock错误）。
		try (var db = new RocksDatabase(dbHome.toString())) {
			Assertions.assertNotNull(db);
		}
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
}
