package Zeze.Netty;

import java.io.Closeable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import Zeze.Util.GlobalTimer;
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
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Netty implements Closeable {
	static final @NotNull Logger logger = LogManager.getLogger(Netty.class);
	private static final @NotNull ZoneId zoneId = ZoneId.of("GMT");
	private static long lastSecond;
	private static String lastDateStr;
	private static final @NotNull Class<? extends ServerChannel> serverChannelClass =
			Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class;

	private final @NotNull EventLoopGroup eventLoopGroup;

	public static @NotNull String getDate() {
		var second = GlobalTimer.getCurrentMillis() / 1000;
		if (second == lastSecond)
			return lastDateStr;
		var dateStr = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.of(
				LocalDateTime.ofEpochSecond(second, 0, ZoneOffset.UTC), zoneId));
		lastDateStr = dateStr;
		lastSecond = second;
		return dateStr;
	}

	public static @NotNull String getDate(long epochSecond) {
		return DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.of(
				LocalDateTime.ofEpochSecond(epochSecond, 0, ZoneOffset.UTC), zoneId));
	}

	public static long parseDate(@NotNull String dateStr) {
		return LocalDateTime.parse(dateStr, DateTimeFormatter.RFC_1123_DATE_TIME).toEpochSecond(ZoneOffset.UTC);
	}

	public static @NotNull HttpHeaders setDate(@NotNull HttpHeaders headers) {
		headers.set(HttpHeaderNames.DATE, getDate());
		return headers;
	}

	public static long getLastDateSecond() {
		return lastSecond;
	}

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
