package Zeze.Netty;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Str;
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
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.concurrent.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Netty {
	static final Logger logger = LogManager.getLogger(HttpExchange.class);
	private static final DateFormat dateFormat = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
	private static long lastSecond;
	private static String lastDateStr;
	private static final Class<? extends ServerChannel> serverChannelClass =
			Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class;

	private final EventLoopGroup eventLoopGroup;

	public static String getDate() {
		var timestamp = System.currentTimeMillis();
		var second = timestamp / 1000;
		if (second == lastSecond)
			return lastDateStr;
		var date = new Date(timestamp);
		String dateStr;
		synchronized (dateFormat) {
			lastDateStr = dateStr = dateFormat.format(date);
		}
		lastSecond = second;
		return dateStr;
	}

	public static String getDate(Date date) {
		synchronized (dateFormat) {
			return dateFormat.format(date);
		}
	}

	public static Date parseDate(String dateStr) throws ParseException {
		synchronized (dateFormat) {
			return dateFormat.parse(dateStr);
		}
	}

	public static HttpHeaders setDate(HttpHeaders headers) {
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
		eventLoopGroup = Epoll.isAvailable() ? new EpollEventLoopGroup(nThreads) : new NioEventLoopGroup(nThreads);
	}

	// 各种选项可配置。ServerBootstrapConfig?
	public ChannelFuture startServer(ChannelHandler handler, int port) {
		var b = new ServerBootstrap();
		if (eventLoopGroup instanceof EpollEventLoopGroup)
			b.option(EpollChannelOption.SO_REUSEPORT, true);
		var future = b.group(eventLoopGroup)
				.option(ChannelOption.SO_BACKLOG, 8192)
				.option(ChannelOption.SO_REUSEADDR, true)
				.childOption(ChannelOption.SO_REUSEADDR, true)
				.childOption(ChannelOption.ALLOW_HALF_CLOSURE, true)
				.channel(serverChannelClass)
				.childHandler(handler)
				.bind(port);
		logger.info("start server on port: {}", port);
		return future;
	}

	public Future<?> stopAsync() {
		return eventLoopGroup.shutdownGracefully();
	}

	public void stop() throws InterruptedException {
		stopAsync().sync();
	}

	public static void main(String[] args) throws InterruptedException {
		// 运行，用浏览器访问 127.0.0.1/hello;127.0.0.1/exp;127.0.0.1/404
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
						trunkCount = 0;
						sendTrunk(x);
						/*
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
						*/
					});
			var future = netty.startServer(http, 80);
			future.sync().channel().closeFuture().sync(); // 同步等待直到被停止
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
		x.closeConnectionNow();
	}

	private static void sendTrunk(HttpExchange x) {
		trunkCount++;
		x.sendStream(("content " + trunkCount + "-").getBytes(StandardCharsets.UTF_8), Netty::processSendTrunkResult);
	}
}
