package Zeze.Transaction;

import java.util.ArrayList;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** 剖析输出应完整遍历上下文树，每个节点只输出一次。 */
@Fast
public class TestProfilerContextTraversal {
	@Test
	public void testNestedContextsAndSiblingsAreRenderedExactlyOnceInOrder() {
		var transaction = Transaction.create(new Locks());
		try {
			var profiler = transaction.profiler;
			var procedureName = getClass().getName() + ".contextTraversal";
			var now = System.nanoTime();
			profiler.onProcedureEnd(procedureName, now, 3_000_000_000L);
			profiler.onProcedureBegin(procedureName, now);
			var names = new ArrayList<String>();
			names.add("context-root");
			try (var root = Profiler.begin(names.getFirst())) {
				Assertions.assertNotNull(root, "必须实际开启当前事务的剖析记录");
				var nested = new Profiler.Context[7];
				try {
					for (var i = 0; i < nested.length; i++) {
						var name = "context-depth-" + (i + 1);
						names.add(name);
						nested[i] = Profiler.begin(name);
						Assertions.assertNotNull(nested[i]);
					}
				} finally {
					for (var i = nested.length - 1; i >= 0; i--) {
						if (nested[i] != null)
							nested[i].close();
					}
				}
				names.add("context-sibling-branch");
				try (var branch = Profiler.begin(names.getLast())) {
					Assertions.assertNotNull(branch);
					// 递归返回后既不能重访深层节点，也不能跳过兄弟分支的子节点。
					names.add("context-sibling-leaf");
					try (var leaf = Profiler.begin(names.getLast())) {
						Assertions.assertNotNull(leaf);
					}
				}
			}
			names.add("context-top-sibling");
			try (var sibling = Profiler.begin(names.getLast())) {
				Assertions.assertNotNull(sibling);
			}

			var output = profiler.toString();
			// 极慢测试环境下 Context 可附加栈；只计上下文行，不计栈帧。
			var lines = output.lines().filter(line -> names.stream()
					.anyMatch(name -> line.endsWith(" " + name))).toList();
			for (var name : names) {
				Assertions.assertEquals(1L, lines.stream().filter(line -> line.endsWith(" " + name)).count(),
						"每个上下文只能输出一次：" + name + "\n" + output);
			}
			Assertions.assertEquals(names, lines.stream()
					.map(line -> line.substring(line.lastIndexOf(' ') + 1)).toList(),
					"递归遍历必须保留所有嵌套节点和兄弟节点的顺序");
			Assertions.assertFalse(lines.get(8).startsWith("    "), "根内兄弟分支应返回第二层缩进");
			Assertions.assertTrue(lines.get(8).startsWith("  "));
			Assertions.assertTrue(lines.get(9).startsWith("    "), "兄弟分支的子节点应使用第三层缩进");
			Assertions.assertFalse(lines.get(10).startsWith(" "), "根外兄弟应返回顶层缩进");
		} finally {
			Transaction.destroy();
		}
	}
}
