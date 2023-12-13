package Zeze.Netty;

import java.io.Closeable;
import Zeze.Util.Task;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("VulnerableCodeUsages")
public class Netty implements Closeable {
	static final @NotNull Logger logger = LogManager.getLogger(Netty.class);
	private static final @NotNull Class<? extends ServerChannel> serverChannelClass =
			Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class;

	private final @NotNull EventLoopGroup eventLoopGroup;

	public Netty() {
		this(Runtime.getRuntime().availableProcessors());
	}

	public Netty(int nThreads) {
		var threadFactory = new DefaultThreadFactory("ZezeNetty");
		eventLoopGroup = Epoll.isAvailable()
				? new EpollEventLoopGroup(nThreads, threadFactory)
				: new NioEventLoopGroup(nThreads, threadFactory);
	}

	public @NotNull EventLoopGroup getEventLoopGroup() {
		return eventLoopGroup;
	}

	public @NotNull ChannelFuture startServer(@NotNull ChannelHandler handler, int port) {
		return startServer(handler, null, port);
	}

	// 各种选项可配置。ServerBootstrapConfig?
	public @NotNull ChannelFuture startServer(@NotNull ChannelHandler handler, @Nullable String host, int port) {
		var b = new ServerBootstrap();
		if (eventLoopGroup instanceof EpollEventLoopGroup)
			b.option(EpollChannelOption.SO_REUSEPORT, true);
		var bs = b.group(eventLoopGroup)
				.option(ChannelOption.SO_BACKLOG, 8192)
				.option(ChannelOption.SO_REUSEADDR, true)
				.childOption(ChannelOption.SO_REUSEADDR, true)
				.childOption(ChannelOption.ALLOW_HALF_CLOSURE, true)
				.channel(serverChannelClass)
				.childHandler(handler);
		ChannelFuture future;
		if (host != null) {
			future = bs.bind(host, port);
			logger.info("startServer {} on {}:{}", handler.getClass().getName(), host, port);
		} else {
			future = bs.bind(port);
			logger.info("startServer {} on any:{}", handler.getClass().getName(), port);
		}
		return future;
	}

	public @NotNull Future<?> closeAsync() {
		return eventLoopGroup.shutdownGracefully();
	}

	@Override
	public void close() {
		try {
			closeAsync().sync();
		} catch (InterruptedException e) {
			Task.forceThrow(e);
		}
	}
}
