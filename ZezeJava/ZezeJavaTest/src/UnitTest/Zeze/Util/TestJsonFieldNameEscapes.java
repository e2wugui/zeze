package UnitTest.Zeze.Util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import Zeze.Util.Json;
import Zeze.Util.JsonWriter;
import harness.Fast;
import org.junit.jupiter.api.Test;

/**
 * FND4-21 回归：bean字段名写出的预留上界。
 * write(byte[],noQuote)内部不做ensure（依赖调用方预留），转义字符每字节最多
 * 输出6字节（反斜杠u00XX形式）；bean字段名路径预留name.length+3，而Map键与字符串值路径
 * （同文件4处判例）均为length*6+3。自定义Json.fieldNameFilter返回含控制符的
 * 名字时实际输出6倍于预留——写满当前4096块后buf[pos++]越界（AIOOBE）。
 * 700个0x01：旧预留703放行，实际输出4200，确定性越界。
 */
@Fast
public class TestJsonFieldNameEscapes {
	public static class WeirdBean {
		public int value = 7;
	}

	@Test
	public void testEscapedFieldNameNoOverflow() {
		var json = new Json();
		json.fieldNameFilter = (klass, field) -> "\u0001".repeat(700);
		var out = new JsonWriter().write(json, new WeirdBean()).toString();

		// 未越界（修复前此处已抛ArrayIndexOutOfBoundsException），且输出完整：
		// 700个\u0001转义 + "":7}
		assertEquals(700, countOccurrences(out, "\\u0001"), "700个控制字符全部转义输出");
		assertTrue(out.endsWith(":7}"), "名字后接值与结束括号，输出未被截断");
	}

	private static int countOccurrences(String s, String token) {
		int count = 0;
		for (int i = s.indexOf(token); i >= 0; i = s.indexOf(token, i + token.length()))
			count++;
		return count;
	}
}
