package Zeze.Netty;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Str;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;

public class Netty {
	private EventLoopGroup eventLoopGroup;

	public Netty() {
		this(Runtime.getRuntime().availableProcessors());
	}

	private Class<? extends ServerChannel> serverChannelClass;
	public Netty(int nThreads) {
		if (Epoll.isAvailable()) {
			eventLoopGroup = new EpollEventLoopGroup(nThreads);
			serverChannelClass = EpollServerSocketChannel.class;
		} else {
			eventLoopGroup = new NioEventLoopGroup(nThreads);
			serverChannelClass = NioServerSocketChannel.class;
		}
	}

	// 各种选项可配置。ServerBootstrapConfig?
	public void addServer(ChannelInitializer<SocketChannel> handler, int port) {
		new ServerBootstrap().group(eventLoopGroup)
				.option(ChannelOption.SO_BACKLOG, 8192)
				.option(ChannelOption.SO_REUSEADDR, true)
				.channel(serverChannelClass)
				.childHandler(handler)
				.bind(port);
	}

	public void start() {
		// 好像不用做什么。
	}

	public void stop() throws InterruptedException {
		eventLoopGroup.shutdownGracefully();
		eventLoopGroup.awaitTermination(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
	}

	public static void main(String args[]) throws InterruptedException {
		/**
		 * 运行，用浏览器访问 127.0.0.1/hello;127.0.0.1/exp;127.0.0.1/404
		 */
		var netty = new Netty();
		try {
			var http = new HttpServer();
			http.addHandler("/hello", // 显示一个文本结果。
					8192, TransactionLevel.Serializable, DispatchMode.Direct,
					(x) -> {
						var sb = new StringBuilder();
						sb.append("uri=").append(x.uri()).append("\n");
						sb.append("path=").append(x.path()).append("\n");
						sb.append("query=").append(x.query()).append("\n");
						for (var header : x.headers())
							sb.append(header.getKey()).append("=").append(header.getValue()).append("\n");
						x.sendPlainText(HttpResponseStatus.OK, sb.toString());
					});
			http.addHandler("/ex", // 抛异常
					8192, TransactionLevel.Serializable, DispatchMode.Direct,
					(x) -> {
						throw new UnsupportedOperationException();
					});
			http.addHandler("/stream",
					8192, TransactionLevel.Serializable, DispatchMode.Direct,
					(x) -> {
						var headers = new DefaultHttpHeaders();
						headers.add(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8");
						x.beginStream(HttpResponseStatus.OK, headers);
						sendTrunk(x);
						/*
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
						*/
					});
			netty.addServer(http, 80);
			netty.start();
			while (true) {
				Thread.sleep(1000);
			}
		} finally {
			netty.stop();
		}
	}
	private static int trunkCount;
	private static void processSendTrunkResult(HttpExchange x, ChannelFuture f) {
		System.out.println("sent: " + trunkCount);
		if (f.isSuccess()) {
			if (trunkCount > 3)
				x.endStream();
			else
				sendTrunk(x);
			return;
		}
		System.out.println("error: " + Str.stacktrace(f.cause()));
		System.out.flush();
		x.close();
	}

	private static void sendTrunk(HttpExchange x) {
		trunkCount++;
		x.sendStream(("content " + trunkCount + "-").getBytes(StandardCharsets.UTF_8), Netty::processSendTrunkResult);
	}
}
