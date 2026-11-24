package Zeze.Net;

import Zeze.Netty.HttpHandler;
import Zeze.Netty.HttpServer;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestWebsocketRouteOwnership {
	@Test
	void stopAfterDuplicateStartPreservesExistingRoute() {
		try (var server = new HttpServer()) {
			var path = "/ws";
			var original = new HttpHandler(0, TransactionLevel.None, DispatchMode.Direct, x -> {});
			server.addHandler(path, original);
			var websocket = new WebsocketHandle(path, server);

			assertThrows(IllegalStateException.class, websocket::start);
			websocket.stop();
			assertSame(original, server.getHandler(path));
		}
	}

	@Test
	void stopRemovesOnlyItsOwnRegistration() {
		try (var server = new HttpServer()) {
			var path = "/ws";
			var websocket = new WebsocketHandle(path, server);

			websocket.start();
			assertSame(websocket, server.getHandler(path).WebSocketHandle);
			websocket.stop();
			assertNull(server.getHandler(path));

			websocket.start();
			server.removeHandler(path);
			var replacement = new HttpHandler(0, TransactionLevel.None, DispatchMode.Direct, x -> {});
			server.addHandler(path, replacement);
			websocket.stop();
			assertSame(replacement, server.getHandler(path));
		}
	}
}
