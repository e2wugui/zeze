package Zeze.Netty;

import java.util.ArrayList;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Fast
public class TestPipelinedHalfCloseDrainsResponses {
	@Test
	public void inputShutdownKeepsPendingResponses() {
		verifyHalfClose(ChannelInputShutdownEvent.INSTANCE);
	}

	@Test
	public void inputShutdownReadCompleteKeepsPendingResponses() {
		verifyHalfClose(ChannelInputShutdownReadComplete.INSTANCE);
	}

	private static void verifyHalfClose(Object event) {
		Task.tryInitThreadPool();
		var server = new HttpServer();
		var requests = new ArrayList<HttpExchange>();
		server.addHandler("/", 0, TransactionLevel.None, DispatchMode.Direct,
				x -> requests.add(x.detach()));
		var channel = new EmbeddedChannel(server);
		try {
			for (int i = 0; i < 2; i++)
				channel.writeInbound(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/"));
			Assertions.assertEquals(2, requests.size());
			var first = requests.get(0);
			var last = requests.get(1);
			last.close(last.sendPlainText(HttpResponseStatus.OK, "second"));
			Assertions.assertTrue(server.exchanges.isEmpty(), "closed tail only remains in response sequence");
			Assertions.assertNull(channel.readOutbound(), "tail waits for first response");
			channel.pipeline().fireUserEventTriggered(event);
			Assertions.assertTrue(channel.isActive(), "input FIN must preserve pending responses");
			first.close(first.sendPlainText(HttpResponseStatus.OK, "first"));
			channel.runPendingTasks();
			var bodies = new ArrayList<String>();
			for (Object msg; (msg = channel.readOutbound()) != null; ) {
				try {
					if (msg instanceof FullHttpResponse response)
						bodies.add(response.content().toString(HttpServer.defaultCharset));
				} finally {
					ReferenceCountUtil.release(msg);
				}
			}
			Assertions.assertEquals(java.util.List.of("first", "second"), bodies);
			Assertions.assertFalse(channel.isActive(), "close after final response is written");
		} finally {
			channel.finishAndReleaseAll();
			server.close();
		}
	}
}
