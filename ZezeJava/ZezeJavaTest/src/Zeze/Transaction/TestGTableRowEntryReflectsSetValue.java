package Zeze.Transaction;

import Zeze.Transaction.GTable.GTable1;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 行视图entry的setValue语义（Map.Entry契约）：写入成功后，本entry的
 * getValue/equals/hashCode必须反映新值——delegate是pcollections不可变entry，
 * 转发读它的值会让调用方刚写入的entry读到旧值（随后据此比较、序列化或
 * 继续处理得到旧数据）。实际表数据经Row.put正确更新，仅entry视图读旧。
 */
@Fast
public class TestGTableRowEntryReflectsSetValue {

	@Test
	public void rowEntryReflectsOwnSetValue() {
		var table = new GTable1<Integer, Integer, Integer>(Integer.class, Integer.class, Integer.class);
		table.put(1, 2, 3);
		var entry = table.row(1).entrySet().iterator().next();

		assertEquals(3, entry.getValue());
		var old = entry.setValue(4);

		assertEquals(3, old, "setValue返回旧值");
		assertEquals(4, entry.getValue(), "setValue成功后entry必须读到新值（修复前仍读delegate旧值3）");
		assertEquals(4, table.get(1, 2), "实际表数据正确更新");

		var entryB = table.row(1).entrySet().iterator().next();
		assertEquals(entry, entryB, "同键同值的entry相等（equals经getValue读新值）");
		assertEquals(entry.hashCode(), entryB.hashCode(), "hashCode与equals契约一致");

		entry.setValue(5);
		assertNotEquals(entry, entryB, "值不同则不等（修复前equals比较恒读旧值，修改不可见）");
		assertEquals(5, table.get(1, 2));
	}
}
