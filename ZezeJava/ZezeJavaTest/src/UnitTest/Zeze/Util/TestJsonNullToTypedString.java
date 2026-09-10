package UnitTest.Zeze.Util;

import java.util.ArrayList;
import java.util.HashMap;
import Zeze.Util.JsonReader;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND3-08 回归：typed String 上下文（字段/List 元素/Map 值）的 null token 必须解析成
 * null，与 JsonWriter 写出的 null 对称。判空用大小写敏感的首字符：写侧约定字符串值
 * 恒带引号、非有限 double 恒写大写 NaN/Infinity——值位置的小写 'n' 只可能是 null
 * token，与 NaN 在首字符即区分；无引号小写 nan/north 等超出写侧约定，一律吞成 null。
 */
@Fast
public class TestJsonNullToTypedString {

	public static class Bean {
		public String s;
		public double d;
		public Double nd;
		public ArrayList<String> list = new ArrayList<>();
		public HashMap<String, String> map = new HashMap<>();
	}

	private static Bean parse(String json) throws ReflectiveOperationException {
		return JsonReader.local().buf(json).parse(Bean.class);
	}

	@Test
	public void testNullTokenToNull() throws ReflectiveOperationException {
		Assertions.assertNull(parse("{s:null}").s);
		var listBean = parse("{list:[a,null,b]}");
		Assertions.assertEquals(3, listBean.list.size());
		Assertions.assertNull(listBean.list.get(1));
		var mapBean = parse("{map:{k:null}}");
		Assertions.assertNull(mapBean.map.get("k"));
	}

	@Test
	public void testNullDistinguishedFromWriterNaN() throws ReflectiveOperationException {
		// writer 的 NaN/Infinity 恒大写：首字符大小写即与 null 区分，typed/无类型路径均不误判
		Assertions.assertTrue(Double.isNaN(parse("{d:NaN}").d));
		Assertions.assertTrue(parse("{nd:NaN}").nd.isNaN());
		Assertions.assertEquals(Double.POSITIVE_INFINITY, parse("{d:Infinity}").d);
		@SuppressWarnings("unchecked")
		var m = (HashMap<String, Object>)JsonReader.local().buf("{x:NaN,y:null}").parse();
		Assertions.assertTrue(((Double)m.get("x")).isNaN());
		Assertions.assertNull(m.get("y"));
	}

	@Test
	public void testUnquotedLowerNSwallowedAsNull() throws ReflectiveOperationException {
		// 无引号字符串值超出写侧约定（writer 恒带引号）：小写 n 开头一律按 null token 处理
		Assertions.assertNull(parse("{s:nan}").s);
		Assertions.assertNull(parse("{s:north}").s);
		Assertions.assertNull(parse("{map:{j:nan}}").map.get("j"));
		Assertions.assertNull(parse("{list:[nulls]}").list.get(0));
	}
}
