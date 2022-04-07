package Zeze.Net;

import java.net.InetSocketAddress;
import org.w3c.dom.Element;

public class Acceptor {
	private final String Ip;
	private int Port;
	private Service Service;
	private AsyncSocket Socket;

	public Acceptor(int port, String ip) {
		Ip = ip != null ? ip : "";
		Port = port;
	}

	public void setPort(int port) {
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

	public final int getPort() {
		return Port;
	}

	public final String getName() {
		return Ip + ':' + Port;
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
					? getService().NewServerSocket(new InetSocketAddress(Port), this)
					: getService().NewServerSocket(Ip, Port, this);
	}

	public final synchronized void Stop() {
		if (Socket != null) {
			Socket.close();
			Socket = null;
		}
	}
}
