package Zeze.Net;

import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Services.Handshake.KeepAlive;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Closing a socket after its dedicated selector group has stopped must still release its resources. */
@Fast
public class TestTcpSocketDisposeAfterSelectorsClose {
	@Test
	@Timeout(15)
	public void stoppedSelectorsSupportCrossSubmittedTasks() throws Exception {
		Task.tryInitThreadPool();
		var selectors = new Selectors("test.dispose.crossTasks", 2);
		var first = selectors.choice();
		var second = selectors.choice();
		var entered = new CountDownLatch(2);
		var completed = new CountDownLatch(2);
		var executions = new AtomicInteger();
		var taskFailure = new AtomicReference<Throwable>();
		var firstWorker = new Thread(() -> first.addTask(() -> {
			entered.countDown();
			try {
				if (!entered.await(5, TimeUnit.SECONDS)) {
					taskFailure.compareAndSet(null, new IllegalStateException("second task did not enter"));
					return;
				}
				second.addTask(() -> {
					executions.incrementAndGet();
					completed.countDown();
				});
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				taskFailure.compareAndSet(null, e);
			}
		}), "test-selector-first-cross-task");
		var secondWorker = new Thread(() -> second.addTask(() -> {
			entered.countDown();
			try {
				if (!entered.await(5, TimeUnit.SECONDS)) {
					taskFailure.compareAndSet(null, new IllegalStateException("first task did not enter"));
					return;
				}
				first.addTask(() -> {
					executions.incrementAndGet();
					completed.countDown();
				});
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				taskFailure.compareAndSet(null, e);
			}
		}), "test-selector-second-cross-task");
		// Both callbacks enter before they submit to the other selector. Running callbacks under
		// per-selector locks creates a deterministic ABBA deadlock, even when those locks are reentrant.
		// Daemons keep the focused RED process bounded when exercising that implementation.
		firstWorker.setDaemon(true);
		secondWorker.setDaemon(true);
		try {
			selectors.close();
			Assertions.assertFalse(first.isAlive());
			Assertions.assertFalse(second.isAlive());
			firstWorker.start();
			secondWorker.start();
			Assertions.assertTrue(entered.await(5, TimeUnit.SECONDS));
			Assertions.assertTrue(completed.await(2, TimeUnit.SECONDS),
					"stopped selector callbacks must be able to submit tasks to each other");
			firstWorker.join(2_000);
			secondWorker.join(2_000);
			Assertions.assertFalse(firstWorker.isAlive());
			Assertions.assertFalse(secondWorker.isAlive());
			Assertions.assertNull(taskFailure.get());
			Assertions.assertEquals(2, executions.get());
		} finally {
			selectors.close();
		}
	}

	@Test
	@Timeout(10)
	public void stoppedSelectorSupportsNestedTasks() throws Exception {
		Task.tryInitThreadPool();
		var selectors = new Selectors("test.dispose.nestedTasks", 1);
		var selector = selectors.choice();
		var completed = new CountDownLatch(1);
		var executions = new AtomicInteger();
		var worker = new Thread(() -> selector.addTask(() -> selector.addTask(() -> {
			executions.incrementAndGet();
			completed.countDown();
		})), "test-selector-nested-task");
		// A non-reentrant shutdown lock self-deadlocks this worker. Keep the failing reproduction
		// bounded; the focused RED test runs in its own process, which releases the daemon afterward.
		worker.setDaemon(true);
		try {
			selectors.close();
			Assertions.assertFalse(selector.isAlive());
			worker.start();
			Assertions.assertTrue(completed.await(2, TimeUnit.SECONDS),
					"a stopped-selector task must be able to submit another task to the same selector");
			worker.join(2_000);
			Assertions.assertFalse(worker.isAlive(), "nested task submission must return");
			Assertions.assertEquals(1, executions.get());
		} finally {
			selectors.close();
		}
	}

	@Test
	@Timeout(20)
	public void concurrentSelectorsCloseWaitsForShutdown() throws Exception {
		Task.tryInitThreadPool();
		var selectors = new Selectors("test.dispose.concurrentClose", 1);
		var selector = selectors.choice();
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var firstClosed = new CountDownLatch(1);
		var secondClosed = new CountDownLatch(1);
		var taskFailure = new AtomicReference<Throwable>();
		var firstCloser = new Thread(() -> {
			try {
				selectors.close();
			} finally {
				firstClosed.countDown();
			}
		}, "test-selector-first-close");
		var secondCloser = new Thread(() -> {
			try {
				selectors.close();
			} finally {
				secondClosed.countDown();
			}
		}, "test-selector-second-close");
		firstCloser.setDaemon(true);
		secondCloser.setDaemon(true);
		try {
			selector.addTask(() -> {
				entered.countDown();
				try {
					if (!release.await(10, TimeUnit.SECONDS))
						taskFailure.set(new IllegalStateException("shutdown task gate was not released"));
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					taskFailure.set(e);
				}
			});
			selector.wakeupDirect();
			Assertions.assertTrue(entered.await(5, TimeUnit.SECONDS));
			firstCloser.start();
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			while (firstCloser.getState() != Thread.State.WAITING && System.nanoTime() < deadline)
				Thread.sleep(1);
			Assertions.assertEquals(Thread.State.WAITING, firstCloser.getState());
			Assertions.assertEquals(1, firstClosed.getCount());

			secondCloser.start();
			deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			while (secondClosed.getCount() != 0 && secondCloser.getState() != Thread.State.WAITING
					&& System.nanoTime() < deadline)
				Thread.sleep(1);
			Assertions.assertEquals(Thread.State.WAITING, secondCloser.getState(),
					"every concurrent close must wait for the still-gated selector to stop");
			Assertions.assertEquals(1, secondClosed.getCount(),
					"another close must not return while the first shutdown is still joining");
			release.countDown();
			Assertions.assertTrue(firstClosed.await(5, TimeUnit.SECONDS));
			Assertions.assertTrue(secondClosed.await(5, TimeUnit.SECONDS));
			Assertions.assertFalse(selector.isAlive());
			Assertions.assertNull(taskFailure.get());
		} finally {
			release.countDown();
			firstCloser.join(5_000);
			secondCloser.join(5_000);
			selectors.close();
			Assertions.assertFalse(firstCloser.isAlive());
			Assertions.assertFalse(secondCloser.isAlive());
		}
	}

	@Test
	@Timeout(20)
	public void queuedAndLateSelectorTasksRunExactlyOnceDuringShutdown() throws Exception {
		Task.tryInitThreadPool();
		var selectors = new Selectors("test.dispose.shutdownTasks", 1);
		var selector = selectors.choice();
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var closed = new CountDownLatch(1);
		var beforeClose = new AtomicInteger();
		var duringClose = new AtomicInteger();
		var afterClose = new AtomicInteger();
		var taskFailure = new AtomicReference<Throwable>();
		var closer = new Thread(() -> {
			try {
				selectors.close();
			} finally {
				closed.countDown();
			}
		}, "test-selector-close");
		closer.setDaemon(true);
		try {
			selector.addTask(() -> {
				entered.countDown();
				try {
					if (!release.await(10, TimeUnit.SECONDS))
						taskFailure.set(new IllegalStateException("shutdown task gate was not released"));
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					taskFailure.set(e);
				}
			});
			selector.addTask(beforeClose::incrementAndGet);
			selector.wakeupDirect();
			Assertions.assertTrue(entered.await(5, TimeUnit.SECONDS));
			closer.start();

			// The selector is still inside our gated task. WAITING here is close()'s join,
			// so shutdown has started without allowing the event loop to finish its queue.
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			while (closer.getState() != Thread.State.WAITING && System.nanoTime() < deadline)
				Thread.sleep(1);
			Assertions.assertEquals(Thread.State.WAITING, closer.getState(),
					"close must be waiting for the still-gated selector thread");
			selector.addTask(duringClose::incrementAndGet);
			release.countDown();
			Assertions.assertTrue(closed.await(5, TimeUnit.SECONDS));
			Assertions.assertFalse(selector.isAlive());

			var lateTaskCompleted = new CountDownLatch(1);
			selector.addTask(() -> {
				afterClose.incrementAndGet();
				lateTaskCompleted.countDown();
			});
			Assertions.assertTrue(lateTaskCompleted.await(2, TimeUnit.SECONDS),
					"a task submitted after close returned must still be executed");
			Assertions.assertAll(
					() -> Assertions.assertNull(taskFailure.get()),
					() -> Assertions.assertEquals(1, beforeClose.get()),
					() -> Assertions.assertEquals(1, duringClose.get()),
					() -> Assertions.assertEquals(1, afterClose.get()));
		} finally {
			release.countDown();
			selectors.close();
			closer.join(5_000);
			Assertions.assertFalse(closer.isAlive(), "the shutdown thread must not leak");
		}
	}

	private static final class DisposeService extends Service {
		final CountDownLatch accepted = new CountDownLatch(1);
		final CountDownLatch disposed = new CountDownLatch(1);
		final AtomicInteger disposeCount = new AtomicInteger();
		volatile TcpSocket socket;

		DisposeService(Selectors selectors) {
			super("test.dispose.stoppedSelectors");
			setSelectors(selectors);
		}

		@Override
		public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
			super.OnHandshakeDone(so);
			socket = (TcpSocket)so;
			accepted.countDown();
		}

		@Override
		public void OnSocketDisposed(@NotNull AsyncSocket so) throws Exception {
			if (so == socket) {
				disposeCount.incrementAndGet();
				try {
					super.OnSocketDisposed(so);
				} finally {
					disposed.countDown();
				}
			} else
				super.OnSocketDisposed(so);
		}
	}

	private static final class CloseCountingCodec implements Codec {
		final AtomicInteger closeCount = new AtomicInteger();

		@Override
		public void update(byte c) {
		}

		@Override
		public void close() {
			closeCount.incrementAndGet();
		}
	}

	@Test
	@Timeout(20)
	public void stoppedSelectorsStillDisposeSocketBufferCodecsAndRpc() throws Exception {
		Task.tryInitThreadPool();
		var selectors = new Selectors("test.dispose.stoppedSelectors", 1);
		var selector = selectors.choice();
		var service = new DisposeService(selectors);
		var buffer = new AtomicReference<OutputBuffer>();
		var rpc = new KeepAlive();
		try {
			var listener = (TcpSocket)service.newServerSocket("127.0.0.1", 0, null);
			var address = listener.getLocalInet();
			Assertions.assertNotNull(address);
			try (var peer = new Socket("127.0.0.1", address.getPort())) {
				Assertions.assertTrue(service.accepted.await(5, TimeUnit.SECONDS));
				var socket = service.socket;
				var input = new CloseCountingCodec();
				var output = new CloseCountingCodec();
				var installed = new CountDownLatch(2);
				socket.setInputSecurityCodec((so, sink) -> {
					installed.countDown();
					return input;
				});
				socket.setOutputSecurityCodec((so, sink) -> {
					buffer.set(sink);
					installed.countDown();
					return output;
				});
				Assertions.assertTrue(installed.await(5, TimeUnit.SECONDS));

				var prepared = new CountDownLatch(1);
				var future = new TaskCompletionSource<EmptyBean>();
				selector.addTask(() -> {
					// Use the owning thread to leave an actual pooled buffer and an in-flight context.
					// No timeout is scheduled, so only socket disposal can complete this future.
					buffer.get().put((byte)1);
					rpc.setSender(socket);
					rpc.setFuture(future);
					rpc.setSessionId(service.addRpcContext(rpc));
					prepared.countDown();
				});
				selector.wakeupDirect();
				Assertions.assertTrue(prepared.await(5, TimeUnit.SECONDS));
				selectors.close(); // Joins the event loop; subsequent disposal cannot depend on another event.
				Assertions.assertFalse(selector.isAlive());
				Assertions.assertFalse(socket.isClosed());
				Assertions.assertEquals(1, buffer.get().size());

				socket.close();
				socket.close(); // Idempotence must hold even for a stopped-selector fallback.
				Assertions.assertTrue(service.disposed.await(2, TimeUnit.SECONDS),
						"socket.close after selectors.close must still notify disposal");
				Assertions.assertEquals(1, service.disposeCount.get());
				Assertions.assertEquals(1, input.closeCount.get());
				Assertions.assertEquals(1, output.closeCount.get());
				Assertions.assertEquals(0, buffer.get().size(), "the pooled output buffer must be returned");
				Assertions.assertTrue(service.getRpcContextsToSender(socket).isEmpty());
				Assertions.assertEquals(Procedure.ErrorSendFail, rpc.getResultCode());
				var failure = Assertions.assertThrows(Exception.class, () -> future.get(1, TimeUnit.SECONDS));
				Assertions.assertInstanceOf(RpcSocketDisposedException.class, failure.getCause());
			}
		} finally {
			service.removeRpcContext(rpc.getSessionId());
			service.stop();
			selectors.close();
			// Cleanup the artificial pending byte even when exercising the original leaking implementation.
			var outputBuffer = buffer.get();
			if (outputBuffer != null)
				outputBuffer.close();
		}
	}
}
