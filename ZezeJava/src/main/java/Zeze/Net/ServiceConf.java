package Zeze.Net;

import Zeze.*;
import Zeze.Util.Str;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

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
	private final String Name;
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

	private final ConcurrentHashMap<String, Acceptor> Acceptors = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Connector> Connectors = new ConcurrentHashMap<> ();

	public void SetService(Service service) throws Throwable {
		synchronized (this) {
			if (getService() != null) {
				throw new RuntimeException(Str.format("ServiceConf of '{}' Service != null", getName()));
			}
			setService(service);
			ForEachAcceptor((a) -> a.SetService(service));
			ForEachConnector((c) -> c.SetService(service));
		}
	}

	public void AddConnector(Connector connector) {
		if (null != Connectors.putIfAbsent(connector.getName(), connector)) {
			throw new RuntimeException("Duplicate Connector=" + connector.getName());
		}
		connector.SetService(getService());
	}

	public Connector FindConnector(String name) {
		return Connectors.get(name);
	}

	public Connector FindConnector(String host, int port) {
		return FindConnector(host + ":" + port);
	}

	/** 
	 查找，不存在则创建。
	 
	 @param host
	 peer address or name
	 @param port
	 peer port
	 @param autoReconnect
	 auto reconnect when socket close.
	 @param getOrAdd
	 out. connector returned.
	 @return true if addNew
	*/
	public boolean TryGetOrAddConnector(String host, int port, boolean autoReconnect,
			Zeze.Util.OutObject<Connector> getOrAdd) {

		var name = host + ":" + port;
		final var addNew = new Zeze.Util.OutObject<Connector>();
		getOrAdd.Value = Connectors.computeIfAbsent(name,
			(key) -> {
				Connector add = new Connector(host, port, autoReconnect);
				add.SetService(getService());
				addNew.Value = add;
				return add;
			});

		return addNew.Value != null;
	}

	public void RemoveConnector(Connector c) {
		Connectors.remove(c.getName(), c);
	}

	public void ForEachConnector(Zeze.Util.Action1<Connector> action) throws Throwable {
		for (var c : Connectors.values()) {
			action.run(c);
		}
	}

	public int ConnectorCount() {
		return Connectors.size();
	}

	public boolean ForEachConnector2(Zeze.Util.Func1<Connector, Boolean> func) throws Throwable {
		for (var c : Connectors.values()) {
			if (!func.call(c)) {
				return false;
			}
		}
		return true;
	}

	public void AddAcceptor(Acceptor a) {
		if (null != Acceptors.putIfAbsent(a.getName(), a)) {
			throw new RuntimeException("Duplicate Acceptor=" + a.getName());
		}
		a.SetService(getService());
	}

	public void RemoveAcceptor(Acceptor a) {
		Acceptors.remove(a.getName(), a);
	}

	public void ForEachAcceptor(Zeze.Util.Action1<Acceptor> action) throws Throwable {
		for (var a : Acceptors.values()) {
			action.run(a);
		}
	}

	public boolean ForEachAcceptor2(Function<Acceptor, Boolean> func) {
		for (var a : Acceptors.values()) {
			if (!func.apply(a)) {
				return false;
			}
		}
		return true;
	}

	public int AcceptorCount() {
		return Acceptors.size();
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
			getHandshakeOptions().setDhGroups(new HashSet<>());
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
		{
			String name = getName();
			if ( name == null || name.isEmpty()) {
				conf.setDefaultServiceConf(this);
			}
			else if (null != conf.getServiceConfMap().putIfAbsent(name, this)) {
				throw new RuntimeException("Duplicate ServiceConf " + getName());
			}
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

	public void Start() throws Throwable {
		ForEachAcceptor(Acceptor::Start);
		ForEachConnector(Connector::Start);
	}

	public void Stop() throws Throwable {
		ForEachAcceptor(Acceptor::Stop);
		ForEachConnector(Connector::Stop);
	}

	public void StopListen() throws Throwable {
		ForEachAcceptor(Acceptor::Stop);
	}
}