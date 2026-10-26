package Zeze.Netty;

import harness.Fast;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Locks;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.TransactionLevel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Fast
public class TestHttpWebSocketFrameIdentity {
	private static final class Exchange extends HttpExchange {
		Exchange(HttpServer server, EmbeddedChannel channel) {
			super(server, channel.pipeline().firstContext());
			handler = new HttpHandler(5, TransactionLevel.None, DispatchMode.Direct, new HttpWebSocketHandle() { });
		}
	}

	@Test
	public void repeatedFrameChecksCountOnceAndDistinctFramesStillAccumulate() {
		try (var server = new HttpServer()) {
			var channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
			var first = new BinaryWebSocketFrame(false, 0, Unpooled.wrappedBuffer(new byte[]{1, 2}));
			var next = new ContinuationWebSocketFrame(false, 0, Unpooled.wrappedBuffer(new byte[]{1, 2}));
			var last = new ContinuationWebSocketFrame(true, 0, Unpooled.wrappedBuffer(new byte[]{1, 2}));
			try {
				var exchange = new Exchange(server, channel);
				Transaction.create(new Locks());
				try {
					Assertions.assertTrue(exchange.checkWebSocketContentSize(first));
					Assertions.assertTrue(exchange.checkWebSocketContentSize(first));
					Assertions.assertTrue(exchange.checkWebSocketContentSize(next));
					Assertions.assertTrue(exchange.checkWebSocketContentSize(next));
				} finally {
					Transaction.destroy();
				}
				Assertions.assertFalse(exchange.checkWebSocketContentSize(last));
				CloseWebSocketFrame close = channel.readOutbound();
				Assertions.assertNotNull(close);
				try {
					Assertions.assertEquals(1009, close.statusCode());
				} finally {
					close.release();
				}
			} finally {
				first.release();
				next.release();
				last.release();
				channel.finishAndReleaseAll();
			}
		}
	}
}
