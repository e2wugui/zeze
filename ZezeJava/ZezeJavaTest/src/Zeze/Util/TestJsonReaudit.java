package Zeze.Util;
import harness.Fast;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import Zeze.Util.JsonReader;

/**
 * Json家族整体复审（FND12后续）四条修复的钉板：
 * 1) keyHash定向碰撞防护：hash命中后keyEquals按解码语义与字段名UTF-8字节比对；
 * 2) 残缺unicode转义（hex不足4位）：终止符不被当hex吞掉，后续结构不错位；
 * 3) skipVar嵌套扫描引号按原始字节判定（裸控制字节0x02/0x07不再伪装成引号）；
 * 6) 0x十六进制：溢出用double累计+饱和强转（对齐十进制/Infinity策略），非hex字节即词终止
 *    （对齐parseDouble的FND4-13既有语义），不再落入十进制小数分支。
 */
@Fast
public final class TestJsonReaudit {
	static class Bean {
		int authLevel;
		int b;
		String s;
		long lv;
		double dv;
		List<Integer> a;
	}

	private static Bean parse(String json) throws ReflectiveOperationException {
		return JsonReader.local().buf(json).parse(Bean.class);
	}

	@Test
	public void testKeyHashCollisionGuard() throws ReflectiveOperationException {
		// "#1@r8$"与"authLevel"的32位keyHash相同（0xE01E8FB8，MITM离线构造，复刻getKeyHash
		// 的带符号字节折叠）。修复前碰撞key静默命中字段并写入999；修复后按内容比对跳过。
		assertEquals(0, parse("{\"#1@r8$\":999}").authLevel, "hash碰撞key不得命中字段");
		assertEquals(5, parse("{\"authLevel\":5}").authLevel, "正控：正常key命中");
		assertEquals(7, parse("{\"\\u0061uthLevel\":7}").authLevel, "转义key按解码语义命中");
	}

	@Test
	public void testTruncatedUnicodeEscapeKeepsStructure() throws ReflectiveOperationException {
		// 残缺unicode转义（"u12"仅2个hex）：修复前闭引号被扫描当hex吞掉，串越界到下一个引号，
		// "b"键丢失；修复后残缺转义按2字节收尾，终止符即时生效。
		var bean = parse("{\"s\":\"\\u12\",\"b\":1}");
		assertNotNull(bean.s);
		assertEquals(1, bean.b, "残缺unicode转义不得吞掉闭引号致后续结构错位");
	}

	@Test
	public void testSkipVarControlByteNotQuote() throws ReflectiveOperationException {
		// 0x02|0x20=='"'：嵌套扫描误判引号起始，一路吞过],"b":3}直至越界AIOOBE。
		// [1 [2\u0002]]的裸控制字节在写侧恒被转义，仅外部输入可达。
		var json = "{\"a\":[1 [2\u0002]],\"b\":3}";
		var bean = assertDoesNotThrow(() -> parse(json));
		assertEquals(List.of(1), bean.a);
		assertEquals(3, bean.b, "裸控制字节不得被当引号吞掉后续结构");
	}

	@Test
	public void testHexLiteralSemantics() throws ReflectiveOperationException {
		assertEquals(Integer.MAX_VALUE, parse("{\"authLevel\":0xFFFFFFFF}").authLevel,
				"hex溢出饱和（原int环绕成-1）");
		assertEquals(Integer.MIN_VALUE, parse("{\"authLevel\":-0x80000000}").authLevel);
		assertEquals(Long.MAX_VALUE, parse("{\"lv\":0xFFFFFFFFFFFFFFFF}").lv,
				"hex溢出饱和（原long环绕成-1）");
		assertEquals(1L, parse("{\"lv\":0x1.8e2}").lv,
				"hex是整数词法：'.'即词终止（原按十进制小数解析成180）");
		assertEquals(16.0, parse("{\"dv\":0x10}").dv, 0.0);
		assertEquals(1.8446744073709552E19, parse("{\"dv\":0x10000000000000000}").dv, 0.0,
				"hex超long按double累计（原long环绕成0.0）");
	}
}
