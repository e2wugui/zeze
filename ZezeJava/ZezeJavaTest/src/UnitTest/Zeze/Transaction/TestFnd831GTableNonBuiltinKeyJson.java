package UnitTest.Zeze.Transaction;

import java.util.Objects;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Vector2;
import Zeze.Transaction.Bean;
import Zeze.Transaction.BeanKey;
import Zeze.Transaction.Collections.Map1Meta;
import Zeze.Transaction.GTable.BeanMap1;
import Zeze.Transaction.GTable.GTable1;
import Zeze.Transaction.GTable.GTable2;
import Zeze.Net.Binary;
import Zeze.Util.Json;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * FND8-31回归：GTable1/GTable2/decodeJsonTypedMap 手建FieldMeta的keyParser裸取
 * keyReaderMap，BeanKey/binary/decimal/vector等schema合法键不在表内（仅十类内建），
 * 读端首键解析即NPE（ensureNotNull只有assert，生产关闭断言恒等返回null）；
 * 写侧JsonWriter对非内建键整体序列化为带引号JSON串，完全支持——往返不对称。
 * 修复：getKeyReaderOrFallback公共工厂（读回键串再JSON解析进键对象，与
 * Json.ClassMeta构造器原处回退收敛为一份）；孪生：GTable1的fm2.klass由
 * Object.class改为真实valueClass，非内建标量值（binary/vector等）不再被
 * 静默解析成裸空Object。
 * 纯单元：Json往返不依赖应用环境（TestFnd710GTableJsonFactoryInit范式）。
 * 修复前：BeanKey/binary/decimal行键在Json.parse处NPE，binary值往返后变裸Object。
 */
@Fast
public class TestFnd831GTableNonBuiltinKeyJson {

	/** 手写BeanKey判例：值语义equals/hashCode + 无参构造 + 公开字段（生成BeanKey同形态）。 */
	public static final class BDeptKey implements BeanKey {
		public String owner;
		public long deptId;

		public BDeptKey() {
		}

		public BDeptKey(String owner, long deptId) {
			this.owner = owner;
			this.deptId = deptId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof BDeptKey that))
				return false;
			return deptId == that.deptId && Objects.equals(owner, that.owner);
		}

		@Override
		public int hashCode() {
			return Objects.hash(owner, deptId);
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteString(owner);
			bb.WriteLong(deptId);
		}

		@Override
		public void decode(IByteBuffer bb) {
			owner = bb.ReadString();
			deptId = bb.ReadLong();
		}
	}

	/** GTable2的Bean值。 */
	public static final class MBean extends Bean {
		public long v;
		@SuppressWarnings("FieldNameHidesFieldInSuperclass")
		private transient Object mapKey;

		public MBean set(long x) {
			v = x;
			return this;
		}

		@Override
		public Object mapKey() {
			return mapKey;
		}

		@Override
		public void mapKey(@NotNull Object mapKey) {
			this.mapKey = mapKey;
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteLong(v);
		}

		@Override
		public void decode(IByteBuffer bb) {
			v = bb.ReadLong();
		}
	}

	public static class H1 {
		final GTable1<BDeptKey, BDeptKey, Integer> g = new GTable1<>(BDeptKey.class, BDeptKey.class, Integer.class);
	}

	public static class H2 {
		final GTable2<BDeptKey, Long, MBean, MBean> g = new GTable2<>(BDeptKey.class, Long.class, MBean.class);
	}

	public static class H3 {
		final GTable1<Binary, Long, Integer> g = new GTable1<>(Binary.class, Long.class, Integer.class);
	}

	public static class H5 {
		final GTable1<Integer, Long, Binary> g = new GTable1<>(Integer.class, Long.class, Binary.class);
	}

	public static class H6 {
		final GTable1<Integer, Long, Vector2> g = new GTable1<>(Integer.class, Long.class, Vector2.class);
	}

	// BeanKey行键+列键：修复前Json.parse首键NPE。
	@Test
	public void testGTable1BeanKeyRoundTrip() {
		var h = new H1();
		h.g.put(new BDeptKey("alice", 5), new BDeptKey("bob", 6), 42);
		var json = Json.toCompactString(h);
		var parsed = (H1)Json.parse(json, new H1());
		var v = parsed.g.get(new BDeptKey("alice", 5), new BDeptKey("bob", 6));
		assertNotNull(v, "BeanKey行/列键必须恢复");
		assertEquals(42, v);
	}

	// GTable2同型：BeanKey行键。
	@Test
	public void testGTable2BeanKeyRoundTrip() {
		var h = new H2();
		h.g.put(new BDeptKey("alice", 5), 2L, new MBean().set(7));
		var json = Json.toCompactString(h);
		var parsed = (H2)Json.parse(json, new H2());
		var v = parsed.g.get(new BDeptKey("alice", 5), 2L);
		assertNotNull(v, "BeanKey行键必须恢复");
		assertEquals(7L, v.v);
	}

	// 扩围触发面：binary行键（IsKeyable=true的schema合法键型）。
	@Test
	public void testGTable1BinaryKeyRoundTrip() {
		var h = new H3();
		h.g.put(new Binary(new byte[]{1, 2, 3}), 2L, 11);
		var json = Json.toCompactString(h);
		var parsed = (H3)Json.parse(json, new H3());
		var v = parsed.g.get(new Binary(new byte[]{1, 2, 3}), 2L);
		assertNotNull(v, "binary行键必须恢复");
		assertEquals(11, v);
	}

	// 审计要点(c)：decimal键实际往返验证——decimal键在工厂层即崩（createCodec），
	// 属FND8-32范围，往返用例见 TestFnd832GTableDecimal。

	// 孪生：GTable1非内建标量值（binary）不再静默解析成裸空Object。
	@Test
	public void testGTable1BinaryValueRoundTrip() {
		var h = new H5();
		h.g.put(1, 2L, new Binary(new byte[]{4, 5, 6}));
		var json = Json.toCompactString(h);
		var parsed = (H5)Json.parse(json, new H5());
		var v = parsed.g.get(1, 2L);
		assertNotNull(v, "binary值必须恢复（修复前为裸空Object）");
		assertArrayEquals(new byte[]{4, 5, 6}, v.bytesUnsafe());
	}

	// 孪生：vector2值按真实类型解析（修复前同为裸空Object）。
	@Test
	public void testGTable1Vector2ValueRoundTrip() {
		var h = new H6();
		h.g.put(1, 2L, new Vector2(1.5f, 2.5f));
		var json = Json.toCompactString(h);
		var parsed = (H6)Json.parse(json, new H6());
		var v = parsed.g.get(1, 2L);
		assertNotNull(v, "vector2值必须恢复（修复前为裸空Object）");
		assertEquals(1.5f, v.x);
		assertEquals(2.5f, v.y);
	}

	// BeanMap1.decodeResultSet路径（Helper.decodeJsonTypedMap）：BeanKey键。
	@Test
	public void testDecodeJsonTypedMapBeanKey() {
		var meta = Map1Meta.get(BDeptKey.class, Integer.class);
		var src = new BeanMap1<>(meta);
		src.put(new BDeptKey("alice", 5), 42);
		var json = Zeze.Serialize.Helper.encodeJson(src);
		var dst = new BeanMap1<>(meta);
		Zeze.Serialize.Helper.decodeJsonTypedMap(dst, meta, json);
		var v = dst.get(new BDeptKey("alice", 5));
		assertNotNull(v, "BeanMap的BeanKey键必须恢复");
		assertEquals(42, v);
	}
}
