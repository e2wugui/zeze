package Zeze.Transaction.Collections;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.Set;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.ChangeCaptureMode;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestSetRemoveAllLiveView {
	@Test
	@SuppressWarnings("unchecked")
	public void replaysRemovalFromLiveView() {
		var meta = Set1Meta.get(Integer.class);
		var source = new PSet1<>(meta);
		source.addAll(Set.of(1, 2));
		var follower = source.copy();
		var delta = (LogSet1Delta<Integer>)source.createLog(ChangeCaptureMode.DELTA);
		var view = new AbstractCollection<Integer>() {
			@Override
			public Iterator<Integer> iterator() {
				return delta.getValue().iterator();
			}

			@Override
			public int size() {
				return delta.getValue().size();
			}
		};

		assertTrue(delta.removeAll(view));
		assertFalse(delta.removeAll(view));
		delta.commit();

		var bb = ByteBuffer.Allocate();
		source.createLogBean(delta).encode(bb);
		follower.followerApply(LogSet1.decode(0, meta, bb));
		assertTrue(source.isEmpty());
		assertEquals(source, follower);
		assertTrue(bb.isEmpty());
	}
}
