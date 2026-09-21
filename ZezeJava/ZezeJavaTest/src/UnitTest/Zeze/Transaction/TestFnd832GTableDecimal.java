package UnitTest.Zeze.Transaction;

import java.math.BigDecimal;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Transaction.Collections.List1Meta;
import Zeze.Transaction.Collections.Set1Meta;
import Zeze.Transaction.Collections.Map1Meta;
import Zeze.Transaction.Collections.SortedMap1Meta;
import Zeze.Transaction.GTable.GTable1;
import Zeze.Transaction.GTable.GTable2;
import Zeze.Util.Json;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * FND8-32回归：decimal（BigDecimal）是注册的内建schema类型且IsKeyable=true，
 * 但SerializeHelper.codecs未注册——gtable/map/list/set/sortedmap全标量集合家族
 * 把BigDecimal送进createCodec时抛UnsupportedOperationException（生成器放行、
 * 生成代码bean类静态初始化即崩，运行期fail-late）。
 * 修复（方案A补注册）：SerializeHelper注册BigDecimal编解码（与LogDecimal同款
 * 全精度字符串双射，FND3-06）；伴生（孪生4）：Json.keyReaderMap补decimal键
 * 读取、BigDecimal的ClassMeta注册自定义读写对（值/字段JSON），decimal键/值
 * 集合的JSON导入导出可用。
 * 纯单元：工厂构造+二进制编解码+JSON往返均不依赖应用环境。
 */
@Fast
public class TestFnd832GTableDecimal {

	public static class H1 {
		final GTable1<BigDecimal, Long, Integer> g = new GTable1<>(BigDecimal.class, Long.class, Integer.class);
	}

	public static class H2 {
		final GTable1<Long, BigDecimal, Integer> g = new GTable1<>(Long.class, BigDecimal.class, Integer.class);
	}

	public static class H3 {
		final GTable1<Long, Long, BigDecimal> g = new GTable1<>(Long.class, Long.class, BigDecimal.class);
	}

	public static class H4 {
		final GTable2<BigDecimal, Long, TestFnd831GTableNonBuiltinKeyJson.MBean,
				TestFnd831GTableNonBuiltinKeyJson.MBean> g =
				new GTable2<>(BigDecimal.class, Long.class, TestFnd831GTableNonBuiltinKeyJson.MBean.class);
	}

	// 工厂层不再崩：gtable的decimal行键/列键/值（修复前getFactory即UnsupportedOperationException）。
	@Test
	public void testFactoryAcceptsDecimal() {
		assertDoesNotThrow(() -> GTable1.getFactory(BigDecimal.class, Long.class, Long.class), "decimal行键");
		assertDoesNotThrow(() -> GTable1.getFactory(Long.class, BigDecimal.class, Long.class), "decimal列键");
		assertDoesNotThrow(() -> GTable1.getFactory(Long.class, Long.class, BigDecimal.class), "decimal值");
		assertDoesNotThrow(() -> GTable2.getFactory(BigDecimal.class, Long.class,
				TestFnd831GTableNonBuiltinKeyJson.MBean.class), "GTable2 decimal行键");
		// 标量集合家族同型（孪生2）：map/list/set/sortedmap的decimal键/值。
		assertDoesNotThrow(() -> Map1Meta.get(BigDecimal.class, Long.class), "map decimal键");
		assertDoesNotThrow(() -> Map1Meta.get(Long.class, BigDecimal.class), "map decimal值");
		assertDoesNotThrow(() -> List1Meta.get(BigDecimal.class), "list decimal值");
		assertDoesNotThrow(() -> Set1Meta.get(BigDecimal.class), "set decimal值");
		assertDoesNotThrow(() -> SortedMap1Meta.get(BigDecimal.class, Long.class), "sortedmap decimal键");
	}

	// 编解码双射：全精度字符串（FND3-06），含负scale（1E+2）与等值不同scale（0.100）。
	@Test
	public void testCodecRoundTrip() {
		var codec = SerializeHelper.createCodec(BigDecimal.class);
		for (var v : new BigDecimal[]{
				new BigDecimal("0"), new BigDecimal("5.5"), new BigDecimal("-12.345"),
				new BigDecimal("0.100"), new BigDecimal("1E+2"), new BigDecimal("1E-10"),
				new BigDecimal("123456789012345678901234567890123456789012345678")}) {
			var bb = ByteBuffer.Allocate();
			codec.encoder.accept(bb, v);
			var decoded = codec.decoder.apply(ByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size()));
			assertEquals(0, v.compareTo(decoded), "数值往返: " + v);
			assertEquals(v.scale(), decoded.scale(), "scale保持: " + v);
			assertEquals(v, decoded, "equals（含scale语义）: " + v);
		}
	}

	// decimal行键的JSON往返（FND8-31遗留的审计要点(c)，此前工厂层即崩不可达）。
	@Test
	public void testDecimalRowKeyJsonRoundTrip() {
		var h = new H1();
		h.g.put(new BigDecimal("5.5"), 2L, 42);
		h.g.put(new BigDecimal("1E+2"), 3L, 43); // 负scale
		var json = Json.toCompactString(h);
		var parsed = (H1)Json.parse(json, new H1());
		var v1 = parsed.g.get(new BigDecimal("5.5"), 2L);
		assertNotNull(v1, "decimal行键必须恢复");
		assertEquals(42, v1);
		var v2 = parsed.g.get(new BigDecimal("1E+2"), 3L);
		assertNotNull(v2, "负scale decimal行键必须恢复");
		assertEquals(43, v2);
	}

	// decimal列键的JSON往返。
	@Test
	public void testDecimalColumnKeyJsonRoundTrip() {
		var h = new H2();
		h.g.put(1L, new BigDecimal("7.25"), 42);
		var json = Json.toCompactString(h);
		var parsed = (H2)Json.parse(json, new H2());
		var v = parsed.g.get(1L, new BigDecimal("7.25"));
		assertNotNull(v, "decimal列键必须恢复");
		assertEquals(42, v);
	}

	// decimal值的JSON往返（自定义读写对）。
	@Test
	public void testDecimalValueJsonRoundTrip() {
		var h = new H3();
		h.g.put(1L, 2L, new BigDecimal("0.100"));
		var json = Json.toCompactString(h);
		var parsed = (H3)Json.parse(json, new H3());
		var v = parsed.g.get(1L, 2L);
		assertNotNull(v, "decimal值必须恢复");
		assertEquals(new BigDecimal("0.100"), v);
	}

	// GTable2的decimal行键 + Bean值JSON往返。
	@Test
	public void testGTable2DecimalRowKeyJsonRoundTrip() {
		var h = new H4();
		h.g.put(new BigDecimal("3.3"), 2L, new TestFnd831GTableNonBuiltinKeyJson.MBean().set(9));
		var json = Json.toCompactString(h);
		var parsed = (H4)Json.parse(json, new H4());
		var v = parsed.g.get(new BigDecimal("3.3"), 2L);
		assertNotNull(v, "GTable2 decimal行键必须恢复");
		assertEquals(9L, v.v);
	}
}
