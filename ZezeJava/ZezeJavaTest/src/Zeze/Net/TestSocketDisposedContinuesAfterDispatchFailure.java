package Zeze.Net;

import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Services.Handshake.KeepAlive;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** A failed dispatch for one disposed RPC must not leave the other RPCs waiting for their timeouts. */
@Fast
public class TestSocketDisposedContinuesAfterDispatchFailure {

	private static final class RejectFirstDispatchService extends Service {
		private boolean rejected;

		RejectFirstDispatchService() {
			super("test.disposed.dispatchFailure");
		}

		@Override
		public <P extends Protocol<?>> void dispatchRpcResponse(@NotNull P rpc,
				@NotNull ProtocolHandle<P> responseHandle, @NotNull ProtocolFactoryHandle<?> factoryHandle)
				throws Exception {
			if (!rejected) {
				rejected = true;
				throw new IllegalStateException("response executor rejected dispatch");
			}
			responseHandle.handle(rpc);
		}
	}

	private static final class StubSocket extends AsyncSocket {
		StubSocket(Service service) {
			super(service);
		}

		@Override
		public Type getType() {
			return Type.eClient;
		}

		@Override
		public @Nullable SocketAddress getRemoteAddress() {
			return null;
		}

		@Override
		public @Nullable TimeThrottle getTimeThrottle() {
			return null;
		}

		@Override
		protected void doClose(@Nullable Throwable ex, boolean gracefully) {
		}

		@Override
		public boolean Send(byte @NotNull [] bytes, int offset, int length) {
			return true;
		}
	}

	@Test
	public void remainingRpcFailsImmediatelyWhenFirstDispatchThrows() {
		var service = new RejectFirstDispatchService();
		service.AddFactoryHandle(KeepAlive.TypeId_, new Service.ProtocolFactoryHandle<>(KeepAlive::new,
				null, TransactionLevel.None, DispatchMode.Direct));
		var socket = new StubSocket(service);
		var completed = new AtomicInteger();
		var requests = new KeepAlive[2];
		for (int i = 0; i < requests.length; i++) {
			var rpc = requests[i] = new KeepAlive();
			rpc.setSender(socket);
			rpc.setResponseHandle(response -> {
				Assertions.assertEquals(Procedure.ErrorSendFail, response.getResultCode());
				completed.incrementAndGet();
				return Procedure.Success;
			});
			rpc.setSessionId(service.addRpcContext(rpc));
		}

		try {
			// No timeout tasks are registered: this checks synchronous disposal, independently of map order.
			Assertions.assertDoesNotThrow(() -> service.OnSocketDisposed(socket));
			Assertions.assertEquals(1, completed.get(), "the other RPC must receive its disposal failure");
			Assertions.assertTrue(service.getRpcContextsToSender(socket).isEmpty(),
					"a failed dispatch must not strand the remaining RPC contexts");
			for (var rpc : requests)
				Assertions.assertEquals(Procedure.ErrorSendFail, rpc.getResultCode());
		} finally {
			for (var rpc : requests)
				service.removeRpcContext(rpc.getSessionId());
		}
	}
}
