package Zeze.Transaction.Collections;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.List;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.ChangeCaptureMode;
import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestListAppendLiveView {
	@Test
	public void replaysOnlyTheAppendedScalarValues() {
		var meta = List1Meta.get(Integer.class);
		var source = new PList1<>(meta);
		source.add(1);
		var follower = source.copy();
		var delta = appendLiveView(source);

		var bb = ByteBuffer.Allocate();
		source.createLogBean(delta).encode(bb);
		follower.followerApply(LogList1.decode(0, meta, bb));
		assertEquals(List.of(1, 1), source);
		assertEquals(source, follower);
		assertTrue(bb.isEmpty());
	}

	@Test
	public void replaysOnlyTheAppendedBeanValues() {
		var source = new PList2<>(BValue.class);
		var follower = new PList2<>(BValue.class);
		var value = new BValue();
		value.setInt_1(7);
		source.add(value);
		follower.add(value.copy());
		var delta = appendLiveView(source);

		var payload = (LogList2<BValue>)source.createLogBean(delta);
		var bb = ByteBuffer.Allocate();
		payload.encode(bb);
		follower.followerApply(LogList2.decode(0, payload.meta, bb));
		assertEquals(2, source.size());
		assertEquals(2, follower.size());
		assertEquals(7, follower.get(1).getInt_1());
		assertTrue(bb.isEmpty());
	}

	@SuppressWarnings("unchecked")
	private static <V> LogList<V> appendLiveView(PList<V> source) {
		var delta = (LogList<V>)source.createLog(ChangeCaptureMode.DELTA);
		var view = new AbstractCollection<V>() {
			@Override
			public Iterator<V> iterator() {
				return delta.getValue().iterator();
			}

			@Override
			public int size() {
				return delta.getValue().size();
			}
		};
		assertTrue(delta.addAll(view));
		delta.commit();
		return delta;
	}
}
