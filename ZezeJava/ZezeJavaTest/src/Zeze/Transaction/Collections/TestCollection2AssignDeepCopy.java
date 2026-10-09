package Zeze.Transaction.Collections;

import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * assign 只存在于1系容器（PList1/PMap1/PSet1/PSortedMap1，引用共享语义），
 * 2系（PList2/PMap2/PSortedMap2）缺失同名API——用户从1系迁到2系时无对应写法。
 * 2系值为可变Bean，assign 语义为深拷贝元素赋值（复用 copy()）：
 * assign 后目标与源互不影响。
 */
@Fast
public class TestCollection2AssignDeepCopy {

	@Test
	public void testPMap2AssignDeepCopy() {
		var src = new PMap2<>(Long.class, BValue.class);
		var srcBean = new BValue();
		srcBean.setInt_1(11);
		src.put(1L, srcBean);

		var dst = new PMap2<>(Long.class, BValue.class);
		dst.assign(src);

		Assertions.assertEquals(11, dst.get(1L).getInt_1());
		Assertions.assertNotSame(srcBean, dst.get(1L), "深拷贝：不得共享元素实例");

		srcBean.setInt_1(99); // 修改源不影响目标
		Assertions.assertEquals(11, dst.get(1L).getInt_1());
		dst.get(1L).setInt_1(22); // 修改目标不影响源
		Assertions.assertEquals(99, src.get(1L).getInt_1());
	}

	@Test
	public void testPMap2AssignReplacesExisting() {
		var src = new PMap2<>(Long.class, BValue.class);
		var b = new BValue();
		b.setInt_1(1);
		src.put(1L, b);
		var dst = new PMap2<>(Long.class, BValue.class);
		dst.put(2L, new BValue()); // 目标原有条目须被清空

		dst.assign(src);

		Assertions.assertEquals(1, dst.size());
		Assertions.assertTrue(dst.containsKey(1L));
	}

	@Test
	public void testPList2AssignDeepCopy() {
		var src = new PList2<>(BValue.class);
		var srcBean = new BValue();
		srcBean.setInt_1(11);
		src.add(srcBean);

		var dst = new PList2<>(BValue.class);
		dst.assign(src);

		Assertions.assertEquals(11, dst.get(0).getInt_1());
		Assertions.assertNotSame(srcBean, dst.get(0));
		srcBean.setInt_1(99);
		Assertions.assertEquals(11, dst.get(0).getInt_1());
		dst.get(0).setInt_1(22);
		Assertions.assertEquals(99, src.get(0).getInt_1());
	}

	@Test
	public void testPSortedMap2AssignDeepCopy() {
		var src = new PSortedMap2<>(Long.class, BValue.class);
		var srcBean = new BValue();
		srcBean.setInt_1(11);
		src.put(1L, srcBean);

		var dst = new PSortedMap2<>(Long.class, BValue.class);
		dst.assign(src);

		Assertions.assertEquals(11, dst.get(1L).getInt_1());
		Assertions.assertNotSame(srcBean, dst.get(1L));
		srcBean.setInt_1(99);
		Assertions.assertEquals(11, dst.get(1L).getInt_1());
		dst.get(1L).setInt_1(22);
		Assertions.assertEquals(99, src.get(1L).getInt_1());
	}
}
