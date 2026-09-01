package Benchmark;

import demo.App;
import demo.Bean1;
import harness.Bench;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"unused", "NewClassNamingConvention"})
@Bench
public class BenchCollections {
	private final static int totalCount = 10_0000;

	@BeforeAll
	public static void testInit() throws Exception {
		App.Instance.Start();
		App.Instance.Zeze.newProcedure(() -> {
			App.Instance.demo_Module1.getTable1().getOrAdd(123L);
			return 0;

		}, "createRecordAndLoad").call();
	}

	@Test
	public void testList() {
		var b = new Zeze.Util.Benchmark();
		for (int i = 0; i < totalCount; i++) {
			// 模拟典型应用操作
			App.Instance.Zeze.newProcedure(() -> {
				var record = App.Instance.demo_Module1.getTable1().getOrAdd(123L);
				var list = record.getList9();
				// append to tail
				list.add(new Bean1());
				list.add(new Bean1());
				list.add(new Bean1());
				list.add(new Bean1());
				list.add(new Bean1());
				// insert middle
				list.add(3, new Bean1());
				// set
				list.set(1, new Bean1());
				list.set(3, new Bean1());
				// 有数据提交进去。
				return 0;
			}, "testList").call();
			App.Instance.Zeze.newProcedure(() -> {
				var record = App.Instance.demo_Module1.getTable1().getOrAdd(123L);
				var list = record.getList9();
				// remove tail 故意不用clear。
				while (!list.isEmpty())
					list.removeLast();
				return 0;
			}, "testList").call();
		}
		b.report(this.getClass().getName() + " list", totalCount);
	}

	private int dummy;

	@Test
	public void testMap() {
		var b = new Zeze.Util.Benchmark();
		for (int i = 0; i < totalCount; i++) {
			// 模拟典型应用操作
			App.Instance.Zeze.newProcedure(() -> {
				var record = App.Instance.demo_Module1.getTable1().getOrAdd(123L);
				var map = record.getMap15();
				map.put(2L, 2L);
				map.put(4L, 4L);
				map.put(1L, 1L);
				map.put(3L, 3L);
				map.put(5L, 5L);
				for (long j = 0; j < 100; ++j)
					if (map.get(j) != null)
						++dummy;
				return 0;
			}, "testMap").call();
			App.Instance.Zeze.newProcedure(() -> {
				var record = App.Instance.demo_Module1.getTable1().getOrAdd(123L);
				var map = record.getMap15();
				map.remove(1L);
				map.remove(3L);
				return 0;
			}, "testMap").call();
			App.Instance.Zeze.newProcedure(() -> {
				var record = App.Instance.demo_Module1.getTable1().getOrAdd(123L);
				var map = record.getMap15();
				map.clear();
				return 0;
			}, "testMap").call();
		}
		b.report(this.getClass().getName() + " map", totalCount);
	}

	@Test
	public void testSet() {
		var b = new Zeze.Util.Benchmark();
		for (int i = 0; i < totalCount; i++) {
			// 模拟典型应用操作
			App.Instance.Zeze.newProcedure(() -> {
				var record = App.Instance.demo_Module1.getTable1().getOrAdd(123L);
				var set = record.getSet10();
				set.add(3);
				set.add(1);
				set.add(5);
				set.add(2);
				set.add(4);
				for (int j = 0; j < 100; ++j)
					if (set.contains(j))
						++dummy;
				return 0;
			}, "testSet").call();
			App.Instance.Zeze.newProcedure(() -> {
				var record = App.Instance.demo_Module1.getTable1().getOrAdd(123L);
				var set = record.getSet10();
				set.remove(1);
				set.remove(2);
				return 0;
			}, "testSet").call();
			App.Instance.Zeze.newProcedure(() -> {
				var record = App.Instance.demo_Module1.getTable1().getOrAdd(123L);
				var set = record.getSet10();
				set.clear();
				return 0;
			}, "testSet").call();
		}
		b.report(this.getClass().getName() + " set", totalCount);
	}

	@Test
	public void testSortedmap() {
		var b = new Zeze.Util.Benchmark();
		for (int i = 0; i < totalCount; i++) {
			// 模拟典型应用操作
			App.Instance.Zeze.newProcedure(() -> {
				var record = App.Instance.demo_Module1.getTable1().getOrAdd(123L);
				var sortedmap = record.getSortedmap1();
				sortedmap.put(1, 1);
				sortedmap.put(3, 3);
				sortedmap.put(2, 2);
				sortedmap.put(5, 5);
				sortedmap.put(4, 4);
				for (int j = 0; j < 100; ++j)
					if (sortedmap.get(j) != null)
						++dummy;
				return 0;
			}, "testSortedmap").call();
			App.Instance.Zeze.newProcedure(() -> {
				var record = App.Instance.demo_Module1.getTable1().getOrAdd(123L);
				var sortedmap = record.getSortedmap1();
				sortedmap.remove(1);
				sortedmap.remove(3);
				return 0;
			}, "testSortedmap").call();
			App.Instance.Zeze.newProcedure(() -> {
				var record = App.Instance.demo_Module1.getTable1().getOrAdd(123L);
				var sortedmap = record.getSortedmap1();
				sortedmap.clear();
				return 0;
			}, "testSortedmap").call();
		}
		b.report(this.getClass().getName() + " sortedmap", totalCount);
	}
}
