package UnitTest.Zeze.Collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import UnitTest.Zeze.BMyBean;
import Zeze.Transaction.Procedure;
import demo.App;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Test;

@SuppressWarnings("DataFlowIssue")
@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestLinkedMap {
	@BeforeEach
	public final void testInit() throws Exception {
		demo.App.getInstance().Start();
	}

	@AfterEach
	public final void testCleanup() throws Exception {
		//demo.App.getInstance().Stop();
	}

	@Test
	public final void test1_LinkedMapPut() {
		var ret = demo.App.getInstance().Zeze.newProcedure(() -> {
			var map = demo.App.getInstance().LinkedMapModule.open("test1", BMyBean.class);
			for (int i = 100; i < 110; i++) {
				var bean = new BMyBean();
				bean.setI(i);
				map.put(i, bean);
			}
			return Procedure.Success;
		}, "test1_LinkedMapPut").call();
		Assertions.assertEquals(Procedure.Success, ret);
	}

	@Test
	public final void test2_LinkedMapGet() {
		var ret = demo.App.getInstance().Zeze.newProcedure(() -> {
			var map = demo.App.getInstance().LinkedMapModule.open("test1", BMyBean.class);
			for (int i = 100; i < 110; i++) {
				var bean = map.get(i);
				Assertions.assertEquals(bean.getI(), i);
			}
			return Procedure.Success;
		}, "test2_LinkedMapGet").call();
		Assertions.assertEquals(Procedure.Success, ret);
	}

	@Test
	public final void test3_LinkedMapWalk() throws Exception {
		var map = demo.App.getInstance().LinkedMapModule.open("test1", BMyBean.class);
		var i = new AtomicInteger(0);
		var arr = Arrays.asList(100, 101, 102, 103, 104, 105, 106, 107, 108, 109);
		Collections.reverse(arr);
		map.walk(((key, value) -> {
			Assertions.assertTrue(i.get() < 10);
			Assertions.assertEquals(value.getI(), (int)arr.get(i.getAndAdd(1)));
			return true;
		}));
		Assertions.assertEquals(10, i.get());
	}

	@Test
	public final void test4_LinkedMapRemove() {
		var ret = demo.App.getInstance().Zeze.newProcedure(() -> {
			var map = demo.App.getInstance().LinkedMapModule.open("test1", BMyBean.class);
			for (int i = 100; i < 110; i++) {
				var bean = map.remove(i);
				Assertions.assertEquals(bean.getI(), i);
			}
			Assertions.assertTrue(map.isEmpty());
			return Procedure.Success;
		}, "test2_LinkedMapRemove").call();
		Assertions.assertEquals(Procedure.Success, ret);
	}

	@Test
	public void test5_PutAndClear() throws Exception {
		var ret = demo.App.getInstance().Zeze.newProcedure(() -> {
			var map = demo.App.getInstance().LinkedMapModule.open("test1", BMyBean.class);
			for (int i = 100; i < 110; i++) {
				var bean = new BMyBean();
				bean.setI(i);
				map.put(i, bean);
			}
			return Procedure.Success;
		}, "test1_LinkedMapPut").call();
		Assertions.assertEquals(Procedure.Success, ret);

		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			App.Instance.LinkedMapModule.open("test1", BMyBean.class).clear();
			return 0;
		}, "clear").call());

		Thread.sleep(2000);
	}

	@Test
	public void test7_MoveKeepsIndexConsistent() throws Exception {
		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			var map = App.Instance.LinkedMapModule.open("testMove", BMyBean.class);
			map.clear(); // 幂等：清掉上次运行可能残留的状态
			for (int i = 0; i < 35; i++) { // nodeSize默认30，35个条目必然跨两个节点
				var bean = new BMyBean();
				bean.setI(i);
				map.put(String.valueOf(i), bean);
			}
			return 0;
		}, "test7.put").call());

		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			var map = App.Instance.LinkedMapModule.open("testMove", BMyBean.class);
			Assertions.assertEquals(35, map.size());
			// 头插模式下node1(先建,持有"0".."29")是尾节点，node2是头节点。
			// moveAhead把"0"从尾节点深处搬到头节点node2，_tValueIdToNodeId必须跟随更新。
			map.moveAhead("0");
			return 0;
		}, "test7.move").call());

		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			var map = App.Instance.LinkedMapModule.open("testMove", BMyBean.class);
			Assertions.assertNotNull(map.get("0"), "move后索引仍指向旧节点，get找不到");
			Assertions.assertEquals(0, map.get("0").getI());
			// put原地更新分支同样走索引，索引不对会抛"NodeId Exist. But Value Not Found."
			var old = map.put("0", new BMyBean());
			Assertions.assertNotNull(old);
			Assertions.assertEquals(35, map.size());
			return 0;
		}, "test7.verify").call());
	}

	@Test
	public void test8_ClearJobRowCleanup() throws Exception {
		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			var map = App.Instance.LinkedMapModule.open("testJobLeak", BMyBean.class);
			map.clear(); // 幂等
			for (int i = 0; i < 35; i++) {
				var bean = new BMyBean();
				bean.setI(i);
				map.put(String.valueOf(i), bean);
			}
			return 0;
		}, "test8.put").call());

		// clear触发delayClearJob（commit后异步逐节点删除），等它跑完
		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			App.Instance.LinkedMapModule.open("testJobLeak", BMyBean.class).clear();
			return 0;
		}, "test8.clear").call());
		Thread.sleep(3000);

		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			Assertions.assertEquals(0, App.Instance.Zeze.getDelayRemove().jobCount(),
					"clear任务跑完后job行必须删除，否则每次clear泄漏一行且重启空跑");
			return 0;
		}, "test8.verifyJobRemoved").call());

		// 空map的clear会提交head=0的job，也必须被立即清掉
		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			App.Instance.LinkedMapModule.open("testJobLeak", BMyBean.class).clear();
			return 0;
		}, "test8.clearEmpty").call());
		Thread.sleep(1500);

		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			Assertions.assertEquals(0, App.Instance.Zeze.getDelayRemove().jobCount(), "空map clear的job行也必须删除");
			return 0;
		}, "test8.verifyEmptyRemoved").call());
	}

	@Test
	public void test9_ClearInvalidatesStaleMappings() throws Exception {
		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			var map = App.Instance.LinkedMapModule.open("testSerial", BMyBean.class);
			map.clear(); // 幂等
			for (int i = 0; i < 40; i++) {
				var bean = new BMyBean();
				bean.setI(i);
				map.put(String.valueOf(i), bean);
			}
			return 0;
		}, "test9.put").call());

		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			App.Instance.LinkedMapModule.open("testSerial", BMyBean.class).clear();
			return 0;
		}, "test9.clear").call());

		// clear返回后、延迟清理任务完成前：所有走索引的操作必须立即一致（旧代映射当作不存在）
		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			var map = App.Instance.LinkedMapModule.open("testSerial", BMyBean.class);
			Assertions.assertNull(map.get("5"), "clear后get旧id必须为null");
			Assertions.assertNull(map.remove("5"), "clear后remove旧id必须为null");
			Assertions.assertEquals(0, map.size(), "clear后size必须为0");
			// clear前已存在的id走getOrAdd必须新建（BMyBean默认i=0），旧代数据(i=6)不得复活
			Assertions.assertEquals(0, map.getOrAdd("6").getI());
			Assertions.assertNotNull(map.get("6"));
			return 0;
		}, "test9.windowVerify").call());

		// 窗口内用旧id重建：数据必须存活。NodeId不复用是内部不变量（delayClearJob按NodeId归属校验依赖它），
		// 其可观察后果就是下面的断言：延迟清理跑完后重建数据不能被误删。
		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			var map = App.Instance.LinkedMapModule.open("testSerial", BMyBean.class);
			var bean = new BMyBean();
			bean.setI(999);
			Assertions.assertNull(map.put("5", bean));
			Assertions.assertEquals(999, map.get("5").getI());
			Assertions.assertEquals(2, map.size());
			return 0;
		}, "test9.rebuild").call());

		// 等延迟清理任务跑完：重建的数据不能被误删（映射已指向新节点，job按NodeId归属校验跳过）
		Thread.sleep(3000);
		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			var map = App.Instance.LinkedMapModule.open("testSerial", BMyBean.class);
			Assertions.assertEquals(999, map.get("5").getI(), "延迟清理不得删除重建数据");
			Assertions.assertNotNull(map.get("6"), "延迟清理不得删除重建数据");
			Assertions.assertEquals(2, map.size());
			Assertions.assertEquals(0, App.Instance.Zeze.getDelayRemove().jobCount());
			return 0;
		}, "test9.afterJob").call());

		var walked = new ArrayList<Integer>();
		App.Instance.LinkedMapModule.open("testSerial", BMyBean.class).walk((k, v) -> walked.add(v.getI()));
		Collections.sort(walked);
		Assertions.assertEquals(List.of(0, 999), walked);
	}

	@Test
	public void test10_DoubleClearAndRebuild() throws Exception {
		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			var map = App.Instance.LinkedMapModule.open("testSerial2", BMyBean.class);
			map.clear(); // 幂等
			for (int i = 0; i < 35; i++) {
				var bean = new BMyBean();
				bean.setI(i);
				map.put(String.valueOf(i), bean);
			}
			return 0;
		}, "test10.put").call());

		// 连续两次clear：代际号连增两次，跨代旧映射都必须失效
		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			var map = App.Instance.LinkedMapModule.open("testSerial2", BMyBean.class);
			map.clear();
			map.clear();
			return 0;
		}, "test10.doubleClear").call());

		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			var map = App.Instance.LinkedMapModule.open("testSerial2", BMyBean.class);
			Assertions.assertNull(map.get("7"));
			Assertions.assertEquals(0, map.size());
			// 双clear后旧id重建
			var bean = new BMyBean();
			bean.setI(777);
			map.put("7", bean);
			Assertions.assertEquals(777, map.get("7").getI());
			Assertions.assertEquals(1, map.size());
			return 0;
		}, "test10.verify").call());

		Thread.sleep(3000);
		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			var map = App.Instance.LinkedMapModule.open("testSerial2", BMyBean.class);
			Assertions.assertEquals(777, map.get("7").getI());
			Assertions.assertEquals(1, map.size());
			Assertions.assertEquals(0, App.Instance.Zeze.getDelayRemove().jobCount(), "两个clear job都必须跑完删行");
			return 0;
		}, "test10.afterJob").call());
	}

	@Test
	public void test11_CraftedJobStateNullNode() throws Exception {
		// 通过addJob用构造的state直接驱动已注册的clear handle（走真实的delayClearJob）：
		// head指向不存在的节点时，null-node分支必须删job行并正常结束循环。
		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			App.Instance.Zeze.getDelayRemove().addJob(
					Zeze.Collections.LinkedMap.Module.eClearJobHandleName,
					new Zeze.Builtin.Collections.LinkedMap.BClearJobState(999999, 999999, "testNoSuchMap"));
			return 0;
		}, "test11.addJob").call());

		Thread.sleep(1500);
		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			Assertions.assertEquals(0, App.Instance.Zeze.getDelayRemove().jobCount(), "null节点分支必须删除job行");
			return 0;
		}, "test11.verify").call());
	}

	@Test
	public void test6_ClearThenPut() throws Exception {
		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			var map = App.Instance.LinkedMapModule.open("test1", BMyBean.class);
			for (int i = 100; i < 110; i++) {
				var bean = new BMyBean();
				bean.setI(i);
				map.put(i, bean);
			}
			return 0;
		}, "test6.put").call());

		// clear和put放在同一个事务内：delayClearJob只能在commit之后启动，稳定覆盖清理窗口。
		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			var map = App.Instance.LinkedMapModule.open("test1", BMyBean.class);
			map.clear();
			var bean = new BMyBean();
			bean.setI(999);
			map.put(100, bean); // clear后立刻用旧id重建，数据必须存活
			return 0;
		}, "test6.clearPut").call());

		var map = App.Instance.LinkedMapModule.open("test1", BMyBean.class);
		var values = new ArrayList<Integer>();
		map.walk((key, value) -> values.add(value.getI()));
		Assertions.assertEquals(List.of(999), values);

		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			Assertions.assertEquals(1, map.size());
			Assertions.assertNotNull(map.get(100));
			Assertions.assertEquals(999, map.get(100).getI());
			return 0;
		}, "test6.verify").call());
	}
}
