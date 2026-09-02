package Zeze.Services;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
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
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import Zeze.Util.TimeThrottle;
import Zeze.Util.TimeThrottleCounter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
		public void OnSocketAccept(@NotNull AsyncSocket so) throws Exception {
			if (LoginQueue.this.tryOnAccept(so)) // 先触发逻辑，super默认的检查maxConnections后来处理。
				super.OnSocketAccept(so);
		}

		@Override
		public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
			LoginQueue.this.onClose(so);
			super.OnSocketClose(so, e);
		}
	}

	private final LoginQueueServer server;
	private final ConcurrentLinkedQueue<AsyncSocket> queue = new ConcurrentLinkedQueue<>();
	// 私有锁: 串行化tryOnAccept/drainQueue/tryResetTimeThrottle;取代原先共用的this监视器,不暴露实例监视器
	private final ReentrantLock allocateLock = new ReentrantLock();
	private final Future<?> allocateTimer;
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
		this.allocateTimer = TaskSpec.ofAction(this::allocateTimer).schedulePeriodNow(1000L, 1000L);
		timeThrottle = new TimeThrottleCounter(1, maxOnlineNew, maxOnlineNew);
	}

	void tryResetTimeThrottle(int providerSize) {
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
		server.getService().start();
		service.start();
	}

	public void stop() throws Exception {
		allocateTimer.cancel(true);
		server.getService().stop();
		service.stop();
		timeThrottle.close(); // 放在service.stop之后：关闭过程中onClose还可能触发tryResetTimeThrottle替换实例
	}

	private void allocateTimer() throws Exception {
		drainQueue();

		// 比分配更长的间隔。每N次timer触发广播一次。
		if (++broadcastCount >= 3) {
			broadcastCount = 0;
			// 给前10000个客户端广播队列长度。
			var i = 0;
			for (var e : queue) {
				if (++i > 10000) // 最多广播10000个，客户端如果没有收到PutQueueSize，就显示>10000。
					break;
				if (e.isClosed()) // 队头之外的中部瞬时残留（drainQueue只清队头）：跳过发送，位置计数保持
					continue;
				var p = new PutQueuePosition();
				p.Argument.setQueuePosition(i);
				p.Send(e);
			}
		}
	}

	/**
	 * 给排队连接分配server。timer周期调用；LoginQueueServer收到provider/link上报时也立即调用，
	 * 让刚变得可分配的排队连接不用等下一个1秒tick。
	 * allocateLock：两个调用方在不同线程，串行化避免同一排队连接被并发分配（putLoginToken+closeGracefully）两次。
	 */
	void drainQueue() throws Exception {
		allocateLock.lock();
		try {
			// 每个server分配OnlineNew，随机一半以上的分配量。
			var max = server.providerSize() * maxOnlineNew;
			var half = max / 2;
			if (half > 0)
				max = half + Zeze.Util.Random.getInstance().nextInt(half);
			var allocate = 0;
			// peek/poll 而非 for-each+poll：队头排队期间断开的连接直接清掉，不占本轮分配配额，
			// 且不依赖分配是否成功（providerSize()==0 时 max==0，for-each 版本会什么都不做，
			// closed 残留越积越多，虚高 queue.size 导致 tryOnAccept 误发 PutQueueFull）。
			for (var e = queue.peek(); e != null; e = queue.peek()) {
				if (e.isClosed()) {
					queue.poll();
					continue;
				}
				if (allocate >= max)
					break;
				if (!tryAllocateServer(e))
					break; // 分配失败
				queue.poll();
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
		if (queue.size() >= so.getService().getConfig().getMaxConnections()) {
			new PutQueueFull().Send(so);
			so.closeGracefully();
			return false;
		}
		// 与drainQueue同锁：choiceServer里setOnline(getOnline()+1)非原子，accept线程与timer线程串行化。
		allocateLock.lock();
		try {
			if (queue.isEmpty() && timeThrottle.checkNow(1)) {
				if (tryAllocateServer(so))
					return false; // 新连接，直接分配成功，done
			}
			queue.add(so);
			return true;
		} finally {
			allocateLock.unlock();
		}
	}

	void onClose(AsyncSocket ignoredSo) {
		// 不在这里 queue.remove(so)：remove 是 O(n)，且每次分配成功 putLoginToken 后的
		// closeGracefully 也会触发 onClose（此时连接已出队，remove 是无效全量扫描），
		// 高吞吐下退化为 O(n²)；排队期间断开的连接由 drainQueue 的队头清理统一负责。
	}

	public static void main(String[] args) throws Exception {
		int maxOnlineNew = 100;
		boolean choiceLinkOnly = false;
		var configXml = "loginQueue.xml";
		for (var i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-maxOnlineNew":
				maxOnlineNew = Integer.parseInt(args[++i]);
				break;
			case "-choiceLinkOnly":
				choiceLinkOnly = Boolean.parseBoolean(args[++i]);
				break;
			case "-config":
				configXml = args[++i];
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
