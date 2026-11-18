package Zeze.Arch;

import java.net.SocketAddress;
import Zeze.Application;
import Zeze.Arch.Beans.BSend;
import Zeze.Builtin.Provider.Send;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestLinkdSendFailureResult {
	@Test
	void liveClientRejectingSendIsReportedInBatchResult() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setNoDatabase(true);
		var app = ProviderDirectTestSupport.newAppWithFakeAgent("LinkdSendFailureResult", config);
		var provider = new LinkdProvider();
		var providerService = new LinkdProviderService("LinkdSendFailureResult.providers", app);
		AsyncSocket[] client = new AsyncSocket[1];
		var clientService = new LinkdService("LinkdSendFailureResult.clients", app) {
			@Override
			public @Nullable AsyncSocket GetSocket(long sessionId) {
				var socket = client[0];
				return socket != null && socket.getSessionId() == sessionId ? socket : null;
			}
		};
		var linkd = new LinkdApp("LinkdSendFailureResult", app, provider, providerService, clientService, new LoadConfig());
		try {
			var socket = new AsyncSocket(clientService) {
				@Override public Type getType() { return Type.eServer; }
				@Override public boolean Send(byte[] bytes, int offset, int length) { return false; }
				@Override public @Nullable TimeThrottle getTimeThrottle() { return null; }
				@Override public @Nullable SocketAddress getRemoteAddress() { return null; }
				@Override protected void doClose(@Nullable Throwable ex, boolean gracefully) { }
			};
			client[0] = socket;
			var payload = new Zeze.Builtin.LinkdBase.ReportError();
			var send = new Send(new BSend(payload.getTypeId(), new Binary(payload.encode())));
			send.Argument.getLinkSids().add(socket.getSessionId());
			assertFalse(socket.isClosed());
			assertEquals(0, provider.ProcessSendRequest(send));
			assertTrue(socket.isClosed());
			assertEquals(1, send.Result.getErrorLinkSids().size());
			assertEquals(socket.getSessionId(), send.Result.getErrorLinkSids().get(0));
		} finally {
			clientService.stop();
			providerService.stop();
			linkd.commandConsoleService.stop();
			app.stop();
		}
	}
}
