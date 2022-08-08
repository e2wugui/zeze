package Zeze.Netty;

import java.util.concurrent.TimeUnit;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;

public class Netty {
	private EventLoopGroup eventLoopGroup;

	public Netty() {
		this(Runtime.getRuntime().availableProcessors());
	}

	public Netty(int nThreads) {
		if (Epoll.isAvailable()) {
			eventLoopGroup = new EpollEventLoopGroup(nThreads);
		} else {
			eventLoopGroup = new NioEventLoopGroup(nThreads);
		}
	}

	// 各种选项可配置。ServerBootstrapConfig?
	public void addServer(ChannelInitializer<SocketChannel> handler, int port) {
		new ServerBootstrap().group(eventLoopGroup)
				.option(ChannelOption.SO_BACKLOG, 8192)
				.option(ChannelOption.SO_REUSEADDR, true)
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
}
