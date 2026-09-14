package UnitTest.Zeze.Util;

import Zeze.Util.Json;
import Zeze.Util.JsonWriter;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND5-46族-FND5-09回归：JsonReader对显式null token，TYPE_STRING/TYPE_OBJECT/
 * 全部TYPE_WRAP_*分支写null，TYPE_CUSTOM（自定义bean字段，无parser路径）却
 * 无条件ctor.create()后parse0——parse0首行next()!='{'原样返回新实例写回字段，
 * null roundtrip后变成非null空对象，与其他分支不一致。
 * 触发面：写侧FLAG_WRITE_NULL+自定义bean字段为null。
 */
@Fast
public class TestJsonNullCustomField {

	public static class Inner {
		public int x;
	}

	public static class Holder {
		public Inner inner;
		public String name = "n";
	}

	@Test
	public void testExplicitNullCustomFieldStaysNull() {
		var holder = Json.parse("{\"inner\":null}", Holder.class);
		Assertions.assertNotNull(holder);
		Assertions.assertNull(holder.inner, "显式null的自定义bean字段必须保持null（FND5-09）");
	}

	@Test
	public void testRoundtripWithWriteNull() {
		var holder = new Holder();
		holder.inner = null; // 显式null
		var jsonStr = JsonWriter.local().clear().setFlags(JsonWriter.FLAG_WRITE_NULL).write(holder).toString();
		Assertions.assertTrue(jsonStr.contains("\"inner\":null"), "写侧必须输出显式null: " + jsonStr);
		var back = Json.parse(jsonStr, Holder.class);
		Assertions.assertNull(back.inner, "roundtrip后null不得变为空对象（FND5-09）");
	}

	@Test
	public void testNonNullCustomFieldUnchanged() {
		var holder = new Holder();
		holder.inner = new Inner();
		holder.inner.x = 42;
		var back = Json.parse(JsonWriter.local().clear().write(holder).toString(), Holder.class);
		Assertions.assertNotNull(back.inner);
		Assertions.assertEquals(42, back.inner.x, "非null自定义字段解析不变");
	}
}
