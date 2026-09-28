package Zeze.Util;
import harness.Fast;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import Zeze.Util.JsonReader;

/**
 * FND12 util-01回归：JsonReader.parseString(boolean) 无引号值分支缺多字节UTF-8处理。
 * 快路径newByteString按LATIN1逐字节成串，非ASCII无引号字符串值静默变乱码（张三→å¼ä¸‰）；
 * 同族带引号parseString(e,intern)与键parseStringNoQuot均有(b^'\\')&lt;1慢路径，唯此分支遗漏。
 * 修复：快扫撞负值字节转parseStringUnquotedSlow逐字符解码UTF-8（反斜杠维持字面量语义）。
 * <p>
 * 可达面：仅类型化路径（String字段/List元素/Map值）——动态parse()对无引号值按约定返回
 * null/标量，不走parseString。修复前所有断言得到的是LATIN1乱码串，断言失败。
 */
@Fast
public final class TestFnd12JsonUnquotedUtf8Value {
	static class Bean {
		String name;
		final List<String> list = new ArrayList<>();
		final Map<String, String> map = new LinkedHashMap<>();
	}

	private static Bean parse(String json) throws ReflectiveOperationException {
		return JsonReader.local().buf(json).parse(Bean.class);
	}

	@Test
	public void testUnquotedUtf8Value() throws ReflectiveOperationException {
		// 3字节（CJK）+ 各终止符：'}'、','、'\n'、']'
		assertEquals("张三", parse("{\"name\":张三}").name);
		assertEquals("张三", parse("{\"name\":张三,\"x\":1}").name);
		assertEquals("张三", parse("{\"name\":张三\n}").name);
		assertEquals("张三", parse("{\"name\":张三}").name);

		// 混合宽度：é=2字节、张=3字节、😀=4字节（代理对），ASCII前后缀保持
		assertEquals("café张三😀ok", parse("{name:café张三😀ok}").name);

		// 末尾\r剥离（\r\n行尾）：终止符'\n'前的内容不带上\r
		assertEquals("张三", parse("{\"name\":张三\r\n,\"x\":1}").name);

		// typed集合元素路径（parseString经List<String>元素）
		var listBean = parse("{\"list\":[张三,李四,café😀]}");
		assertEquals(List.of("张三", "李四", "café😀"), listBean.list);

		// typed Map值路径
		var mapBean = parse("{\"map\":{\"a\":张三,\"b\":café😀}}");
		assertEquals(Map.of("a", "张三", "b", "café😀"), mapBean.map);

		// 反斜杠为字面量：无引号值无转义语义（修复前后一致，钉住语义不被顺手改掉）
		assertEquals("b\\c", parse("{name:b\\c}").name);

		// 带引号与纯ASCII无引号值不受影响（快路径原样）
		assertEquals("张三", parse("{\"name\":\"张三\"}").name);
		assertEquals("abc", parse("{name:abc}").name);
	}
}
