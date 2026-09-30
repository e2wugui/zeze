package Zeze.Transaction;

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
		Log.register(LogLong::new);
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
		assertEquals("original-key", value.mapKey());
		value.setLong2(7);
		assertEquals(3, original.getLong2(), "复制值的原位修改不得改变源记录");

		var change = new LogBean(null, 0, value);
		var field = new LogLong(2); // BValue.long2
		field.value = 7;
		change.getVariablesOrNew().put(2, field);
		@SuppressWarnings("unchecked")
		var log = (LogMap2<String, BValue>)copy.createLogBean();
		log.getChanged().add(change);
		var encoded = ByteBuffer.Allocate();
		log.encode(encoded);
		assertEquals(1, log.getChangedWithKey().size(), "copy后changed不可因缺失mapKey而被过滤");
		assertSame(change, log.getChangedWithKey().get("original-key"));

		var follower = source.copy();
		var followerValue = follower.get("original-key");
		@SuppressWarnings("unchecked")
		var decoded = (LogMap2<String, BValue>)follower.createLogBean();
		var input = ByteBuffer.Wrap(encoded.Bytes, encoded.ReadIndex, encoded.size());
		decoded.decode(input);
		assertTrue(input.isEmpty());
		follower.followerApply(decoded);
		assertSame(followerValue, follower.get("original-key"), "回放须修改原有值Bean而非换记录");
		assertEquals(7, followerValue.getLong2());
		assertEquals(1, follower.size());
	}
}
