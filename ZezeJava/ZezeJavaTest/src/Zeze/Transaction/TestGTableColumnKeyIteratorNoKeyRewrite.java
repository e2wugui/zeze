package Zeze.Transaction;

import Zeze.Transaction.GTable.GTable2;
import demo.Module1.BSimple;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 列键去重迭代（columnKeySet的iterator）不得改写受管value bean：
 * 旧实现的seen去重表借用行map工厂，GTable2下产出受管BeanMap2——
 * seen.put(colKey, 活受管value)对每个唯一列的bean做mapKey字段写
 * （迭代只读却改写对象状态），并强引用全部列bean。去重表只存键。
 */
@Fast
public class TestGTableColumnKeyIteratorNoKeyRewrite {

	@Test
	public void columnKeyIterationLeavesValueMapKeyUntouched() {
		var table = new GTable2<Integer, Integer, BSimple, demo.Module1.BSimpleReadOnly>(
				Integer.class, Integer.class, BSimple.class);
		var a = new BSimple();
		a.setInt_1(1);
		table.put(1, 10, a); // put本身设置value的mapKey（合法，=列键10）
		table.put(1, 20, a); // 同bean放第二列（非受管路径允许），mapKey=20

		var it = table.columnKeySet().iterator();
		assertEquals(10, it.next(), "第一列键");
		// 迭代是只读操作：seen去重表不得对value bean做mapKey字段写
		// （修复前seen.put(10,a)把mapKey从20改写回10，靠"覆写值恒等"侥幸无害）。
		assertEquals(20, a.mapKey(), "列键迭代不得对value bean做mapKey字段写");
		assertEquals(20, it.next(), "第二列键");
		assertEquals(20, a.mapKey());
	}
}
