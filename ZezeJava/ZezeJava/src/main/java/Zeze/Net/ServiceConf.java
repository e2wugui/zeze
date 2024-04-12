package Zeze.Net;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import Zeze.Config;
import Zeze.Services.HandshakeOptions;
import Zeze.Util.Action1;
import Zeze.Util.Func3;
import Zeze.Util.IntHashSet;
import Zeze.Util.OutObject;
import Zeze.Util.Str;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class ServiceConf extends ReentrantLock {
	private Service service;
	private final @NotNull String name;
	private @NotNull SocketOptions socketOptions = new SocketOptions();
	private @NotNull HandshakeOptions handshakeOptions = new HandshakeOptions();
	private final ConcurrentHashMap<String, Acceptor> acceptors = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Connector> connectors = new ConcurrentHashMap<>();
	private int maxConnections = 1024; // 适合绝大多数网络服务，对于连接机，比如Linkd，Gated等需要自己加大。

	public Service getService() {
		return service;
	}

	public @NotNull String getName() {
		return name;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	public @NotNull SocketOptions getSocketOptions() {
		return socketOptions;
	}

	public void setSocketOptions(@NotNull SocketOptions value) {
		//noinspection ConstantValue
		if (value != null)
			socketOptions = value;
	}

	public @NotNull HandshakeOptions getHandshakeOptions() {
		return handshakeOptions;
	}

	public void setHandshakeOptions(@NotNull HandshakeOptions value) {
		//noinspection ConstantValue
		if (value != null)
			handshakeOptions = value;
	}

	public void setService(Service service) {
		lock();
		try {
			if (this.service != null) {
				throw new IllegalStateException(String.format("ServiceConf of '%s' Service != null", getName()));
			}
			this.service = service;
			forEachAcceptor(a -> a.SetService(service));
			forEachConnector(c -> c.SetService(service));
		} finally {
			unlock();
		}
	}

	public void addConnector(@NotNull Connector connector) {
		if (null != connectors.putIfAbsent(connector.getName(), connector)) {
			throw new IllegalStateException("Duplicate Connector=" + connector.getName());
		}
		connector.SetService(service);
	}

	public @Nullable Connector findConnector(@NotNull String name) {
		return connectors.get(name);
	}

	public @Nullable Connector findConnector(@NotNull String host, int port) {
		return findConnector(host + "_" + port);
	}

	/**
	 * 查找，不存在则创建。
	 *
	 * @param host          peer address or name
	 * @param port          peer port
	 * @param autoReconnect auto reconnect when socket close.
	 * @param getOrAdd      out. connector returned.
	 * @return true if addNew
	 */
	public boolean tryGetOrAddConnector(@NotNull String host, int port, boolean autoReconnect,
										@Nullable OutObject<Connector> getOrAdd) {
		return tryGetOrAddConnector(host, port, autoReconnect, getOrAdd, Connector::new);
	}

	public boolean tryGetOrAddConnector(@NotNull String host, int port, boolean autoReconnect,
										@Nullable OutObject<Connector> getOrAdd,
										Func3<String, Integer, Boolean, Connector> cf) {
		var name = host + "_" + port;
		final var addNew = new OutObject<Connector>();
		var c = connectors.computeIfAbsent(name, __ -> {
			Connector add;
			try {
				add = cf.call(host, port, autoReconnect);
			} catch (Exception e) {
				Task.forceThrow(e);
				return null; // never run here
			}
			add.SetService(service);
			addNew.value = add;
			return add;
		});
		if (getOrAdd != null)
			getOrAdd.value = c;
		return addNew.value != null;
	}

	public void removeConnector(@NotNull Connector c) {
		connectors.remove(c.getName(), c);
	}

	public void ForEachConnector(@NotNull Action1<Connector> action) throws Exception {
		for (var c : connectors.values()) {
			action.run(c);
		}
	}

	public void forEachConnector(@NotNull Consumer<Connector> action) {
		for (var a : connectors.values())
			action.accept(a);
	}

	public int connectorCount() {
		return connectors.size();
	}

	public boolean forEachConnector2(@NotNull Predicate<Connector> func) {
		for (var c : connectors.values()) {
			if (!func.test(c))
				return false;
		}
		return true;
	}

	public void addAcceptor(@NotNull Acceptor a) {
		if (null != acceptors.putIfAbsent(a.getName(), a)) {
			throw new IllegalStateException("Duplicate Acceptor=" + a.getName());
		}
		a.SetService(service);
	}

	public void removeAcceptor(@NotNull Acceptor a) {
		acceptors.remove(a.getName(), a);
	}

	public void ForEachAcceptor(@NotNull Action1<Acceptor> action) throws Exception {
		for (var a : acceptors.values())
			action.run(a);
	}

	public void forEachAcceptor(@NotNull Consumer<Acceptor> action) {
		for (var a : acceptors.values())
			action.accept(a);
	}

	public boolean forEachAcceptor2(@NotNull Function<Acceptor, Boolean> func) {
		for (var a : acceptors.values()) {
			if (!func.apply(a))
				return false;
		}
		return true;
	}

	public int acceptorCount() {
		return acceptors.size();
	}

	public ServiceConf() {
		name = "";
	}

	public ServiceConf(@NotNull Config conf, @NotNull Element self) {
		name = self.getAttribute("Name").trim();

		String attr;

		// SocketOptions
		attr = self.getAttribute("NoDelay");
		if (!attr.isBlank())
			getSocketOptions().setNoDelay(Boolean.parseBoolean(attr));

		attr = self.getAttribute("SendBuffer");
		if (!attr.isBlank())
			getSocketOptions().setSendBuffer(Str.parseIntSize(attr));

		attr = self.getAttribute("ReceiveBuffer");
		if (!attr.isBlank())
			getSocketOptions().setReceiveBuffer(Str.parseIntSize(attr));

		attr = self.getAttribute("InputBufferMaxProtocolSize");
		if (!attr.isBlank())
			getSocketOptions().setInputBufferMaxProtocolSize(Str.parseIntSize(attr));

		attr = self.getAttribute("OutputBufferMaxSize");
		if (!attr.isBlank())
			getSocketOptions().setOutputBufferMaxSize(Str.parseLongSize(attr));

		attr = self.getAttribute("Backlog");
		if (!attr.isBlank())
			getSocketOptions().setBacklog(Integer.parseInt(attr));

		getSocketOptions().setTimeThrottle(self.getAttribute("TimeThrottle"));

		attr = self.getAttribute("TimeThrottleSeconds");
		if (!attr.isBlank())
			getSocketOptions().setTimeThrottleSeconds(Integer.parseInt(attr));

		attr = self.getAttribute("TimeThrottleLimit");
		if (!attr.isBlank())
			getSocketOptions().setTimeThrottleLimit(Integer.parseInt(attr));

		attr = self.getAttribute("TimeThrottleBandwidth");
		if (!attr.isBlank())
			getSocketOptions().setTimeThrottleBandwidth(Str.parseIntSize(attr));

		attr = self.getAttribute("OverBandwidth");
		if (!attr.isBlank())
			getSocketOptions().setOverBandwidth(Str.parseLongSize(attr));

		attr = self.getAttribute("OverBandwidthFusingRate");
		if (!attr.isBlank())
			getSocketOptions().setOverBandwidthFusingRate(Double.parseDouble(attr));
		attr = self.getAttribute("OverBandwidthNormalRate");
		if (!attr.isBlank())
			getSocketOptions().setOverBandwidthNormalRate(Double.parseDouble(attr));

		attr = self.getAttribute("CloseWhenMissHandle");
		if (!attr.isBlank())
			getSocketOptions().setCloseWhenMissHandle(Boolean.parseBoolean(attr));

		// HandshakeOptions
		attr = self.getAttribute("DhGroups");
		if (!attr.isBlank()) {
			getHandshakeOptions().setDhGroups(new IntHashSet());
			for (String dg : attr.split(",", -1)) {
				String dgTmp = dg.strip();
				if (!dgTmp.isEmpty())
					getHandshakeOptions().addDhGroup(Integer.parseInt(dgTmp));
			}
		}
		attr = self.getAttribute("SecureIp");
		if (!attr.isBlank()) {
			try {
				getHandshakeOptions().setSecureIp(InetAddress.getByName(attr).getAddress());
			} catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}
		attr = self.getAttribute("CompressS2c");
		if (!attr.isBlank())
			getHandshakeOptions().setCompressS2c(Integer.parseInt(attr));

		attr = self.getAttribute("CompressC2s");
		if (!attr.isBlank())
			getHandshakeOptions().setCompressC2s(Integer.parseInt(attr));

		attr = self.getAttribute("EncryptType");
		if (!attr.isBlank())
			getHandshakeOptions().setEncryptType(Integer.parseInt(attr));

		attr = self.getAttribute("maxConnections");
		if (!attr.isBlank())
			maxConnections = Integer.parseInt(attr);

		attr = self.getAttribute("KeepCheckPeriod");
		if (!attr.isBlank())
			getHandshakeOptions().setKeepCheckPeriod(Integer.parseInt(attr));

		attr = self.getAttribute("KeepRecvTimeout");
		if (!attr.isBlank())
			getHandshakeOptions().setKeepRecvTimeout(Integer.parseInt(attr));

		attr = self.getAttribute("KeepSendTimeout");
		if (!attr.isBlank())
			getHandshakeOptions().setKeepSendTimeout(Integer.parseInt(attr));

		{
			String name = getName();
			if (name.isBlank()) {
				conf.setDefaultServiceConf(this);
			} else if (null != conf.getServiceConfMap().putIfAbsent(name, this)) {
				throw new IllegalStateException("Duplicate ServiceConf " + getName());
			}
		}

		// connection creator options
		NodeList childNodes = self.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); ++i) {
			Node node = childNodes.item(i);
			if (Node.ELEMENT_NODE != node.getNodeType()) {
				continue;
			}

			Element e = (Element)node;
			switch (e.getNodeName()) {
			case "Acceptor":
				addAcceptor(new Acceptor(e));
				break;
			case "Connector":
				addConnector(Connector.Create(e));
				break;
			default:
				throw new IllegalStateException("unknown node name: " + e.getNodeName());
			}
		}
	}

	public void start() {
		forEachAcceptor(Acceptor::Start);
		forEachConnector(Connector::start);
	}

	public void stop() {
		forEachAcceptor(Acceptor::Stop);
		forEachConnector(Connector::stop);
	}

	public void stopListen() {
		forEachAcceptor(Acceptor::Stop);
	}
}
