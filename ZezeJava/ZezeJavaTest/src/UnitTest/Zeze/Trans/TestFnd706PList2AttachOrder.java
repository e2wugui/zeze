package UnitTest.Zeze.Trans;

import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Config;
import Zeze.Util.TaskSpec;
import demo.Bean1;
import demo.Module1.BValue;
import demo.Module1.Table1;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-06 回归：PList2.set/add(index) 先 initRootInfoWithRedo 后越界检查。
 * 挂接直接改写bean归属且不受事务回滚保护：越界IOOBE后调用方catch继续时，item携带
 * 脏归属——复用（再加入任何容器）抛HasManagedException，原位字段修改的日志被encode期
 * 静默丢弃。修复：先按getList()验界（set要求0<=index<size，add要求0<=index<=size）
 * 再挂接，异常类型仍为IOOBE（FND6-02"先验后挂"判例的越界维度）。
 */
@Fast
public class TestFnd706PList2AttachOrder {
	// 独立serverId隔离本地zeze_cache目录与其他@Fast测试（T1组：130起）。
	private static final AtomicInteger nextServerId = new AtomicInteger(130);

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(nextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("t1_fnd706_plist2_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf); // Memory库，独立url=独立存储
		var app = new Application("TestFnd706PList2AttachOrder@" + conf.getServerId(), conf);
		// Table1是关系映射表，open需要Schemas（对齐TestDynamicBeanCollect的搭建方式）。
		app.setSchemas(new demo.Schemas());
		app.addTable(conf.getTableConf("demo_Module1_Table1").getDatabaseName(), new Table1());
		app.start();
		return app;
	}

	@Test
	public void testOutOfBoundsCheckedBeforeAttach() throws Exception {
		var app = newApp();
		var table = (Table1)app.getTable("demo_Module1_Table1");
		Assertions.assertNotNull(table);
		try {
			// 建记录：list9托管，size=1。
			Assertions.assertEquals(0L, TaskSpec.ofProcedure(app.newProcedure(() -> {
				var v = new BValue();
				v.getList9().add(new Bean1());
				table.insert(1L, v);
				return 0L;
			}, "TestFnd706.setup")).call());

			// 越界set/add：IOOBE必须抛出，且item不得被挂接（调用方catch后继续，事务正常提交）。
			final var items = new Bean1[4];
			final var caught = new int[] {0, 0, 0, 0};
			Assertions.assertEquals(0L, TaskSpec.ofProcedure(app.newProcedure(() -> {
				var list = table.getOrAdd(1L).getList9();
				var size = list.size();
				for (var i = 0; i < 4; i++)
					items[i] = new Bean1();
				try {
					list.set(size, items[0]); // ==size，越上界
				} catch (IndexOutOfBoundsException e) {
					caught[0]++;
				}
				try {
					list.set(-1, items[1]); // 越下界
				} catch (IndexOutOfBoundsException e) {
					caught[2]++;
				}
				try {
					list.add(size + 1, items[2]); // >size，越上界
				} catch (IndexOutOfBoundsException e) {
					caught[1]++;
				}
				try {
					list.add(-1, items[3]); // 越下界
				} catch (IndexOutOfBoundsException e) {
					caught[3]++;
				}
				return 0L;
			}, "TestFnd706.oob")).call());

			Assertions.assertEquals(1, caught[0], "set(size)必须抛IOOBE");
			Assertions.assertEquals(1, caught[1], "add(size+1)必须抛IOOBE");
			Assertions.assertEquals(1, caught[2], "set(-1)必须抛IOOBE");
			Assertions.assertEquals(1, caught[3], "add(-1)必须抛IOOBE");
			for (var i = 0; i < 4; i++)
				Assertions.assertFalse(items[i].isManaged(),
						"越界set/add的item不得携带脏归属（isManaged必须为false），item[" + i + "]");

			// 脏归属的实质危害验证：未被挂接的item可以正常复用（加入容器不得抛HasManagedException）。
			final var sizeAfterReuse = new int[] {-1};
			Assertions.assertEquals(0L, TaskSpec.ofProcedure(app.newProcedure(() -> {
				table.getOrAdd(1L).getList9().add(items[0]); // 复用越界set的item
				sizeAfterReuse[0] = table.getOrAdd(1L).getList9().size();
				return 0L;
			}, "TestFnd706.reuse")).call());
			Assertions.assertEquals(2, sizeAfterReuse[0]);

			// 合法index行为不变：set返回旧值，add中间插入。
			final var oldSet = new Bean1[] {null};
			final var sizeAfterLegal = new int[] {-1};
			Assertions.assertEquals(0L, TaskSpec.ofProcedure(app.newProcedure(() -> {
				var list = table.getOrAdd(1L).getList9();
				oldSet[0] = list.set(0, new Bean1());
				list.add(1, new Bean1());
				sizeAfterLegal[0] = list.size();
				return 0L;
			}, "TestFnd706.legal")).call());
			Assertions.assertNotNull(oldSet[0]);
			Assertions.assertEquals(3, sizeAfterLegal[0]);
		} finally {
			app.stop();
		}
	}
}
