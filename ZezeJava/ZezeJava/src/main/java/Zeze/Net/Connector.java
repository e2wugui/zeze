package Zeze.Net;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskSpec;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * 连接器：建立并保持一个连接，可以设置自动重连及相关参数。
 * 可以继承并重载相关事件函数。重载实现里面需要调用 base.OnXXX。
 * 继承是为了给链接扩充状态，比如：应用的连接需要login，可以维护额外的状态。
 * 继承类启用方式：
 * 1. 在配置中通过 class="FullClassName" 的。
 * 2. 动态创建并加入Service
 */
public class Connector extends ReentrantLock {
	private static final int READY_TIMEOUT = 5000;

	private final String hostNameOrAddress;
	private final int port;
	private final @Nullable String url;
	private final String name;
	private @Nullable Service service;
	private volatile @Nullable AsyncSocket socket; // getSocket()无锁读，需要可见性保证
	private volatile TaskCompletionSource<AsyncSocket> futureSocket = new TaskCompletionSource<>();

	public volatile @Nullable Object userState;

	private volatile boolean isAutoReconnect; // isAutoReconnect()无锁读，需要可见性保证
	private volatile boolean isConnected; // isConnected()无锁读，需要可见性保证
	private @Nullable Future<?> reconnectTask;
	// 仅锁内读写：start()/stop()/关闭自动重连时递增，使已派发未执行的旧重连任务失效。
	private long epoch;
	private int maxReconnectDelay = 8000; // 毫秒
	private int reConnectDelay;
	private long brokenTime = System.currentTimeMillis();

	public static Connector Create(Element e) {
		String className = e.getAttribute("Class");
		if (className.isEmpty())
			return new Connector(e);
		try {
			Class<?> cls = Class.forName(className);
			return (Connector)cls.getConstructor(Element.class).newInstance(e);
		} catch (Exception ex) {
			throw Task.forceThrow(ex);
		}
	}

	public Connector(String hostAndPort, boolean autoReconnect) {
		var ipp = hostAndPort.split("_");
		this.hostNameOrAddress = ipp[0];
		this.port = Integer.parseInt(ipp[1]);
		this.url = null;
		this.name = this.hostNameOrAddress + "_" + this.port;
		this.isAutoReconnect = autoReconnect;
	}

	public Connector(String host, int port) {
		this(host, port, true);
	}

	public Connector(boolean autoReconnect, String url) {
		if (url.isBlank())
			throw new RuntimeException("url is blank.");
		this.hostNameOrAddress = "";
		this.port = 0;
		this.url = url;
		this.name = url;
		this.isAutoReconnect = autoReconnect;
	}

	public Connector(String host, int port, boolean autoReconnect) {
		this.hostNameOrAddress = host;
		this.port = port;
		this.url = null;
		this.name = host + '_' + port;
		this.isAutoReconnect = autoReconnect;
	}

	public Connector(Element self) {
		hostNameOrAddress = self.getAttribute("HostNameOrAddress");
		this.url = self.getAttribute("Url");
		if (url.isBlank()) {
			port = Integer.parseInt(self.getAttribute("Port"));
			name = hostNameOrAddress + '_' + port;
		} else {
			port = 0;
			name = url;
		}
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

	public final String getHostNameOrAddress() {
		return hostNameOrAddress;
	}

	public final int getPort() {
		return port;
	}

	public final @Nullable String getUrl() {
		return url;
	}

	public String getName() {
		return name;
	}

	public final @Nullable Service getService() {
		return service;
	}

	public final @Nullable AsyncSocket getSocket() {
		return socket;
	}

	public final boolean isAutoReconnect() {
		return isAutoReconnect;
	}

	public final void setAutoReconnect(boolean value) {
		lock();
		try {
			isAutoReconnect = value;
			if (value)
				tryReconnect();
			else {
				epoch++; // 使在途重连任务失效
				if (reconnectTask != null) {
					reconnectTask.cancel(false);
					reconnectTask = null;
				}
			}
		} finally {
			unlock();
		}
	}

	public final boolean isConnected() {
		return isConnected;
	}

	public final boolean isHandshakeDone() {
		return TryGetReadySocket() != null;
	}

	public final void SetService(@Nullable Service service) {
		lock();
		try {
			if (this.service != null)
				throw new IllegalStateException("Connector of '" + getName() + "' Service != null");
			this.service = service;
		} finally {
			unlock();
		}
	}

	// 允许子类重新定义Ready.
	public AsyncSocket WaitReady() {
		return GetReadySocket();
	}

	public final AsyncSocket GetReadySocket() {
		return futureSocket.getNonNull(READY_TIMEOUT, TimeUnit.MILLISECONDS);
	}

	public final @Nullable AsyncSocket TryGetReadySocket() {
		try {
			return futureSocket.getNow();
		} catch (Exception e) {
			return null;
		}
	}

	public boolean checkBrokenTimeout(long timeout) {
		lock();
		try {
			return !isConnected && (System.currentTimeMillis() - brokenTime > timeout);
		} finally {
			unlock();
		}

	}
	/** 仅由已置死的 AsyncSocket 在 doClose 中调用，确保经 stop(e) 重入 close 时为 no-op，维持单向锁序。 */
	public void OnSocketClose(AsyncSocket closed, @Nullable Throwable e) throws Exception {
		lock();
		try {
			if (socket == closed) {
				brokenTime = System.currentTimeMillis();
				stop(e);
				tryReconnect();
			}
		} finally {
			unlock();
		}
	}

	public void OnSocketConnected(AsyncSocket so) {
		lock();
		try {
			if (socket != so)
				return;
			isConnected = true;
			reConnectDelay = 0;
		} finally {
			unlock();
		}
	}

	private void tryReconnect() {
		lock();
		try {
			if (!isAutoReconnect || socket != null || reconnectTask != null)
				return;
			reConnectDelay = reConnectDelay > 0 ? Math.min(reConnectDelay * 2, maxReconnectDelay) : 1000;
			final long gen = epoch;
			reconnectTask = TaskSpec.ofAction(() -> startIfFresh(gen)).scheduleNow(reConnectDelay);
		} finally {
			unlock();
		}
	}

	// 需要逻辑相关的握手行为时，重载这个方法。
	public void OnSocketHandshakeDone(AsyncSocket so) {
		lock();
		try {
			if (socket == so) {
				// java 没有TrySetResult，所以如果上面的检查不充分，仍然会有问题。
				futureSocket.setResult(so);
				return;
			}
		} finally {
			unlock();
		}
		so.close(new Exception("not owner?"));
	}

	public void start() {
		lock();
		try {
			if (service == null)
				throw new IllegalStateException("Connector '" + getName() + "' start() before SetService()");
			epoch++;
			startInternal();
		} finally {
			unlock();
		}
	}

	// 仅当排程代数未过期时才发起重连。
	private void startIfFresh(long gen) {
		lock();
		try {
			if (gen != epoch || socket != null)
				return;
			startInternal();
		} finally {
			unlock();
		}
	}

	private void startInternal() {
		// always try cancel reconnect task
		if (reconnectTask != null) {
			reconnectTask.cancel(false);
			reconnectTask = null;
		}
		if (socket != null)
			return;
		try {
			// socket 构造非阻塞（DNS/connect 异步执行），锁内同步发布以便 stop() 可立即关闭。
			//noinspection DataFlowIssue
			socket = null == url || url.isBlank()
					? getService().newClientSocket(hostNameOrAddress, port, userState, this)
					: getService().newWebsocketClient(url, userState, this);
		} catch (Exception e) {
			tryReconnect();
			throw e;
		}
	}

	public void stop() {
		stop(null);
	}

	public void stop(@Nullable Throwable e) {
		AsyncSocket as;
		lock();
		try {
			epoch++;
			// always try cancel reconnect task
			if (reconnectTask != null) {
				reconnectTask.cancel(false);
				reconnectTask = null;
			}
			if (socket == null)
				return; // not start or has stopped.
			if (e == null)
				e = new IOException("Connector Stopped: " + getName());
			futureSocket.setException(e); // try set
			futureSocket = new TaskCompletionSource<>(); // prepare future to next connect.
			isConnected = false;
			as = socket;
			socket = null; // 阻止递归。
		} finally {
			unlock();
		}
		//noinspection DataFlowIssue
		as.close(e);
	}
}
