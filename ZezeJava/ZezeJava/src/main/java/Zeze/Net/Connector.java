package Zeze.Net;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import Zeze.Util.TaskCompletionSource;
import org.w3c.dom.Element;

/**
 * 连接器：建立并保持一个连接，可以设置自动重连及相关参数。
 * 可以继承并重载相关事件函数。重载实现里面需要调用 base.OnXXX。
 * 继承是为了给链接扩充状态，比如：应用的连接需要login，可以维护额外的状态。
 * 继承类启用方式：
 * 1. 在配置中通过 class="FullClassName" 的。
 * 2. 动态创建并加入Service
 */
public class Connector {
	private static final int READY_TIMEOUT = 5000;

	private final String hostNameOrAddress;
	private final int port;
	private final String name;
	private Service service;
	private AsyncSocket socket;
	private volatile TaskCompletionSource<AsyncSocket> futureSocket = new TaskCompletionSource<>();

	public volatile Object userState;

	private boolean isAutoReconnect;
	private boolean isConnected;
	private Future<?> reconnectTask;
	private int maxReconnectDelay = 8000; // 毫秒
	private int reConnectDelay;

	public static Connector Create(Element e) {
		String className = e.getAttribute("Class");
		if (className.isEmpty())
			return new Connector(e);
		try {
			Class<?> ccls = Class.forName(className);
			return (Connector)ccls.getConstructor(Element.class).newInstance(e);
		} catch (Throwable ex) {
			throw new RuntimeException(ex);
		}
	}

	public Connector(String host, int port) {
		this(host, port, true);
	}

	public Connector(String host, int port, boolean autoReconnect) {
		hostNameOrAddress = host;
		this.port = port;
		name = host + ':' + port;
		isAutoReconnect = autoReconnect;
	}

	public Connector(Element self) {
		hostNameOrAddress = self.getAttribute("HostNameOrAddress");
		port = Integer.parseInt(self.getAttribute("Port"));
		name = hostNameOrAddress + ':' + port;
		String attr = self.getAttribute("IsAutoReconnect");
		isAutoReconnect = !attr.isEmpty() && Boolean.parseBoolean(attr);
		if (!attr.isEmpty())
			setMaxReconnectDelay(Integer.parseInt(attr) * 1000);
	}

	public final int getMaxReconnectDelay() {
		return maxReconnectDelay;
	}

	public final void setMaxReconnectDelay(int value) {
		maxReconnectDelay = Math.max(value, 1000);
	}

	public final String getHostNameOrAddress() {
		return hostNameOrAddress;
	}

	public final int getPort() {
		return port;
	}

	public final String getName() {
		return name;
	}

	public final Service getService() {
		return service;
	}

	public final AsyncSocket getSocket() {
		return socket;
	}

	public final boolean isAutoReconnect() {
		return isAutoReconnect;
	}

	public final void setAutoReconnect(boolean value) {
		isAutoReconnect = value;
		if (isAutoReconnect) {
			TryReconnect();
		} else {
			synchronized (this) {
				if (reconnectTask != null) {
					reconnectTask.cancel(false);
					reconnectTask = null;
				}
			}
		}
	}

	public final boolean isConnected() {
		return isConnected;
	}

	public final boolean isHandshakeDone() {
		return TryGetReadySocket() != null;
	}

	public final synchronized void SetService(Service service) {
		if (this.service != null)
			throw new IllegalStateException("Connector of '" + getName() + "' Service != null");
		this.service = service;
	}

	// 允许子类重新定义Ready.
	public AsyncSocket WaitReady() {
		return GetReadySocket();
	}

	public final AsyncSocket GetReadySocket() {
		try {
			return futureSocket.get(READY_TIMEOUT, TimeUnit.MILLISECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new RuntimeException(e);
		}
	}

	public final AsyncSocket TryGetReadySocket() {
		try {
			return futureSocket.getNow();
		} catch (ExecutionException e) {
			return null;
		}
	}

	public synchronized void OnSocketClose(AsyncSocket closed, Throwable e) throws Throwable {
		if (socket == closed) {
			Stop(e);
			TryReconnect();
		}
	}

	public synchronized void OnSocketConnected(@SuppressWarnings("unused") AsyncSocket so) {
		isConnected = true;
		reConnectDelay = 0;
	}

	public synchronized void TryReconnect() {
		if (!isAutoReconnect || socket != null || reconnectTask != null)
			return;

		reConnectDelay = reConnectDelay > 0 ? Math.min(reConnectDelay * 2, maxReconnectDelay) : 1000;
		reconnectTask = Zeze.Util.Task.scheduleUnsafe(reConnectDelay, this::Start);
	}

	// 需要逻辑相关的握手行为时，重载这个方式。
	public void OnSocketHandshakeDone(AsyncSocket so) {
		synchronized (this) {
			if (socket == so) {
				// java 没有TrySetResult，所以如果上面的检查不充分，仍然会有问题。
				futureSocket.SetResult(so);
				return;
			}
		}
		so.close(new Exception("not owner?"));
	}

	public synchronized void Start() {
		// always try cancel reconnect task
		if (reconnectTask != null) {
			reconnectTask.cancel(false);
			reconnectTask = null;
		}
		if (socket == null)
			socket = service.newClientSocket(hostNameOrAddress, port, userState, this);
	}

	public void Stop() {
		Stop(null);
	}

	public void Stop(Throwable e) {
		AsyncSocket as;
		synchronized (this) {
			// always try cancel reconnect task
			if (reconnectTask != null) {
				reconnectTask.cancel(false);
				reconnectTask = null;
			}
			if (socket == null)
				return; // not start or has stopped.
			futureSocket.SetException(e != null ? e : new Exception("Connector Stopped: " + getName())); // try set
			futureSocket = new TaskCompletionSource<>(); // prepare future to next connect.
			isConnected = false;
			as = socket;
			socket = null; // 阻止递归。
		}
		as.close();
	}
}
