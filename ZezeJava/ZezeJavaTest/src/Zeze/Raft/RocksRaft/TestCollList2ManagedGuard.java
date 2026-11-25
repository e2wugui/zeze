package Zeze.Raft.RocksRaft;

import Zeze.Raft.RocksRaft.TestSortedMapEquivalentKeysReplay.BValue;
import Zeze.Transaction.HasManagedException;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestCollList2ManagedGuard {
	@Test
	void unmanagedCollList2RejectsManagedElement() {
		var managedItem = new BValue(1);
		managedItem.initRootInfo(new Record.RootInfo(null, new TableKey("owner", 1)), null);
		assertTrue(managedItem.isManaged());

		var unmanagedList = new CollList2<>(BValue.class);
		unmanagedList.add(new BValue(0));
		assertThrows(HasManagedException.class, () -> unmanagedList.add(managedItem));
		assertThrows(HasManagedException.class, () -> unmanagedList.set(0, managedItem));
		assertThrows(HasManagedException.class, () -> unmanagedList.add(0, managedItem));
		assertEquals(1, unmanagedList.size());
		assertEquals(0, unmanagedList.get(0).value);
	}
}
