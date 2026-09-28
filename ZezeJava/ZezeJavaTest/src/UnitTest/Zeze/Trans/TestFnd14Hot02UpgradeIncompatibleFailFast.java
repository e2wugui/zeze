package UnitTest.Zeze.Trans;

import harness.FastServerIds;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Config;
import Zeze.Hot.HotUpgradeMemoryTable;
import Zeze.Util.TaskSpec;
import demo.Bean1;
import demo.Module1.tMemorySize;
import demo.Module1.tWalkPage;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND13 hot-02回归（用户预裁决fail-fast回滚）：HotUpgradeMemoryTable.upgrade首键不兼容时
 * 记日志即静默中止迁移，finally仍无条件禁用旧表并按成功返回——半迁移假成功（数据留在已
 * 禁用表上不可达）。修复：首键不兼容经标志带出lambda抛IllegalStateException且保留旧表；
 * 调用处于热更"不能出错阶段"，异常上抛=按阶段契约停止程序。
 * 构造：tMemorySize(Long键)→tWalkPage(Integer键)为现成不兼容对，旧表放一条记录后直接
 * 构造HotUpgradeMemoryTable调用upgrade，首键encodeKey抛CCE即"新表不接受旧表key类型"。
 * 断言：必须抛ISE（修复前红：静默容忍正常返回）且旧表保留可用（修复前被无条件disable）。
 */
@Fast
public class TestFnd14Hot02UpgradeIncompatibleFailFast {
	// 独立serverId+url：@Fast类并行时避免本地库互撞（对齐TestHotRollbackMemoryTable；
	// 810段：避开其800起的递增段）。
	private static final AtomicInteger nextServerId = new AtomicInteger(FastServerIds.TEST_FND14_HOT02_UPGRADE_INCOMPATIBLE_FAILFAST);

	private static Application newApp(int serverId) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(serverId);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("fnd14_hot02_" + serverId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestFnd14Hot02@" + serverId, conf);
	}

	@Test
	public void testFirstKeyIncompatibleFailsFastKeepsOld() throws Exception {
		var app = newApp(nextServerId.getAndIncrement());
		var oldTable = new tMemorySize(); // TableX<Long, Bean1>，内存表
		app.addTable("", oldTable);
		app.start();
		try {
			var key = 1L;
			var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
				var v = new Bean1();
				v.setV1(42);
				oldTable.put(key, v);
				return 0L;
			}, "TestFnd14Hot02.put")).call();
			Assertions.assertEquals(0L, rc);
			app.checkpointRun(); // 脏数据flush进本地Rocks：tMemorySize受限容量，walkMemoryAny的数据源

			// cur用未打开的tWalkPage即可：不兼容路径只走到encodeKey(Object)的(K)强转
			// （Long→Integer CCE），不触碰任何open状态
			var up = new HotUpgradeMemoryTable(oldTable, new tWalkPage());
			// 首键不兼容（Long键喂Integer键表，encodeKey内CCE）：必须fail-fast上抛
			Assertions.assertThrows(IllegalStateException.class, up::upgrade,
					"首键不兼容必须fail-fast回滚上抛（修复前记日志静默容忍并假报成功）");

			// 回滚=旧表保留可用，不得被disable
			var got = new int[] {-1};
			rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
				var v = oldTable.get(key);
				got[0] = v != null ? v.getV1() : -1;
				return 0L;
			}, "TestFnd14Hot02.get")).call();
			Assertions.assertEquals(0L, rc);
			Assertions.assertEquals(42, got[0], "回滚语义=旧表保留可用（修复前被无条件disable）");
		} finally {
			app.stop();
		}
	}
}
