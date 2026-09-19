package Zeze.Net;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskSpec;
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
public class Connector extends ReentrantLock {
	private static final int READY_TIMEOUT = 5000;

	private final @NotNull String hostNameOrAddress;
	private final int port;
	private final @Nullable String url;
	private final @NotNull String name;
	private Service service;
	private volatile AsyncSocket socket; // getSocket()无锁读，需要可见性保证
	private volatile @NotNull TaskCompletionSource<AsyncSocket> futureSocket = new TaskCompletionSource<>();

	public volatile @Nullable Object userState;

	private volatile boolean isAutoReconnect; // isAutoReconnect()无锁读，需要可见性保证
	private volatile boolean isConnected; // isConnected()无锁读，需要可见性保证
	private @Nullable Future<?> reconnectTask;
	// 意图代数（仅锁内读写）：start()/stop()各开新纪元，排程任务携带排程时代数、醒来不匹配
	// 即弃——作废stop前已派发、cancel(false)撤不回的在途重连。
	private long epoch;
	private int maxReconnectDelay = 8000; // 毫秒
	private int reConnectDelay;

	public static @NotNull Connector Create(@NotNull Element e) {
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

	public Connector(@NotNull String hostAndPort, boolean autoReconnect) {
		var ipp = hostAndPort.split("_");
		this.hostNameOrAddress = ipp[0];
		this.port = Integer.parseInt(ipp[1]);
		this.url = null;
		this.name = this.hostNameOrAddress + "_" + this.port;
		this.isAutoReconnect = autoReconnect;
	}

	public Connector(@NotNull String host, int port) {
		this(host, port, true);
	}

	public Connector(boolean autoReconnect, @NotNull String url) {
		if (url.isBlank())
			throw new RuntimeException("url is blank.");
		this.hostNameOrAddress = "";
		this.port = 0;
		this.url = url;
		this.name = url;
		this.isAutoReconnect = autoReconnect;
	}

	public Connector(@NotNull String host, int port, boolean autoReconnect) {
		this.hostNameOrAddress = host;
		this.port = port;
		this.url = null;
		this.name = host + '_' + port;
		this.isAutoReconnect = autoReconnect;
	}

	public Connector(@NotNull Element self) {
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

	public final @NotNull String getHostNameOrAddress() {
		return hostNameOrAddress;
	}

	public final int getPort() {
		return port;
	}

	public final @Nullable String getUrl() {
		return url;
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
			tryReconnect();
		} else {
			lock();
			try {
				epoch++; // 撤销重连意图同样开新纪元：作废已派发、cancel(false)撤不回的任务（对齐stop）
				if (reconnectTask != null) {
					reconnectTask.cancel(false);
					reconnectTask = null;
				}
			} finally {
				unlock();
			}
		}
	}

	public final boolean isConnected() {
		return isConnected;
	}

	public final boolean isHandshakeDone() {
		return TryGetReadySocket() != null;
	}

	public final void SetService(@NotNull Service service) {
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
	public @NotNull AsyncSocket WaitReady() {
		return GetReadySocket();
	}

	public final @NotNull AsyncSocket GetReadySocket() {
		return futureSocket.get(READY_TIMEOUT, TimeUnit.MILLISECONDS);
	}

	public final @Nullable AsyncSocket TryGetReadySocket() {
		try {
			return futureSocket.getNow();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 契约：仅由 AsyncSocket 死亡流程（doClose，置死之后）调用——本方法持锁经 stop(e) 重入
	 * as.close(e)，"socket==closed ⇒ 已置死"使其恒为no-op；否则doClose将在Connector锁内展开
	 * （含取Service锁），违反Service锁→回调锁单向锁序（见Service.stop契约）。
	 */
	public void OnSocketClose(@NotNull AsyncSocket closed, @Nullable Throwable e) throws Exception {
		lock();
		try {
			if (socket == closed) {
				stop(e);
				tryReconnect();
			}
		} finally {
			unlock();
		}
	}

	public void OnSocketConnected(@NotNull AsyncSocket so) {
		lock();
		try {
			// socket!=so为stale回调（stop已置null或已被新一代取代）：不得置isConnected（FND8-52）
			if (socket != so)
				return;
			isConnected = true;
			reConnectDelay = 0;
		} finally {
			unlock();
		}
	}

	// 重连引擎内部入口（setAutoReconnect/OnSocketClose/构造失败续排）：带退避排程start。
	private void tryReconnect() {
		lock();
		try {
			// socket!=null（含在途建连）即已有一次在途尝试
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
	public void OnSocketHandshakeDone(@NotNull AsyncSocket so) {
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
		so.close(new Exception("not owner?")); // stale握手属已stop/已替换连接，防御性关闭（幂等）
	}

	// 不变量（异步建连）：socket所有权在start()锁段内随构造同步发布（构造微秒级非阻塞，
	// DNS/connect异步进行），stop()恒有可立即close的已发布句柄，在途连接由channel关闭+
	// resolver侧isClosed检查点回收；建连/解析失败统一走close→OnSocketClose链；
	public void start() {
		lock();
		try {
			if (service == null) // 未SetService即start：编程错误，快速失败（否则NPE进退避循环不可诊断）
				throw new IllegalStateException("Connector '" + getName() + "' start() before SetService()");
			epoch++; // 新意图开启新纪元，作废在途调度任务
			startInternal();
		} finally {
			unlock();
		}
	}

	// 排程任务入口：仅当排程代数仍是当前代数（排程后未发生start()/stop()）才建连。
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
			// 构造非阻塞：锁内同步发布所有权（不变量前提，见字段区注释）
			socket = null == url || url.isBlank()
					? service.newClientSocket(hostNameOrAddress, port, userState, this)
					: service.newWebsocketClient(url, userState, this);
		} catch (Exception e) {
			tryReconnect(); // 同步构造失败（配置/编程错误）仍续排重试，保持原契约
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
			epoch++; // 作废在途调度任务
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
		// 在途连接（含DNS/建连未完成）直接close，resolver侧isClosed检查点放弃
		as.close(e);
	}
}
