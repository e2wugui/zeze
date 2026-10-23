package Zeze.Transaction.Collections;

import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pcollections.Empty;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Map2TestSupport;
import Zeze.Transaction.Collections.SortedMap2Meta;

/** Sorted Map2 has the same payload/write-intent boundary as unordered Map2. */
@Fast
public class TestLogSortedMap2PayloadBoundary {

	@Test
	public void decodedPayloadCannotCommitOrBeginSavepoint() {
		var meta = SortedMap2Meta.get(Long.class, BValue.class);
		// leader侧编码一段含replaced的日志
		var leader = Map2TestSupport.emptyWirePayload(new LogSortedMap2<Long, BValue>(null, 0, null, Empty.sortedMap(), meta));
		var v1 = new BValue();
		v1.mapKey(1L);
		leader.getReplaced().put(1L, v1);
		var bb = ByteBuffer.Allocate();
		leader.encode(bb);

		// 回放侧解码：decoded态
		var follower = new LogSortedMap2<Long, BValue>(null, 0, null, Empty.sortedMap(), meta);
		follower.decode(bb);

		Assertions.assertThrows(UnsupportedOperationException.class, follower::commit);
		Assertions.assertThrows(UnsupportedOperationException.class, follower::beginSavepoint);
		Assertions.assertEquals(1, follower.getReplaced().size());
	}

	@Test
	public void sourcePayloadCannotCommitOrBeginSavepoint() {
		var meta = SortedMap2Meta.get(Long.class, BValue.class);
		var leader = new LogSortedMap2<Long, BValue>(null, 0, null, Empty.sortedMap(), meta);
		Assertions.assertThrows(UnsupportedOperationException.class, leader::commit);
		Assertions.assertThrows(UnsupportedOperationException.class, leader::beginSavepoint);
	}
}
