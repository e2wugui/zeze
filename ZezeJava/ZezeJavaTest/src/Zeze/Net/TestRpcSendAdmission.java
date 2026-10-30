package Zeze.Net;

import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Util.Task;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestRpcSendAdmission {
	private static final class Socket extends AsyncSocket {
		Socket(Service service) { super(service); }
		@Override public Type getType() { return Type.eClient; }
		@Override public @Nullable SocketAddress getRemoteAddress() { return null; }
		@Override public @Nullable TimeThrottle getTimeThrottle() { return null; }
		@Override protected void doClose(@Nullable Throwable cause, boolean gracefully) { }
		@Override public boolean Send(byte[] bytes, int offset, int length) { return true; }
	}

	private static void respond(Service service, Socket socket, TestRpcNoReuse.TestRpc request) throws Exception {
		var response = new TestRpcNoReuse.TestRpc();
		response.setRequest(false);
		response.setSessionId(request.getSessionId());
		response.setSender(socket);
		response.dispatch(service, new Service.ProtocolFactoryHandle<>(TestRpcNoReuse.TestRpc::new));
	}

	@Test
	void rejectedWaitKeepsTheOriginalFuture() throws Exception {
		Task.tryInitThreadPool();
		var service = new Service("TestRpcSendAdmission.rejectedWait");
		var socket = new Socket(service);
		try {
			var rpc = new TestRpcNoReuse.TestRpc();
			var original = rpc.SendForWait(socket, 60_000);
			assertThrows(IllegalStateException.class, () -> rpc.SendForWait(socket, 60_000));
			assertSame(original, rpc.getFuture(), "被拒绝的调用不得改写在飞请求的结果归属");
			respond(service, socket, rpc);
			assertTrue(original.isDone(), "真实应答必须唤醒第一次调用者");
		} finally {
			service.OnSocketDisposed(socket);
			service.stop();
		}
	}

	@Test
	void nullSocketWaitDoesNotStealAnInflightFuture() throws Exception {
		Task.tryInitThreadPool();
		var service = new Service("TestRpcSendAdmission.nullWait");
		var socket = new Socket(service);
		try {
			var rpc = new TestRpcNoReuse.TestRpc();
			var original = rpc.SendForWait(socket, 60_000);
			assertTrue(rpc.SendForWait(null, 60_000).isDone());
			assertSame(original, rpc.getFuture());
			respond(service, socket, rpc);
			assertTrue(original.isDone());
		} finally {
			service.OnSocketDisposed(socket);
			service.stop();
		}
	}

	@Test
	void concurrentSendCannotEnterBeforeTheFirstSessionIdIsPublished() throws Exception {
		Task.tryInitThreadPool();
		var service = new Service("TestRpcSendAdmission.concurrent");
		var socket = new Socket(service);
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var allocations = new AtomicInteger();
		service.setSessionIdGenerator(() -> {
			int id = allocations.incrementAndGet();
			if (id == 1) {
				entered.countDown();
				try {
					assertTrue(release.await(5, TimeUnit.SECONDS));
				} catch (InterruptedException e) {
					throw new AssertionError(e);
				}
			}
			return id;
		});
		var rpc = new TestRpcNoReuse.TestRpc();
		var failure = new AtomicReference<@Nullable Throwable>();
		var first = new Thread(() -> {
			try {
				assertTrue(rpc.Send(socket));
			} catch (Throwable e) {
				failure.set(e);
			}
		});
		first.start();
		try {
			assertTrue(entered.await(5, TimeUnit.SECONDS));
			assertThrows(IllegalStateException.class, () -> rpc.SendForWait(socket, 60_000));
		} finally {
			release.countDown();
			first.join(5000);
			service.OnSocketDisposed(socket);
			service.stop();
		}
		assertFalse(first.isAlive());
		assertNull(failure.get());
		assertEquals(1, allocations.get());
	}

	@Test
	void publishedRequestRejectsAnotherConnectionBeforeTransportSendStarts() throws Exception {
		var service = new Service("TestRpcSendAdmission.connectionBinding");
		var target = new Socket(service);
		var other = new Socket(service);
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var rpc = new TestRpcNoReuse.TestRpc() {
			@Override
			public void schedule(Service service, long sessionId, int timeout) {
				entered.countDown();
				try {
					assertTrue(release.await(5, TimeUnit.SECONDS));
				} catch (InterruptedException e) {
					throw new AssertionError(e);
				}
			}
		};
		var failure = new AtomicReference<@Nullable Throwable>();
		var sender = new Thread(() -> {
			try {
				rpc.SendForWait(target, 60_000);
			} catch (Throwable e) {
				failure.set(e);
			}
		});
		sender.start();
		try {
			assertTrue(entered.await(5, TimeUnit.SECONDS));
			assertSame(rpc, service.getRpcContext(rpc.getSessionId()));
			respond(service, other, rpc);
			assertSame(rpc, service.getRpcContext(rpc.getSessionId()), "其他连接不得消费刚登记的上下文");
			assertFalse(java.util.Objects.requireNonNull(rpc.getFuture()).isDone());
			release.countDown();
			sender.join(5000);
			assertFalse(sender.isAlive());
			assertNull(failure.get());
			respond(service, target, rpc);
			assertTrue(rpc.getFuture().isDone(), "原连接的应答仍可完成第一次等待");
		} finally {
			release.countDown();
			sender.join(5000);
			service.OnSocketDisposed(target);
			service.OnSocketDisposed(other);
			service.stop();
		}
	}
}
