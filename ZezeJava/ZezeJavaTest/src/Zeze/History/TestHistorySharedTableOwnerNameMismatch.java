package Zeze.History;

import Zeze.Application;
import Zeze.Config;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND33 history-02 回归：gid 数值空间绑定 history 发号名而 tHistory 主键裸用 gid，
 * 不同发号名的 app 解析到同一物理存储（默认表配置即同库）时，writeOnly 的 replace
 * 以数值相等的 gid 静默互覆——历史行丢失且两套序列各自连续，空洞检测无感，此前无
 * 任何 fail-fast。修复：tHistory 所属库 DirectOperates 区写归属标记，启动期校验
 * ——同库不同名 fail-fast；同名共享合法（多 app 协作既有语义：同名 gid 从同一 SM
 * 取号天然不重叠）；换名重启同样被拦截（新名从零发号，对存量行就是他名覆盖）。
 */
@Fast
public class TestHistorySharedTableOwnerNameMismatch {

	// 独立号段+派生url（每用例独立url隔离DatabaseMemory静态桶的归属标记残留）。
	private static final int SERVER_ID = FastServerIds.TEST_HISTORY_SHARED_TABLE_OWNER_NAME_MISMATCH;

	private static Application newApp(int serverId, String url, String historyName) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setTakeoverMode("off");
		conf.setServerId(serverId);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl(url);
		conf.getDatabaseConfMap().put("", dbConf);
		conf.setHistory(historyName);
		return new Application("TestHistorySharedTableOwner", conf);
	}

	private static void quietStop(Application app) {
		if (app == null)
			return;
		try {
			app.stop();
		} catch (Throwable e) {
			// 被拒绝启动的app收尾异常不掩盖断言（部分启动状态的stop路径尽力而为）。
		}
	}

	// 同库（同url内存桶）不同发号名：第二个 app 启动必须 fail-fast——现状（修复前）
	// 两个 app 都静默启动成功，随后同数值 gid 经 writeOnly replace 互相覆盖历史行。
	@Test
	public void testDifferentOwnerNamesRejectedOnSharedPhysicalTable() throws Exception {
		var url = "history_owner_mismatch_" + SERVER_ID;
		var app1 = newApp(SERVER_ID, url, "TestOwnerNameA");
		try {
			app1.start(); // 首启认领归属
			Application app2 = null;
			try {
				app2 = newApp(SERVER_ID + 1, url, "TestOwnerNameB");
				Assertions.assertThrows(IllegalStateException.class, app2::start,
						"同库不同发号名必须启动期拒绝（gid数值重叠经writeOnly replace静默互覆）");
			} finally {
				quietStop(app2);
			}
		} finally {
			quietStop(app1);
		}
	}

	// 同名共享同一物理表：合法（多app同库协作模式），第二个 app 正常启动。
	@Test
	public void testSameOwnerNameSharedTableAllowed() throws Exception {
		var url = "history_owner_same_" + (SERVER_ID + 3);
		var app1 = newApp(SERVER_ID + 3, url, "TestOwnerShared");
		try {
			app1.start();
			var app2 = newApp(SERVER_ID + 4, url, "TestOwnerShared");
			try {
				Assertions.assertDoesNotThrow(app2::start, "同名共享同一物理tHistory是多app协作的合法形态");
			} finally {
				quietStop(app2);
			}
		} finally {
			quietStop(app1);
		}
	}

	// 换名重启（同url先以A启动停止，再以B启动）：必须 fail-fast——新名从零发号，
	// 与旧名遗留记录在同一tHistory内数值重叠，正是互覆形态。
	@Test
	public void testRenameRestartRejected() throws Exception {
		var url = "history_owner_rename_" + (SERVER_ID + 5);
		var app1 = newApp(SERVER_ID + 5, url, "TestOwnerBeforeRename");
		try {
			app1.start();
			quietStop(app1);
			var app2 = newApp(SERVER_ID + 5, url, "TestOwnerAfterRename");
			try {
				Assertions.assertThrows(IllegalStateException.class, app2::start,
						"换名重启必须启动期拒绝（新名从零发号，对存量行就是他名覆盖）");
			} finally {
				quietStop(app2);
			}
		} finally {
			quietStop(app1);
		}
	}
}
