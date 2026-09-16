package UnitTest.Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.Meta1;
import Zeze.Transaction.Collections.Meta2;
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
 * Meta1/Meta2公开工厂未拒绝Bean组合（R2-T backlog④）：容器类构造器已拦Bean key/value
 * （FND6-41/FND7-05/FND7-09），但直建meta再走PMap1(meta)/PList1(meta)等Meta构造器可绕过
 * ——1系容器装Bean值原位修改静默丢失、任意map族装Bean键日志簿记哈希漏命中。工厂是
 * 公开meta的唯一构建入口，在工厂层收口即封死全部绕行路径（含GTable Bean行/列）。
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
		Assertions.assertThrows(IllegalArgumentException.class, () -> Meta1.getList1Meta(MyBean.class),
				"getList1Meta必须拒绝Bean值（1系按值拷贝记账，原位修改静默丢失）");
		Assertions.assertThrows(IllegalArgumentException.class, () -> Meta1.getSet1Meta(MyBean.class),
				"getSet1Meta必须拒绝Bean值");
	}

	@Test
	public void testMap1SortedMap1RejectBeanKeyAndValue() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> Meta2.getMap1Meta(MyBean.class, Long.class),
				"getMap1Meta必须拒绝Bean key（日志簿记哈希漏命中）");
		Assertions.assertThrows(IllegalArgumentException.class, () -> Meta2.getMap1Meta(Long.class, MyBean.class),
				"getMap1Meta必须拒绝Bean值（原位修改静默丢失）");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> Meta2.getSortedMap1Meta(MyBean.class, Long.class), "getSortedMap1Meta必须拒绝Bean key");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> Meta2.getSortedMap1Meta(Long.class, MyBean.class), "getSortedMap1Meta必须拒绝Bean值");
	}

	@Test
	public void testMap2SortedMap2RejectBeanKey() {
		// 2系Bean值合法（受管），只拦Bean key。
		Assertions.assertThrows(IllegalArgumentException.class, () -> Meta2.getMap2Meta(MyBean.class, BValue.class),
				"getMap2Meta必须拒绝Bean key");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> Meta2.createMap2Meta(MyBean.class, BValue.class, BValue::new), "createMap2Meta必须拒绝Bean key");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> Meta2.createDynamicMapMeta(MyBean.class, b -> 0L, id -> new BValue()),
				"createDynamicMapMeta必须拒绝Bean key");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> Meta2.getSortedMap2Meta(MyBean.class, BValue.class), "getSortedMap2Meta必须拒绝Bean key");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> Meta2.createSortedMap2Meta(MyBean.class, BValue.class, BValue::new),
				"createSortedMap2Meta必须拒绝Bean key");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> Meta2.createDynamicSortedMapMeta(MyBean.class, b -> 0L, id -> new BValue()),
				"createDynamicSortedMapMeta必须拒绝Bean key");
	}

	@Test
	public void testLegalCombinationsStillWork() {
		// 合法组合零变化：2系Bean值、BeanKey专用meta、1系标量、DynamicBean工厂。
		Assertions.assertNotNull(Meta2.getMap2Meta(Long.class, BValue.class));
		Assertions.assertNotNull(Meta2.createMap2Meta(Long.class, BValue.class, BValue::new));
		Assertions.assertNotNull(Meta2.createDynamicMapMeta(Long.class, b -> 0L, id -> new BValue()));
		Assertions.assertNotNull(Meta2.getMap1Meta(Long.class, Long.class));
		Assertions.assertNotNull(Meta2.getSortedMap1Meta(Long.class, Long.class));
		Assertions.assertNotNull(Meta1.getList2Meta(BValue.class));
		Assertions.assertNotNull(Meta1.getBeanMeta(MyBean.class)); // LogBeanKey专用，不受影响
		Assertions.assertNotNull(Meta1.getList1Meta(Long.class));
		Assertions.assertNotNull(Meta1.getSet1Meta(Long.class));
	}

	@Test
	public void testGTableBeanKeyDimensionsNotIntercepted() {
		// R3-T对抗性防回归：schema的gtable行/列键合法形态只有内建类型与BeanKey（生成器
		// Gen/Types/TypeGTable.cs要求IsKeyable，Types.Bean.IsKeyable=false，Types.BeanKey=true；
		// History.Helper.dependsGTable注释"must be BeanKey"）。BeanKey实现Zeze.Transaction.BeanKey
		// 接口（extends Serializable）而不继承Bean（Meta1.java的valueFactory两分支判据可证），
		// 工厂层Bean拦截只匹配Bean子类——BeanKey维度必须原样放行，覆盖GTable2.getFactory的
		// getMap2Meta(col)与createMap2Meta(row)两次调用。此用例红=拦截过宽误伤合法schema形态。
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
