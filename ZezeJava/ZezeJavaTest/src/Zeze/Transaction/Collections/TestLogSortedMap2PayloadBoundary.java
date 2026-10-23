package Zeze.Transaction.Collections;

import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Map2TestSupport;
import Zeze.Transaction.Collections.SortedMap2Meta;

/** Sorted Map2 has the same payload/write-intent boundary as unordered Map2. */
@Fast
public class TestLogSortedMap2PayloadBoundary {

	@Test
	public void decodedPayloadCannotCommitOrBeginSavepoint() {
		var meta = SortedMap2Meta.get(Long.class, BValue.class);
		Zeze.History.Helper.registerLogSortedMap2Meta(meta);
		// leader侧编码一段含replaced的日志
		var leader = Map2TestSupport.emptyWirePayload(new LogSortedMap2<Long, BValue>(0, null, null, meta, null, null));
		var v1 = new BValue();
		v1.mapKey(1L);
		leader.getReplaced().put(1L, v1);
		var bb = ByteBuffer.Allocate();
		leader.encode(bb);

		// 回放侧解码：decoded态
		var follower = new LogSortedMap2<Long, BValue>(0, null, null, meta, null, null);
		follower = Zeze.Transaction.Map2TestSupport.decode(follower, bb);

		Assertions.assertFalse(Zeze.Transaction.TransactionLog.class.isInstance(follower));
		Assertions.assertTrue(java.util.Arrays.stream(LogSortedMap2.class.getMethods()).noneMatch(method -> method.getName().equals("commit")));
		Assertions.assertEquals(1, follower.getReplaced().size());
	}

	@Test
	public void sourcePayloadCannotCommitOrBeginSavepoint() {
		var meta = SortedMap2Meta.get(Long.class, BValue.class);
		Zeze.History.Helper.registerLogSortedMap2Meta(meta);
		var leader = new LogSortedMap2<Long, BValue>(0, null, null, meta, null, null);
		Assertions.assertFalse(Zeze.Transaction.TransactionLog.class.isInstance(leader));
		Assertions.assertTrue(java.util.Arrays.stream(LogSortedMap2.class.getMethods()).noneMatch(method -> method.getName().equals("commit")));
	}
}
