package Zeze.Transaction;

import harness.Fast;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Fast
public class TestResolveOnceOwnerIdentity {
	@Test
	public void equalOwnersKeepIndependentResources() {
		var transaction = Transaction.create(new Locks());
		try {
			var first = new String("owner");
			var second = new String("owner");
			Assertions.assertEquals(first, second);
			Assertions.assertNotSame(first, second);
			var resolves = new AtomicInteger();
			Assertions.assertEquals(1, transaction.<Integer>resolveOnce(first, 7, __ -> resolves.incrementAndGet()));
			Assertions.assertEquals(2, transaction.<Integer>resolveOnce(second, 7, __ -> resolves.incrementAndGet()));
			Assertions.assertEquals(1, transaction.<Integer>resolveOnce(first, 7, __ -> resolves.incrementAndGet()));
			Assertions.assertEquals(2, transaction.<Integer>resolveOnce(second, 7, __ -> resolves.incrementAndGet()));
			Assertions.assertEquals(3, transaction.<Integer>resolveOnce(first, 8, __ -> resolves.incrementAndGet()));
			Assertions.assertEquals(3, resolves.get());
		} finally {
			Transaction.destroy();
		}
	}

	@Test
	public void ownerMutationKeepsNullVerdictCached() {
		var transaction = Transaction.create(new Locks());
		try {
			var owner = new ArrayList<String>();
			var resolves = new AtomicInteger();
			Assertions.assertNull(transaction.resolveOnce(owner, 7, __ -> {
				resolves.incrementAndGet();
				return null;
			}));
			owner.add("changed");
			Assertions.assertNull(transaction.resolveOnce(owner, 7, __ -> {
				resolves.incrementAndGet();
				return null;
			}));
			Assertions.assertEquals(1, resolves.get());
		} finally {
			Transaction.destroy();
		}
	}
}
