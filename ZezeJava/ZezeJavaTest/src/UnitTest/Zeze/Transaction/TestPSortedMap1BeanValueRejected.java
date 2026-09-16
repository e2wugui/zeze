package UnitTest.Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.PSortedMap1;
import demo.Module1.BValue;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * PSortedMap1未拒绝Bean值（FND7-09同族缺口：FND7-05只拦了Bean key）：排序map与
 * PMap1同为1系按值拷贝记账，put不挂接rootInfo（对比PSortedMap2.put的initRootInfo），
 * 装入的bean永不受管——原位修改不产生日志，提交后静默丢失。修复：构造器对Bean值类型
 * 显式抛IAE（对齐PList1/PMap1判例），变静默丢数据为构造期fail-fast。
 */
@Fast
public class TestPSortedMap1BeanValueRejected {

	public static class MyBean extends Bean {
		@Override
		public void encode(@NotNull ByteBuffer bb) {
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
		}
	}

	@Test
	public void testBeanValueRejected() {
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new PSortedMap1<>(Long.class, MyBean.class), "PSortedMap1必须拒绝Bean值（原位修改静默丢失）");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new PSortedMap1<>(Long.class, BValue.class), "生成Bean值同样必须拒绝");
	}

	@Test
	public void testLegalValuesStillWork() {
		var m = new PSortedMap1<Long, String>(Long.class, String.class);
		Assertions.assertNull(m.put(1L, "a"));
		Assertions.assertEquals("a", m.get(1L));
		Assertions.assertEquals("a", m.remove(1L));
		Assertions.assertTrue(m.isEmpty());
	}
}
