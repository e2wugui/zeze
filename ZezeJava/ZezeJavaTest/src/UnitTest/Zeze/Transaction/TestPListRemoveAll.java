package UnitTest.Zeze.Transaction;

import java.util.List;
import java.util.Set;

import Zeze.Transaction.Collections.PList1;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND4-08：removeAll 违反 java.util.List 契约——原实现逐元素 remove(v)（indexOf 首个出现），
 * 列表含重复元素时 [x,x].removeAll(Set.of(x)) 结果为 [x]（应为 []）。托管路径（LogList1）与
 * 非托管路径（minusAll）行为一致地错，重放端按 opLogs 忠实重放——三方契约对齐修正：
 * 删除 c 中每个元素在列表里的全部出现。本测试覆盖非托管路径（纯单元）；
 * 托管侧同一算法（降序索引删全部命中，opLog 词表不变、序列可重放），以定向核查覆盖。
 */
@Fast
public class TestPListRemoveAll {

	private static PList1<Integer> newList(Integer... items) {
		var list = new PList1<>(Integer.class);
		list.addAll(List.of(items));
		return list;
	}

	@Test
	public void testRemoveAllDuplicates() {
		var list = newList(1, 2, 1, 3, 1);
		Assertions.assertTrue(list.removeAll(Set.of(1)));
		Assertions.assertEquals(List.of(2, 3), list.getList());

		// c 自身含重复：仍删全部命中
		var list2 = newList(1, 2, 1, 2);
		Assertions.assertTrue(list2.removeAll(List.of(1, 2)));
		Assertions.assertTrue(list2.isEmpty(), "重复元素必须全部删除（JDK契约），got " + list2.getList());
	}

	@Test
	public void testRemoveAllNoHitAndReturnSemantics() {
		var list = newList(1, 2, 3);
		Assertions.assertFalse(list.removeAll(Set.of(9)));
		Assertions.assertEquals(List.of(1, 2, 3), list.getList());

		Assertions.assertFalse(list.removeAll(Set.of()), "空集必须false且无变化");

		var empty = new PList1<Integer>(Integer.class);
		Assertions.assertFalse(empty.removeAll(Set.of(1)));
	}

	@Test
	public void testRemoveAllKeepsOrder() {
		var list = newList(5, 1, 6, 1, 7, 1);
		list.removeAll(Set.of(1));
		Assertions.assertEquals(List.of(5, 6, 7), list.getList(), "非命中元素相对顺序保持");
	}
}
