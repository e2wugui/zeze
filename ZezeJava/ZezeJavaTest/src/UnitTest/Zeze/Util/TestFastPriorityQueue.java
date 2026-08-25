package UnitTest.Zeze.Util;

import Zeze.Util.FastPriorityQueue;
import Zeze.Util.FastPriorityQueueNode;
import Zeze.Util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
	}

	@Test
	public void test() {
		var testCount = 10000;
		var fq = new FastPriorityQueue<Node>(0, Integer.MAX_VALUE);
		var nodes = new Node[testCount];
		for (int i = 0; i < testCount; i++)
			nodes[i] = new Node(i);
		Random.shuffle(nodes);

		Assertions.assertEquals(0, fq.maxSize());
		for (int i = 0; i < testCount; i++)
			fq.enqueue(nodes[i]);
		Assertions.assertEquals(testCount, fq.count());

		for (int i = 0; i < testCount; i++)
			Assertions.assertTrue(fq.contains(nodes[i]));
		Assertions.assertFalse(fq.contains(new Node(0)));
		Assertions.assertTrue(fq.isValidQueue());
		for (int i = 0; i < testCount; i++) {
			nodes[i].value = testCount - nodes[i].value - 1;
			fq.updatePriority(nodes[i]);
		}

		int n = 0;
		var nodeMark = new boolean[testCount];
		for (var node : fq) {
			Assertions.assertFalse(nodeMark[node.getQueueIndex()]);
			nodeMark[node.getQueueIndex()] = true;
			n++;
		}
		Assertions.assertEquals(testCount, n);

		Assertions.assertEquals(0, fq.first().value);
		var node = new Node(Integer.MIN_VALUE);
		fq.enqueue(node);
		Assertions.assertEquals(Integer.MIN_VALUE, fq.first().value);
		fq.remove(node);
		node.value = Integer.MAX_VALUE;
		fq.enqueue(node);
		fq.remove(node);

		Assertions.assertEquals(testCount, fq.count());
		for (int i = 0; i < testCount; i++)
			Assertions.assertEquals(i, fq.dequeue().value);
		Assertions.assertEquals(0, fq.count());

		fq.clear();
	}
}
