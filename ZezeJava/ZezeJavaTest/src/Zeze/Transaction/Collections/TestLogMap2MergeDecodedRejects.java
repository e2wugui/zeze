package Zeze.Transaction.Collections;

import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pcollections.Empty;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.Map2Meta;

/**
 * LogMap2.mergeChangedToReplaced 对解码态日志行为未定义且无报错：decoded 状态下
 * changed 为空、changedWithKey 才是完整回放日志，merge 把解码重建的 bean 塞进
 * replaced——增量丢失/错挂。History/回放侧若误调将静默产生错误结果。
 *
 * 修复：decoded 态调用显式抛 ISE（leader-only 契约由异常承载）。
 */
@Fast
public class TestLogMap2MergeDecodedRejects {

	@Test
	public void testDecodedLogMergeThrows() {
		var meta = Map2Meta.get(Long.class, BValue.class);
		// leader侧编码一段含replaced的日志
		var leader = new LogMap2<Long, BValue>(null, 0, null, Empty.map(), meta);
		var v1 = new BValue();
		v1.mapKey(1L);
		leader.getReplaced().put(1L, v1);
		var bb = ByteBuffer.Allocate();
		leader.encode(bb);

		// 回放侧解码：decoded态
		var follower = new LogMap2<Long, BValue>(null, 0, null, Empty.map(), meta);
		follower.decode(bb);

		Assertions.assertThrows(IllegalStateException.class, follower::mergeChangedToReplaced,
				"解码态调用merge必须显式拒绝（旧行为：静默no-op/错挂）");
	}

	@Test
	public void testLeaderMergeUnaffected() {
		var meta = Map2Meta.get(Long.class, BValue.class);
		var leader = new LogMap2<Long, BValue>(null, 0, null, Empty.map(), meta);
		// leader态正常merge（含重复调用幂等）不受守卫影响
		Assertions.assertDoesNotThrow(leader::mergeChangedToReplaced);
		Assertions.assertDoesNotThrow(leader::mergeChangedToReplaced);
	}
}
