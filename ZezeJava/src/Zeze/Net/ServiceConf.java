package Zeze.Net;

import Zeze.*;

import java.net.InetAddress;
import java.util.*;

import org.apache.logging.log4j.Level;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class ServiceConf {
	private Service Service;
	public Service getService() {
		return Service;
	}
	private void setService(Service value) {
		Service = value;
	}
	private String Name;
	public String getName() {
		return Name;
	}
	private Zeze.Net.SocketOptions SocketOptions = new Zeze.Net.SocketOptions();
	public Zeze.Net.SocketOptions getSocketOptions() {
		return SocketOptions;
	}
	public void setSocketOptions(Zeze.Net.SocketOptions value) {
		SocketOptions = value;
	}
	private Zeze.Services.HandshakeOptions HandshakeOptions = new Zeze.Services.HandshakeOptions();
	public Zeze.Services.HandshakeOptions getHandshakeOptions() {
		return HandshakeOptions;
	}
	public void setHandshakeOptions(Zeze.Services.HandshakeOptions value) {
		HandshakeOptions = value;
	}

	private java.util.concurrent.ConcurrentHashMap<String, Acceptor> Acceptors = new java.util.concurrent.ConcurrentHashMap<String, Acceptor> ();
	private java.util.concurrent.ConcurrentHashMap<String, Acceptor> getAcceptors() {
		return Acceptors;
	}
	private java.util.concurrent.ConcurrentHashMap<String, Connector> Connectors = new java.util.concurrent.ConcurrentHashMap<String, Connector> ();
	private java.util.concurrent.ConcurrentHashMap<String, Connector> getConnectors() {
		return Connectors;
	}

	public void SetService(Service service) {
		synchronized (this) {
			if (getService() != null) {
				throw new RuntimeException(String.format("ServiceConf of '%1$s' Service != null", getName()));
			}
			setService(service);
			ForEachAcceptor((a) -> a.SetService(service));
			ForEachConnector((c) -> c.SetService(service));
		}
	}

	public void AddConnector(Connector connector) {
		if (null != getConnectors().putIfAbsent(connector.getName(), connector)) {
			throw new RuntimeException(String.format("Duplicate Connector=%1$s", connector.getName()));
		}
		connector.SetService(getService());
	}

	public Connector FindConnector(String name) {
		return getConnectors().get(name);
	}

	public Connector FindConnector(String host, int port) {
		return FindConnector(String.format("%1$s:%2$s", host, port));
	}

	/** 
	 查找，不存在则创建。
	 
	 @param host
	 @param port
	 @param autoReconnect
	 @param getOrAdd
	 @return true if addNew
	*/
	public boolean TryGetOrAddConnector(String host, int port, boolean autoReconnect,
			Zeze.Util.OutObject<Connector> getOrAdd) {

		var name = String.format("%1$s:%2$s", host, port);
		final var addNew = new Zeze.Util.OutObject<Connector>();
		getOrAdd.Value = getConnectors().computeIfAbsent(name,
			(key) -> {
				Connector add = new Connector(host, port, autoReconnect);
				add.SetService(getService());
				addNew.Value = add;
				return add;
			});

		return addNew.Value != null;
	}

	public void RemoveConnector(Connector c) {
		getConnectors().remove(c.getName(), c);
	}

	public void ForEachConnector(tangible.Action1Param<Connector> action) {
		for (var c : getConnectors().values()) {
			action.invoke(c);
		}
	}

	public int ConnectorCount() {
		return getConnectors().size();
	}

	public boolean ForEachConnector2(tangible.Func1Param<Connector, Boolean> func) {
		for (var c : getConnectors().values()) {
			if (false == func.invoke(c)) {
				return false;
			}
		}
		return true;
	}

	public void AddAcceptor(Acceptor a) {
		if (null != getAcceptors().putIfAbsent(a.getName(), a)) {
			throw new RuntimeException(String.format("Duplicate Acceptor=%1$s", a.getName()));
		}
		a.SetService(getService());
	}

	public void RemoveAcceptor(Acceptor a) {
		getAcceptors().remove(a.getName(), a);
	}

	public void ForEachAcceptor(tangible.Action1Param<Acceptor> action) {
		for (var a : getAcceptors().values()) {
			action.invoke(a);
		}
	}

	public boolean ForEachAcceptor2(tangible.Func1Param<Acceptor, Boolean> func) {
		for (var a : getAcceptors().values()) {
			if (false == func.invoke(a)) {
				return false;
			}
		}
		return true;
	}

	public int AcceptorCount() {
		return getAcceptors().size();
	}

	public ServiceConf() {
		Name = "";
	}

	public ServiceConf(Config conf, Element self) {
		Name = self.getAttribute("Name");

		String attr;

		// SocketOptions
		attr = self.getAttribute("NoDelay");
		if (attr.length() > 0) {
			getSocketOptions().setNoDelay(Boolean.parseBoolean(attr));
		}
		attr = self.getAttribute("SendBuffer");
		if (attr.length() > 0) {
			getSocketOptions().setSendBuffer(Integer.parseInt(attr));
		}
		attr = self.getAttribute("ReceiveBuffer");
		if (attr.length() > 0) {
			getSocketOptions().setReceiveBuffer(Integer.parseInt(attr));
		}
		attr = self.getAttribute("InputBufferSize");
		if (attr.length() > 0) {
			getSocketOptions().setInputBufferSize(Integer.parseInt(attr));
		}
		attr = self.getAttribute("InputBufferMaxProtocolSize");
		if (attr.length() > 0) {
			getSocketOptions().setInputBufferMaxProtocolSize(Integer.parseInt(attr));
		}
		attr = self.getAttribute("OutputBufferMaxSize");
		if (attr.length() > 0) {
			getSocketOptions().setOutputBufferMaxSize(Integer.parseInt(attr));
		}
		attr = self.getAttribute("Backlog");
		if (attr.length() > 0) {
			getSocketOptions().setBacklog(Integer.parseInt(attr));
		}
		attr = self.getAttribute("SocketLogLevel");
		if (attr.length() > 0) {
			getSocketOptions().setSocketLogLevel(Level.toLevel(attr));
		}

		// HandshakeOptions
		attr = self.getAttribute("DhGroups");
		if (attr.length() > 0) {
			getHandshakeOptions().setDhGroups(new HashSet<Integer>());
			for (String dg : attr.split("[,]", -1)) {
				String dgtmp = dg.strip();
				if (dgtmp.length() == 0) {
					continue;
				}
				getHandshakeOptions().AddDhGroup(Integer.parseInt(dgtmp));
			}
		}
		attr = self.getAttribute("SecureIp");
		if (attr.length() > 0) {
			try {
				getHandshakeOptions().setSecureIp(InetAddress.getByName(attr).getAddress());
			}
			catch (Throwable ex) {
				throw new RuntimeException(ex);
			}
		}
		attr = self.getAttribute("S2cNeedCompress");
		if (attr.length() > 0) {
			getHandshakeOptions().setS2cNeedCompress(Boolean.parseBoolean(attr));
		}
		attr = self.getAttribute("C2sNeedCompress");
		if (attr.length() > 0) {
			getHandshakeOptions().setC2sNeedCompress(Boolean.parseBoolean(attr));
		}
		attr = self.getAttribute("DhGroup");
		if (attr.length() > 0) {
			getHandshakeOptions().setDhGroup(Byte.parseByte(attr));
		}

		if (tangible.StringHelper.isNullOrEmpty(getName())) {
			conf.setDefaultServiceConf(this);
		}
		else if (null != conf.getServiceConfMap().putIfAbsent(getName(), this)) {
			throw new RuntimeException(String.format("Duplicate ServiceConf '%1$s'", getName()));
		}

		// connection creator options
		NodeList childnodes = self.getChildNodes();
		for (int i = 0; i < childnodes.getLength(); ++i) {
			Node node = childnodes.item(i);
			if (Node.ELEMENT_NODE != node.getNodeType()) {
				continue;
			}

			Element e = (Element) node;
			switch (e.getNodeName()) {
				case "Acceptor":
					AddAcceptor(new Acceptor(e));
					break;
				case "Connector":
					AddConnector(Connector.Create(e));
					break;
				default:
					throw new RuntimeException("unknown node name: " + e.getNodeName());
			}
		}
	}

	public void Start() {
		ForEachAcceptor((a) -> a.Start());
		ForEachConnector((c) -> c.Start());
	}

	public void Stop() {
		ForEachAcceptor((a) -> a.Stop());
		ForEachConnector((c) -> c.Stop());
	}

	public void StopListen() {
		ForEachAcceptor((a) -> a.Stop());
	}
}