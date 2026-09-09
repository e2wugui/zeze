package UnitTest.Zeze.Transaction;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ConcurrentModificationException;
import java.util.List;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.PList1;

@Fast
public class TestPListIteratorRemove {
	// 纯单元：非受管PList1直接操作底层pcollections数据，不需要应用环境。

	private static PList1<Integer> newList(Integer... items) {
		var list = new PList1<>(Integer.class);
		list.addAll(List.of(items));
		return list;
	}

	@Test
	public void testIteratorChainedRemoveAll() {
		// FND2-T4-1：标准惯用法"迭代中删除全部元素"。修复前remove()用快照同下标做身份比较，
		// 第一次删除后当前列表与快照错位1，第二次remove起必抛假阳性CME（97e28d64e回归）。
		var list = newList(1, 2, 3, 4);
		var it = list.iterator();
		while (it.hasNext()) {
			it.next();
			it.remove(); // 连续删除4个：修复前第2个起抛CME
		}
		assertTrue(list.isEmpty());
	}

	@Test
	public void testIteratorChainedRemovePart() {
		// 非首位链式删除：next(1),next(2),remove(2),next(3),remove(3) → [1,4]
		var list = newList(1, 2, 3, 4);
		var it = list.iterator();
		assertEquals(1, it.next());
		assertEquals(2, it.next());
		it.remove(); // 删2（下标1）
		assertEquals(3, it.next());
		it.remove(); // 删3：修复前current[1]=3 vs snapshot[1]=2 必抛CME
		assertEquals(4, it.next());
		assertFalse(it.hasNext());
		assertEquals(List.of(1, 4), list.getList());
	}

	@Test
	public void testIteratorRemoveIllegalState() {
		var list = newList(1, 2);
		var it = list.iterator();
		assertThrows(IllegalStateException.class, it::remove); // 未next先remove
		assertEquals(1, it.next());
		it.remove();
		assertThrows(IllegalStateException.class, it::remove); // 同一元素连续remove
		assertEquals(List.of(2), list.getList());
	}

	@Test
	public void testIteratorRemoveExternalModificationFailFast() {
		// 97e28d64e的fail-fast意图必须保留：next之后、remove之前的外部结构性修改
		// 使按当前下标删除会删错元素时，必须抛CME而不是静默删错。
		var list = newList(1, 2, 3);
		var it = list.iterator();
		assertEquals(1, it.next());
		assertEquals(2, it.next()); // lastReturned=2，当前下标1
		list.remove(Integer.valueOf(1)); // 外部删除2之前的元素：[2,3]，2左移到下标0
		assertThrows(ConcurrentModificationException.class, it::remove);
	}

	@Test
	public void testIteratorRemoveExternalModificationSafe() {
		// 不引起lastReturned下标错位的外部修改（删除更后面的元素）：按下标删除仍恰好正确，
		// 身份比较通过，放行——这是设计语义（身份相等是删除正确性的充分条件），锁定防回归。
		var list = newList(1, 2, 3);
		var it = list.iterator();
		assertEquals(1, it.next());
		assertEquals(2, it.next()); // lastReturned=2，当前下标1
		list.remove(Integer.valueOf(3)); // [1,2]，下标1仍是2
		it.remove();
		assertEquals(List.of(1), list.getList());
	}

	@Test
	public void testDecodeNegativeSize() {
		// FND2-Z1-2联动（PList1同型）：恶意/损坏流的无符号集合长度varint落在[2^31,2^32)
		// 时ReadUInt读回为负，修复前静默clear+空循环产出"合法但空"的列表；修复后循环头改用
		// ReadUIntPositive抛ISE（校验收进buffer原语，容器decode与生成代码同款紧凑形态）。
		// 抛异常后bean整体作废，容器内容不保证保留。
		var list = newList(1, 2, 3);
		var bb = ByteBuffer.Wrap(new byte[]{(byte)0xF0, (byte)0x80, 0, 0, 0}); // 0x80000000，读回int为负
		assertThrows(IllegalStateException.class, () -> list.decode(bb));

		// 正常路径不变：clear后按流内容重建
		var ok = ByteBuffer.Allocate();
		ok.WriteUInt(2);
		ok.WriteInt(10);
		ok.WriteInt(20);
		list.decode(ok);
		assertEquals(List.of(10, 20), list.getList());
	}
}
