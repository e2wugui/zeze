package Zeze.Collections;

import java.util.List;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.pcollections.Empty;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestKtVectorEmptyInsertion {
	@Test
	void anEmptyBatchStillChecksItsInsertionPosition() {
		var reference = Empty.<Integer>vector().plus(42);
		var vector = KtVector.<Integer>empty().plus(42);
		for (int index : new int[]{-1, 2, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
			assertThrows(IndexOutOfBoundsException.class, () -> reference.plusAll(index, List.of()));
			assertThrows(IndexOutOfBoundsException.class, () -> vector.plusAll(index, List.of()), "index=" + index);
		}
		assertSame(vector, vector.plusAll(0, List.of()));
		assertSame(vector, vector.plusAll(1, List.of()));
		assertEquals(List.of(42), vector);
		var empty = KtVector.<Integer>empty();
		assertSame(empty, empty.plusAll(0, List.of()));
		assertThrows(IndexOutOfBoundsException.class, () -> empty.plusAll(1, List.of()));
	}
}
