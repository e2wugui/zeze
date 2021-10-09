package Zeze.Net;

import Zeze.*;

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
		return String.format("%1$s:%2$s", getIp(), getPort());
	}

	public Acceptor(int port, String ip) {
		Port = port;
		Ip = ip;
	}

	public Acceptor(XmlElement self) {
		String attr = self.GetAttribute("Port");
		if (attr.length() > 0) {
			Port = Integer.parseInt(attr);
		}
		Ip = self.GetAttribute("Ip");
	}

	public final void SetService(Service service) {
		synchronized (this) {
			if (getService() != null) {
				throw new RuntimeException(String.format("Acceptor of '%1$s' Service != null", getName()));
			}
			setService(service);
		}
	}

	public final void Start() {
		synchronized (this) {
			if (null != getSocket()) {
				return;
			}

			setSocket(getIp().length() > 0 ? getService().NewServerSocket(getIp(), getPort()) : getService().NewServerSocket(System.Net.IPAddress.Any, getPort()));
			getSocket().setAcceptor(this);
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