package UnitTest.Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.PMap1;
import Zeze.Transaction.Collections.PMap2;
import demo.Module1.BValue;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND6-41：PMap1/PMap2构造器未拒绝Bean key（PSet1判例姊妹）——Bean是值语义equals但
 * 身份hashCode，哈希容器对bean键静默漏命中（put/get/remove/contains失真）；
 * ac1302f6f（FND5-05）的自证「Bean无hashCode漏命中」只守了set族。
 * 修复：构造器对Bean key类型显式拒绝（变静默错为显式失败），非Bean key不受影响。
 */
@Fast
public class TestPMapBeanKeyRejected {

	public static class MyBean extends Bean {
		@Override
		public void encode(@NotNull ByteBuffer bb) {
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
		}
	}

	@Test
	public void testBeanKeyRejected() {
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new PMap1<>(MyBean.class, String.class), "PMap1必须拒绝Bean key（哈希漏命中）");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new PMap2<>(MyBean.class, BValue.class), "PMap2必须拒绝Bean key（哈希漏命中）");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new PMap2<>(MyBean.class, BValue.class, BValue::new),
				"PMap2(Supplier)构造器同样必须拒绝（Supplier形态要求V extends Bean）");
	}

	@Test
	public void testLegalKeysStillWork() {
		var m1 = new PMap1<>(Long.class, String.class);
		Assertions.assertNull(m1.put(1L, "a"));
		Assertions.assertEquals("a", m1.get(1L));

		var m2 = new PMap2<>(Long.class, BValue.class); // PMap2值类型须为生成Bean
		Assertions.assertNull(m2.put(1L, new BValue()));
		Assertions.assertNotNull(m2.get(1L));
		Assertions.assertNotNull(m2.remove(1L));
		Assertions.assertTrue(m2.isEmpty());

		// Supplier构造器：合法key + Bean值不受影响。
		var m2b = new PMap2<>(Long.class, BValue.class, BValue::new);
		Assertions.assertNull(m2b.put(2L, new BValue()));
		Assertions.assertNotNull(m2b.get(2L));
	}
}
