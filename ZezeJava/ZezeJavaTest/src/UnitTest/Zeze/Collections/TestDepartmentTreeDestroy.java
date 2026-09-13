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
 * FND4-83 回归：destroy只删根行（_tDepartment），子部门行（_tDepartmentTree）
 * 与成员数据全部残留；重建同名树时NextDepartmentId归零、新部门从dId=1重新
 * 分配，按(name,dId)命中旧残留行——新旧数据混串。修复：destroy复用
 * deleteDepartment的递归删除（子树+成员清理）后再删根行。
 */
@Fast
public class TestDepartmentTreeDestroy {
	// 与TestDepartmentTreeManagerGuards的730段错开。
	private static final AtomicInteger NextServerId = new AtomicInteger(738);

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

	private DepartmentTree.Module departmentTreeModule;

	private Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(NextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("dept_tree_destroy_test_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		var app = new Application("TestDepartmentTreeDestroy" + conf.getServerId(), conf);
		var linkedMapModule = new LinkedMap.Module(app);
		departmentTreeModule = new DepartmentTree.Module(app, linkedMapModule);
		return app;
	}

	private static void run(Application app, String name, FuncLong action) {
		var rc = TaskSpec.ofProcedure(app.newProcedure(action, name)).call();
		Assertions.assertEquals(0L, rc);
	}

	@Test
	public void testDestroyClearsRootGroupMembers() throws Exception {
		// FND5-42：根级group成员map（"0@"+name）不在子部门dId空间内，destroy曾无任何路径
		// 清理——重建同名树时旧成员全部"复活"（count非零、成员可查）。
		var app = newApp();
		try {
			app.start();
			var tree = departmentTreeModule.open("destroy2",
					Manager.class, Member.class, DepartmentMember.class, GroupData.class, DepartmentData.class);
			run(app, "create+members", () -> {
				tree.create();
				tree.getGroupMembers().put("oldMember", new Member());
				tree.createDepartment(0, "child1", 10, new OutLong());
				tree.getDepartmentMembers(1).put("deptMember", new DepartmentMember());
				return 0L;
			});

			run(app, "destroy", () -> {
				tree.destroy();
				return 0L;
			});

			// 重建同名树：根级成员必须为空（修复前命中旧"0@"map，旧成员复活）。
			run(app, "recreate", () -> {
				tree.create();
				Assertions.assertEquals(0L, tree.getGroupMembers().size(),
						"重建同名树的根级成员必须为空（FND5-42）");
				return 0L;
			});
			// 部门级成员对照（FND4-83已覆盖，随带复核）。
			run(app, "recreateDept", () -> {
				tree.createDepartment(0, "newChild", 10, new OutLong());
				Assertions.assertEquals(0L, tree.getDepartmentMembers(1).size(),
						"重建部门的成员必须为空");
				return 0L;
			});
		} finally {
			try {
				app.stop();
			} catch (Exception ignored) {
			}
		}
	}

	@Test
	public void testDestroyRemovesWholeTree() throws Exception {
		var app = newApp();
		try {
			app.start();
			var tree = departmentTreeModule.open("destroy1",
					Manager.class, Member.class, DepartmentMember.class, GroupData.class, DepartmentData.class);
			run(app, "create", () -> {
				tree.create();
				return 0L;
			});
			run(app, "createD1", () -> {
				Assertions.assertEquals(0L, tree.createDepartment(0, "child1", 10, new OutLong()));
				Assertions.assertEquals(0L, tree.createDepartment(1, "child2", 10, new OutLong())); // 嵌套子部门
				return 0L;
			});

			run(app, "destroy", () -> {
				tree.destroy();
				return 0L;
			});

			Assertions.assertNull(tree.selectDepartmentTreeNode(1), "destroy必须删除子部门行（FND4-83）");
			Assertions.assertNull(tree.selectDepartmentTreeNode(2), "嵌套子部门行同样必须删除");
			Assertions.assertNull(tree.selectRoot(), "根行删除（既有行为）");

			// 重建同名树：新dId=1的部门不得命中旧残留行（孩子集合须为空而非继承旧child2）。
			run(app, "recreate", () -> {
				tree.create();
				var out = new OutLong();
				Assertions.assertEquals(0L, tree.createDepartment(0, "newChild", 10, out));
				Assertions.assertEquals(1L, out.value);
				return 0L;
			});
			var rebuilt = tree.selectDepartmentTreeNode(1);
			Assertions.assertNotNull(rebuilt, "重建后新部门行存在");
			Assertions.assertTrue(rebuilt.getChildren().isEmpty(), "新建部门不得继承旧残留子部门（混串）");
			Assertions.assertEquals(0L, rebuilt.getParentDepartment());
			Assertions.assertEquals("newChild", rebuilt.getName());
		} finally {
			try {
				app.stop();
			} catch (Exception ignored) {
			}
		}
	}
}
