package UnitTest.Zeze.Transaction;

import Zeze.Transaction.Collections.Meta2;
import Zeze.Transaction.GTable.BeanMap1;
import Zeze.Transaction.GTable.BeanMap2;
import demo.Bean1;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND3-07 回归：gtable 行 bean（BeanMap1/BeanMap2）的 variables() 必须从 meta 推导
 * 真实的列 key/value 类型（schema 名），与生成器 GetTypeFullName 的输出对齐。
 * 修复前 BeanMap1 硬编码 "int","int"，BeanMap2 值类型抄自 MQ 模板
 * "Zeze.Builtin.MQ.BOptions"——schema 写 gtable[string, long, Bean1] 时报告的全是假类型。
 */
@Fast
public class TestGTableVariablesMeta {

	@Test
	public void testBeanMap1ReportsRealTypes() {
		var bm = new BeanMap1<>(Meta2.getMap1Meta(String.class, Long.class));
		var d = bm.variables().get(0);
		Assertions.assertEquals(1, d.getId());
		Assertions.assertEquals("Map1", d.getName());
		Assertions.assertEquals("map", d.getType());
		Assertions.assertEquals("string", d.getKey());
		Assertions.assertEquals("long", d.getValue());
	}

	@Test
	public void testBeanMap2ReportsRealTypes() {
		var bm = new BeanMap2<>(Meta2.getMap2Meta(Integer.class, Bean1.class));
		var d = bm.variables().get(0);
		Assertions.assertEquals("map", d.getType());
		Assertions.assertEquals("int", d.getKey());
		Assertions.assertEquals("demo.Bean1", d.getValue()); // bean 用 schema 全名（==Java 类名）
	}
}
