package Zeze.Netty;

import java.io.Closeable;
import java.net.InetSocketAddress;
import Zeze.Util.Task;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class Netty implements Closeable {
	static final @NotNull Logger logger = LogManager.getLogger(Netty.class);

	private final @NotNull EventLoopGroup eventLoopGroup;

	public Netty() {
		this(0); // Netty默认的线程数,可用"-Dio.netty.eventLoopThreads="指定,默认是2倍核心数
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

	// 简单的文件下载服务
	public static void main(String @NotNull [] args) throws Exception {
		String host = null;
		int port = 0;
		int threads = 1;
		String urlpath = "/";
		String filepath = ".";
		boolean canListPath = false;

		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-host":
				host = args[++i];
				break;
			case "-port":
				port = Integer.parseInt(args[++i]);
				break;
			case "-threads":
				threads = Integer.parseInt(args[++i]);
				break;
			case "-urlpath":
				urlpath = args[++i];
				break;
			case "-filepath":
				filepath = args[++i];
				break;
			case "-canListPath":
				canListPath = true;
				break;
			}
		}

		Task.tryInitThreadPool();
		try (var netty = new Netty(threads); var server = new HttpServer()) {
			server.addFileHandler(urlpath, filepath, canListPath);
			var channel = server.start(netty, host, port).sync().channel();
			port = ((InetSocketAddress)channel.localAddress()).getPort();
			logger.info("listening http port: {}", port);
			channel.closeFuture().sync();
		}
	}
}
