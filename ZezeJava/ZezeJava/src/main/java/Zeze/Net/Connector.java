package Zeze.Net;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

	private final @NotNull String hostNameOrAddress;
	private final int port;
	private final @NotNull String name;
	private Service service;
	private AsyncSocket socket;
	private volatile @NotNull TaskCompletionSource<AsyncSocket> futureSocket = new TaskCompletionSource<>();

	public volatile @Nullable Object userState;

	private boolean isAutoReconnect;
	private boolean isConnected;
	private @Nullable Future<?> reconnectTask;
	private int maxReconnectDelay = 8000; // 毫秒
	private int reConnectDelay;

	public static @NotNull Connector Create(@NotNull Element e) {
		String className = e.getAttribute("Class");
		if (className.isEmpty())
			return new Connector(e);
		try {
			Class<?> ccls = Class.forName(className);
			return (Connector)ccls.getConstructor(Element.class).newInstance(e);
		} catch (Exception ex) {
			Task.forceThrow(ex);
			return null; // never run here
		}
	}

	public Connector(@NotNull String host, int port) {
		this(host, port, true);
	}

	public Connector(@NotNull String host, int port, boolean autoReconnect) {
		hostNameOrAddress = host;
		this.port = port;
		name = host + ':' + port;
		isAutoReconnect = autoReconnect;
	}

	public Connector(@NotNull Element self) {
		hostNameOrAddress = self.getAttribute("HostNameOrAddress");
		port = Integer.parseInt(self.getAttribute("Port"));
		name = hostNameOrAddress + ':' + port;
		String attr = self.getAttribute("IsAutoReconnect");
		isAutoReconnect = !attr.isEmpty() && Boolean.parseBoolean(attr);
		attr = self.getAttribute("MaxReconnectDelay");
		if (!attr.isEmpty())
			setMaxReconnectDelay(Integer.parseInt(attr) * 1000);
	}

	public final int getMaxReconnectDelay() {
		return maxReconnectDelay;
	}

	public final void setMaxReconnectDelay(int value) {
		maxReconnectDelay = Math.max(value, 1000);
	}

	public final @NotNull String getHostNameOrAddress() {
		return hostNameOrAddress;
	}

	public final int getPort() {
		return port;
	}

	public @NotNull String getName() {
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

	public final synchronized void SetService(@NotNull Service service) {
		if (this.service != null)
			throw new IllegalStateException("Connector of '" + getName() + "' Service != null");
		this.service = service;
	}

	// 允许子类重新定义Ready.
	public @NotNull AsyncSocket WaitReady() {
		return GetReadySocket();
	}

	public final @NotNull AsyncSocket GetReadySocket() {
		try {
			return futureSocket.get(READY_TIMEOUT, TimeUnit.MILLISECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new IllegalStateException(getName(), e);
		}
	}

	public final @Nullable AsyncSocket TryGetReadySocket() {
		try {
			return futureSocket.getNow();
		} catch (ExecutionException e) {
			return null;
		}
	}

	public synchronized void OnSocketClose(@NotNull AsyncSocket closed, @Nullable Throwable e) throws Exception {
		if (socket == closed) {
			stop(e);
			TryReconnect();
		}
	}

	public synchronized void OnSocketConnected(@SuppressWarnings("unused") @NotNull AsyncSocket so) {
		isConnected = true;
		reConnectDelay = 0;
	}

	public synchronized void TryReconnect() {
		if (!isAutoReconnect || socket != null || reconnectTask != null)
			return;

		reConnectDelay = reConnectDelay > 0 ? Math.min(reConnectDelay * 2, maxReconnectDelay) : 1000;
		reconnectTask = Task.scheduleUnsafe(reConnectDelay, this::start);
	}

	// 需要逻辑相关的握手行为时，重载这个方法。
	public void OnSocketHandshakeDone(@NotNull AsyncSocket so) {
		synchronized (this) {
			if (socket == so) {
				// java 没有TrySetResult，所以如果上面的检查不充分，仍然会有问题。
				futureSocket.setResult(so);
				return;
			}
		}
		so.close(new Exception("not owner?"));
	}

	public synchronized void start() {
		// always try cancel reconnect task
		if (reconnectTask != null) {
			reconnectTask.cancel(false);
			reconnectTask = null;
		}
		if (socket == null)
			socket = service.newClientSocket(hostNameOrAddress, port, userState, this);
	}

	public void stop() {
		stop(null);
	}

	public void stop(@Nullable Throwable e) {
		AsyncSocket as;
		synchronized (this) {
			// always try cancel reconnect task
			if (reconnectTask != null) {
				reconnectTask.cancel(false);
				reconnectTask = null;
			}
			if (socket == null)
				return; // not start or has stopped.
			futureSocket.setException(e != null ? e : new Exception("Connector Stopped: " + getName())); // try set
			futureSocket = new TaskCompletionSource<>(); // prepare future to next connect.
			isConnected = false;
			as = socket;
			socket = null; // 阻止递归。
		}
		as.close();
	}
}
