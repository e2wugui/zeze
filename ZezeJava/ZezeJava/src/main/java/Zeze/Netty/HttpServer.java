package Zeze.Netty;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Util.FewModifySortedMap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class HttpServer extends ChannelInitializer<SocketChannel> {
	private ConcurrentHashMap<ChannelHandlerContext, HttpExchange> exchanges = new ConcurrentHashMap<>();
	FewModifySortedMap<String, HttpHandler> handlers = new FewModifySortedMap<>();

	@Override
	protected void initChannel(SocketChannel ch) {
		ch.pipeline()
				.addLast("encoder", new HttpResponseEncoder())
				.addLast("decoder", new HttpRequestDecoder(
						4096, 8192, 8192, false))
				.addLast("handler", new Handler());
	}

	public class Handler extends ChannelInboundHandlerAdapter {

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			exchanges.computeIfAbsent(ctx, (k) -> new HttpExchange(HttpServer.this, k)).channelRead(msg);
		}

		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) {
			ctx.flush();
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			var x = exchanges.remove(ctx);
			if (null != x) {
				x.sendFullResponse500(cause);
				x.close(); // todo 生命期管理
			}
			ctx.close();
		}

	}
}
