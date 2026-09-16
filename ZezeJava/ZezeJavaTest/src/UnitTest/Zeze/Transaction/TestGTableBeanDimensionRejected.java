package UnitTest.Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.GTable.GTable1;
import Zeze.Transaction.GTable.GTable2;
import demo.Module1.AutoKey;
import demo.Module1.BValue;
import demo.Module1.Key;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * R3-T复审C2：GTable1/GTable2构造器对Bean行/列键的报错前置显式化。工厂层（Meta2.
 * checkNonBeanKey）已拦截，但报错深在getFactory内部且为"LogMap2/LogMap1"家族名——不点名
 * GTable也不指明行/列维度（GTable1行键还会误报LogMap2）。显式检查在构造器前置报错点名
 * 维度并指引BeanKey；异常类型不变（IllegalArgumentException）。schema合法键只有内建类型
 * 与BeanKey（生成器IsKeyable约束），Bean维度仅手写可触发。
 */
@Fast
public class TestGTableBeanDimensionRejected {

	public static class MyBean extends Bean {
		@Override
		public void encode(@NotNull ByteBuffer bb) {
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
		}
	}

	private static void assertDimensionRejected(Executable ctor,
	                                             @NotNull String table, @NotNull String dimension) {
		var ex = Assertions.assertThrows(IllegalArgumentException.class, ctor,
				table + "必须拒绝Bean " + dimension + "键（哈希put/get失真，工厂层判例FND6-41同族）");
		Assertions.assertTrue(ex.getMessage().contains(table), "报错必须点名" + table + "而非深层日志家族名");
		Assertions.assertTrue(ex.getMessage().contains(dimension), "报错必须指明" + dimension + "维度");
	}

	@Test
	public void testGTable2BeanRowColumnRejected() {
		assertDimensionRejected(() -> new GTable2<>(MyBean.class, Long.class, BValue.class), "GTable2", "row");
		assertDimensionRejected(() -> new GTable2<>(Long.class, MyBean.class, BValue.class), "GTable2", "column");
	}

	@Test
	public void testGTable1BeanRowColumnRejected() {
		assertDimensionRejected(() -> new GTable1<>(MyBean.class, Long.class, Integer.class), "GTable1", "row");
		assertDimensionRejected(() -> new GTable1<>(Long.class, MyBean.class, Integer.class), "GTable1", "column");
	}

	@Test
	public void testBeanKeyDimensionsStillLegal() {
		// 合法形态防回归：BeanKey行/列（不继承Bean）不受显式检查与工厂拦截影响。
		Assertions.assertDoesNotThrow(() -> new GTable2<>(Key.class, AutoKey.class, BValue.class));
		Assertions.assertDoesNotThrow(() -> new GTable1<>(Key.class, AutoKey.class, Integer.class));
	}
}
