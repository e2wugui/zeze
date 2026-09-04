package UnitTest.Zeze.Util;
import harness.Fast;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import Zeze.Util.Json;

/**
 * 无引号 key 的简单转义（非 unicode 转义"反斜杠u+4位hex"形态）必须按"解码一次"计算
 * hash：转义字母本身不得被二次掺入 hash（见 FND2-U1-1，8ef08c5db 镜像改写引入的回归：
 * 非 'u' 分支掺 hash 后 pos 不前进，下方 buf[pos] 把转义字母再读一次）。ESCAPE 表对未知
 * 转义字母（如反斜杠+c）取 lenient 恒等解码，因此 {ab+c 转义形态} 的 key 解码后就是 "abc"。
 */
@Fast
public final class TestJsonUnquotSimpleEscape {
	static class Bean {
		int abc;
		int x;
	}

	@Test
	public void testIdentityEscapeMatches() throws ReflectiveOperationException {
		// \c -> 'c'（lenient 恒等解码），key 解码后为 "abc"；修复前 'c' 被二次掺入
		// hash（H(a,b,c,c)），字段静默丢失
		Bean b = Json.parse("{ab\\c:1}", Bean.class);
		assertNotNull(b);
		assertEquals(1, b.abc);
	}

	@Test
	public void testEscapeAtKeyStart() throws ReflectiveOperationException {
		// 转义出现在 key 首字符（\a 恒等解码为 'a'），key 解码后为 "abc"
		Bean b = Json.parse("{\\abc:2}", Bean.class);
		assertEquals(2, b.abc);
	}

	@Test
	public void testControlEscapeKeySkipped() throws ReflectiveOperationException {
		// \t 解码为 0x09：key 实际是 "a<TAB>bc"，不匹配任何字段应被静默跳过，
		// 且 pos 正确前进、后继字段不受影响（修复前 key hash 错乱但不影响本断言，
		// 本用例锁定"解码一次 + 词法边界正确"的组合行为）
		Bean b = Json.parse("{a\\tbc:1,x:2}", Bean.class);
		assertEquals(0, b.abc);
		assertEquals(2, b.x);
	}

	@Test
	public void testEscapedBackslashConsumedOnce() throws ReflectiveOperationException {
		// \\ 解码为单个反斜杠（key "a\b"）：转义的反斜杠恰好消费一次，
		// 不把第二个 '\' 再当转义起点吞掉 'b' 重解码（修复前 hash 彻底错乱的族路径）
		Bean b = Json.parse("{a\\\\b:1,x:3}", Bean.class);
		assertEquals(0, b.abc);
		assertEquals(3, b.x);
	}

	@Test
	public void testUnicodeEscapeRegression() throws ReflectiveOperationException {
		// 回归：'u' 路径（pos+=5）不受影响
		Bean b = Json.parse("{\\u0061bc:4}", Bean.class);
		assertEquals(4, b.abc);
	}

	public static void main(String[] args) throws ReflectiveOperationException {
		var t = new TestJsonUnquotSimpleEscape();
		t.testIdentityEscapeMatches();
		t.testEscapeAtKeyStart();
		t.testControlEscapeKeySkipped();
		t.testEscapedBackslashConsumedOnce();
		t.testUnicodeEscapeRegression();
		System.out.println(t.getClass().getSimpleName() + ": 5 tests OK!");
	}
}
