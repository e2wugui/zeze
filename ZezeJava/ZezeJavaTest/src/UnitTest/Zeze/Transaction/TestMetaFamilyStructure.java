package UnitTest.Zeze.Transaction;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import Zeze.Transaction.Collections.BeanKeyMeta;
import Zeze.Transaction.Collections.List1Meta;
import Zeze.Transaction.Collections.List2Meta;
import Zeze.Transaction.Collections.LogOneMeta;
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
import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Meta家族结构守卫：跨家族meta混用已由类型闭合（sealed+包私有构造器+容器构造器收子类型），
 * 负向用例无法用JUnit表达——本测试反射断言类型形状本身，防将来签名回宽（洞重开的充要条件）：
 * 1. 两个基类 sealed 且 permits 恰为预期封闭集（防悄悄扩容）；
 * 2. 容器公共构造器没有基类 Meta1/Meta2 参数（防签名回宽）；
 * 3. 基类构造器非 public（防闸门打开，包外无法继承造私家族）。
 * 附同家族正控（copy()/GTable 等生产路径形态不受影响）。
 */
@Fast
public class TestMetaFamilyStructure {

	private static void assertPermits(Class<?> sealedBase, Class<?>... expected) {
		Assertions.assertTrue(sealedBase.isSealed(), sealedBase.getSimpleName() + " 必须保持 sealed");
		var permitted = sealedBase.getPermittedSubclasses();
		Assertions.assertNotNull(permitted, sealedBase.getSimpleName() + " permits 不可为空");
		Set<Class<?>> actual = new HashSet<>(Arrays.asList(permitted));
		Set<Class<?>> want = new HashSet<>(Arrays.asList(expected));
		Assertions.assertEquals(want, actual, sealedBase.getSimpleName() + " permits 必须恰为预期封闭集");
	}

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

	@Test
	public void testSealedWithExactPermits() {
		assertPermits(Meta2.class, Map1Meta.class, Map2Meta.class, SortedMap1Meta.class, SortedMap2Meta.class);
		assertPermits(Meta1.class, BeanKeyMeta.class, List1Meta.class, List2Meta.class, LogOneMeta.class, Set1Meta.class);
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
	public void testBaseCtorsNotPublic() {
		for (var base : new Class<?>[] {Meta1.class, Meta2.class})
			for (Constructor<?> ctor : base.getDeclaredConstructors())
				Assertions.assertFalse(Modifier.isPublic(ctor.getModifiers()),
						base.getSimpleName() + " 构造器必须保持包私有（家族由构造闭合的闸门）: " + ctor);
	}

	// 同家族正控：Meta构造器（copy()/GTable 等生产路径形态）不受影响。
	@Test
	public void testSameFamilyStillWork() {
		Assertions.assertNotNull(new PMap2<>(Map2Meta.get(Long.class, BValue.class)).copy());
		Assertions.assertNotNull(new PMap2<>(Map2Meta.create(Long.class, BValue.class, BValue::new)).copy());
		Assertions.assertNotNull(new PMap2<>(Map2Meta.createDynamic(Long.class, b -> 0L, id -> new BValue())).copy());
		Assertions.assertNotNull(new PSortedMap2<>(SortedMap2Meta.get(Long.class, BValue.class)).copy());
		Assertions.assertNotNull(new PSortedMap2<>(SortedMap2Meta.create(Long.class, BValue.class, BValue::new)).copy());
		Assertions.assertNotNull(new PSortedMap2<>(SortedMap2Meta.createDynamic(Long.class, b -> 0L, id -> new BValue())).copy());
		Assertions.assertNotNull(new PMap1<>(Map1Meta.get(Long.class, String.class)).copy());
		Assertions.assertNotNull(new PSortedMap1<>(SortedMap1Meta.get(Long.class, String.class)).copy());
		Assertions.assertNotNull(new PList1<>(List1Meta.get(Long.class)).copy());
		Assertions.assertNotNull(new PList2<>(List2Meta.get(BValue.class)).copy());
		Assertions.assertNotNull(new PSet1<>(Set1Meta.get(Long.class)).copy());
	}
}
