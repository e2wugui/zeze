package UnitTest.Zeze.Transaction;

import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.util.ArrayList;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Helper;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.GTable.BeanMap1;
import Zeze.Transaction.GTable.BeanMap2;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-11：BeanMap1/BeanMap2的SQL decode（decodeResultSet→Helper.decodeJsonMap）以
 * {"Map1":json}包装走反射字段名匹配，但行bean字段名经fieldNameFilter后为pMap1/pMap2
 * （filter只剥_前缀），"Map1"查不到字段——整段JSON静默空转，叠加前置map.clear()把现有
 * 数据也清空；且行bean泛型C/V是类型变量，反射路径退化为Map&lt;String,Object&gt;。
 * 修复：按meta的keyClass/valueClass构建FieldMeta直接parseMap0（GTable解析器同款模式），
 * 键值按真实类型解码。
 */
@Fast
public class TestFnd711BeanMapSqlJsonRoundtrip {

	public static class MB extends Bean {
		public long v;
		private transient Object mapKey;

		@Override
		public Object mapKey() {
			return mapKey;
		}

		@Override
		public void mapKey(@NotNull Object mapKey) {
			this.mapKey = mapKey;
		}

		@Override
		public void encode(@NotNull ByteBuffer bb) {
			bb.WriteLong(v);
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
			v = bb.ReadLong();
		}
	}

	/** decodeResultSet唯一依赖getString(String)的最小ResultSet替身。 */
	private static ResultSet resultSet(String map1, String map2) {
		return (ResultSet)Proxy.newProxyInstance(TestFnd711BeanMapSqlJsonRoundtrip.class.getClassLoader(),
				new Class<?>[]{ResultSet.class}, (proxy, method, args) -> {
					switch (method.getName()) {
						case "getString":
							if ("Map1".equals(args[0]))
								return map1;
							if ("Map2".equals(args[0]))
								return map2;
							return null;
						case "hashCode":
							return System.identityHashCode(proxy);
						case "equals":
							return proxy == args[0];
						case "toString":
							return "FakeResultSet";
						default:
							return null;
					}
				});
	}

	@Test
	public void testBeanMap1RoundtripThroughDecodeResultSet() throws Exception {
		var src = new BeanMap1<Long, Float>(Long.class, Float.class);
		src.put(2L, 3.5f);
		src.put(4L, 5.5f);
		var json = Helper.encodeJson(src.getPMap1()); // encodeSQLStatement写入列Map1的内容

		var dst = new BeanMap1<Long, Float>(Long.class, Float.class);
		dst.put(9L, 9.9f); // 现有数据：decode必须是整体替换而不是清空后丢失
		dst.decodeResultSet(new ArrayList<>(), resultSet(json, null));
		Assertions.assertEquals(2, dst.size(), "整段JSON必须恢复（修复前静默丢弃且清空现有数据）");
		Assertions.assertEquals(3.5f, dst.get(2L), "键必须按Long定型解码（修复前反射路径键成String）");
		Assertions.assertEquals(5.5f, dst.get(4L));
		Assertions.assertNull(dst.get(9L), "decode语义为替换，旧键不残留");
	}

	@Test
	public void testBeanMap2RoundtripThroughDecodeResultSet() throws Exception {
		var src = new BeanMap2<Long, MB, MB>(Long.class, MB.class);
		var b1 = new MB();
		b1.v = 7;
		var b2 = new MB();
		b2.v = 8;
		src.put(2L, b1);
		src.put(4L, b2);
		var json = Helper.encodeJson(src.getPMap2()); // encodeSQLStatement写入列Map2的内容

		var dst = new BeanMap2<Long, MB, MB>(Long.class, MB.class);
		dst.decodeResultSet(new ArrayList<>(), resultSet(null, json));
		Assertions.assertEquals(2, dst.size(), "整段JSON必须恢复");
		var got = dst.get(2L);
		Assertions.assertNotNull(got, "键必须按Long定型解码");
		Assertions.assertEquals(7L, got.v, "Bean值字段必须恢复");
		Assertions.assertEquals(8L, dst.get(4L).v);
	}

	@Test
	public void testNullColumnClearsOnly() throws Exception {
		// null列（SQL NULL）语义与decodeJsonMap一致：仅清空，不抛异常。
		var dst = new BeanMap1<Long, Float>(Long.class, Float.class);
		dst.put(9L, 9.9f);
		dst.decodeResultSet(new ArrayList<>(), resultSet(null, null));
		Assertions.assertTrue(dst.isEmpty());

		var dst2 = new BeanMap2<Long, MB, MB>(Long.class, MB.class);
		var b = new MB();
		b.v = 1;
		dst2.put(1L, b);
		dst2.decodeResultSet(new ArrayList<>(), resultSet(null, null));
		Assertions.assertTrue(dst2.isEmpty());
	}

	@Test
	public void testMetaDrivenDecodeDirect() {
		// 不经ResultSet直测Helper.decodeJsonTypedMap：含String键、double值的混合类型。
		var src = new BeanMap1<String, Double>(String.class, Double.class);
		src.put("a", 1.5);
		src.put("b", 2.5);
		var json = Helper.encodeJson(src.getPMap1());

		var dst = new BeanMap1<String, Double>(String.class, Double.class);
		Helper.decodeJsonTypedMap(dst.getPMap1(), dst.getPMap1().getMeta(), json);
		Assertions.assertEquals(2, dst.size());
		Assertions.assertEquals(1.5, dst.get("a"));
		Assertions.assertEquals(2.5, dst.get("b"));
	}
}
