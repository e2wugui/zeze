package Zeze.Net;

import java.net.InetSocketAddress;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

public class Acceptor {
	private @NotNull String Ip;
	private int Port;
	private Service Service;
	private AsyncSocket Socket;

	public Acceptor(int port, String ip) {
		Ip = ip != null ? ip : "";
		Port = port;
	}

	public Acceptor(Element self) {
		Ip = self.getAttribute("Ip");
		String attr = self.getAttribute("Port");
		Port = attr.isEmpty() ? 0 : Integer.parseInt(attr);
	}

	public final String getIp() {
		return Ip;
	}

	public final void setIp(String ip) {
		Ip = ip != null ? ip : "";
	}

	public final int getPort() {
		return Port;
	}

	public final void setPort(int port) {
		Port = port;
	}

	public final String getName() {
		return Ip + '_' + Port;
	}

	public final Service getService() {
		return Service;
	}

	public final AsyncSocket getSocket() {
		return Socket;
	}

	public final synchronized void SetService(Service service) {
		if (Service != null)
			throw new IllegalStateException("Acceptor of '" + getName() + "' Service != null");
		Service = service;
	}

	public final synchronized void Start() {
		if (Socket == null)
			Socket = Ip.isEmpty()
					? Service.newServerSocket(new InetSocketAddress(Port), this)
					: Service.newServerSocket(Ip, Port, this);
	}

	public final synchronized void Stop() {
		if (Socket != null) {
			Socket.close();
			Socket = null;
		}
	}
}
