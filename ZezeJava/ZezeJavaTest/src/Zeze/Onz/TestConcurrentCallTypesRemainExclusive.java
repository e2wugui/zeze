package Zeze.Onz;

import java.lang.reflect.Field;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Net.AsyncSocket;
import Zeze.Transaction.EmptyBean;
import harness.Fast;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Two asynchronous continuations must not register procedure and saga calls in one transaction. */
@Fast
public class TestConcurrentCallTypesRemainExclusive {

	private static final class Transaction extends OnzTransaction<EmptyBean.Data, EmptyBean.Data> {
		@Override
		protected long perform() {
			return 0;
		}
	}

	private static final class BarrierServer extends OnzServer {
		CyclicBarrier callers;

		BarrierServer() throws Exception {
			super("unused=unused", null); // Allocated without a constructor; no network/database resources.
		}

		@Override
		public AsyncSocket getZezeInstance(String name) {
			try {
				callers.await(5, TimeUnit.SECONDS);
			} catch (Exception e) {
				throw new AssertionError(e);
			}
			return null; // Normal Send(false) path completes the future without sending any network traffic.
		}

		@Override
		void saveSagaPreparingForCrashWindow(OnzTransaction<?, ?> txn) {
			// This test covers registration exclusivity, not persistence.
		}
	}

	@Test
	public void rejectsOneOfConcurrentProcedureAndSagaRegistrations() throws Exception {
		var txn = new Transaction();
		var server = allocate(BarrierServer.class);
		server.callers = new CyclicBarrier(2);
		Field serverField = OnzTransaction.class.getDeclaredField("onzServer");
		serverField.setAccessible(true);
		serverField.set(txn, server);
		var accepted = new AtomicInteger();
		var rejected = new AtomicInteger();
		try (var workers = Executors.newFixedThreadPool(2)) {
			var procedure = workers.submit(() -> {
				try {
					txn.callProcedureAsync("procedure-cluster", "procedure", EmptyBean.Data.instance, EmptyBean.Data.instance);
					accepted.incrementAndGet();
				} catch (RuntimeException expected) {
					rejected.incrementAndGet();
				}
			});
			var saga = workers.submit(() -> {
				try {
					txn.callSagaAsync("saga-cluster", "saga", EmptyBean.Data.instance, EmptyBean.Data.instance);
					accepted.incrementAndGet();
				} catch (RuntimeException expected) {
					rejected.incrementAndGet();
				}
			});
			procedure.get(10, TimeUnit.SECONDS);
			saga.get(10, TimeUnit.SECONDS);
		}
		assertEquals(1, accepted.get(), "Exactly one call type may be registered in a transaction");
		assertEquals(1, rejected.get(), "The incompatible continuation must fail before registration");
		assertEquals(1, txn.buildSavedCommits().getOnzs().size(), "The decision record must have one call type");
	}

	private static <T> T allocate(Class<T> type) throws Exception {
		var field = Unsafe.class.getDeclaredField("theUnsafe");
		field.setAccessible(true);
		return type.cast(((Unsafe)field.get(null)).allocateInstance(type));
	}
}
