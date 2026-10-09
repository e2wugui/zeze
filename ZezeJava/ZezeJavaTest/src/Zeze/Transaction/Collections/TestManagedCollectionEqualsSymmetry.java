package Zeze.Transaction.Collections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 受管集合实现JDK集合接口，equals必须与同内容JDK对象对称：
 * 旧实现收窄到私有实现家族（instanceof PList/PMap/PSet/PSortedMap），
 * JDK集合从反方向按接口内容比较——p.equals(j)==false而j.equals(p)==true，
 * HashSet去重、HashMap键命中等依赖比较方向，等值性随插入顺序漂移。
 */
@Fast
public class TestManagedCollectionEqualsSymmetry {

	@Test
	public void listEqualsSymmetricWithArrayList() {
		PList1<Integer> p = new PList1<>(Integer.class);
		p.add(1);
		List<Integer> j = new ArrayList<>(List.of(1));

		assertTrue(j.equals(p), "JDK方向：ArrayList.equals(PList)按List契约为true");
		assertTrue(p.equals(j), "PList方向必须对称（修复前instanceof收窄为false）");

		var setA = new HashSet<Object>();
		setA.add(p);
		setA.add(j);
		assertEquals(1, setA.size(), "等值对象去重不得依赖插入顺序（修复前PList先入为2）");
		var setB = new HashSet<Object>();
		setB.add(j);
		setB.add(p);
		assertEquals(1, setB.size(), "反序插入同样去重为1");
		assertEquals(setA, setB);
	}

	@Test
	public void mapEqualsSymmetricWithHashMap() {
		PMap1<Integer, Integer> p = new PMap1<>(Integer.class, Integer.class);
		p.put(1, 1);
		Map<Integer, Integer> j = new HashMap<>(Map.of(1, 1));

		assertTrue(j.equals(p));
		assertTrue(p.equals(j), "PMap方向必须对称（修复前为false）");
	}

	@Test
	public void setEqualsSymmetricWithHashSet() {
		PSet1<Integer> p = new PSet1<>(Integer.class);
		p.add(1);
		Set<Integer> j = new HashSet<>(Set.of(1));

		assertTrue(j.equals(p));
		assertTrue(p.equals(j), "PSet方向必须对称（修复前为false）");
	}

	@Test
	public void sortedMapEqualsSymmetricWithTreeMap() {
		PSortedMap1<Integer, Integer> p = new PSortedMap1<>(Integer.class, Integer.class);
		p.put(1, 1);
		Map<Integer, Integer> j = new TreeMap<>(Map.of(1, 1));

		assertTrue(j.equals(p));
		assertTrue(p.equals(j), "PSortedMap方向必须对称（修复前为false）");
	}
}
