package Zeze.Transaction;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import Zeze.Transaction.Collections.BeanKeyMeta;
import Zeze.Transaction.Collections.List1Meta;
import Zeze.Transaction.Collections.List2Meta;
import Zeze.Transaction.Collections.LogList1;
import Zeze.Transaction.Collections.LogList2;
import Zeze.Transaction.Collections.LogMap1;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Transaction.Collections.LogOneMeta;
import Zeze.Transaction.Collections.LogSet1;
import Zeze.Transaction.Collections.LogSortedMap1;
import Zeze.Transaction.Collections.LogSortedMap2;
import Zeze.Transaction.Collections.Map1Meta;
import Zeze.Transaction.Collections.Map2Meta;
import Zeze.Transaction.Collections.Meta1;
import Zeze.Transaction.Collections.Meta2;
import Zeze.Transaction.Collections.PList1;
import Zeze.Transaction.Collections.PList2;
import Zeze.Transaction.Collections.PMap1;
import Zeze.Transaction.Collections.PMap2;
import Zeze.Transaction.Collections.PSet1;
import Zeze.Transaction.Collections.PSortedMap1;
import Zeze.Transaction.Collections.PSortedMap2;
import Zeze.Transaction.Collections.Set1Meta;
import Zeze.Transaction.Collections.SortedMap1Meta;
import Zeze.Transaction.Collections.SortedMap2Meta;
import Zeze.Transaction.GTable.GTable1;
import Zeze.Transaction.GTable.GTable2;
import demo.Module1.BValue;
import dynamicred.Demo.Order;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** 校验容器和日志的元数据参数，以及元数据基类的构造器可见性。 */
@Fast
public class TestMetaFamilyStructure {

	private static void assertNoBaseMetaParam(Class<?> container) {
		for (Constructor<?> ctor : container.getConstructors()) {
			for (Class<?> param : ctor.getParameterTypes()) {
				Assertions.assertNotEquals(Meta1.class, param,
						container.getSimpleName() + " 公共构造器不得收基类 Meta1（跨家族洞重开）: " + ctor);
				Assertions.assertNotEquals(Meta2.class, param,
						container.getSimpleName() + " 公共构造器不得收基类 Meta2（跨家族洞重开）: " + ctor);
			}
		}
	}

	private static void assertLogMeta(Class<?> logType, Class<?> metaType) {
		for (var ctor : logType.getConstructors())
			Assertions.assertTrue(Arrays.asList(ctor.getParameterTypes()).contains(metaType), ctor::toString);
		for (var method : logType.getDeclaredMethods())
			if (method.getName().equals("decode"))
				Assertions.assertTrue(Arrays.asList(method.getParameterTypes()).contains(metaType), method::toString);
	}

	private static void assertPermits(Class<?> base, Class<?>... subclasses) {
		Assertions.assertTrue(base.isSealed(), base::getName);
		Assertions.assertEquals(Set.of(subclasses), Set.of(base.getPermittedSubclasses()), base::getName);
	}

	@Test
	public void testMetaFamiliesAreSealed() {
		var beanValueMapMeta = Map2Meta.class.getSuperclass();
		assertPermits(Meta1.class, BeanKeyMeta.class, List1Meta.class, List2Meta.class, LogOneMeta.class, Set1Meta.class);
		assertPermits(Meta2.class, Map1Meta.class, SortedMap1Meta.class, beanValueMapMeta);
		assertPermits(beanValueMapMeta, Map2Meta.class, SortedMap2Meta.class);
	}

	@Test
	public void testLogFactoriesTakeTheirOwnMeta() {
		assertLogMeta(LogList1.class, List1Meta.class);
		assertLogMeta(LogList2.class, List2Meta.class);
		assertLogMeta(LogSet1.class, Set1Meta.class);
		assertLogMeta(LogMap1.class, Map1Meta.class);
		assertLogMeta(LogMap2.class, Map2Meta.class);
		assertLogMeta(LogSortedMap1.class, SortedMap1Meta.class);
		assertLogMeta(LogSortedMap2.class, SortedMap2Meta.class);
	}

	@Test
	public void testContainerCtorsTakeNoBaseMeta() {
		for (var container : new Class<?>[] {
				PMap1.class, PMap2.class, PSortedMap1.class, PSortedMap2.class,
				PList1.class, PList2.class, PSet1.class,
				Zeze.Transaction.GTable.BeanMap1.class, Zeze.Transaction.GTable.BeanMap2.class})
			assertNoBaseMetaParam(container);
	}

	@Test
	public void testMetaAndGTableFactoryConstructorVisibility() {
		int accessMask = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE;
		for (var base : new Class<?>[] {Meta1.class, Meta2.class, Map2Meta.class.getSuperclass()})
			for (Constructor<?> ctor : base.getDeclaredConstructors())
				Assertions.assertEquals(0, ctor.getModifiers() & accessMask, ctor::toString);

		for (var concrete : new Class<?>[] {
				BeanKeyMeta.class, List1Meta.class, List2Meta.class, LogOneMeta.class, Set1Meta.class,
				Map1Meta.class, Map2Meta.class, SortedMap1Meta.class, SortedMap2Meta.class,
				GTable1.Factory.class, GTable2.Factory.class})
			for (var ctor : concrete.getDeclaredConstructors())
				Assertions.assertTrue(Modifier.isPrivate(ctor.getModifiers()), ctor::toString);
	}

	// 同家族正控：Meta构造器（copy()/GTable 等生产路径形态）不受影响。
	@Test
	public void testSameFamilyStillWork() {
		var order = new Order();
		Assertions.assertNotNull(new PMap2<>(Map2Meta.get(Long.class, BValue.class)).copy());
		Assertions.assertNotNull(new PMap2<>(Map2Meta.create(Long.class, BValue.class, BValue::new)).copy());
		Assertions.assertNotNull(order.getQuantityMap().copy());
		Assertions.assertNotNull(new PSortedMap2<>(SortedMap2Meta.get(Long.class, BValue.class)).copy());
		Assertions.assertNotNull(new PSortedMap2<>(SortedMap2Meta.create(Long.class, BValue.class, BValue::new)).copy());
		Assertions.assertNotNull(order.getAmountSorted().copy());
		Assertions.assertNotNull(new PMap1<>(Map1Meta.get(Long.class, String.class)).copy());
		Assertions.assertNotNull(new PSortedMap1<>(SortedMap1Meta.get(Long.class, String.class)).copy());
		Assertions.assertNotNull(new PList1<>(List1Meta.get(Long.class)).copy());
		Assertions.assertNotNull(new PList2<>(List2Meta.get(BValue.class)).copy());
		Assertions.assertNotNull(new PSet1<>(Set1Meta.get(Long.class)).copy());
	}
}
