package harness;

import java.net.InetSocketAddress;
import java.net.Socket;

public final class TestEnv {
	private TestEnv() {
	}

	/**
	 * 探测端口是否已有进程监听（本地、短超时）。
	 * 用于判断外部服务（ServiceManager/GlobalCacheManager）是否已手工启动。
	 */
	public static boolean portReachable(String host, int port) {
		try (var socket = new Socket()) {
			socket.connect(new InetSocketAddress(host, port), 200);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
