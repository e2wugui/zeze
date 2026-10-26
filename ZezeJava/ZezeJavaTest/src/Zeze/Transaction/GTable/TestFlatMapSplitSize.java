package Zeze.Transaction.GTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestFlatMapSplitSize {
	@Test
	void transferringLastPrefixLeavesAnEmptyRemainder() {
		var flat = CollectSpliterators2.flatMap(List.of(List.of(1, 2, 3)).spliterator(),
				List::spliterator, Spliterator.SIZED | Spliterator.ORDERED, 3);
		assertTrue(flat.tryAdvance(value -> assertEquals(1, value)));
		var prefix = flat.trySplit();
		assertNotNull(prefix);
		assertEquals(List.of(2, 3), StreamSupport.stream(prefix, false).toList());
		assertEquals(0, StreamSupport.stream(flat, false).count(), "转交的前缀不能继续计入原分支");
	}

	@Test
	void transferringPrefixKeepsTheUnsplittableOuterRemainder() {
		var rows = List.of(List.of(1, 2, 3), List.of(4, 5));
		var outer = new Spliterators.AbstractSpliterator<List<Integer>>(2, Spliterator.ORDERED | Spliterator.SIZED) {
			private int index;
			@Override public Spliterator<List<Integer>> trySplit() { return null; }
			@Override public boolean tryAdvance(java.util.function.Consumer<? super List<Integer>> action) {
				if (index == rows.size())
					return false;
				action.accept(rows.get(index++));
				return true;
			}
		};
		var flat = CollectSpliterators2.flatMap(outer, List::spliterator,
				Spliterator.SIZED | Spliterator.ORDERED, 5);
		var output = new ArrayList<Integer>();
		assertTrue(flat.tryAdvance(output::add));
		var prefix = flat.trySplit();
		assertNotNull(prefix);
		prefix.forEachRemaining(output::add);
		assertEquals(List.of(4, 5), StreamSupport.stream(flat, false).toList());
		assertEquals(List.of(1, 2, 3), output);
	}
}
