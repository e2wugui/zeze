package Zeze.Net;

import org.w3c.dom.Element;
import Zeze.Util.TaskCompletionSource;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 连接器：建立并保持一个连接，可以设置自动重连及相关参数。
 可以继承并重载相关事件函数。重载实现里面需要调用 base.OnXXX。
 继承是为了给链接扩充状态，比如：应用的连接需要login，可以维护额外的状态。
 继承类启用方式：
 1. 在配置中通过 class="FullClassName" 的。
 2. 动态创建并加入Service
*/
public class Connector {
	private Service Service;
	public final Service getService() {
		return Service;
	}

	private final String HostNameOrAddress;
	public final String getHostNameOrAddress() {
		return HostNameOrAddress;
	}

	private int Port = 0;
	public final int getPort() {
		return Port;
	}

	private boolean IsAutoReconnect = true;
	public final boolean isAutoReconnect() {
		return IsAutoReconnect;
	}
	public final void setAutoReconnect(boolean value) {
		IsAutoReconnect = value;
	}

	private int MaxReconnectDelay = 8000;
	public final int getMaxReconnectDelay() {
		return MaxReconnectDelay;
	}
	public final void setMaxReconnectDelay(int value) {
		MaxReconnectDelay = value;
		if (MaxReconnectDelay < 8000) {
			MaxReconnectDelay = 8000;
		}
	}

	private boolean IsConnected = false;
	public final boolean isConnected() {
		return IsConnected;
	}

	private int ConnectDelay;

	public final boolean isHandshakeDone() {
		return null != TryGetReadySocket();
	}

	public final String getName() {
		return getHostNameOrAddress() + ":" + getPort();
	}

	private volatile TaskCompletionSource<AsyncSocket> FutureSocket = new TaskCompletionSource<>();
	private AsyncSocket Socket;
	public final AsyncSocket getSocket() {
		return Socket;
	}

	private Zeze.Util.Task ReconnectTask;

	public volatile Object UserState;

	public Connector(String host, int port) {
		this(host, port, true);
	}

	public Connector(String host) {
		this(host, 0, true);
	}

	public Connector(String host, int port, boolean autoReconnect) {
		HostNameOrAddress = host;
		Port = port;
		setAutoReconnect(autoReconnect);
	}

	public static Connector Create(Element e) {
		var className = e.getAttribute("Class");
		if (className.isEmpty())
			return new Connector(e);
		try {
			Class<?> ccls = java.lang.Class.forName(className);
			return (Connector)ccls.getConstructor(Element.class).newInstance(e);
		}
		catch (Throwable ex) {
			throw new RuntimeException(ex);
		}
	}

	public Connector(Element self) {
		String attr = self.getAttribute("Port");
		if (attr.length() > 0) {
			Port = Integer.parseInt(attr);
		}
		HostNameOrAddress = self.getAttribute("HostNameOrAddress");
		attr = self.getAttribute("IsAutoReconnect");
		if (attr.length() > 0) {
			setAutoReconnect(Boolean.parseBoolean(attr));
		}
		attr = self.getAttribute("MaxReconnectDelay");
		if (attr.length() > 0) {
			setMaxReconnectDelay(Integer.parseInt(attr) * 1000);
		}
	}

	public final void SetService(Service service) {
		synchronized (this) {
			if (getService() != null) {
				throw new RuntimeException("Connector of '" + getName() + "' Service != null");
			}
			Service = service;
		}
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
			return FutureSocket.get(0, TimeUnit.MILLISECONDS);
		} catch (TimeoutException | InterruptedException | ExecutionException e) {
			return null;
		}
	}

	public void OnSocketClose(AsyncSocket closed, Throwable e) throws Throwable {
		synchronized (this) {
			if (Socket != closed) {
				return;
			}
			Stop(e);
			TryReconnect();
		}
	}

	public void OnSocketConnected(@SuppressWarnings("unused") AsyncSocket so) {
		synchronized (this) {
			ConnectDelay = 0;
			IsConnected = true;
		}
	}

	// 需要逻辑相关的握手行为时，重载这个方式。
	public void OnSocketHandshakeDone(AsyncSocket so) {
		FutureSocket.SetResult(so);
	}

	public void TryReconnect() {
		synchronized (this) {
			if (!IsAutoReconnect || null != Socket || null != ReconnectTask) {
				return;
			}

			if (ConnectDelay <= 0) {
				ConnectDelay = 1000;
			}
			else {
				ConnectDelay *= 2;
				if (ConnectDelay > MaxReconnectDelay) {
					ConnectDelay = MaxReconnectDelay;
				}
			}
			ReconnectTask = Zeze.Util.Task.schedule((ThisTask) -> Start(), ConnectDelay);
		}
	}

	public void Start() throws Throwable {
		synchronized (this) {
			// always try cancel reconnect task
			if (ReconnectTask != null) {
				ReconnectTask.Cancel();
			}
			ReconnectTask = null;

			if (null != Socket) {
				return;
			}
			Socket = Service.NewClientSocket(HostNameOrAddress, Port, UserState,this);
		}
	}

	public void Stop() {
		Stop(null);
	}

	public void Stop(Throwable e) {
		AsyncSocket tmp;
		synchronized (this) {
			if (null == Socket) {
				// not start or has stopped.
				return;
			}
			FutureSocket.SetException(null != e ? e : new Exception("Connector Stopped: " + getName())); // try set
			FutureSocket = new TaskCompletionSource<>(); // prepare future to next connect.
			IsConnected = false;
			tmp = Socket;
			Socket = null; // 阻止递归。
		}
		tmp.close();
	}
}
