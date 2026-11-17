package Zeze.Services;

import java.net.SocketAddress;
import java.util.Map;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestTokenHandshakeKeepsSubscriptions {
	@Test
	void repeatedCompletionKeepsSessionAndDisposalRemovesItsTopics() throws Exception {
		var server = new Token.TokenServer(new Config());
		var socket = new Socket(server);
		server.OnHandshakeDone(socket);
		var session = socket.getUserState();
		var subscribe = Token.TokenServer.class.getDeclaredMethod("subTopic", session.getClass(), String.class);
		subscribe.setAccessible(true);
		assertTrue((boolean)subscribe.invoke(server, session, "retained-topic"));
		server.OnHandshakeDone(socket);
		assertSame(session, socket.getUserState(), "重复完成握手不能遗失持有订阅的会话");
		assertTrue(socket.isHandshakeDone());
		server.OnSocketDisposed(socket);
		var topics = Token.TokenServer.class.getDeclaredField("topicMap");
		topics.setAccessible(true);
		assertTrue(((Map<?, ?>)topics.get(server)).isEmpty(), "销毁连接后必须移除全部订阅");
	}

	private static final class Socket extends AsyncSocket {
		Socket(Service service) {
			super(service);
		}

		@Override
		public Type getType() {
			return Type.eServer;
		}

		@Override
		protected void doClose(@Nullable Throwable error, boolean gracefully) {
		}

		@Override
		public boolean Send(byte[] bytes, int offset, int length) {
			return true;
		}

		@Override
		public @Nullable TimeThrottle getTimeThrottle() {
			return null;
		}

		@Override
		public @Nullable SocketAddress getRemoteAddress() {
			return null;
		}
	}
}
