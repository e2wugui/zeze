package UnitTest.Zeze.Collections;

import java.util.HashSet;
import UnitTest.Zeze.BMyBean;
import demo.App;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestCHashMap {
	@BeforeEach
	public final void testInit() throws Exception {
		demo.App.getInstance().Start();
	}

	@Test
	public void test1_PutGetWalkClear() throws Exception {
		var map = App.Instance.LinkedMapModule.openConcurrent("testCHashMap", BMyBean.class);
		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			map.clear(); // 幂等
			for (int i = 0; i < 100; i++) {
				var b = new BMyBean();
				b.setI(i);
				map.put(String.valueOf(i), b);
			}
			return 0;
		}, "test1.put").call());

		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			Assertions.assertEquals(100, map.size()); // 提交时刷新的分片缓存计数
			for (int i = 0; i < 100; i++)
				Assertions.assertEquals(i, map.get(String.valueOf(i)).getI());
			return 0;
		}, "test1.verify").call());

		var ids = new HashSet<String>();
		map.walk((k, v) -> ids.add(k));
		Assertions.assertEquals(100, ids.size());

		// clear：分片计数缓存必须同步归零，walk为空
		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			map.clear();
			return 0;
		}, "test1.clear").call());

		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			Assertions.assertEquals(0, map.size(), "clear后分片sizes缓存必须归零");
			Assertions.assertNull(map.get("5"));
			Assertions.assertTrue(map.isEmpty());
			return 0;
		}, "test1.verifyClear").call());

		var walked = new HashSet<String>();
		map.walk((k, v) -> walked.add(k));
		Assertions.assertTrue(walked.isEmpty());

		// clear后重建
		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			var b = new BMyBean();
			b.setI(999);
			map.put("5", b);
			return 0;
		}, "test1.rebuild").call());
		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			Assertions.assertEquals(999, map.get("5").getI());
			Assertions.assertEquals(1, map.size());
			return 0;
		}, "test1.verifyRebuild").call());
	}

	@Test
	public void test2_WalkEarlyStop() throws Exception {
		var map = App.Instance.LinkedMapModule.openConcurrent("testCHashMap2", BMyBean.class);
		Assertions.assertEquals(0, App.Instance.Zeze.newProcedure(() -> {
			map.clear();
			for (int i = 0; i < 50; i++) {
				var b = new BMyBean();
				b.setI(i);
				map.put(String.valueOf(i), b);
			}
			return 0;
		}, "test2.put").call());

		// 早停：handle返回false后不得继续进入下一个分片
		var visited = new int[1];
		var r = map.walk((k, v) -> {
			visited[0]++;
			return false;
		});
		Assertions.assertEquals(1, visited[0], "早停后必须立即停止，不得跨分片继续");
		Assertions.assertEquals(1, r);
	}
}
