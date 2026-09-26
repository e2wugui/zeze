package Zeze.log;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileSessionManager {

	// HTTP处理器在Normal线程池并发get/put，必须是并发容器。键是纯IP：同IP（NAT多用户/
	// 同用户切换Session↔SessionAll视图）会互相替换——被替换的session不得立即close（增量审
	// R1-05：会踩掉正在1分钟browse的在飞会话），也不得并发close（互踩升级）。替换出的旧session
	// 句柄滞留到进程结束，完整的空闲淘汰/多会话键见 design-GE-D06。
	private static final Map<String, Object> map = new ConcurrentHashMap<>(1000);

	public static void put(SocketAddress socketAddress, Object session) {
		map.put(getIP(socketAddress), session);
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
