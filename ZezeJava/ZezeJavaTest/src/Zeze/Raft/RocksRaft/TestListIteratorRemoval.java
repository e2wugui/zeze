package Zeze.Raft.RocksRaft;

import java.util.List;
import java.util.ConcurrentModificationException;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestListIteratorRemoval {
	private static CollList1<String> values() {
		var list = new CollList1<>(String.class);
		for (var value : List.of("same", "middle", "same", "tail"))
			list.add(value);
		return list;
	}

	@Test
	public void removesTheReturnedOccurrenceAmongDuplicates() {
		var list = values();
		var iterator = list.iterator();
		iterator.next();
		iterator.next();
		assertEquals("same", iterator.next());
		iterator.remove();
		assertEquals(List.of("same", "middle", "tail"), list.list);
		assertEquals("tail", iterator.next());
		iterator.remove();
		assertEquals(List.of("same", "middle"), list.list);
	}

	@Test
	public void permitsOneRemovalPerSuccessfulNext() {
		var list = values();
		var iterator = list.iterator();
		iterator.next();
		iterator.remove();
		assertThrows(IllegalStateException.class, iterator::remove);
		assertEquals(List.of("middle", "same", "tail"), list.list);
	}

	@Test
	public void rejectsRemovalBeforeNext() {
		var list = values();
		assertThrows(IllegalStateException.class, () -> list.iterator().remove());
		assertEquals(List.of("same", "middle", "same", "tail"), list.list);
	}

	@Test
	public void removesEachSnapshotElementOnce() {
		var list = values();
		var iterator = list.iterator();
		while (iterator.hasNext()) {
			iterator.next();
			iterator.remove();
		}
		assertTrue(list.isEmpty());
	}

	@Test
	public void rejectsRemovalAfterExternalStructuralChange() {
		var list = values();
		var iterator = list.iterator();
		iterator.next();
		list.add(0, "new");
		assertThrows(ConcurrentModificationException.class, iterator::remove);
		assertEquals(List.of("new", "same", "middle", "same", "tail"), list.list);
	}

	@Test
	public void recordsRemovalAtTheReturnedPositionInTheTransactionLog() {
		var list = values();
		list.variableId(1);
		var parent = new CollList1<>(String.class);
		list.initRootInfo(new Record.RootInfo(null, new TableKey("IteratorRemoval", 1)), parent);
		var transaction = Transaction.create();
		transaction.begin();
		try {
			var iterator = list.iterator();
			iterator.next();
			iterator.next();
			iterator.next();
			iterator.remove();
			assertEquals(3, list.size());
			assertEquals("same", list.get(0));
			assertEquals("middle", list.get(1));
			assertEquals("tail", list.get(2));
			var log = (LogList1<?>)transaction.getLog(parent.objectId() + list.variableId());
			assertNotNull(log);
			assertEquals(2, log.getOpLogs().getFirst().index);
		} finally {
			Transaction.destroy();
		}
	}
}
