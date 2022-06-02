package Zeze.Net;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
	private final String HostNameOrAddress;
	private final int Port;
	private Service Service;
	private AsyncSocket Socket;
	private volatile TaskCompletionSource<AsyncSocket> FutureSocket = new TaskCompletionSource<>();

	public volatile Object UserState;

	private boolean IsAutoReconnect;
	private boolean IsConnected;
	private Future<?> ReconnectTask;
	private int MaxReconnectDelay = 8000; // 毫秒
	private int ReConnectDelay;

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
		HostNameOrAddress = host;
		Port = port;
		IsAutoReconnect = autoReconnect;
	}

	public Connector(Element self) {
		HostNameOrAddress = self.getAttribute("HostNameOrAddress");
		Port = Integer.parseInt(self.getAttribute("Port"));
		String attr = self.getAttribute("IsAutoReconnect");
		IsAutoReconnect = !attr.isEmpty() && Boolean.parseBoolean(attr);
		if (!attr.isEmpty())
			setMaxReconnectDelay(Integer.parseInt(attr) * 1000);
	}

	public final int getMaxReconnectDelay() {
		return MaxReconnectDelay;
	}

	public final void setMaxReconnectDelay(int value) {
		MaxReconnectDelay = Math.max(value, 1000);
	}

	public final String getHostNameOrAddress() {
		return HostNameOrAddress;
	}

	public final int getPort() {
		return Port;
	}

	public final String getName() {
		return HostNameOrAddress + ':' + Port;
	}

	public final Service getService() {
		return Service;
	}

	public final AsyncSocket getSocket() {
		return Socket;
	}

	public final boolean isAutoReconnect() {
		return IsAutoReconnect;
	}

	public final void setAutoReconnect(boolean value) {
		IsAutoReconnect = value;
		if (IsAutoReconnect) {
			TryReconnect();
		} else {
			synchronized (this) {
				if (ReconnectTask != null) {
					ReconnectTask.cancel(false);
					ReconnectTask = null;
				}
			}
		}
	}

	public final boolean isConnected() {
		return IsConnected;
	}

	public final boolean isHandshakeDone() {
		return TryGetReadySocket() != null;
	}

	public final synchronized void SetService(Service service) {
		if (Service != null)
			throw new IllegalStateException("Connector of '" + getName() + "' Service != null");
		Service = service;
	}

	// 允许子类重新定义Ready.
	public AsyncSocket WaitReady() {
		return GetReadySocket();
	}

	public final AsyncSocket GetReadySocket() {
		try {
			return FutureSocket.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	public final AsyncSocket TryGetReadySocket() {
		try {
			return FutureSocket.getNow();
		} catch (ExecutionException e) {
			return null;
		}
	}

	public synchronized void OnSocketClose(AsyncSocket closed, Throwable e) throws Throwable {
		if (Socket == closed) {
			Stop(e);
			TryReconnect();
		}
	}

	public synchronized void OnSocketConnected(@SuppressWarnings("unused") AsyncSocket so) {
		IsConnected = true;
		ReConnectDelay = 0;
	}

	public synchronized void TryReconnect() {
		if (!IsAutoReconnect || Socket != null || ReconnectTask != null)
			return;

		ReConnectDelay = ReConnectDelay > 0 ? Math.min(ReConnectDelay * 2, MaxReconnectDelay) : 1000;
		ReconnectTask = Zeze.Util.Task.schedule(ReConnectDelay, this::Start);
	}

	// 需要逻辑相关的握手行为时，重载这个方式。
	public void OnSocketHandshakeDone(AsyncSocket so) {
		synchronized (this) {
			if (Socket == so) {
				// java 没有TrySetResult，所以如果上面的检查不充分，仍然会有问题。
				FutureSocket.SetResult(so);
				return;
			}
		}
		so.Close(new Exception("not owner?"));
	}

	public synchronized void Start() {
		// always try cancel reconnect task
		if (ReconnectTask != null) {
			ReconnectTask.cancel(false);
			ReconnectTask = null;
		}
		if (Socket == null)
			Socket = Service.NewClientSocket(HostNameOrAddress, Port, UserState, this);
	}

	public void Stop() {
		Stop(null);
	}

	public void Stop(Throwable e) {
		AsyncSocket as;
		synchronized (this) {
			// always try cancel reconnect task
			if (ReconnectTask != null) {
				ReconnectTask.cancel(false);
				ReconnectTask = null;
			}
			if (Socket == null)
				return; // not start or has stopped.
			FutureSocket.SetException(e != null ? e : new Exception("Connector Stopped: " + getName())); // try set
			FutureSocket = new TaskCompletionSource<>(); // prepare future to next connect.
			IsConnected = false;
			as = Socket;
			Socket = null; // 阻止递归。
		}
		as.close();
	}
}
