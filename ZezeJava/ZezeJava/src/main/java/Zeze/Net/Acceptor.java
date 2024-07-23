package Zeze.Net;

import java.net.InetSocketAddress;
import java.util.concurrent.locks.ReentrantLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

public class Acceptor extends ReentrantLock {
	private @NotNull String ip;
	private int port;
	private Service service;
	private AsyncSocket socket;

	public Acceptor(int port, @Nullable String ip) {
		this.ip = ip != null ? ip : "";
		this.port = port;
		fixIp();
	}

	public Acceptor(@NotNull Element self) {
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

	public final @NotNull String getIp() {
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

	public final @NotNull String getName() {
		return ip + '_' + port;
	}

	public final Service getService() {
		return service;
	}

	public final AsyncSocket getSocket() {
		return socket;
	}

	public final void SetService(Service service) {
		lock();
		try {
			if (this.service != null)
				throw new IllegalStateException("Acceptor of '" + getName() + "' Service != null");
			this.service = service;
		} finally {
			unlock();
		}
	}

	public final void Start() {
		lock();
		try {
			if (socket == null) {
				socket = ip.isEmpty()
						? service.newServerSocket(new InetSocketAddress(port), this)
						: service.newServerSocket(ip, port, this);
			}
		} finally {
			unlock();
		}
	}

	public final void Stop() {
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
