package Zeze.Transaction.GTable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BeanMap1/BeanMap2（GTable行视图的底层Map）的equals收窄到私有家族
 * （instanceof BeanMap*），与同内容JDK Map不对称：JDK方向按Map契约内容相等为
 * true，BeanMap方向为false——HashSet去重、HashMap键命中等依赖比较方向，
 * 等值性随插入顺序漂移。与PList/PMap族已完成的对称化修复同型。
 *
 * 修复：放宽为instanceof Map按内容比较（委托pMap），并补内容化hashCode
 * （equals内容化后hashCode不同步会破坏Map契约）。
 */
@Fast
public class TestGTableBeanMapEqualsSymmetry {

	@Test
	public void beanMap1EqualsSymmetricWithHashMap() {
		// BeanMap1是值类型列（GTable1），Map1Meta按设计拒绝Bean值
		var p = new BeanMap1<Integer, String>(Integer.class, String.class);
		p.put(1, "x");
		Map<Integer, String> j = new HashMap<>(Map.of(1, "x"));

		assertTrue(j.equals(p), "JDK方向：HashMap.equals(BeanMap1)按Map契约为true");
		assertTrue(p.equals(j), "BeanMap1方向必须对称（修复前instanceof收窄为false）");

		var setA = new HashSet<Object>();
		setA.add(p);
		setA.add(j);
		assertEquals(1, setA.size(), "等值对象去重不得依赖插入顺序");
		assertEquals(p.hashCode(), j.hashCode(), "内容化hashCode须与等值JDK Map一致");
	}

	@Test
	public void beanMap2EqualsSymmetricWithHashMap() {
		var p = new BeanMap2<Integer, BValue, Object>(Integer.class, BValue.class);
		var v = new BValue();
		v.setInt_1(7);
		p.put(1, v);
		Map<Integer, BValue> j = new HashMap<>();
		j.put(1, v);

		assertTrue(j.equals(p), "JDK方向：HashMap.equals(BeanMap2)按Map契约为true");
		assertTrue(p.equals(j), "BeanMap2方向必须对称（修复前instanceof收窄为false）");
		assertEquals(p.hashCode(), j.hashCode());
	}

	@Test
	public void sameFamilyEqualsStillWorks() {
		var p1 = new BeanMap1<Integer, String>(Integer.class, String.class);
		var p2 = new BeanMap1<Integer, String>(Integer.class, String.class);
		p1.put(1, "x");
		p2.put(1, "x");

		assertTrue(p1.equals(p2), "同内容BeanMap1之间按内容相等（既有语义保留）");
		assertEquals(p1.hashCode(), p2.hashCode());
	}
}
