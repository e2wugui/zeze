package Zeze.Arch;

import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Application;
import Zeze.Builtin.LinkdBase.BReportError;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Util.Task;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestLinkdDelayedCloseIdentity {
	@Test
	void delayedErrorClosureBelongsToTheSocketThatReceivedTheError() throws Exception {
		Task.tryInitThreadPool();
		var config = new Config();
		config.setNoDatabase(true);
		config.setServiceManager("disable");
		var app = new Application("DelayedLinkError", config);
		var closed = new CountDownLatch(1);
		var original = new SinkSocket(closed);
		var replacement = new SinkSocket(closed);
		var registered = new AtomicReference<AsyncSocket>(original);
		var service = new LinkdService("DelayedLinkError", app) {
			@Override
			public AsyncSocket GetSocket(long sessionId) {
				return registered.get();
			}
		};
		try {
			service.reportError(17, BReportError.FromProvider, 1, "close this connection");
			registered.set(replacement); // The numeric lookup now belongs to another socket.
			assertTrue(closed.await(10, TimeUnit.SECONDS));
			assertTrue(original.isClosed(), "the scheduled action belongs to the reported socket");
			assertFalse(replacement.isClosed(), "a replacement must not inherit an old close action");
		} finally {
			service.stop();
			app.stop();
		}
	}

	private static final class SinkSocket extends AsyncSocket {
		private final CountDownLatch closed;

		SinkSocket(CountDownLatch closed) {
			super(null);
			this.closed = closed;
		}

		@Override
		public Type getType() { return Type.eClient; }
		@Override
		public boolean Send(byte[] bytes, int offset, int length) { return true; }
		@Override
		public @Nullable SocketAddress getRemoteAddress() { return null; }
		@Override
		public @Nullable TimeThrottle getTimeThrottle() { return null; }
		@Override
		protected void doClose(@Nullable Throwable error, boolean gracefully) { closed.countDown(); }
	}
}
