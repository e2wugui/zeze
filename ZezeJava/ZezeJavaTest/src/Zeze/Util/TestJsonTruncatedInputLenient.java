package Zeze.Util;

import org.jspecify.annotations.Nullable;

import harness.Fast;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * JsonReader 截断/尾部注释输入的 EOF 越界回归（20261020-09）：
 * skipComment 首读、end、skipNext、skipVar（循环头/','快跳/字符串扫描/嵌套扫描）此前
 * 均无 EOF 守卫——"[1 /"、"/"、"[1"、"{"、"[ " 等畸形输入直接 AIOOBE 穿透
 * Json.parse（只捕 ReflectiveOperationException）到达 DbWeb 等 HTTP 入口成 500+栈，
 * 绕过既有 400 "parse value failed!" 分类通道（与在档 /api/search 空 body 事故同形）。
 * 修复口径对齐 next() 的 NUL 哨兵哲学：注释到 EOF 按耗尽、容器截断按闭合定界符宽容收尾。
 */
@Fast
public final class TestJsonTruncatedInputLenient {

	static class Bean {
		int value;
		@Nullable String name;
	}

	/** 全语料不抛（此前各自命中一个无守卫读点）。 */
	@Test
	public void testTruncatedInputsNoThrow() {
		for (var s : new String[] {
				"[1 /", "/", "//c", "/*c", "[1", "{", "[ ", "[1,", "[1, \"ab", "[{", "[1, {2",
				"[\"ab", "{\"a\"", "{\"a\\", // 带引号串/键截断与转义截断
				"[1] /" /* 尾随注释（end路径，仅手工end触达，顶层parse不读尾随） */}) {
			assertDoesNotThrow(() -> Json.parse(s, Bean.class), "输入[" + s + "]不得抛AIOOBE");
			assertDoesNotThrow(() -> Json.parse(s, Object.class), "输入[" + s + "]泛型解析同");
		}
	}

	/** 宽容收尾的值语义：截断容器按闭合处理、纯注释/耗尽回退默认对象。 */
	@Test
	public void testTruncatedLenientValues() {
		assertEquals(java.util.List.of(1), Json.parse("[1 /", Object.class), "值后截断注释→宽容闭合");
		assertEquals(java.util.List.of(1), Json.parse("[1", Object.class), "截断于值后→宽容闭合");
		var bean = Json.parse("/", Bean.class);
		assertNotNull(bean, "纯注释耗尽→默认对象（对齐空输入哨兵契约）");
		assertEquals(0, bean.value);
	}
}
