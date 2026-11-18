package Zeze.Net;

import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Services.Handshake.KeepAlive;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@Fast
public class TestSocketDisposalPreservesReplacementRpc {
	@Test
	void disposalRemovesOnlyTheRequestsInItsSnapshot() throws Exception {
		Task.tryInitThreadPool();
		var service = new Service("TestSocketDisposalPreservesReplacementRpc");
		service.AddFactoryHandle(KeepAlive.TypeId_, new Service.ProtocolFactoryHandle<>(
				KeepAlive::new, null, TransactionLevel.None, DispatchMode.Direct));
		var closedSocket = new AsyncSocket(service) {
			@Override public Type getType() { return Type.eClient; }
			@Override protected void doClose(@Nullable Throwable ex, boolean gracefully) { }
			@Override public boolean Send(byte[] bytes, int offset, int length) { return false; }
			@Override public @Nullable TimeThrottle getTimeThrottle() { return null; }
			@Override public @Nullable SocketAddress getRemoteAddress() { return null; }
		};
		var first = new KeepAlive();
		var second = new KeepAlive();
		var replacement = new KeepAlive();
		first.setSender(closedSocket);
		second.setSender(closedSocket);
		first.setSessionId(service.addRpcContext(first));
		second.setSessionId(service.addRpcContext(second));
		var callbacks = new AtomicInteger();
		first.setResponseHandle(rpc -> {
			replaceContext(service, second, replacement);
			callbacks.incrementAndGet();
			return Procedure.Success;
		});
		second.setResponseHandle(rpc -> {
			replaceContext(service, first, replacement);
			callbacks.incrementAndGet();
			return Procedure.Success;
		});

		try {
			closedSocket.close();
			service.OnSocketDisposed(closedSocket);
			assertEquals(1, callbacks.get());
			assertSame(replacement, service.getRpcContext(replacement.getSessionId()));
		} finally {
			service.removeRpcContext(first.getSessionId());
			service.removeRpcContext(second.getSessionId());
			service.stop();
		}
	}

	private static void replaceContext(Service service, KeepAlive old, KeepAlive replacement) {
		long sid = old.getSessionId();
		assertSame(old, service.removeRpcContext(sid));
		service.setSessionIdGenerator(() -> sid);
		replacement.setSessionId(service.addRpcContext(replacement));
	}
}
