package Zeze.Net;

import Zeze.*;
import java.util.*;

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
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (!getConnectors().TryAdd(connector.getName(), connector)) {
			throw new RuntimeException(String.format("Duplicate Connector=%1$s", connector.getName()));
		}
		connector.SetService(getService());
	}

	public Connector FindConnector(String name) {
		TValue exist;
		tangible.OutObject<TValue> tempOut_exist = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (getConnectors().TryGetValue(name, tempOut_exist)) {
		exist = tempOut_exist.outArgValue;
			return exist;
		}
	else {
		exist = tempOut_exist.outArgValue;
	}
		return null;
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
	public boolean TryGetOrAddConnector(String host, int port, boolean autoReconnect, tangible.OutObject<Connector> getOrAdd) {
		var name = String.format("%1$s:%2$s", host, port);
		Connector addNew = null;
		getOrAdd.outArgValue = getConnectors().putIfAbsent(name, (_) -> {
				addNew = new Connector(host, port, autoReconnect);
				return addNew;
		});
		if (addNew != null) {
			addNew.SetService(getService());
		}

		return addNew != null;
	}

	public void RemoveConnector(Connector c) {
		TValue _;
		tangible.OutObject<Connector> tempOut__ = new tangible.OutObject<Connector>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getConnectors().TryRemove(c.getName(), tempOut__);
	_ = tempOut__.outArgValue;
	}

	public void ForEachConnector(tangible.Action1Param<Connector> action) {
		for (var c : getConnectors().values()) {
			action.invoke(c);
		}
	}

	public int ConnectorCount() {
		return getConnectors().size();
	}

	public boolean ForEachConnector(tangible.Func1Param<Connector, Boolean> func) {
		for (var c : getConnectors().values()) {
			if (false == func.invoke(c)) {
				return false;
			}
		}
		return true;
	}

	public void AddAcceptor(Acceptor a) {
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (!getAcceptors().TryAdd(a.getName(), a)) {
			throw new RuntimeException(String.format("Duplicate Acceptor=%1$s", a.getName()));
		}
		a.SetService(getService());
	}

	public void RemoveAcceptor(Acceptor a) {
		getAcceptors().remove(a.getName());
	}

	public void ForEachAcceptor(tangible.Action1Param<Acceptor> action) {
		for (var a : getAcceptors().values()) {
			action.invoke(a);
		}
	}

	public boolean ForEachAcceptor(tangible.Func1Param<Acceptor, Boolean> func) {
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

	public ServiceConf(Config conf, XmlElement self) {
		Name = self.GetAttribute("Name");

		String attr;

		// SocketOptions
		attr = self.GetAttribute("NoDelay");
		if (attr.length() > 0) {
			getSocketOptions().setNoDelay(Boolean.parseBoolean(attr));
		}
		attr = self.GetAttribute("SendBuffer");
		if (attr.length() > 0) {
			getSocketOptions().setSendBuffer(Integer.parseInt(attr));
		}
		attr = self.GetAttribute("ReceiveBuffer");
		if (attr.length() > 0) {
			getSocketOptions().setReceiveBuffer(Integer.parseInt(attr));
		}
		attr = self.GetAttribute("InputBufferSize");
		if (attr.length() > 0) {
			getSocketOptions().setInputBufferSize(Integer.parseInt(attr));
		}
		attr = self.GetAttribute("InputBufferMaxProtocolSize");
		if (attr.length() > 0) {
			getSocketOptions().setInputBufferMaxProtocolSize(Integer.parseInt(attr));
		}
		attr = self.GetAttribute("OutputBufferMaxSize");
		if (attr.length() > 0) {
			getSocketOptions().setOutputBufferMaxSize(Integer.parseInt(attr));
		}
		attr = self.GetAttribute("Backlog");
		if (attr.length() > 0) {
			getSocketOptions().setBacklog(Integer.parseInt(attr));
		}
		attr = self.GetAttribute("SocketLogLevel");
		if (attr.length() > 0) {
			getSocketOptions().setSocketLogLevel(Level.FromString(attr));
		}

		// HandshakeOptions
		attr = self.GetAttribute("DhGroups");
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
		attr = self.GetAttribute("SecureIp");
		if (attr.length() > 0) {
			getHandshakeOptions().setSecureIp(System.Net.IPAddress.Parse(attr).GetAddressBytes());
		}
		attr = self.GetAttribute("S2cNeedCompress");
		if (attr.length() > 0) {
			getHandshakeOptions().setS2cNeedCompress(Boolean.parseBoolean(attr));
		}
		attr = self.GetAttribute("C2sNeedCompress");
		if (attr.length() > 0) {
			getHandshakeOptions().setC2sNeedCompress(Boolean.parseBoolean(attr));
		}
		attr = self.GetAttribute("DhGroup");
		if (attr.length() > 0) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: HandshakeOptions.DhGroup = byte.Parse(attr);
			getHandshakeOptions().setDhGroup(Byte.parseByte(attr));
		}

		if (tangible.StringHelper.isNullOrEmpty(getName())) {
			conf.setDefaultServiceConf(this);
		}
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		else if (!conf.getServiceConfMap().TryAdd(getName(), this)) {
			throw new RuntimeException(String.format("Duplicate ServiceConf '%1$s'", getName()));
		}

		// connection creator options
		XmlNodeList childNodes = self.ChildNodes;
		for (XmlNode node : childNodes) {
			if (XmlNodeType.Element != node.NodeType) {
				continue;
			}

			XmlElement e = (XmlElement)node;
			switch (e.Name) {
				case "Acceptor":
					AddAcceptor(new Acceptor(e));
					break;
				case "Connector":
					AddConnector(Connector.Create(e));
					break;
				default:
					throw new RuntimeException("unknown node name: " + e.Name);
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