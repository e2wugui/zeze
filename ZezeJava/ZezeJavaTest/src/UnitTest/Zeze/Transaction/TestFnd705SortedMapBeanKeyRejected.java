package UnitTest.Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.PSortedMap1;
import Zeze.Transaction.Collections.PSortedMap2;
import demo.Module1.BValue;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-05：PSortedMap1/PSortedMap2构造器未拒绝Bean key（PMap1/PSet1判例姊妹）。
 * 排序map本体TreePMap按compareTo定序没问题，但日志簿记LogSortedMap1.replaced/removed
 * 是HashMap/HashSet——Bean值语义equals配身份hashCode，等值bean落不同桶静默漏命中：
 * mergeChangeNote漏合并，encode按身份哈希迭代序写出重复条目，follower解码plusAll的
 * 终值依赖迭代序，可致静默主从分歧。修复：构造器对Bean key类型显式拒绝。
 */
@Fast
public class TestFnd705SortedMapBeanKeyRejected {

	public static class MyBean extends Bean implements Comparable<MyBean> {
		@Override
		public void encode(@NotNull ByteBuffer bb) {
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
		}

		@Override
		public int compareTo(@NotNull MyBean o) {
			return 0; // 等值可比较：正是排序map能接受Bean key的形态
		}
	}

	@Test
	public void testBeanKeyRejected() {
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new PSortedMap1<>(MyBean.class, String.class), "PSortedMap1必须拒绝Bean key（日志簿记哈希漏命中）");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new PSortedMap2<>(MyBean.class, BValue.class), "PSortedMap2必须拒绝Bean key（日志簿记哈希漏命中）");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new PSortedMap2<>(MyBean.class, BValue.class, BValue::new),
				"PSortedMap2(Supplier)构造器同样必须拒绝");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new PSortedMap2<>(MyBean.class, b -> 0L, id -> new BValue()),
				"PSortedMap2(DynamicBean值专用)构造器同样必须拒绝Bean key");
	}

	@Test
	public void testLegalKeysStillWork() {
		var m1 = new PSortedMap1<>(Long.class, String.class);
		Assertions.assertNull(m1.put(1L, "a"));
		Assertions.assertEquals("a", m1.get(1L));
		Assertions.assertEquals("a", m1.remove(1L));
		Assertions.assertTrue(m1.isEmpty());

		var m2 = new PSortedMap2<>(Long.class, BValue.class); // PSortedMap2值类型须为生成Bean
		Assertions.assertNull(m2.put(1L, new BValue()));
		Assertions.assertNotNull(m2.get(1L));
		Assertions.assertNotNull(m2.remove(1L));
		Assertions.assertTrue(m2.isEmpty());

		// Supplier构造器：合法key + Bean值不受影响。
		var m2b = new PSortedMap2<>(Long.class, BValue.class, BValue::new);
		Assertions.assertNull(m2b.put(2L, new BValue()));
		Assertions.assertNotNull(m2b.get(2L));
	}
}
