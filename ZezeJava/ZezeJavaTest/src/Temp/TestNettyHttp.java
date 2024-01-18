package Temp;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;

/*                                   - DefaultHttpObject = DefaultHttpMessage ====== DefaultHttpRequest/Response = DefaultFullHttpRequest/Response
                                    /                    / /- HttpRequest/Response <                             /
           DecoderResultProvider - HttpObject - HttpMessage ------ FullHttpMessage - FullHttpRequest/Response ---
ReferenceCounted - ByteBufHolder - HttpContent - LastHttpContent /
*/
public class TestNettyHttp {
	static class Handler extends ChannelInboundHandlerAdapter {
		private static final byte[] RES_BODY = "Hello, World!".getBytes(StandardCharsets.UTF_8);
		private static final AsciiString CONTENT_LENGTH = AsciiString.cached(String.valueOf(RES_BODY.length));
		private static final AsciiString CONTENT_TYPE = AsciiString.cached("text/plain; charset=UTF-8");
		private static final AsciiString SERVER = AsciiString.cached("Netty");
		private static String date;

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			if (msg instanceof HttpRequest) {
				try {
					if (((HttpRequest)msg).uri().equals("/")) {
						var res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
								HttpResponseStatus.OK, Unpooled.wrappedBuffer(RES_BODY));
						//noinspection VulnerableCodeUsages
						res.headers()
								.set(HttpHeaderNames.CONTENT_LENGTH, CONTENT_LENGTH)
								.set(HttpHeaderNames.CONTENT_TYPE, CONTENT_TYPE)
								.set(HttpHeaderNames.SERVER, SERVER)
								.set(HttpHeaderNames.DATE, date);
						ctx.write(res, ctx.voidPromise());
					} else
						ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND,
								Unpooled.EMPTY_BUFFER)).addListener(ChannelFutureListener.CLOSE);
				} finally {
					ReferenceCountUtil.release(msg);
				}
			}
			if (msg instanceof LastHttpContent)
				ctx.flush();
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			ctx.close();
		}
	}

	public static void main(String[] args) {
		EventLoopGroup loopGroup;
		Class<? extends ServerChannel> serverChannelClass;
		var b = new ServerBootstrap();
		if (Epoll.isAvailable()) {
			b.option(EpollChannelOption.SO_REUSEPORT, true);
			loopGroup = new EpollEventLoopGroup();
			serverChannelClass = EpollServerSocketChannel.class;
		} else {
			loopGroup = new NioEventLoopGroup();
			serverChannelClass = NioServerSocketChannel.class;
		}
		//HttpContent
		//HttpObjectAggregator
		var format = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
		loopGroup.next().scheduleWithFixedDelay(() -> Handler.date = format.format(new Date()), 0, 1, TimeUnit.SECONDS);
		var addr = new InetSocketAddress(Integer.getInteger("port", 80));
		System.out.println("use " + serverChannelClass.getName() + ", listen " + addr);
		//noinspection VulnerableCodeUsages
		b.group(loopGroup)
				.option(ChannelOption.SO_BACKLOG, 8192)
				.option(ChannelOption.SO_REUSEADDR, true)
				.childOption(ChannelOption.SO_REUSEADDR, true)
				.channel(serverChannelClass).childHandler(new ChannelInitializer<SocketChannel>() {
					private static final HttpDecoderConfig decCfg = new HttpDecoderConfig()
							.setMaxInitialLineLength(4096)
							.setMaxHeaderSize(8192)
							.setMaxChunkSize(8192)
							.setChunkedSupported(true)
							.setValidateHeaders(false);

					@Override
					protected void initChannel(SocketChannel ch) {
						ch.pipeline()
								.addLast("encoder", new HttpResponseEncoder())
								.addLast("decoder", new HttpRequestDecoder(decCfg))
								.addLast("handler", new Handler());
					}
				}).bind(addr); //.sync().channel().closeFuture().sync();
		// loopGroup.shutdownGracefully().sync();
	}
}
