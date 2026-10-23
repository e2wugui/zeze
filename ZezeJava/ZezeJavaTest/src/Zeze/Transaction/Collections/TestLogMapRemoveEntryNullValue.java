package Zeze.Transaction.Collections;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pcollections.Empty;
import org.pcollections.TreePMap;

/**
 * LogMap1/LogSortedMap1.remove(K,V) 空值防御对齐回归（20261020-06）：
 * 托管路径旧形态 value.equals(old) 对 null 值 NPE，非托管分支（PMap1/PMap2/PSortedMap1/2
 * 的 exist!=null && exist.equals(v)）同输入安静返回 false——同输入两分支行为对齐为 false。
 */
@Fast
public class TestLogMapRemoveEntryNullValue {

	@Test
	public void testLogMap1RemoveNullValue() {
		var meta = Map1Meta.get(Long.class, Long.class);
		Zeze.History.Helper.registerLogMap1Meta(meta);
		var log = new LogMap1Delta<Long, Long>(new PMap1<>(meta), Empty.map());
		log.put(1L, 1L);
		Assertions.assertFalse(log.remove(1L, null), "null值删除须安静返回false（旧：NPE）");
		Assertions.assertFalse(log.remove(999L, null), "不存在key+null同样false");
		Assertions.assertTrue(log.remove(1L, 1L), "正常删除不受影响");
		Assertions.assertEquals(0, log.getValue().size());
	}

	@Test
	public void testLogSortedMap1RemoveNullValue() {
		var meta = SortedMap1Meta.get(Long.class, Long.class);
		Zeze.History.Helper.registerLogSortedMap1Meta(meta);
		var log = new LogSortedMap1Delta<Long, Long>(new PSortedMap1<>(meta), TreePMap.empty());
		log.put(1L, 1L);
		Assertions.assertFalse(log.remove(1L, null), "null值删除须安静返回false（旧：NPE）");
		Assertions.assertFalse(log.remove(999L, null), "不存在key+null同样false");
		Assertions.assertTrue(log.remove(1L, 1L), "正常删除不受影响");
		Assertions.assertEquals(0, log.getValue().size());
	}
}
