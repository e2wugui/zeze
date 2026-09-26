package Zeze.log;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileSessionManager {
	private static final Logger logger = LogManager.getLogger(FileSessionManager.class);

	// HTTP处理器在Normal线程池并发get/put，必须是并发容器；
	// 被替换的旧session（Session/SessionAll均AutoCloseable）立即关闭，防walker/游标滞留累积。
	private static final Map<String, Object> map = new ConcurrentHashMap<>(1000);

	public static void put(SocketAddress socketAddress, Object session) {
		var old = map.put(getIP(socketAddress), session);
		if (old != null && old != session && old instanceof AutoCloseable closeable) {
			try {
				closeable.close();
			} catch (Exception ex) {
				logger.error("close replaced session", ex);
			}
		}
	}

	public static Object get(SocketAddress socketAddress) {
		return map.get(getIP(socketAddress));
	}

	private static String getIP(SocketAddress socketAddress) {
		// InetSocketAddress.toString()对IPv6形如"/[0:0:0:0:0:0:0:1]:5678"，字符串切分会把
		// 所有IPv6客户端坍缩成同一个键；取规范host地址。
		if (socketAddress instanceof InetSocketAddress inet) {
			var address = inet.getAddress();
			return address != null ? address.getHostAddress() : inet.getHostString();
		}
		return socketAddress.toString();
	}
}
