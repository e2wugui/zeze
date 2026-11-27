package Zeze.Services;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.LoginQueue.BToken;
import Zeze.Builtin.LoginQueue.PutLoginToken;
import Zeze.Builtin.LoginQueue.PutQueueFull;
import Zeze.Builtin.LoginQueue.PutQueuePosition;
import Zeze.Builtin.LoginQueueServer.BServerLoad;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Util.DaemonTimer;
import Zeze.Util.Task;
import Zeze.Util.TimeThrottle;
import Zeze.Util.TimeThrottleCounter;
import org.jspecify.annotations.Nullable;
import static Zeze.Util.Args.requireBool;
import static Zeze.Util.Args.requireInt;
import static Zeze.Util.Args.requireValue;

/**
 * 登录排队服务：把瞬时过量的登录连接排队，按provider/link负载与令牌节流分配登录令牌。
 */
public class LoginQueue extends AbstractLoginQueue {
	/**
	 * 网络服务类 Acceptor
	 * 接受客户端连接。
	 */
	public class LoginQueueService extends Service {
		public LoginQueueService(Config config) {
			super("LoginQueue", config);
		}

		@Override
		public void OnSocketAccept(AsyncSocket so) throws Exception {
			if (LoginQueue.this.tryOnAccept(so)) // 先触发逻辑，super默认的检查maxConnections后来处理。
				super.OnSocketAccept(so);
		}

		@Override
		public void OnSocketClose(AsyncSocket so, @Nullable Throwable e) throws Exception {
			LoginQueue.this.onClose(so);
			super.OnSocketClose(so, e);
		}
	}

	/**
	 * 每连接排队占槽标记：true 表示仍占有一个 {@link #queuedCount} 槽位。
	 * 入队时挂到 {@link AsyncSocket#setUserState(Object)}，{@link #onClose}、{@link #drainQueue}、
	 * {@link #broadcastQueuePositions} 与 {@link #stop} 经 {@link #tryRelease()} 原子仲裁恰一次扣减。
	 */
	private static final class QueueSlot extends AtomicBoolean {
		final AsyncSocket socket;

		QueueSlot(AsyncSocket socket) {
			super(true);
			this.socket = socket;
		}

		boolean tryRelease() {
			return compareAndSet(true, false);
		}

		boolean isAlive() {
			return get() && !socket.isClosed();
		}
	}

	private static final int BROADCAST_INTERVAL_TICKS = 3;
	private static final int MAX_BROADCAST_POSITIONS = 10_000;

	private final LoginQueueServer server;
	// 使用 ConcurrentLinkedDeque 以便分配失败时原子放回队头，避免与遍历清理竞态
	private final ConcurrentLinkedDeque<QueueSlot> queue = new ConcurrentLinkedDeque<>();
	private final ReentrantLock allocateLock = new ReentrantLock(); // 串行化入队、出队分配与限流重置
	// 活跃排队连接数：allocateLock 内递增，由 QueueSlot.tryRelease() 原子仲裁恰一次递减
	private final AtomicInteger queuedCount = new AtomicInteger();
	private final DaemonTimer allocateDaemon = new DaemonTimer("LoginQueue.allocate", 1000, this::allocateTimer);
	private int broadcastCount;
	private final int maxOnlineNew;
	private final boolean choiceLinkOnly;
	private volatile TimeThrottle timeThrottle;
	private int providerSize;
	private final AtomicLong serialIdSeed = new AtomicLong();
	private final LoginQueueService service;

	// expire 因为排队完成客户端要登陆（输密码），所以这个时间不能太短。
	public static final int eLoginTokenExpireTime = 30 * 60 * 1000;

	public LoginQueue(Config config) {
		this(config, 100, false);
	}

	public LoginQueue(Config config, int maxOnlineNew, boolean choiceLinkOnly) {
		this.maxOnlineNew = maxOnlineNew;
		this.choiceLinkOnly = choiceLinkOnly;
		this.server = new LoginQueueServer(this, config);
		this.service = new LoginQueueService(config);
		RegisterProtocols(service);
		timeThrottle = new TimeThrottleCounter(1, maxOnlineNew, maxOnlineNew);
	}

	void tryResetTimeThrottle(int providerSize) {
		if (choiceLinkOnly)
			providerSize = server.linkSize();
		allocateLock.lock();
		try {
			if (this.providerSize != providerSize) {
				this.providerSize = providerSize;
				var old = timeThrottle;
				timeThrottle = new TimeThrottleCounter(1, maxOnlineNew * providerSize, maxOnlineNew * providerSize);
				old.close(); // 先更新引用再关闭，减小并发checkNow拿到已关闭实例的窗口
			}
		} finally {
			allocateLock.unlock();
		}
	}

	public void start() throws Exception {
		allocateLock.lock();
		try {
			// 重置限流器为初始状态，确保 stop 后重启不受旧实例已关闭或空载限流影响
			var old = timeThrottle;
			timeThrottle = new TimeThrottleCounter(1, maxOnlineNew, maxOnlineNew);
			providerSize = 0;
			broadcastCount = 0;
			old.close(); // 先更新引用再关闭，减小并发checkNow拿到已关闭实例的窗口
		} finally {
			allocateLock.unlock();
		}
		// 接受连接后负载上报即可重算限流。先初始化，避免覆盖已经收到的首轮负载。
		server.getService().start();
		service.start();
		allocateDaemon.start();
	}

	public void stop() throws Exception {
		// 锁外停止定时器（drainQueue 持有 allocateLock，避免持锁等待在飞轮次）
		allocateDaemon.stop();
		server.getService().stop();
		service.stop();
		clearQueue();
		timeThrottle.close(); // 放在 service.stop 之后，防止关闭期间 onClose 重建限流器
	}

	private void clearQueue() {
		allocateLock.lock();
		try {
			for (var slot = queue.pollFirst(); slot != null; slot = queue.pollFirst())
				releaseSlot(slot);
		} finally {
			allocateLock.unlock();
		}
	}

	private void releaseSlot(QueueSlot slot) {
		if (slot.tryRelease())
			queuedCount.decrementAndGet();
	}

	int getQueuedCount() {
		return queuedCount.get();
	}

	void allocateTimer() throws Exception {
		drainQueue();

		// 比分配更长的间隔。每N次timer触发广播一次。
		if (++broadcastCount >= BROADCAST_INTERVAL_TICKS) {
			broadcastCount = 0;
			broadcastQueuePositions();
		}
	}

	void broadcastQueuePositions() {
		// 给前 MAX_BROADCAST_POSITIONS 个活跃客户端广播排队位置，并顺带通过 Iterator.remove() 清除中部已关闭僵尸节点。
		var position = 0;
		for (var it = queue.iterator(); it.hasNext(); ) {
			var slot = it.next();
			if (!slot.isAlive()) {
				it.remove();
				releaseSlot(slot);
				continue;
			}
			if (++position > MAX_BROADCAST_POSITIONS) // 最多广播10000个活跃连接，客户端如果没有收到PutQueuePosition，就显示>10000。
				break;
			var p = new PutQueuePosition();
			p.Argument.setQueuePosition(position);
			p.Send(slot.socket);
		}
	}

	private void requeueHeadIfAlive(QueueSlot slot) {
		if (slot.isAlive())
			queue.addFirst(slot);
		else
			releaseSlot(slot);
	}

	/**
	 * 为排队连接分配服务器（定时器和负载上报均可触发，经 allocateLock 串行化）。
	 */
	void drainQueue() throws Exception {
		allocateLock.lock();
		try {
			// 每个server分配OnlineNew，随机一半以上的分配量。
			var max = (choiceLinkOnly ? server.linkSize() : server.providerSize()) * maxOnlineNew;
			var half = max / 2;
			if (half > 0)
				max = half + Zeze.Util.Random.getInstance().nextInt(half);
			var allocate = 0;
			// 队头死节点无视配额直接清理；活跃节点原子出队，分配失败再放回队头
			for (;;) {
				var head = queue.peekFirst();
				if (head == null)
					break;
				if (allocate >= max && head.isAlive())
					break;
				var slot = queue.pollFirst();
				if (slot == null)
					break;
				if (!slot.isAlive()) {
					releaseSlot(slot);
					continue;
				}
				if (allocate >= max) {
					requeueHeadIfAlive(slot);
					break; // 本轮配额耗尽
				}
				var allocated = false;
				try {
					allocated = tryAllocateServer(slot.socket);
				} finally {
					if (!allocated)
						requeueHeadIfAlive(slot);
				}
				if (!allocated)
					break; // 分配失败
				releaseSlot(slot);
				++allocate;
			}
		} finally {
			allocateLock.unlock();
		}
	}

	private void putLoginToken(AsyncSocket so, BServerLoad.Data link, int providerServerId) throws Exception {
		var p = new PutLoginToken();
		p.Argument.setLinkIp(link.getServiceIp());
		p.Argument.setLinkPort(link.getServicePort());
		var token = new BToken.Data();
		token.setServerId(providerServerId);
		token.setExpireTime(System.currentTimeMillis() + eLoginTokenExpireTime);
		token.setSerialId(serialIdSeed.incrementAndGet());
		token.setLinkServerId(link.getServerId());
		p.Argument.setToken(LoginQueueServer.encodeToken(server.getSecret(), token));
		p.Send(so);
		so.closeGracefully();
	}

	private boolean tryAllocateLink(AsyncSocket so) throws Exception {
		if (so.isClosed())
			return true; // 对于关闭的目标连接，总是认为分配成功。

		var link = server.choiceLink();
		if (null != link) {
			putLoginToken(so, link, -1);
			return true;
		}

		return false;
	}

	private boolean tryAllocateServer(AsyncSocket so) throws Exception {
		if (choiceLinkOnly)
			return tryAllocateLink(so);

		if (so.isClosed())
			return true; // 对于关闭的目标连接，总是认为分配成功。

		var provider = server.choiceProvider();
		if (null != provider) {
			var link = server.choiceLink();
			if (null != link) {
				putLoginToken(so, link, provider.getServerId());
				return true;
			}
		}
		return false;
	}

	boolean tryOnAccept(AsyncSocket so) throws Exception {
		if (so.isClosed())
			return false;
		//noinspection DataFlowIssue
		var maxConnections = so.getService().getConfig().getMaxConnections();
		if (queuedCount.get() < maxConnections) {
			allocateLock.lock();
			try {
				if (so.isClosed())
					return false;
				var count = queuedCount.get();
				if (count < maxConnections) {
					if (count == 0 && timeThrottle.checkNow(1) && tryAllocateServer(so))
						return false; // 新连接已直接分配
					var slot = new QueueSlot(so);
					// 先增计数再发布 slot，随后复查 isClosed（Dekker 双检），保证计数不负且不泄漏
					queuedCount.incrementAndGet();
					so.setUserState(slot);
					if (so.isClosed()) {
						releaseSlot(slot);
						return false;
					}
					queue.addLast(slot);
					return true;
				}
			} finally {
				allocateLock.unlock();
			}
		}
		new PutQueueFull().Send(so);
		so.closeGracefully();
		return false;
	}

	void onClose(AsyncSocket so) {
		// 仅 O(1) 释放计数，队列中残留的死节点由 drainQueue 和 broadcastQueuePositions 惰性移除
		if (so.getUserState() instanceof QueueSlot slot)
			releaseSlot(slot);
	}

	public static void main(String[] args) throws Exception {
		int maxOnlineNew = 100;
		boolean choiceLinkOnly = false;
		var configXml = "loginQueue.xml";
		for (var i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-maxOnlineNew":
				maxOnlineNew = requireInt(args, ++i, "-maxOnlineNew");
				break;
			case "-choiceLinkOnly":
				choiceLinkOnly = requireBool(args, ++i, "-choiceLinkOnly");
				break;
			case "-config":
				configXml = requireValue(args, ++i, "-config");
				break;
			}
		}
		Task.tryInitThreadPool();
		var lq = new LoginQueue(Config.load(configXml), maxOnlineNew, choiceLinkOnly);
		lq.start();
		synchronized (Thread.currentThread()) {
			Thread.currentThread().wait();
		}
	}
}
