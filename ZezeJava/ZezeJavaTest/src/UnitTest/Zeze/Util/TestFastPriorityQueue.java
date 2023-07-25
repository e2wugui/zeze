package UnitTest.Zeze.Util;

import Zeze.Util.FastPriorityQueue;
import Zeze.Util.FastPriorityQueueNode;
import Zeze.Util.Random;
import org.junit.Assert;
import org.junit.Test;

public class TestFastPriorityQueue {
	public static final class Node implements FastPriorityQueueNode<Node> {
		private int value;
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
		var testCount = 10000;
		var fq = new FastPriorityQueue<Node>(0, Integer.MAX_VALUE);
		var nodes = new Node[testCount];
		for (int i = 0; i < testCount; i++)
			nodes[i] = new Node(i);
		Random.shuffle(nodes);

		Assert.assertEquals(0, fq.maxSize());
		for (int i = 0; i < testCount; i++)
			fq.enqueue(nodes[i]);
		Assert.assertEquals(testCount, fq.count());

		for (int i = 0; i < testCount; i++)
			Assert.assertTrue(fq.contains(nodes[i]));
		Assert.assertFalse(fq.contains(new Node(0)));
		Assert.assertTrue(fq.isValidQueue());
		for (int i = 0; i < testCount; i++) {
			nodes[i].value = testCount - nodes[i].value - 1;
			fq.updatePriority(nodes[i]);
		}

		int n = 0;
		var nodeMark = new boolean[testCount];
		for (var node : fq) {
			Assert.assertFalse(nodeMark[node.getQueueIndex()]);
			nodeMark[node.getQueueIndex()] = true;
			n++;
		}
		Assert.assertEquals(testCount, n);

		Assert.assertEquals(0, fq.first().value);
		var node = new Node(Integer.MIN_VALUE);
		fq.enqueue(node);
		Assert.assertEquals(Integer.MIN_VALUE, fq.first().value);
		fq.remove(node);
		node.value = Integer.MAX_VALUE;
		fq.enqueue(node);
		fq.remove(node);

		for (int i = 0; i < testCount; i++)
			Assert.assertEquals(i, fq.dequeue().value);

		fq.clear();
		Assert.assertEquals(0, fq.count());
	}
}
