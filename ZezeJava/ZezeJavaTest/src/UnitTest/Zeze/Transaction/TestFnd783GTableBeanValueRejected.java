package UnitTest.Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.GTable.GTable1;
import Zeze.Util.Json;
import demo.Module1.BValue;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-83：GTable1为动态标量值设计（bean值由GTable2的带valueClass路径承担），但公开
 * 构造器不拒绝Bean值类型——其Json解析的fm2以klass=Object.class构造，JsonReader.
 * parseMap0的TYPE_CUSTOM分支按fm.klass建实例（fm.ctor从不使用），Bean值被静默解析成
 * 裸空Object，读取即ClassCastException/静默数据错误。修复：构造器对Bean值类型显式拒绝
 * （PList1/PMap1拒绝Bean值判例同族，FND7-09）。
 */
@Fast
public class TestFnd783GTableBeanValueRejected {

	public static class MyBean extends Bean {
		@Override
		public void encode(@NotNull ByteBuffer bb) {
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
		}
	}

	public static class H {
		final GTable1<Integer, Long, Float> g = new GTable1<>(Integer.class, Long.class, Float.class);
	}

	@Test
	public void testBeanValueRejected() {
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new GTable1<>(Integer.class, Long.class, BValue.class),
				"GTable1必须拒绝生成Bean值（Json值解析成裸空Object，静默数据错误）");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new GTable1<>(Integer.class, Long.class, MyBean.class),
				"GTable1必须拒绝手写Bean值（同族判例FND7-09）");
	}

	@Test
	public void testScalarValueJsonRoundTrip() {
		// 防回归：合法标量值路径（生成器全部产出）不受影响。
		var h = new H();
		h.g.put(1, 2L, 3.5f);
		var json = Json.toCompactString(h);
		var parsed = (H)Json.parse(json, new H());
		Assertions.assertEquals(3.5f, parsed.g.get(1, 2L), "标量值JSON往返必须原样恢复");
	}
}
