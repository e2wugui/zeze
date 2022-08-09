package Zeze.Netty;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import io.netty.bootstrap.ServerBootstrap;
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
		var netty = new Netty();
		try {
			var http = new HttpServer();
			http.addHandler("/hello",
					8192, TransactionLevel.Serializable, DispatchMode.Normal,
					(x) -> {
						x.sendFullResponsePlainText(HttpResponseStatus.OK, "hello " + Calendar.getInstance());
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
}
