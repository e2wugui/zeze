package Zeze.Transaction.Collections;

import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pcollections.Empty;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.SortedMap2Meta;

/**
 * LogSortedMap2.mergeChangedToReplaced 的 decoded 守卫回归（20261020-05）：
 * 守卫契约由 LogMap2 侧 3c96a10f8 确立（TestLogMap2MergeDecodedRejects 钉住），
 * 排序镜像此前漏接——解码态调用为裸 NPE（changedWithKey 非空）或静默 no-op（空），
 * 现对齐显式抛 ISE。
 */
@Fast
public class TestLogSortedMap2MergeDecodedRejects {

	@Test
	public void testDecodedLogMergeThrows() {
		var meta = SortedMap2Meta.get(Long.class, BValue.class);
		// leader侧编码一段含replaced的日志
		var leader = new LogSortedMap2<Long, BValue>(null, 0, null, Empty.sortedMap(), meta);
		var v1 = new BValue();
		v1.mapKey(1L);
		leader.getReplaced().put(1L, v1);
		var bb = ByteBuffer.Allocate();
		leader.encode(bb);

		// 回放侧解码：decoded态
		var follower = new LogSortedMap2<Long, BValue>(null, 0, null, Empty.sortedMap(), meta);
		follower.decode(bb);

		Assertions.assertThrows(IllegalStateException.class, follower::mergeChangedToReplaced,
				"解码态调用merge必须显式拒绝（旧行为：裸NPE/静默no-op）");
	}

	@Test
	public void testLeaderMergeUnaffected() {
		var meta = SortedMap2Meta.get(Long.class, BValue.class);
		var leader = new LogSortedMap2<Long, BValue>(null, 0, null, Empty.sortedMap(), meta);
		// leader态正常merge（含重复调用幂等）不受守卫影响
		Assertions.assertDoesNotThrow(leader::mergeChangedToReplaced);
		Assertions.assertDoesNotThrow(leader::mergeChangedToReplaced);
	}
}
