package Zeze.Services;

import java.net.SocketAddress;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Builtin.Token.BNewTokenArg;
import Zeze.Builtin.Token.NewToken;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Fast
public class TestTokenHandshakeKeepsSubscriptions {
	@Test
	void ttlStartsAfterRandomGenerationAndOverflowDoesNotInsertAToken() throws Exception {
		var token = new Token();
		var randomFinished = new AtomicLong();
		var random = Token.class.getDeclaredField("tokenRandom");
		random.setAccessible(true);
		random.set(token, new Random() {
			@Override
			public void nextBytes(byte[] bytes) {
				assertDoesNotThrow(() -> Thread.sleep(50));
				super.nextBytes(bytes);
				randomFinished.set(System.currentTimeMillis());
			}
		});
		var issue = Token.class.getDeclaredMethod("newTokenLocked", NewToken.class);
		issue.setAccessible(true);
		var socket = new Socket(new Token.TokenServer(new Config()));
		var request = new NewToken(new BNewTokenArg.Data(null, 1));
		request.setSender(socket);
		issue.invoke(token, request);
		assertEquals(0, request.getResultCode());
		var table = Token.class.getDeclaredField("tokenMap");
		table.setAccessible(true);
		var tokens = (Map<?, ?>)table.get(token);
		var state = tokens.get(request.Result.getToken());
		var created = state.getClass().getDeclaredField("createTime");
		var expires = state.getClass().getDeclaredField("endTime");
		created.setAccessible(true);
		expires.setAccessible(true);
		assertTrue(created.getLong(state) >= randomFinished.get());
		assertEquals(1, expires.getLong(state) - created.getLong(state));

		var overflow = new NewToken(new BNewTokenArg.Data(null, Long.MAX_VALUE));
		overflow.setSender(socket);
		issue.invoke(token, overflow);
		assertEquals(-1, overflow.getResultCode());
		assertEquals(1, tokens.size());
	}

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
