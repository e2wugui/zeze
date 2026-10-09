package Zeze.Util;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 普通数组字段写读对称：写侧输出数组格式（[1,2]），读侧此前把数组归类
 * TYPE_CUSTOM只收'{'——解析返回正常bean但数组字段静默保留构造器默认值，
 * 新数组数据全部丢失；null数组字段（无初值）还因数组abstract修饰符报
 * InstantiationException。读侧按元素类型适配（Array.newInstance），
 * 返回新数组替换字段。
 */
@Fast
public class TestJsonArrayFieldRoundTrip {

	public static class ArraysBean {
		public int[] ints = {9};
		public String[] names = {"old"};
		public long[] longs = {7L};
		public double[] doubles = {1.5};
		public boolean[] flags = {false};
		public byte[] bytes = {3};
	}

	public static class NullArraysBean {
		public int[] ints;
		public String[] names;
	}

	@Test
	public void writableArrayFieldsAreNowReadable() throws Exception {
		var original = new ArraysBean();
		original.ints = new int[]{1, 2};
		original.names = new String[]{"new"};
		original.longs = new long[]{Long.MAX_VALUE};
		original.doubles = new double[]{2.25};
		original.flags = new boolean[]{true, false};
		original.bytes = new byte[]{1, 2, 3};

		var json = Json.toCompactString(original);
		var parsed = new JsonReader().buf(json).parse(ArraysBean.class);

		assertNotNull(parsed);
		assertArrayEquals(new int[]{1, 2}, parsed.ints, "int[]字段必须读入（此前静默保留默认值[9]）");
		assertArrayEquals(new String[]{"new"}, parsed.names, "String[]字段必须读入（此前保留[old]）");
		assertArrayEquals(new long[]{Long.MAX_VALUE}, parsed.longs);
		assertArrayEquals(new double[]{2.25}, parsed.doubles, 0.0);
		assertArrayEquals(new boolean[]{true, false}, parsed.flags);
		assertArrayEquals(new byte[]{1, 2, 3}, parsed.bytes);
	}

	@Test
	public void nullArrayFieldReadsIntoFreshArray() throws Exception {
		var parsed = new JsonReader().buf("{ints:[4,5],names:[\"x\"]}").parse(NullArraysBean.class);
		assertArrayEquals(new int[]{4, 5}, parsed.ints, "null数组字段读入新数组（此前报abstract field）");
		assertArrayEquals(new String[]{"x"}, parsed.names);
	}

	@Test
	public void explicitNullKeepsFieldNull() throws Exception {
		var parsed = new JsonReader().buf("{ints:null}").parse(NullArraysBean.class);
		assertNull(parsed.ints, "显式null保持null");
	}

	@Test
	public void emptyArrayAndRoundTripStability() throws Exception {
		var parsed = new JsonReader().buf("{ints:[],names:[]}").parse(NullArraysBean.class);
		assertEquals(0, parsed.ints.length);
		assertEquals(0, parsed.names.length);
		var again = Json.toCompactString(parsed);
		Assertions.assertEquals("{\"ints\":[],\"names\":[]}", again, "空数组往返稳定");
	}
}
