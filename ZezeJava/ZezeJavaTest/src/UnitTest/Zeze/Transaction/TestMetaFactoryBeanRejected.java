package UnitTest.Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.BeanKeyMeta;
import Zeze.Transaction.Collections.List1Meta;
import Zeze.Transaction.Collections.List2Meta;
import Zeze.Transaction.Collections.Map1Meta;
import Zeze.Transaction.Collections.Map2Meta;
import Zeze.Transaction.Collections.Set1Meta;
import Zeze.Transaction.Collections.SortedMap1Meta;
import Zeze.Transaction.Collections.SortedMap2Meta;
import Zeze.Transaction.GTable.GTable1;
import Zeze.Transaction.GTable.GTable2;
import demo.Module1.AutoKey;
import demo.Module1.BValue;
import demo.Module1.Key;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Meta家族工厂必须拒绝Bean组合：1系容器装Bean值原位修改静默丢失、任意map族装Bean键
 * 日志簿记哈希漏命中。工厂是公开meta的唯一构建入口，在工厂层收口即封死全部绕行路径
 * （含GTable Bean行/列）。
 */
@Fast
public class TestMetaFactoryBeanRejected {

	public static class MyBean extends Bean {
		@Override
		public void encode(@NotNull ByteBuffer bb) {
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
		}
	}

	@Test
	public void testList1Set1RejectBeanValue() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> List1Meta.get(MyBean.class),
				"List1Meta.get必须拒绝Bean值（1系按值拷贝记账，原位修改静默丢失）");
		Assertions.assertThrows(IllegalArgumentException.class, () -> Set1Meta.get(MyBean.class),
				"Set1Meta.get必须拒绝Bean值");
	}

	@Test
	public void testMap1SortedMap1RejectBeanKeyAndValue() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> Map1Meta.get(MyBean.class, Long.class),
				"Map1Meta.get必须拒绝Bean key（日志簿记哈希漏命中）");
		Assertions.assertThrows(IllegalArgumentException.class, () -> Map1Meta.get(Long.class, MyBean.class),
				"Map1Meta.get必须拒绝Bean值（原位修改静默丢失）");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> SortedMap1Meta.get(MyBean.class, Long.class), "SortedMap1Meta.get必须拒绝Bean key");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> SortedMap1Meta.get(Long.class, MyBean.class), "SortedMap1Meta.get必须拒绝Bean值");
	}

	@Test
	public void testMap2SortedMap2RejectBeanKey() {
		// 2系Bean值合法（受管），只拦Bean key。
		Assertions.assertThrows(IllegalArgumentException.class, () -> Map2Meta.get(MyBean.class, BValue.class),
				"Map2Meta.get必须拒绝Bean key");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> Map2Meta.create(MyBean.class, BValue.class, BValue::new), "Map2Meta.create必须拒绝Bean key");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> Map2Meta.createDynamic(MyBean.class, b -> 0L, id -> new BValue()),
				"Map2Meta.createDynamic必须拒绝Bean key");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> SortedMap2Meta.get(MyBean.class, BValue.class), "SortedMap2Meta.get必须拒绝Bean key");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> SortedMap2Meta.create(MyBean.class, BValue.class, BValue::new),
				"SortedMap2Meta.create必须拒绝Bean key");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> SortedMap2Meta.createDynamic(MyBean.class, b -> 0L, id -> new BValue()),
				"SortedMap2Meta.createDynamic必须拒绝Bean key");
	}

	@Test
	public void testLegalCombinationsStillWork() {
		// 合法组合零变化：2系Bean值、BeanKey专用meta、1系标量、DynamicBean工厂。
		Assertions.assertNotNull(Map2Meta.get(Long.class, BValue.class));
		Assertions.assertNotNull(Map2Meta.create(Long.class, BValue.class, BValue::new));
		Assertions.assertNotNull(Map2Meta.createDynamic(Long.class, b -> 0L, id -> new BValue()));
		Assertions.assertNotNull(Map1Meta.get(Long.class, Long.class));
		Assertions.assertNotNull(SortedMap1Meta.get(Long.class, Long.class));
		Assertions.assertNotNull(List2Meta.get(BValue.class));
		Assertions.assertNotNull(BeanKeyMeta.get(MyBean.class)); // LogBeanKey专用，不受影响
		Assertions.assertNotNull(List1Meta.get(Long.class));
		Assertions.assertNotNull(Set1Meta.get(Long.class));
	}

	@Test
	public void testGTableBeanKeyDimensionsNotIntercepted() {
		// 对抗性防回归：schema的gtable行/列键合法形态只有内建类型与BeanKey（生成器
		// Gen/Types/TypeGTable.cs要求IsKeyable，Types.Bean.IsKeyable=false，Types.BeanKey=true；
		// History.Helper.dependsGTable注释"must be BeanKey"）。BeanKey实现Zeze.Transaction.BeanKey
		// 接口（extends Serializable）而不继承Bean（Meta1的valueFactory两分支判据可证），
		// 工厂层Bean拦截只匹配Bean子类——BeanKey维度必须原样放行，覆盖GTable2.getFactory的
		// Map2Meta.get(col)与Map2Meta.create(row)两次调用。此用例红=拦截过宽误伤合法schema形态。
		Assertions.assertNotNull(GTable2.getFactory(Key.class, AutoKey.class, BValue.class),
				"gtable[beanKey,beanKey,bean]（生成器恒产GTable2）的meta工厂必须放行");
		Assertions.assertNotNull(GTable1.getFactory(Key.class, AutoKey.class, Integer.class),
				"gtable[beanKey,beanKey,标量]（生成器恒产GTable1）的meta工厂必须放行");

		// 构造+put/get往返：BeanKey行/列维度端到端可用（非托管，无环境依赖）。
		var row = new Key((short)1, "r");
		var col = new AutoKey("t", 7L);
		var t2 = new GTable2<>(Key.class, AutoKey.class, BValue.class);
		var v = new BValue();
		Assertions.assertNull(t2.put(row, col, v));
		Assertions.assertSame(v, t2.get(row, col), "BeanKey行/列+Bean值必须按值语义equals命中");

		var t1 = new GTable1<>(Key.class, AutoKey.class, Integer.class);
		Assertions.assertNull(t1.put(row, col, 42));
		Assertions.assertEquals(Integer.valueOf(42), t1.get(row, col), "BeanKey行/列+标量值必须命中");
	}
}
