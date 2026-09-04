package UnitTest.Zeze.Collections;

import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.Collections.DepartmentTree;
import Zeze.Collections.LinkedMap;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Util.FuncLong;
import Zeze.Util.OutLong;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND2-C0-3回归：getOrAddManager/deleteManager对部门行缺失的防护。
 * c97e4c566修了create/delete/moveDepartment的同族场景，但管理员增删入口仍是裸NPE：
 * 现约定getOrAddManager抛带语义IllegalArgumentException（与getDepartmentMembers的
 * not found处理对齐），deleteManager按"没有这个管理员"返回null（删除幂等）。
 */
@Fast
public class TestDepartmentTreeManagerGuards {

	// 与TestQueueCompatible的500+、TestDelayRemoveOnTimer的600+、TestLinkedMapBrokenData的700+错开。
	private static final AtomicInteger NextServerId = new AtomicInteger(730);

	// 最小Bean：仅需encode/decode（typeId默认按类名hash）。
	public static class Manager extends Bean {
		@Override
		public void encode(@NotNull ByteBuffer bb) {
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
		}
	}

	public static class Member extends Bean {
		@Override
		public void encode(@NotNull ByteBuffer bb) {
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
		}
	}

	public static class DepartmentMember extends Bean {
		@Override
		public void encode(@NotNull ByteBuffer bb) {
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
		}
	}

	public static class GroupData extends Bean {
		@Override
		public void encode(@NotNull ByteBuffer bb) {
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
		}
	}

	public static class DepartmentData extends Bean {
		@Override
		public void encode(@NotNull ByteBuffer bb) {
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
		}
	}

	private static final long MissingDepartmentId = 999_999;

	private DepartmentTree.Module departmentTreeModule;

	private Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(NextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("dept_tree_manager_test_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		var app = new Application("TestDepartmentTreeManagerGuards" + conf.getServerId(), conf);
		// 模块必须在start之前注册（demo.App同序）
		var linkedMapModule = new LinkedMap.Module(app);
		departmentTreeModule = new DepartmentTree.Module(app, linkedMapModule);
		return app;
	}

	private static void run(Application app, String name, FuncLong action) {
		var rc = TaskSpec.ofProcedure(app.newProcedure(action, name)).call();
		Assertions.assertEquals(0L, rc);
	}

	@Test
	public void test1_ManagerGuardsOnMissingDepartment() throws Exception {
		var app = newApp();
		try {
			app.start();
			var tree = departmentTreeModule.open("guards1",
					Manager.class, Member.class, DepartmentMember.class, GroupData.class, DepartmentData.class);
			run(app, "create", () -> {
				tree.create();
				return 0L;
			});

			// getOrAddManager(不存在部门)：抛带语义IAE而非裸NPE
			var caught = new Throwable[1];
			run(app, "getOrAddManager.missing", () -> {
				try {
					tree.getOrAddManager(MissingDepartmentId, "acc1");
				} catch (Throwable e) {
					caught[0] = e;
				}
				return 0L;
			});
			Assertions.assertTrue(caught[0] instanceof IllegalArgumentException,
					"getOrAddManager对缺失部门必须抛IllegalArgumentException，实际: " + caught[0]);
			Assertions.assertTrue(caught[0].getMessage().contains("not found"),
					"异常消息必须带语义，实际: " + caught[0].getMessage());

			// deleteManager(不存在部门)：返回null（删除幂等，不抛）
			run(app, "deleteManager.missing", () -> {
				Assertions.assertNull(tree.deleteManager(MissingDepartmentId, "acc1"),
						"deleteManager对缺失部门必须返回null而非NPE");
				return 0L;
			});

			// 正常路径回归：真实部门上增删管理员不受影响
			var out = new OutLong();
			run(app, "createDepartment", () -> tree.createDepartment(0, "d1", 100, out));
			var departmentId = out.value;
			run(app, "getOrAddManager.ok", () -> {
				Assertions.assertNotNull(tree.getOrAddManager(departmentId, "acc1"));
				return 0L;
			});
			run(app, "deleteManager.ok", () -> {
				var m = tree.deleteManager(departmentId, "acc1");
				Assertions.assertNotNull(m, "已添加的管理员必须能删除");
				Assertions.assertNull(tree.deleteManager(departmentId, "acc1"), "重复删除返回null");
				return 0L;
			});
		} finally {
			app.stop();
		}
	}
}
