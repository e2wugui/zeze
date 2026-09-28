package Benchmark;
import harness.Bench;
import harness.MacroBench;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import demo.App;
import org.junit.jupiter.api.Assertions;

// 单线程容器 bean 事务：tMap2Bean1 的 map[int, demo.Bean1]（嵌套 PMap1/PMap2 持久化容器）。
// A/B/C 的单 long 字段不经过 Collections delta-log 路径，本场景补该覆盖：
// 每事务 getOrAdd + 新建 Bean1（含 4 项 PMap1）+ PMap2 put 覆盖既有 entry。
@SuppressWarnings("NewClassNamingConvention")
@Bench
@Tag("core")
public class DBasicContainerTransaction {
	public static final int Keys = 1_000;       // 工作集（默认缓存容量内，全命中）
	public static final int TxnCount = 200_000; // 每轮事务数
	public static final int Warmups = 2;
	public static final int Rounds = 5;

	@Test
	public void testBenchmark() throws Exception {
		App.Instance.Stop();
		App.Instance.Start();
		try {
			MacroBench.run("D_ContainerTransaction", Warmups, Rounds, TxnCount, () -> {
				clearAll();
				for (int i = 0; i < TxnCount; ++i) {
					final int key = i % Keys;
					final int value = i;
					App.Instance.Zeze.newProcedure(() -> add(key, value), "add").call();
				}
			});
			App.Instance.Zeze.newProcedure(DBasicContainerTransaction::check, "check").call();
			clearAll();
		} finally {
			//App.Instance.Stop();
		}
	}

	private static long add(long key, int i) {
		var bean = App.Instance.demo_web.getMap2Bean1().getOrAdd(key);
		var b1 = new demo.Bean1();
		b1.setV1(i);
		for (int j = 0; j < 4; ++j)
			b1.getV2().put(j, i + j);
		bean.getMap2().put((int)key, b1);
		return 0;
	}

	private static long check() {
		int entries = 0;
		for (long key = 0; key < Keys; ++key) {
			var bean = App.Instance.demo_web.getMap2Bean1().getOrAdd(key);
			entries += bean.getMap2().size();
			for (var b1 : bean.getMap2().values())
				Assertions.assertEquals(4, b1.getV2().size());
		}
		Assertions.assertEquals(Keys, entries);
		return 0;
	}

	private static void clearAll() throws Exception {
		App.Instance.Zeze.newProcedure(() -> {
			for (long key = 0; key < Keys; ++key)
				App.Instance.demo_web.getMap2Bean1().remove(key);
			return 0L;
		}, "clear").call();
	}
}
