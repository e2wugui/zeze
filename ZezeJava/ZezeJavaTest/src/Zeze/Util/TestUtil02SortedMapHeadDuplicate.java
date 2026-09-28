package Zeze.Util;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import Zeze.Util.SortedMap;

/**
 * FND15 util-02 回归：addEntry对"链头完全重复条目"（hash同且value.compareTo==0）返回-1，
 * 与链中c==0分支同判。修复前落到末尾return 0：条目未链接任何位置却被调用方计size——
 * size口径永久虚增（isEmpty恒false）。
 */
@Fast
public class TestUtil02SortedMapHeadDuplicate {

	// 链头完全重复：第二次add不得虚增size（修复前红：size()==2而keySize()==1）。
	@Test
	public final void testHeadDuplicateNotCounted() {
		var map = new SortedMap<String, Integer>();
		map.add("k", 1, 0);
		map.add("k", 1, 0); // 与链头hash同且compareTo==0
		Assertions.assertEquals(1, map.keySize(), "同key不应新增链");
		Assertions.assertEquals(1, map.size(), "链头完全重复不得计size（修复前虚增为2）");
	}

	// 链中重复（既有行为守护）：三节点链后重加中间节点，同判-1不计size。
	@Test
	public final void testMidChainDuplicateNotCounted() {
		var map = new SortedMap<String, Integer>();
		map.add("k", 1, 0);
		map.add("k", 2, 1);
		map.add("k", 3, 2);
		Assertions.assertEquals(1, map.keySize());
		Assertions.assertEquals(3, map.size());
		map.add("k", 2, 1); // 与链中节点hash同且compareTo==0（hash由value+index决定，(2,1)重复）
		Assertions.assertEquals(3, map.size(), "链中重复既有路径不得回归");
	}

	// 正常插入（守护）：同key不同value/index仍正常挂链计size。
	@Test
	public final void testDistinctEntriesStillAdded() {
		var map = new SortedMap<String, Integer>();
		map.add("k", 1, 0);
		map.add("k", 2, 1);
		Assertions.assertEquals(1, map.keySize());
		Assertions.assertEquals(2, map.size());
		Assertions.assertFalse(map.isEmpty());
	}
}
