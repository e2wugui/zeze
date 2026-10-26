package Zeze.Util;

import java.util.NoSuchElementException;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestFastPriorityQueueIterator {

	@Test
	public void emptyIteratorsReportExhaustionRegardlessOfBackingCapacity() {
		for (var capacity : new int[]{0, 8}) {
			var queue = new FastPriorityQueue<TestFastPriorityQueue.Node>(capacity, 8);
			var iterator = queue.iterator();
			assertFalse(iterator.hasNext());
			assertThrows(NoSuchElementException.class, iterator::next);
			assertThrows(NoSuchElementException.class, iterator::next);
		}
	}

	@Test
	public void consumingTheLastNodeLeavesTheIteratorExhausted() {
		var queue = new FastPriorityQueue<TestFastPriorityQueue.Node>(2, 8);
		var node = new TestFastPriorityQueue.Node(10);
		queue.enqueue(node);
		var iterator = queue.iterator();
		assertSame(node, iterator.next());
		assertFalse(iterator.hasNext());
		assertThrows(NoSuchElementException.class, iterator::next);
		assertThrows(NoSuchElementException.class, iterator::next);
	}
}
