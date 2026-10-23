package Zeze.Transaction.Collections;

import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pcollections.Empty;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Map2TestSupport;
import Zeze.Transaction.Collections.Map2Meta;

/** Canonical source and decoded Map2 payloads cannot be used as transaction write intents. */
@Fast
public class TestLogMap2PayloadBoundary {

	@Test
	public void decodedPayloadCannotCommitOrBeginSavepoint() {
		var meta = Map2Meta.get(Long.class, BValue.class);
		// leader侧编码一段含replaced的日志
		var leader = Map2TestSupport.emptyWirePayload(new LogMap2<Long, BValue>(null, 0, null, Empty.map(), meta));
		var v1 = new BValue();
		v1.mapKey(1L);
		leader.getReplaced().put(1L, v1);
		var bb = ByteBuffer.Allocate();
		leader.encode(bb);

		// 回放侧解码：decoded态
		var follower = new LogMap2<Long, BValue>(null, 0, null, Empty.map(), meta);
		follower.decode(bb);

		Assertions.assertThrows(UnsupportedOperationException.class, follower::commit);
		Assertions.assertThrows(UnsupportedOperationException.class, follower::beginSavepoint);
		Assertions.assertEquals(1, follower.getReplaced().size());
	}

	@Test
	public void sourcePayloadCannotCommitOrBeginSavepoint() {
		var meta = Map2Meta.get(Long.class, BValue.class);
		var leader = new LogMap2<Long, BValue>(null, 0, null, Empty.map(), meta);
		Assertions.assertThrows(UnsupportedOperationException.class, leader::commit);
		Assertions.assertThrows(UnsupportedOperationException.class, leader::beginSavepoint);
	}
}
