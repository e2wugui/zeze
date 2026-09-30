package Zeze.Util;

import java.util.ArrayList;
import java.util.HashMap;
import harness.Fast;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * JsonReader.next() 输入耗尽哨兵回归：空/全空白/纯注释输入直接打穿解析链——
 * next() 无越界防护，buf[pos] 抛 ArrayIndexOutOfBoundsException（Index 0 out
 * of bounds for length 0）。调用面全是"期待定界符否则宽容回退"形态（parse0 的
 * next()!='{' 返回默认对象、parseArray0/parseMap0 同理），但耗尽根本到不了比较。
 * 生产实证（2026-09-30 IDEA 全量轮）：/api/search 空 body（Content-Length: 0）
 * → Json.parse AIOOBE → 处理器 catch 兜成 system error 200 + ERROR 日志整栈，
 * 客户端错误被伪装成服务端错误。
 * 修复：next() 耗尽返回 NUL(0) 哨兵——实字节 0x00 本被当空白跳过，永不为真实
 * token，无碰撞；耗尽与"非定界符垃圾"统一走宽容回退（默认对象/空容器）。
 */
@Fast
public final class TestJsonEmptyInputLenientDefaults {

	static class Bean {
		int value;
		String name;
	}

	/** 空/空白/纯注释输入：bean 解析返回默认对象（与既有的非'{'垃圾输入宽容契约一致），不抛。 */
	@Test
	public void testEmptyInputParsesToDefaults() {
		for (var s : new String[] {"", "   ", "\t\r\n ", "/* comment only */", "  // line comment\n"}) {
			var b = assertDoesNotThrow(() -> Json.parse(s, Bean.class), "输入[" + s + "]不得抛AIOOBE");
			assertNotNull(b, "宽容回退返回默认对象而非null: [" + s + "]");
			assertEquals(0, b.value);
			assertNull(b.name);
		}
	}

	/** 容器路径同款：空输入回退空容器，不抛。 */
	@Test
	public void testEmptyInputParsesToEmptyContainers() {
		var list = assertDoesNotThrow(() -> Json.parse("", ArrayList.class));
		assertNotNull(list);
		var map = assertDoesNotThrow(() -> Json.parse("  ", HashMap.class));
		assertNotNull(map);
	}

	/** keyed 路径（keyReaderMap：String/Integer/Object…）：空输入无 token 可读，回退 null 不抛。 */
	@Test
	public void testEmptyInputKeyedReaderPath() {
		assertNull(assertDoesNotThrow(() -> Json.parse("", Object.class)),
				"Object.class 注册为 parseStringKey（Json.java keyReaderMap），耗尽须在分发点回退");
		assertNull(assertDoesNotThrow(() -> Json.parse("", String.class)));
		assertNull(assertDoesNotThrow(() -> Json.parse("  ", Integer.class)));
	}

	/** 边界对照：非'{'垃圾输入的宽容回退是既有契约，本修复不得改变其行为。 */
	@Test
	public void testGarbageInputContractUnchanged() {
		var b = Json.parse("x", Bean.class);
		assertNotNull(b);
		assertEquals(0, b.value);
		// 正常解析不受影响。
		var ok = Json.parse("{\"value\":42,\"name\":\"n\"}", Bean.class);
		assertEquals(42, ok.value);
		assertEquals("n", ok.name);
	}
}
