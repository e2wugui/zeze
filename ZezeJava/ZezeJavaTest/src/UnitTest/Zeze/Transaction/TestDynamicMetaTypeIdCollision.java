package UnitTest.Zeze.Transaction;

import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Collections.Meta1;
import Zeze.Transaction.Collections.Meta2;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND-T4-2 证据固化（升级待裁，本测试断言的是**缺陷现状**，升级修复后应同步翻转断言）：
 * dynamic 容器 Meta 的 logTypeId 不含工厂身份——任意两个 list&lt;dynamic&gt; 共享同一 typeId，
 * 同 keyClass 的 map/sortedmap&lt;K,dynamic&gt; 共享同一 typeId。配合 Log.register 先到先得
 * （typeName 相同连 error 都不打），isHistory 重放端 Log.create 只能拿到先注册变量的工厂，
 * decode 第二个变量的值时错工厂返回 null，DynamicBean.newBean 抛 IllegalStateException 中断重放。
 * typeId 纳入工厂身份会改变 History/Raft 日志流的编码格式（升级条件 2），故当前不改；
 * 本测试不触碰全局 Log.factorys（无 register），避免污染其他测试的注册状态。
 */
@Fast
public class TestDynamicMetaTypeIdCollision {
	// 与生成代码（BValue.createBeanFromSpecialTypeId_N）同构的最小工厂：只认 EmptyBean，其余返回 null。
	private static long getSpecialTypeId(@NotNull Bean b) {
		return b.typeId() == EmptyBean.TYPEID ? EmptyBean.TYPEID : 1L;
	}

	private static @Nullable Bean createBean(long specialTypeId) {
		return specialTypeId == EmptyBean.TYPEID ? new EmptyBean() : null;
	}

	@Test
	public final void testDynamicListSharesTypeId() {
		var metaA = Meta1.<Bean>createDynamicListMeta(TestDynamicMetaTypeIdCollision::getSpecialTypeId,
				TestDynamicMetaTypeIdCollision::createBean);
		var metaB = Meta1.<Bean>createDynamicListMeta(
				b -> 2L, // 不同工厂
				t -> null);
		// 现状：工厂不同 typeId 也相同——重放端无法区分（升级后应断言不同）
		Assertions.assertEquals(metaA.logTypeId, metaB.logTypeId);
		Assertions.assertEquals(metaA.name, metaB.name); // name 也不含工厂身份，Log.register 连 error 都不打
	}

	@Test
	public final void testDynamicMapSharesTypeIdPerKeyClass() {
		var metaA = Meta2.createDynamicMapMeta(String.class,
				TestDynamicMetaTypeIdCollision::getSpecialTypeId, TestDynamicMetaTypeIdCollision::createBean);
		var metaB = Meta2.createDynamicMapMeta(String.class,
				b -> 2L, t -> null);
		Assertions.assertEquals(metaA.logTypeId, metaB.logTypeId); // 只含 keyClass，不含工厂
		Assertions.assertEquals(metaA.name, metaB.name);

		var metaOtherKey = Meta2.createDynamicMapMeta(Long.class, b -> 2L, t -> null);
		Assertions.assertNotEquals(metaA.logTypeId, metaOtherKey.logTypeId); // keyClass 不同则 typeId 不同
	}

	@Test
	public final void testDynamicSortedMapSharesTypeIdPerKeyClass() {
		var metaA = Meta2.createDynamicSortedMapMeta(String.class,
				TestDynamicMetaTypeIdCollision::getSpecialTypeId, TestDynamicMetaTypeIdCollision::createBean);
		var metaB = Meta2.createDynamicSortedMapMeta(String.class, b -> 2L, t -> null);
		Assertions.assertEquals(metaA.logTypeId, metaB.logTypeId);
	}

	/**
	 * 触发链末端：decode 时用错工厂（对方不认识本变量的 specialTypeId）→ newBean 抛异常中断重放。
	 * 用生成的 demo 工厂模拟"先注册的工厂 decode 第二个变量的值"。
	 */
	@Test
	public final void testWrongFactoryDecodeThrows() {
		var factoryDynamic14 = demo.Module1.BValue.newDynamicBean_Dynamic14(); // 认 1L→demo.Bean1
		var factoryMap26 = demo.Module1.BValue.newDynamicBean_Map26(); // 只认 BSimple 的原始hash 和 EmptyBean
		Assertions.assertNotNull(factoryDynamic14.newBean(1L)); // 本工厂能解
		Assertions.assertThrows(IllegalStateException.class, () -> factoryMap26.newBean(1L)); // 错工厂中断
	}
}
