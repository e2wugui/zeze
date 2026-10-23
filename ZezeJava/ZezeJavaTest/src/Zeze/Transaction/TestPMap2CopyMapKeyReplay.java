package Zeze.Transaction;

import static java.util.Objects.requireNonNull;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Transaction.Collections.PMap2;
import Zeze.Transaction.Logs.LogLong;
import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestPMap2CopyMapKeyReplay {
	static {
		Zeze.Transaction.LogRegistry.register(new Zeze.Transaction.LogRegistry.Factory(LogLong.TYPE_ID, "LogLong", LogLong::decode));
	}

	@Test
	public void copiedBeanCanEmitInPlaceChangesThatReplayToTheOriginalKey() {
		var source = new PMap2<String, BValue>(String.class, BValue.class);
		var original = new BValue();
		original.setLong2(3);
		source.put("original-key", original);
		var copy = source.copy();
		var value = copy.get("original-key");
		assertNotSame(original, value);
		assertEquals("original-key", requireNonNull(value).mapKey());
		value.setLong2(7);
		assertEquals(3, original.getLong2(), "复制值的原位修改不得改变源记录");

		var change = new LogBean(0, value);
		var field = Map2TestSupport.longLog(2, 7);
		change.getVariablesOrNew().put(2, field);
		@SuppressWarnings("unchecked")
		var log = (LogMap2<String, BValue>)Map2TestSupport.collect(copy, change);
		var encoded = ByteBuffer.Allocate();
		log.encode(encoded);
		assertEquals(1, log.getChangedWithKey().size(), "copy后changed不可因缺失mapKey而被过滤");
		assertSame(change, log.getChangedWithKey().get("original-key"));

		var follower = source.copy();
		var followerValue = follower.get("original-key");
		@SuppressWarnings("unchecked")
		var decoded = Map2TestSupport.wireLog(follower);
		var input = ByteBuffer.Wrap(encoded.Bytes, encoded.ReadIndex, encoded.size());
		decoded = Zeze.Transaction.Map2TestSupport.decode(decoded, input);
		assertTrue(input.isEmpty());
		follower.followerApply(decoded);
		assertSame(followerValue, follower.get("original-key"), "回放须修改原有值Bean而非换记录");
		assertEquals(7, requireNonNull(followerValue).getLong2());
		assertEquals(1, follower.size());
	}
}
