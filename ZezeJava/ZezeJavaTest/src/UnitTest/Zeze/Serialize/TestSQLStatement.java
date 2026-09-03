package UnitTest.Zeze.Serialize;
import harness.Fast;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import Zeze.Serialize.SQLStatement;

@Fast
public class TestSQLStatement {
	@Test
	public void testAppendFloatDouble() {
		// 有限值：产出合法的内联数值字面量（REPLACE/UPDATE ... SET col=value 语法可解析）
		var st = new SQLStatement();
		st.appendFloat("f", 1.5f);
		assertEquals("f=1.5", st.getSql().toString());
		st.appendDouble("d", -2.25);
		assertEquals("f=1.5, d=-2.25", st.getSql().toString());
		st.appendFloat("f2", 0.0f);
		st.appendDouble("d2", -0.0);
		assertEquals("f=1.5, d=-2.25, f2=0.0, d2=-0.0", st.getSql().toString());
		assertTrue(st.getParams().isEmpty()); // 数值路径不产生绑定参数

		// NaN/±Infinity：抛明确异常，不产出非法SQL字面量(x=NaN)，且不留半截SQL
		for (var v : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
			var st2 = new SQLStatement();
			var e = assertThrows(IllegalStateException.class, () -> st2.appendFloat("f", v));
			assertTrue(e.getMessage().contains("'f'"));
			assertEquals(0, st2.getSql().length());
		}
		for (var v : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
			var st2 = new SQLStatement();
			assertThrows(IllegalStateException.class, () -> st2.appendDouble("d", v));
			assertEquals(0, st2.getSql().length());
		}
		// 已有前缀时抛错，前缀保持完整不受污染
		var st3 = new SQLStatement();
		st3.appendInt("id", 7);
		assertThrows(IllegalStateException.class, () -> st3.appendFloat("f", Float.NaN));
		assertEquals("id=7", st3.getSql().toString());
	}
}
