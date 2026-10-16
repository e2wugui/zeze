package Zeze.Collections;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pcollections.Empty;
import org.pcollections.PMap;
import org.pcollections.PSet;
import org.pcollections.PVector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 持久化容器三后端契约对齐测试：Kt 适配类（kt 循环 / ktt builder 双路径）vs pcollections 参照。
 *
 * <p>钉住的是 Zeze 事务集合写路径依赖的隐式契约（换内核不得漂移）：
 * <ul>
 *   <li><b>no-op 引用恒等</b>：写同值/删不存在/加已存在/批量全 no-op 返回同一实例
 *       （PList1.minus、PSet1.add/remove、LogMap1.plusAll 的 {@code !=} 判定依赖）；</li>
 *   <li><b>越界时机</b>：plus(int)/with/minus(int) 非法下标按 JDK 时机抛 IOOBE，
 *       持久化语义无部分修改；</li>
 *   <li><b>内容与 JDK 契约</b>：equals/hashCode/迭代与 ArrayList/HashMap/HashSet 模型逐 op 对账。</li>
 * </ul>
 *
 * <p>ktt 的 builder 批量路径经 {@link Persistents#BULK} 翻档覆盖（try/finally 复原）。
 */
@Fast
public class TestPersistentsBackendsContract {
	// ===== Vector =====

	@Test
	public void testVectorContentAndJdkContract() {
		var model = new ArrayList<>(List.of(0, 1, 2, 3, 4));
		PVector<Integer> v = KtVector.empty();
		for (var e : List.of(0, 1, 2, 3, 4))
			v = v.plus(e);
		assertVectorEquals(model, v);

		v = v.plus(0, -1); // 头插
		model.add(0, -1);
		assertVectorEquals(model, v);

		v = v.with(2, 99); // 覆盖
		model.set(2, 99);
		assertVectorEquals(model, v);

		v = v.plus(2, 55); // 中间插
		model.add(2, 55);
		assertVectorEquals(model, v);

		v = v.minus(1); // 按下标删
		model.remove(1);
		assertVectorEquals(model, v);

		v = v.plusAll(List.of(100, 101)); // 尾部批量
		model.addAll(List.of(100, 101));
		assertVectorEquals(model, v);

		v = v.plusAll(1, List.of(200, 201)); // 中间批量
		model.addAll(1, List.of(200, 201));
		assertVectorEquals(model, v);

		v = v.minus(Integer.valueOf(99)); // 按值删
		model.remove(Integer.valueOf(99));
		assertVectorEquals(model, v);

		v = v.minusAll(List.of(200, 201, 12345)); // 批量按值删（含不存在）
		model.removeAll(List.of(200, 201));
		assertVectorEquals(model, v);
	}

	@Test
	public void testVectorNoOpIdentity() {
		PVector<Integer> v = KtVector.empty();
		for (int i = 0; i < 100; i++)
			v = v.plus(i);

		Assertions.assertSame(v, v.minus(Integer.valueOf(-1)), "minus 不存在元素须返回同一实例");
		Assertions.assertSame(v, v.plusAll(List.of()), "plusAll 空集须返回同一实例");
		Assertions.assertSame(v, v.minusAll(List.of()), "minusAll 空集须返回同一实例");
		var bulkState = Persistents.BULK;
		try {
			Persistents.BULK = false;
			Assertions.assertSame(v, v.minusAll(List.of(-1, -2, -3)), "minusAll 全不存在（循环）须返回同一实例");
			Persistents.BULK = true;
			Assertions.assertSame(v, v.minusAll(List.of(-1, -2, -3)), "minusAll 全不存在（builder）须返回同一实例");
		} finally {
			Persistents.BULK = bulkState;
		}
		Assertions.assertNotSame(v, v.plus(42), "真变更须产生新实例");

		// pcollections 参照：同样契约（防"测试钉的是新行为而非历史行为"）
		PVector<Integer> ref = Empty.vector();
		for (int i = 0; i < 100; i++)
			ref = ref.plus(i);
		Assertions.assertSame(ref, ref.minus(Integer.valueOf(-1)));
		Assertions.assertSame(ref, ref.plusAll(List.of()));
		Assertions.assertSame(ref, ref.minusAll(List.of(-1, -2, -3)));
	}

	@Test
	public void testVectorIndexBounds() {
		PVector<Integer> v = KtVector.empty();
		for (int i = 0; i < 10; i++)
			v = v.plus(i);
		var fv = v;
		// List 契约：插入位合法域 [0, size]（==size 即尾追加），覆盖/删除位合法域 [0, size-1]。
		for (int bad : new int[]{-1, 11}) {
			Assertions.assertThrows(IndexOutOfBoundsException.class, () -> fv.plus(bad, 0), "plus(int) 越界");
			Assertions.assertThrows(IndexOutOfBoundsException.class, () -> fv.plusAll(bad, List.of(0)), "plusAll(int,) 越界");
		}
		for (int bad : new int[]{-1, 10, 11}) {
			Assertions.assertThrows(IndexOutOfBoundsException.class, () -> fv.with(bad, 0), "with 越界");
			Assertions.assertThrows(IndexOutOfBoundsException.class, () -> fv.minus(bad), "minus(int) 越界");
		}
		Assertions.assertEquals(10, v.size(), "越界失败后内容不变（持久化无部分修改）");
		// ==size 插入=尾追加（List 契约合法），与 pcollections 参照一致
		Assertions.assertEquals(11, fv.plus(10, 99).size());
		Assertions.assertEquals(2, Empty.vector().plusAll(List.of(0)).plusAll(1, List.of(1)).size());
		Assertions.assertThrows(UnsupportedOperationException.class, () -> fv.subList(0, 1), "KtVector subList 不支持（Zeze 层同禁）");
	}

	// ===== Map =====

	@Test
	public void testMapContentAndJdkContract() {
		var model = new HashMap<Integer, String>();
		PMap<Integer, String> m = KtMap.empty();
		for (int i = 0; i < 100; i++) {
			m = m.plus(i, "v" + i);
			model.put(i, "v" + i);
		}
		assertMapEquals(model, m);

		m = m.plus(5, "overwritten"); // 覆盖
		model.put(5, "overwritten");
		assertMapEquals(model, m);

		var batch = new HashMap<Integer, String>();
		batch.put(200, "x");
		batch.put(201, "y");
		batch.put(5, "z"); // 与批量覆盖混合
		m = m.plusAll(batch);
		model.putAll(batch);
		assertMapEquals(model, m);

		m = m.minus(200);
		model.remove(200);
		assertMapEquals(model, m);

		m = m.minusAll(List.of(201, 999)); // 批量删（含不存在）
		model.remove(201);
		assertMapEquals(model, m);
	}

	@Test
	public void testMapNoOpIdentity() {
		PMap<Integer, String> m = KtMap.empty();
		var values = new HashMap<Integer, String>(); // 持有与 m 内部一致的实例引用（不驻留构造）
		for (int i = 0; i < 100; i++) {
			var v = new String("v" + i);
			values.put(i, v);
			m = m.plus(i, v);
		}

		// 同实例 put：跳过（身份守卫，强于基线的纯优化）
		Assertions.assertSame(m, m.plus(5, values.get(5)), "put 同实例须返回同一实例");
		// 相等异实例：真实入核——PMap2/LogMap2 是可变 bean 值、按身份判等，不得按 equals 短路
		Assertions.assertNotSame(m, m.plus(5, new String("v5")), "put 相等异实例须新实例（对齐基线）");
		Assertions.assertEquals(100, m.size());
		Assertions.assertSame(m, m.minus(-1), "删不存在 key 须返回同一实例");
		Assertions.assertSame(m, m.plusAll(Map.of()), "plusAll 空 map 须返回同一实例");
		Assertions.assertSame(m, m.minusAll(List.of()), "minusAll 空集须返回同一实例");

		var eqDiff = new HashMap<Integer, String>();
		eqDiff.put(8, new String("v8")); // 相等异实例 → 须入核
		var allSameRef = Map.of(9, values.get(9)); // 全同实例 → no-op
		var bulkState = Persistents.BULK;
		try {
			Persistents.BULK = false;
			Assertions.assertNotSame(m, m.plusAll(eqDiff), "plusAll 相等异实例（循环）须新实例");
			Assertions.assertSame(m, m.plusAll(allSameRef), "plusAll 全同实例（循环）须返回同一实例");
			Assertions.assertSame(m, m.minusAll(List.of(-1, -2)), "minusAll 全不存在（循环）须返回同一实例");
			Persistents.BULK = true;
			Assertions.assertNotSame(m, m.plusAll(eqDiff), "plusAll 相等异实例（builder）须新实例");
			Assertions.assertEquals("v8", m.plusAll(eqDiff).get(8));
			Assertions.assertSame(m, m.plusAll(allSameRef), "plusAll 全同实例（builder）须返回同一实例");
			Assertions.assertSame(m, m.minusAll(List.of(-1, -2)), "minusAll 全不存在（builder）须返回同一实例");
		} finally {
			Persistents.BULK = bulkState;
		}

		// 混合批量（同实例 no-op + 真变更）：须新实例且内容正确
		var mixed = new HashMap<Integer, String>();
		mixed.put(7, values.get(7));
		mixed.put(300, "new");
		var m2 = m.plusAll(mixed);
		Assertions.assertNotSame(m, m2);
		Assertions.assertEquals("new", m2.get(300));
		Assertions.assertEquals(101, m2.size());

		// pcollections 基线存档（jshell 实测）：plus/plusAll 同值恒新实例——
		// LogMap1.putAll 的 newMap != old 判定在基线上恒真（死分支）；Kt 系列以身份守卫
		// 复活该判定，语义差异仅限"同实例 put 跳过"（内容与增量记录均不可观测）。
		PMap<Integer, String> ref = Empty.map();
		for (int i = 0; i < 100; i++)
			ref = ref.plus(i, "v" + i);
		Assertions.assertNotSame(ref, ref.plus(5, "v5"), "基线存档：pcollections put 同值恒新实例");
		Assertions.assertSame(ref, ref.minus(-1), "基线存档：remove 不存在恒等");
		Assertions.assertNotSame(ref, ref.plusAll(Map.of(5, "v5")), "基线存档：pcollections plusAll 同值恒新实例");
	}

	@Test
	public void testMapMutatorsReject() {
		PMap<Integer, String> m = KtMap.<Integer, String>empty().plus(1, "a");
		Assertions.assertThrows(UnsupportedOperationException.class, () -> m.put(2, "b"));
		Assertions.assertThrows(UnsupportedOperationException.class, () -> m.remove(1));
		Assertions.assertThrows(UnsupportedOperationException.class, () -> m.putAll(Map.of(2, "b")));
		Assertions.assertThrows(UnsupportedOperationException.class, m::clear);
	}

	// ===== Set =====

	@Test
	public void testSetContentAndJdkContract() {
		var model = new HashSet<Integer>();
		PSet<Integer> s = KtSet.empty();
		for (int i = 0; i < 100; i++) {
			s = s.plus(i);
			model.add(i);
		}
		assertSetEquals(model, s);

		s = s.plusAll(Set.copyOf(List.of(100, 101, 5))); // 含已存在
		model.addAll(Set.copyOf(List.of(100, 101, 5)));
		assertSetEquals(model, s);

		s = s.minus(100);
		model.remove(100);
		assertSetEquals(model, s);

		s = s.minusAll(List.of(101, 999));
		model.removeAll(List.of(101, 999));
		assertSetEquals(model, s);
	}

	@Test
	public void testSetNoOpIdentity() {
		PSet<Integer> s = KtSet.empty();
		for (int i = 0; i < 100; i++)
			s = s.plus(i);

		Assertions.assertSame(s, s.plus(5), "add 已存在须返回同一实例");
		Assertions.assertSame(s, s.minus(-1), "remove 不存在须返回同一实例");
		Assertions.assertSame(s, s.plusAll(List.of()), "plusAll 空集须返回同一实例");
		Assertions.assertSame(s, s.minusAll(List.of()), "minusAll 空集须返回同一实例");

		var allExist = List.of(1, 2, 3);
		var allAbsent = List.of(-1, -2, -3);
		var bulkState = Persistents.BULK;
		try {
			Persistents.BULK = false;
			Assertions.assertSame(s, s.plusAll(allExist), "plusAll 全已存在（循环）须返回同一实例");
			Assertions.assertSame(s, s.minusAll(allAbsent), "minusAll 全不存在（循环）须返回同一实例");
			Persistents.BULK = true;
			Assertions.assertSame(s, s.plusAll(allExist), "plusAll 全已存在（builder）须返回同一实例");
			Assertions.assertSame(s, s.minusAll(allAbsent), "minusAll 全不存在（builder）须返回同一实例");
		} finally {
			Persistents.BULK = bulkState;
		}

		// pcollections 参照（MapPSet 单项路径；其 plusAll 恒新建是已知历史坑，调用点已绕行，不在契约内）
		PSet<Integer> ref = Empty.set();
		for (int i = 0; i < 100; i++)
			ref = ref.plus(i);
		Assertions.assertSame(ref, ref.plus(5));
		Assertions.assertSame(ref, ref.minus(-1));
	}

	// ===== 断言工具 =====

	private static void assertVectorEquals(List<Integer> model, PVector<Integer> v) {
		Assertions.assertEquals(model.size(), v.size(), "size");
		Assertions.assertEquals(model, v, "内容 equals（List 契约，方向1）");
		Assertions.assertEquals(v, model, "内容 equals（方向2，对称性）");
		Assertions.assertEquals(model.hashCode(), v.hashCode(), "hashCode 契约");
		Assertions.assertArrayEquals(model.toArray(), v.toArray(), "toArray");
		for (int i = 0; i < model.size(); i++)
			Assertions.assertEquals(model.get(i), v.get(i), "get(" + i + ")");
		var iterated = new ArrayList<Integer>();
		for (var e : v)
			iterated.add(e);
		Assertions.assertEquals(model, iterated, "迭代内容（无序差异在 List 不存在，须全序一致）");
	}

	private static void assertMapEquals(Map<Integer, String> model, PMap<Integer, String> m) {
		Assertions.assertEquals(model.size(), m.size(), "size");
		Assertions.assertEquals(model, m, "内容 equals（Map 契约，方向1）");
		Assertions.assertEquals(m, model, "内容 equals（方向2，对称性）");
		Assertions.assertEquals(model.hashCode(), m.hashCode(), "hashCode 契约");
		for (var e : model.entrySet()) {
			Assertions.assertEquals(e.getValue(), m.get(e.getKey()), "get(" + e.getKey() + ")");
			Assertions.assertTrue(m.containsKey(e.getKey()));
			Assertions.assertTrue(m.containsValue(e.getValue()));
		}
		Assertions.assertFalse(m.containsKey(-1));
		var entries = new HashMap<Integer, String>();
		for (var e : m.entrySet())
			entries.put(e.getKey(), e.getValue());
		Assertions.assertEquals(model, entries, "entrySet 迭代内容");
	}

	private static void assertSetEquals(Set<Integer> model, PSet<Integer> s) {
		Assertions.assertEquals(model.size(), s.size(), "size");
		Assertions.assertEquals(model, s, "内容 equals（Set 契约，方向1）");
		Assertions.assertEquals(s, model, "内容 equals（方向2，对称性）");
		Assertions.assertEquals(model.hashCode(), s.hashCode(), "hashCode 契约");
		for (var e : model)
			Assertions.assertTrue(s.contains(e), "contains(" + e + ")");
		Assertions.assertFalse(s.contains(-1));
	}
}
