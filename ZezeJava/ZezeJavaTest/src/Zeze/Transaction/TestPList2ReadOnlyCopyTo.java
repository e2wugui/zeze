package Zeze.Transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import demo.Bean1;
import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Test;

/**
 * kimi-audit01 T-23回归：PList2ReadOnly.copyTo/toArray直接泄漏可变Bean元素。
 * 2系ReadOnly契约是"元素以VReadOnly暴露"（PMap2ReadOnly/PSortedMap2ReadOnly的copyTo
 * 均声明为VReadOnly[]），唯独PList2ReadOnly.copyTo声明为可变V[]、toArray()把受管活Bean
 * 原样放进数组返回——ro.copyTo(arr)即得全部活Bean可变引用，只读视图被静默绕过。
 * 修复：copyTo签名改为VReadOnly[]（对齐Map家族），toArray移除（2系Map家族无此方法，
 * 且无法返回只读化元素）。本测试同时是编译期契约锁定：copyTo只接受只读类型数组。
 */
@Fast
public class TestPList2ReadOnlyCopyTo {

	@Test
	public void testCopyToFillsReadOnlyArray() {
		var bv = new BValue();
		var b1 = new Bean1();
		b1.setV1(1);
		var b2 = new Bean1();
		b2.setV1(2);
		var list = bv.getList9(); // PList2<demo.Bean1>
		list.add(b1);
		list.add(b2);

		// 修复前该调用无法编译（copyTo只接受可变V[]）；修复后只接受只读数组
		demo.Bean1ReadOnly[] arr = new demo.Bean1ReadOnly[2];
		bv.getList9ReadOnly().copyTo(arr, 0);

		assertEquals(2, bv.getList9ReadOnly().size());
		assertSame(bv.getList9ReadOnly().get(0), arr[0], "copyTo填充的必须是与get()一致的只读视图元素");
		assertSame(bv.getList9ReadOnly().get(1), arr[1]);
	}
}
