package Zeze.Net;

import java.net.InetSocketAddress;
import org.w3c.dom.Element;

public class Acceptor {
	private Service Service;
	public final Service getService() {
		return Service;
	}
	private void setService(Service value) {
		Service = value;
	}
	private int Port = 0;
	public final int getPort() {
		return Port;
	}
	private String Ip = "";
	public final String getIp() {
		return Ip;
	}
	private AsyncSocket Socket;
	public final AsyncSocket getSocket() {
		return Socket;
	}
	private void setSocket(AsyncSocket value) {
		Socket = value;
	}
	public final String getName() {
		return getIp() + ":"  + getPort();
	}

	public Acceptor(int port, String ip) {
		Port = port;
		Ip = ip;
	}

	public Acceptor(Element self) {
		String attr = self.getAttribute("Port");
		if (attr.length() > 0) {
			Port = Integer.parseInt(attr);
		}
		Ip = self.getAttribute("Ip");
	}

	public final void SetService(Service service) {
		synchronized (this) {
			if (getService() != null) {
				throw new RuntimeException("Acceptor of '" + getName() + "' Service != null");
			}
			setService(service);
		}
	}

	public final void Start() throws Throwable {
		synchronized (this) {
			if (null != getSocket()) {
				return;
			}

			setSocket(getIp().length() > 0
					? getService().NewServerSocket(getIp(), getPort(), this)
					: getService().NewServerSocket(new InetSocketAddress(0).getAddress(), getPort(), this));
		}
	}

	public final void Stop() {
		synchronized (this) {
			if (getSocket() != null) {
				getSocket().close();
			}
			setSocket(null);
		}
	}
}