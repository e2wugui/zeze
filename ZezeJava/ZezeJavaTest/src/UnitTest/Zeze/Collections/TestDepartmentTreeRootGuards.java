package UnitTest.Zeze.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

import Zeze.Application;
import Zeze.Collections.DepartmentTree;
import Zeze.Collections.LinkedMap;
import Zeze.Config;
import Zeze.Util.FuncLong;
import Zeze.Util.TaskSpec;
import harness.Fast;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * CO1-F4 回归（P3）：checkManagePermission/checkParentManagePermission在根行缺失
 * （树未create/已destroy）时裸NPE——getRoot()返回null直接解引用。
 * 修复：root==null返回errorCode(Module.ErrorDepartmentNotExist)（与department查不到
 * 的分支对齐）。同文件getOrAddManager/deleteDepartment/moveDepartment均已有同型防护。
 */
@Fast
public class TestDepartmentTreeRootGuards {

	// 与TestQueueCompatible的500+、TestDelayRemoveOnTimer的600+、TestLinkedMapBrokenData的
	// 700+、TestDepartmentTreeManagerGuards的730+错开。
	private static final AtomicInteger NextServerId = new AtomicInteger(733);

	private DepartmentTree.Module departmentTreeModule;

	private Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(NextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("dept_tree_root_guards_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		var app = new Application("TestDepartmentTreeRootGuards" + conf.getServerId(), conf);
		var linkedMapModule = new LinkedMap.Module(app);
		departmentTreeModule = new DepartmentTree.Module(app, linkedMapModule);
		return app;
	}

	@Test
	public void testPermissionChecksOnMissingRoot() throws Exception {
		var app = newApp();
		try {
			app.start();
			// 不调用tree.create()：根行缺失（等价于已destroy后访问）
			var tree = departmentTreeModule.open("rootGuards1",
					TestDepartmentTreeManagerGuards.Manager.class, TestDepartmentTreeManagerGuards.Member.class,
					TestDepartmentTreeManagerGuards.DepartmentMember.class,
					TestDepartmentTreeManagerGuards.GroupData.class,
					TestDepartmentTreeManagerGuards.DepartmentData.class);

			var expected = departmentTreeModule.errorCode(DepartmentTree.Module.ErrorDepartmentNotExist);
			var out = new long[2];
			run(app, "checkManagePermission.root.missing", () -> {
				out[0] = tree.checkManagePermission("acc1", 0);
				return 0L;
			});
			run(app, "checkParentManagePermission.root.missing", () -> {
				out[1] = tree.checkParentManagePermission("acc1", 0);
				return 0L;
			});
			Assertions.assertNotEquals(0L, expected, "前置：ErrorDepartmentNotExist必须是非0错误码");
			// 修复前：两处均裸NPE（getRoot()为null直接解引用）
			assertEquals(expected, out[0], "checkManagePermission对根行缺失必须返回ErrorDepartmentNotExist而非NPE");
			assertEquals(expected, out[1], "checkParentManagePermission对根行缺失必须返回ErrorDepartmentNotExist而非NPE");
		} finally {
			app.stop();
		}
	}

	private static void run(Application app, String name, FuncLong action) {
		var rc = TaskSpec.ofProcedure(app.newProcedure(action, name)).call();
		Assertions.assertEquals(0L, rc);
	}
}
