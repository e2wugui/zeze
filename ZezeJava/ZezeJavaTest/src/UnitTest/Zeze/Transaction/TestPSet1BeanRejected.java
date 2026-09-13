package UnitTest.Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.PSet1;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND5-44同族（复审）：Bean是值语义equals但身份hashCode（可变bean不覆写hashCode防
 * 哈希漂移），哈希容器对bean元素静默漏命中——PSet1的去重/remove/removeAll失真。
 * 修复：构造时对Bean值类型显式拒绝（变静默错为显式失败）。框架无PSet2，bean集合
 * 属设计不支持（LogList2用IdentityHashSet+身份比较是既有约定）。
 */
@Fast
public class TestPSet1BeanRejected {

	public static class MyBean extends Bean {
		@Override
		public void encode(@NotNull ByteBuffer bb) {
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
		}
	}

	@Test
	public void testBeanTypeRejected() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> new PSet1<>(MyBean.class),
				"PSet1必须拒绝Bean值类型（哈希漏命中）");
	}

	@Test
	public void testValueTypeStillWorks() {
		var set = new PSet1<>(String.class);
		Assertions.assertTrue(set.add("a"));
		Assertions.assertFalse(set.add("a")); // 值类型的哈希去重不受影响
		Assertions.assertTrue(set.remove("a"));
		Assertions.assertTrue(set.isEmpty());
	}
}
