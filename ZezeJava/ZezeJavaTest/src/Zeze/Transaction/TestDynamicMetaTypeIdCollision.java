package Zeze.Transaction;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.Map2Meta;
import Zeze.Transaction.Collections.SortedMap2Meta;
import dynamicred.Demo.Order;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** 动态集合的日志类型号描述日志家族和键类型，History 另用字段身份区分工厂。 */
@Fast
public class TestDynamicMetaTypeIdCollision {
	private abstract static class Fields extends Bean {
	}

	private static DynamicField field(int fieldId, long specialId) {
		return DynamicField.declare(Fields.class, Bean.hash64(Fields.class.getName()), fieldId,
				b -> specialId, id -> null);
	}

	@Test
	public final void testDynamicListSharesTypeId() {
		var order = new Order();
		var amounts = order.getAmounts().createLogBean(null);
		var quantities = order.getQuantities().createLogBean(null);
		Assertions.assertEquals(Bean.hash32("Zeze.Transaction.Collections.LogList2<Zeze.Transaction.DynamicBean>"),
				amounts.getTypeId());
		Assertions.assertEquals(amounts.getTypeId(), quantities.getTypeId());
	}

	@Test
	public final void testDynamicMapSharesTypeIdPerKeyClass() {
		var metaA = Map2Meta.createDynamic(String.class, field(1, 1));
		var metaB = Map2Meta.createDynamic(String.class, field(2, 2));
		Assertions.assertEquals(metaA.logTypeId, metaB.logTypeId); // 只含 keyClass，不含工厂
		Assertions.assertEquals(metaA.name, metaB.name);

		var metaOtherKey = Map2Meta.createDynamic(Long.class, field(3, 2));
		Assertions.assertNotEquals(metaA.logTypeId, metaOtherKey.logTypeId); // keyClass 不同则 typeId 不同
	}

	@Test
	public final void testDynamicSortedMapSharesTypeIdPerKeyClass() {
		var metaA = SortedMap2Meta.createDynamic(String.class, field(4, 1));
		var metaB = SortedMap2Meta.createDynamic(String.class, field(5, 2));
		Assertions.assertEquals(metaA.logTypeId, metaB.logTypeId);
	}

	/** 字段各自解释 specialTypeId。 */
	@Test
	public final void testWrongFactoryDecodeThrows() {
		var factoryDynamic14 = demo.Module1.BValue.newDynamicBean_Dynamic14(); // 认 1L→demo.Bean1
		var factoryMap26 = demo.Module1.BValue.newDynamicBean_Map26(); // 只认 BSimple 的原始hash 和 EmptyBean
		Assertions.assertNotNull(factoryDynamic14.newBean(1L)); // 本工厂能解
		Assertions.assertThrows(IllegalStateException.class, () -> factoryMap26.newBean(1L)); // 错工厂中断
	}
}
