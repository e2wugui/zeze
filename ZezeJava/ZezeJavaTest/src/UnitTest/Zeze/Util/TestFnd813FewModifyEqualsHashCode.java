package UnitTest.Zeze.Util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import Zeze.Util.FewModifyList;
import Zeze.Util.FewModifyMap;
import Zeze.Util.FewModifySortedMap;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-13回归：FewModify*家族直接implements Map/List/NavigableMap未覆写
 * equals/hashCode，落回Object身份相等——违反集合契约（Map.equals规定按内容比较
 * 且跨实现对称）：内容相同的FewModifyMap与HashMap单向不相等（hashMap.equals(fm)
 * 为true、fm.equals(hashMap)为false），clone()与自身内容恒等却判不等，
 * 入HashSet后按内容定位失败。修复：基于prepareRead()快照的内容比较（接受任意
 * Map/List，跨实现对称），hashCode同源；相等性为调用时刻快照。
 */
@Fast
public class TestFnd813FewModifyEqualsHashCode {

	@Test
	public void testFewModifyMapContentEqualsSymmetric() {
		var fm = new FewModifyMap<String, Integer>();
		fm.put("a", 1);
		fm.put("b", 2);
		Map<String, Integer> hm = new HashMap<>(Map.of("a", 1, "b", 2));

		// 修复前红：fm.equals(hm)为false（身份），hashMap.equals(fm)为true——不对称
		Assertions.assertEquals(hm, fm, "同内容HashMap.equals(FewModifyMap)必须为真");
		Assertions.assertEquals(fm, hm, "FewModifyMap.equals(同内容HashMap)必须为真（对称）");
		Assertions.assertEquals(hm.hashCode(), fm.hashCode());

		// clone内容恒等
		Assertions.assertEquals(fm, Assertions.assertDoesNotThrow(fm::clone));
		// 自身与内容跟踪
		Assertions.assertEquals(fm, fm);
		var other = new FewModifyMap<String, Integer>();
		other.put("a", 1);
		Assertions.assertNotEquals(fm, other);
		Assertions.assertNotEquals(fm, new Object());

		// 写后相等性跟随调用时刻快照
		other.put("b", 2);
		Assertions.assertEquals(fm, other);
		other.put("c", 3);
		Assertions.assertNotEquals(fm, other);

		// 内容hashCode使HashSet按内容定位（修复前按身份定位失败）
		var set = new HashSet<FewModifyMap<String, Integer>>();
		set.add(fm);
		Assertions.assertTrue(set.contains(new FewModifyMap<>(Map.of("a", 1, "b", 2))));
		Assertions.assertFalse(set.contains(new FewModifyMap<>(Map.of("a", 1))));
	}

	@Test
	public void testFewModifyListContentEqualsSymmetric() {
		var fl = new FewModifyList<String>();
		fl.add("x");
		fl.add("y");
		List<String> al = Arrays.asList("x", "y");

		// 修复前红：fl.equals(al)为false而al.equals(fl)为true
		Assertions.assertEquals(al, fl);
		Assertions.assertEquals(fl, al);
		Assertions.assertEquals(al.hashCode(), fl.hashCode());
		Assertions.assertEquals(fl, Assertions.assertDoesNotThrow(fl::clone));

		fl.add("z");
		Assertions.assertNotEquals(al, fl); // 快照跟随内容
		Assertions.assertEquals(Arrays.asList("x", "y", "z"), fl);
	}

	@Test
	public void testFewModifySortedMapContentEqualsSymmetric() {
		var fs = new FewModifySortedMap<String, Integer>();
		fs.put("k1", 1);
		fs.put("k2", 2);
		var tm = new TreeMap<String, Integer>();
		tm.put("k1", 1);
		tm.put("k2", 2);

		// 修复前红：fs.equals(tm)为false而tm.equals(fs)为true
		Assertions.assertEquals(tm, fs);
		Assertions.assertEquals(fs, tm);
		Assertions.assertEquals(tm.hashCode(), fs.hashCode());
		Assertions.assertEquals(fs, Assertions.assertDoesNotThrow(fs::clone));

		fs.put("k3", 3);
		Assertions.assertNotEquals(tm, fs);
		tm.put("k3", 3);
		Assertions.assertEquals(tm, fs);
	}
}
