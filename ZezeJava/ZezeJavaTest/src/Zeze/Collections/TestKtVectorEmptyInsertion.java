package Zeze.Collections;

import java.util.List;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.pcollections.Empty;
import org.pcollections.PVector;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestKtVectorEmptyInsertion {
	@Test
	void anEmptyBatchStillChecksItsInsertionPosition() {
		var reference = Empty.<Integer>vector().plus(42);
		for (var empty : List.<PVector<Integer>>of(KtVector.empty(), MyKtVector.empty())) {
			var vector = empty.plus(42);
			for (int index : new int[]{-1, 2, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
				assertThrows(IndexOutOfBoundsException.class, () -> reference.plusAll(index, List.of()));
				assertThrows(IndexOutOfBoundsException.class, () -> vector.plusAll(index, List.of()),
						vector.getClass().getSimpleName() + " index=" + index);
			}
			assertSame(vector, vector.plusAll(0, List.of()));
			assertSame(vector, vector.plusAll(1, List.of()));
			assertEquals(List.of(42), vector);
			assertSame(empty, empty.plusAll(0, List.of()));
			assertThrows(IndexOutOfBoundsException.class, () -> empty.plusAll(1, List.of()));
		}
	}
}
