package Zeze.Collections;

import demo.App;
import demo.Bean1;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * FND12 coll-01回归：Queue.Module.openCsQueue原在queues.computeIfAbsent的映射
 * 函数内经CsQueue构造函数对同一个map嵌套_open(name@serverId)，两键哈希到同一桶
 * 时JDK对ReservationNode抛IllegalStateException("Recursive update")，且同名重试
 * 确定性复现——特定队列名永久无法打开（同型缺陷判例：LinkedMap.openConcurrent，
 * 其注释记载了同一个坑）。修复=先注册内层键再进外层computeIfAbsent：碰撞桶非空
 * 不装ReservationNode，嵌套查询走可重入链遍历直接命中。
 * 修复前大量开名必命中桶碰撞（JDK实测约6.5%名字，随表增长递减），修复后必须
 * 全部成功；8000个名字跨多个表扩容阶段，兼顾捕捉率与用例时长。
 */
public class TestOpenCsQueueNoRecursiveUpdate {
	@BeforeEach
	public final void testInit() throws Exception {
		demo.App.getInstance().Start();
	}

	@Test
	public void testOpenManyNamesNoRecursiveUpdate() {
		var qm = App.getInstance().Zeze.getQueueModule();
		for (int i = 0; i < 8000; i++) {
			var name = "Fnd12Coll01#" + i;
			var csq = qm.openCsQueue(name, Bean1.class);
			Assertions.assertEquals(name, csq.getName());
		}
	}
}
