package UnitTest.Zeze;

import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.Procedure;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND8-26 回归：LocalRocksCacheDb目录名仅含serverId（zeze_cache_&lt;serverId&gt;），
 * 同JVM两个同serverId的App（多App测试派生同配置、serverId默认同0时是默认值行为）
 * 后启者的start()/先停者的stop()会无条件deleteDirectory正被活跃使用的目录。
 * 修复：deleteDirectory之前对同级锁文件zeze_cache_&lt;serverId&gt;.lock上排它锁
 * （持有整个"删-开-跑-关-删"生命周期）——同JVM撞号实例
 * tryLock抛OverlappingFileLockException转IllegalStateException即时fail-fast
 * （修复前Windows上卡约10s后IOException且startState滞留eStarting）；
 * stop释放锁后同serverId可再启动。
 */
@Fast
public class TestFnd826CacheDirLock {
	// 独立serverId：本类独占zeze_cache_<serverId>目录与锁文件（CWD为测试worker共享目录）。
	private static final int SERVER_ID = 12826;

	private static Config newConf() {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(SERVER_ID);
		conf.setDefaultTableConf(new Config.TableConf()); // 裸Config不会补默认值
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("a2_fnd826_memory");
		conf.getDatabaseConfMap().put("", dbConf);
		return conf;
	}

	/** 同JVM同serverId第二实例：必须在deleteDirectory之前被锁拒绝（ISE即时失败），首实例不受影响。 */
	@Test
	public void testSameServerIdSecondAppFailsFast() throws Exception {
		var app1 = new Application("Fnd826First", newConf());
		app1.start();
		try {
			var app2 = new Application("Fnd826Second", newConf());
			var ex = assertThrows(IllegalStateException.class, app2::start,
					"同serverId第二实例必须在删目录前被锁拒绝（修复前deleteDirectory互删活跃目录）");
			assertTrue(ex.getMessage().contains("serverId=" + SERVER_ID),
					"报错必须带serverId与路径上下文便于定位，实际: " + ex.getMessage());
			try {
				app2.stop(); // eStarting滞留实例的清理路径（锁未获取，释放为空操作）
			} catch (Throwable ignored) {
			}

			// 首实例不受影响：目录未被误删，事务照常。
			var rc = app1.newProcedure(() -> 0L, "Fnd826.StillAlive").call();
			assertEquals(Procedure.Success, rc, "首实例必须不受第二实例启动尝试的影响");
		} finally {
			app1.stop();
		}
	}

	/** stop释放锁后：同serverId的新实例可以正常启动（锁随生命周期获取与释放）。 */
	@Test
	public void testLockReleasedAfterStopAllowsRestart() throws Exception {
		var app1 = new Application("Fnd826RestartA", newConf());
		app1.start();
		app1.stop();

		// 前一实例已停（锁释放+目录删除）：同serverId再启必须成功。
		var app2 = new Application("Fnd826RestartB", newConf());
		assertDoesNotThrow(app2::start, "锁释放后同serverId再启动不得被残留锁拒绝");
		try {
			assertEquals(Procedure.Success, app2.newProcedure(() -> 0L, "Fnd826.AfterRestart").call());
		} finally {
			app2.stop();
		}
	}
}
