package Zeze.Net;

import java.net.InetSocketAddress;
import java.util.concurrent.locks.ReentrantLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

public class Acceptor extends ReentrantLock {
	private @NotNull String Ip;
	private int Port;
	private Service Service;
	private AsyncSocket Socket;

	public Acceptor(int port, @Nullable String ip) {
		Ip = ip != null ? ip : "";
		Port = port;
		fixIp();
	}

	public Acceptor(@NotNull Element self) {
		Ip = self.getAttribute("Ip");
		String attr = self.getAttribute("Port");
		Port = attr.isEmpty() ? 0 : Integer.parseInt(attr);
		fixIp();
	}

	private void fixIp() {
		if (Ip.equals("@internal"))
			Ip = Helper.selectOneIpAddress(true);
		else if (Ip.equals("@external"))
			Ip = Helper.selectOneIpAddress(false);
		else
			return;

		if (Ip.isBlank())
			throw new IllegalStateException("Acceptor.Ip invalid: " + Ip);
	}

	public final @NotNull String getIp() {
		return Ip;
	}

	public final void setIp(@Nullable String ip) {
		Ip = ip != null ? ip : "";
	}

	public final int getPort() {
		return Port;
	}

	public final void setPort(int port) {
		Port = port;
	}

	public final @NotNull String getName() {
		return Ip + '_' + Port;
	}

	public final Service getService() {
		return Service;
	}

	public final AsyncSocket getSocket() {
		return Socket;
	}

	public final void SetService(Service service) {
		lock();
		try {
			if (Service != null)
				throw new IllegalStateException("Acceptor of '" + getName() + "' Service != null");
			Service = service;
		} finally {
			unlock();
		}
	}

	public final void Start() {
		lock();
		try {
			if (Socket == null)
				Socket = Ip.isEmpty()
						? Service.newServerSocket(new InetSocketAddress(Port), this)
						: Service.newServerSocket(Ip, Port, this);
		} finally {
			unlock();
		}
	}

	public final void Stop() {
		lock();
		try {
			if (Socket != null) {
				Socket.close();
				Socket = null;
			}
		} finally {
			unlock();
		}
	}
}
