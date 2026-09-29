package Zeze.Transaction;

import Zeze.Transaction.GTable.BeanMap2;
import Zeze.Transaction.GTable.GTable1;
import Zeze.Transaction.GTable.GTable2;
import demo.ModuleGTable.Bean1;
import demo.ModuleGTable.Bean1ReadOnly;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * kimi-audit01 T-28回归：GTable1/GTable2/BeanMap2的assign自赋值清空全部数据。
 * 三处都是clear()后遍历_o_逐项put回：_o_==this时clear后源已空，循环零次，
 * 整表被静默清空并随事务提交持久化（生成代码bean.assign(bean)可传导至此）。
 * 对照BeanMap1.assign经PMap1.assign先快照再清写的自赋值安全语义。
 * 修复：自赋值守卫（同源直接返回）。测试：put数据后assign(this)，数据必须完好
 * （修复前：清空）。
 */
@Fast
public class TestGTableSelfAssignKeepsData {

	@Test
	public void testGTable1SelfAssignKeepsData() {
		var t = new GTable1<Long, Long, Integer>(Long.class, Long.class, Integer.class);
		t.put(1L, 1L, 11);
		t.put(1L, 2L, 22);
		t.put(2L, 1L, 33);
		Assertions.assertEquals(3, t.size());

		t.assign(t); // 自赋值（红：静默清空）

		Assertions.assertEquals(3, t.size(), "自赋值不得清空整表");
		Assertions.assertEquals(11, t.get(1L, 1L));
		Assertions.assertEquals(22, t.get(1L, 2L));
		Assertions.assertEquals(33, t.get(2L, 1L));
	}

	@Test
	public void testGTable2SelfAssignKeepsData() {
		var t = new GTable2<Integer, Integer, Bean1, Bean1ReadOnly>(
				Integer.class, Integer.class, Bean1.class);
		t.put(1, 1, new Bean1());
		t.put(2, 1, new Bean1());
		Assertions.assertEquals(2, t.size());

		t.assign(t); // 自赋值（红：静默清空）

		Assertions.assertEquals(2, t.size(), "自赋值不得清空整表");
		Assertions.assertTrue(t.containsRow(1) && t.containsRow(2));
	}

	@Test
	public void testBeanMap2SelfAssignKeepsData() {
		var m = new BeanMap2<Integer, Bean1, Bean1ReadOnly>(Integer.class, Bean1.class);
		m.put(1, new Bean1());
		m.put(2, new Bean1());
		Assertions.assertEquals(2, m.size());

		m.assign(m); // 自赋值（红：静默清空）

		Assertions.assertEquals(2, m.size(), "自赋值不得清空整表");
	}
}
