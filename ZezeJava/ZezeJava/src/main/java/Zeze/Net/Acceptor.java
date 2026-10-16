package Zeze.Net;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * 服务端监听项：绑定 ip/port 配置并持有监听 socket。
 */
public class Acceptor extends ReentrantLock {
	private String ip;
	private int port;
	private @Nullable Service service;
	private @Nullable AsyncSocket socket;

	public Acceptor(int port, @Nullable String ip) {
		this.ip = ip != null ? ip : "";
		this.port = port;
		fixIp();
	}

	public Acceptor(Element self) {
		ip = self.getAttribute("Ip");
		String attr = self.getAttribute("Port");
		port = attr.isEmpty() ? 0 : Integer.parseInt(attr);
		fixIp();
	}

	private void fixIp() {
		if (ip.equals("@internal"))
			ip = Helper.selectOneIpAddress(true);
		else if (ip.equals("@external"))
			ip = Helper.selectOneIpAddress(false);
		else
			return;

		if (ip.isBlank())
			throw new IllegalStateException("Acceptor.Ip invalid: " + ip);
	}

	public final String getIp() {
		return ip;
	}

	public final void setIp(@Nullable String ip) {
		this.ip = ip != null ? ip : "";
	}

	public final int getPort() {
		return port;
	}

	public final void setPort(int port) {
		this.port = port;
	}

	public final String getName() {
		return ip + '_' + port;
	}

	public final @Nullable Service getService() {
		return service;
	}

	public final @Nullable AsyncSocket getSocket() {
		return socket;
	}

	public final void SetService(@Nullable Service service) {
		lock();
		try {
			if (this.service != null)
				throw new IllegalStateException("Acceptor of '" + getName() + "' Service != null");
			this.service = service;
		} finally {
			unlock();
		}
	}

	public final void start() {
		Objects.requireNonNull(service, "Acceptor of '" + getName() + "' Service == null, 请先 SetService"); // 防御NPE：直接提示配置遗漏
		lock();
		try {
			if (socket == null) {
				socket = ip.isEmpty()
						? getService().newServerSocket(new InetSocketAddress(port), this)
						: getService().newServerSocket(ip, port, this);
			}
		} finally {
			unlock();
		}
	}

	public final void stop() {
		lock();
		try {
			if (socket != null) {
				socket.close();
				socket = null;
			}
		} finally {
			unlock();
		}
	}
}
