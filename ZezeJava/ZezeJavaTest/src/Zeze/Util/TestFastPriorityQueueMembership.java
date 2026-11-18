package Zeze.Util;

import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestFastPriorityQueueMembership {
	private static final class Node implements FastPriorityQueueNode<Node> {
		private int queueIndex = -1;

		@Override
		public int getQueueIndex() {
			return queueIndex;
		}

		@Override
		public void setQueueIndex(int index) {
			queueIndex = index;
		}

		@Override
		public boolean hasHigherPriority(Node lower) {
			return false;
		}
	}

	@Test
	public void containsAcceptsNodeWithUnsetIndex() {
		var queue = new FastPriorityQueue<Node>(1, 1);
		var node = new Node();
		assertFalse(queue.contains(node));
		queue.enqueue(node);
		assertTrue(queue.contains(node));
		assertFalse(queue.contains(new Node()));
		assertTrue(queue.dequeue() == node);
		assertFalse(queue.contains(node));
	}
}
