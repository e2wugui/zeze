package Zeze.Net;

import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import Zeze.Util.Action0;
import Zeze.Util.Task;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** A continuously replenished write queue must yield to the other work on its selector. */
@Fast
public class TestTcpSocketWriteFairness {

	private static final class AcceptService extends Service {
		final CountDownLatch accepted = new CountDownLatch(1);
		volatile TcpSocket socket;

		AcceptService(String name, Selectors selectors) {
			super(name);
			setSelectors(selectors);
		}

		@Override
		public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
			super.OnHandshakeDone(so);
			socket = (TcpSocket)so;
			accepted.countDown();
		}
	}

	@Test
	@Timeout(20)
	public void replenishedActionsYieldToSelectorTasks() throws Exception {
		Task.tryInitThreadPool();
		var selectors = new Selectors("test.write.actions", 1);
		var service = new AcceptService("test.write.actions", selectors);
		var repeat = new AtomicBoolean(true);
		try {
			var listener = (TcpSocket)service.newServerSocket("127.0.0.1", 0, null);
			var address = listener.getLocalInet();
			Assertions.assertNotNull(address);
			try (var peer = new Socket("127.0.0.1", address.getPort())) {
				Assertions.assertTrue(service.accepted.await(5, TimeUnit.SECONDS));
				var entered = new CountDownLatch(1);
				var socket = service.socket;
				// submitAction is public and need not emit bytes (codec installation is one such operation).
				// Keeping exactly one continuation queued makes the original drain loop deterministic,
				// without requiring an unbounded producer or relying on the peer's read speed.
				Action0 replenish = new Action0() {
					@Override
					public void run() {
						entered.countDown();
						if (repeat.get())
							socket.submitAction(this);
					}
				};
				Assertions.assertTrue(socket.submitAction(replenish));
				Assertions.assertTrue(entered.await(5, TimeUnit.SECONDS));

				var taskRan = new CountDownLatch(1);
				var ranWhileProducing = new AtomicBoolean();
				var selector = selectors.choice();
				selector.addTask(() -> {
					ranWhileProducing.set(repeat.get());
					taskRan.countDown();
				});
				selector.wakeupDirect();
				Assertions.assertTrue(taskRan.await(3, TimeUnit.SECONDS),
						"a hot socket must yield before its operation producer stops");
				Assertions.assertTrue(ranWhileProducing.get());
			}
		} finally {
			repeat.set(false); // Always let the original implementation leave its drain loop before join.
			service.stop();
			selectors.close();
		}
	}

	@Test
	@Timeout(20)
	public void replenishedFlushRoundsYieldToSelectorTasks() throws Exception {
		Task.tryInitThreadPool();
		var selectors = new Selectors("test.write.flush", 1);
		var service = new AcceptService("test.write.flush", selectors);
		var repeat = new AtomicBoolean(true);
		try {
			var listener = (TcpSocket)service.newServerSocket("127.0.0.1", 0, null);
			var address = listener.getLocalInet();
			Assertions.assertNotNull(address);
			try (var peer = new Socket("127.0.0.1", address.getPort())) {
				Assertions.assertTrue(service.accepted.await(5, TimeUnit.SECONDS));
				var entered = new CountDownLatch(1);
				var socket = service.socket;
				socket.setOutputSecurityCodec((so, sink) -> new Codec() {
					@Override
					public void update(byte c) {
						sink.update(c);
					}

					@Override
					public void flush() {
						entered.countDown();
						if (repeat.get())
							socket.submitAction(() -> { });
					}
				});
				Assertions.assertTrue(entered.await(5, TimeUnit.SECONDS));

				var taskRan = new CountDownLatch(1);
				var ranWhileProducing = new AtomicBoolean();
				var selector = selectors.choice();
				selector.addTask(() -> {
					ranWhileProducing.set(repeat.get());
					taskRan.countDown();
				});
				selector.wakeupDirect();
				// Each inner queue drain completes, but codec.flush queues the next round. A bound only
				// on a single drain still allows the outer write loop to monopolize the selector.
				Assertions.assertTrue(taskRan.await(3, TimeUnit.SECONDS),
						"successive fully drained write rounds must yield to other selector work");
				Assertions.assertTrue(ranWhileProducing.get());
			}
		} finally {
			repeat.set(false);
			service.stop();
			selectors.close();
		}
	}
}
