package Zeze.Util;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 包装类型数组字段的写读对称：元数据(typeMap)接受包装组件、编码与原始数组相同，
 * 但读侧parseArrayElement此前只特判原始类型class——Integer[]等组件落入通用Object
 * 解析得到Long/Double等不兼容对象，Array.set报array element type mismatch，
 * 自己的Writer输出读不回同类型对象。修复：包装组件与字段级TYPE_WRAP分支同语义
 * （null保留、数值转换同原始类型）。byte[]的专用字符串编码不受影响。
 */
@Fast
public class TestJsonBoxedArrayRoundTrip {

	public static class BoxedBean {
		public Integer[] ints;
		public Long[] longs;
		public Double[] doubles;
		public Float[] floats;
		public Short[] shorts;
		public Byte[] bytes;
		public Character[] chars;
		public Boolean[] bools;
		public byte[] blob; // 既有的byte[]字符串编码：同bean共存不受影响
	}

	/** Writer→Reader全组件往返：八种包装类型数组+null元素+空数组+byte[]共存。 */
	@Test
	public void boxedArrayFieldsRoundTrip() throws Exception {
		var original = new BoxedBean();
		original.ints = new Integer[]{1, null, 3};
		original.longs = new Long[]{Long.MAX_VALUE, null};
		original.doubles = new Double[]{2.25, null};
		original.floats = new Float[]{1.5f};
		original.shorts = new Short[]{(short)7};
		original.bytes = new Byte[]{(byte)3};
		original.chars = new Character[]{'A'};
		original.bools = new Boolean[]{true, false, null};
		original.blob = new byte[]{1, 2, 3};

		var json = Json.toCompactString(original);
		var parsed = new JsonReader().buf(json).parse(BoxedBean.class);

		assertNotNull(parsed);
		Assertions.assertArrayEquals(new Integer[]{1, null, 3}, parsed.ints, "Integer[]往返（修复前element type mismatch）");
		Assertions.assertArrayEquals(new Long[]{Long.MAX_VALUE, null}, parsed.longs);
		Assertions.assertArrayEquals(new Double[]{2.25, null}, parsed.doubles);
		Assertions.assertArrayEquals(new Float[]{1.5f}, parsed.floats);
		Assertions.assertArrayEquals(new Short[]{(short)7}, parsed.shorts);
		Assertions.assertArrayEquals(new Byte[]{(byte)3}, parsed.bytes);
		Assertions.assertArrayEquals(new Character[]{'A'}, parsed.chars);
		Assertions.assertArrayEquals(new Boolean[]{true, false, null}, parsed.bools);
		assertArrayEquals(new byte[]{1, 2, 3}, parsed.blob, "byte[]字符串编码不受影响");
	}

	/** null字段/json null数组保持null；空数组往返稳定。 */
	@Test
	public void nullAndEmptyBoxedArrays() throws Exception {
		var parsed = new JsonReader().buf("{ints:[4,5],longs:[]}")
				.parse(new BoxedBean().getClass());
		assertArrayEquals(new Integer[]{4, 5}, parsed.ints);
		Assertions.assertEquals(0, parsed.longs.length, "空数组读入空实例");
		Assertions.assertNull(parsed.doubles, "未出现的字段保持null");

		var explicitNull = new JsonReader().buf("{ints:null}").parse(BoxedBean.class);
		Assertions.assertNull(explicitNull.ints, "显式null保持null");
	}

	/** 手写json直接解析（数字/字符串/布尔/null元素混合形态）。 */
	@Test
	public void handwrittenBoxedArraysParse() throws Exception {
		var parsed = new JsonReader().buf("""
				{ints:[1,null,3],bools:[true,null,false],chars:[65],floats:[2.5]}
				""").parse(BoxedBean.class);
		Assertions.assertArrayEquals(new Integer[]{1, null, 3}, parsed.ints);
		Assertions.assertArrayEquals(new Boolean[]{true, null, false}, parsed.bools);
		Assertions.assertArrayEquals(new Character[]{(char)65}, parsed.chars);
		Assertions.assertArrayEquals(new Float[]{2.5f}, parsed.floats);
	}
}
