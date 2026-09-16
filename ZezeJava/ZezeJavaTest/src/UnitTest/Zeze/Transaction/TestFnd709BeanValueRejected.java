package UnitTest.Zeze.Transaction;

import java.util.List;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.PList1;
import Zeze.Transaction.Collections.PMap1;
import demo.Module1.BValue;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-09：PList1/PMap1未拒绝Bean值——1系容器按值拷贝记账、不挂接rootInfo（对比
 * PList2.add/PMap2.put的initRootInfoWithRedo），装入的bean永不受管，原位修改不产生
 * 日志、提交后静默丢失。修复：构造器对Bean值类型显式抛IAE（对齐PSet1/PMap1-BeanKey
 * 判例，046851476风格），变静默丢数据为启动期fail-fast。
 */
@Fast
public class TestFnd709BeanValueRejected {

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
				() -> new PList1<>(MyBean.class), "PList1必须拒绝Bean值（原位修改静默丢失）");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new PList1<>(BValue.class), "生成Bean值同样必须拒绝");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new PMap1<>(Long.class, MyBean.class), "PMap1必须拒绝Bean值（原位修改静默丢失）");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new PMap1<>(Long.class, BValue.class), "生成Bean值同样必须拒绝");
	}

	@Test
	public void testLegalValuesStillWork() {
		var list = new PList1<Integer>(Integer.class);
		Assertions.assertTrue(list.add(1));
		Assertions.assertTrue(list.addAll(List.of(2, 3)));
		Assertions.assertEquals(List.of(1, 2, 3), list.getList());

		var map = new PMap1<Long, String>(Long.class, String.class);
		Assertions.assertNull(map.put(1L, "a"));
		Assertions.assertEquals("a", map.get(1L));
	}
}
