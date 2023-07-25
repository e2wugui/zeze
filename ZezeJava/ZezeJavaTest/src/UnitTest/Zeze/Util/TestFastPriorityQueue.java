package UnitTest.Zeze.Util;

import Zeze.Util.FastPriorityQueue;
import Zeze.Util.FastPriorityQueueNode;
import org.junit.Assert;
import org.junit.Test;

public class TestFastPriorityQueue {
	public static final class Node implements FastPriorityQueueNode<Node> {
		private final int value;
		private int index;

		public Node(int value) {
			this.value = value;
		}

		@Override
		public int getQueueIndex() {
			return index;
		}

		@Override
		public void setQueueIndex(int index) {
			this.index = index;
		}

		@Override
		public boolean hasHigherPriority(Node lower) {
			return value < lower.value;
		}

		@Override
		public boolean hasHigherOrEqualPriority(Node lower) {
			return value <= lower.value;
		}
	}

	@Test
	public void test() {
		var fq = new FastPriorityQueue<Node>(100, Integer.MAX_VALUE);
		fq.enqueue(new Node(3));
		fq.enqueue(new Node(1));
		fq.enqueue(new Node(5));
		fq.enqueue(new Node(4));
		fq.enqueue(new Node(2));
		for (int i = 1; i <= 5; i++)
			Assert.assertEquals(i, fq.dequeue().value);

	}
}
