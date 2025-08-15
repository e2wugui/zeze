package Zeze.Services;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Builtin.LoginQueue.BToken;
import Zeze.Builtin.LoginQueue.PutLoginToken;
import Zeze.Builtin.LoginQueue.PutQueueFull;
import Zeze.Builtin.LoginQueue.PutQueuePosition;
import Zeze.Builtin.LoginQueueServer.BServerLoad;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Util.Task;
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

	public LoginQueue() {
		this(100, false);
	}

	public LoginQueue(int maxOnlineNew, boolean choiceLinkOnly) {
		var config = Config.load("loginQueue.xml");
		this.maxOnlineNew = maxOnlineNew;
		this.choiceLinkOnly = choiceLinkOnly;
		this.server = new LoginQueueServer(this, config);
		this.service = new LoginQueueService(config);
		RegisterProtocols(service);
		this.allocateTimer = Task.scheduleUnsafe(1000L, 1000L, this::allocateTimer);
		timeThrottle = new TimeThrottleCounter(1, maxOnlineNew, maxOnlineNew);
	}

	synchronized void tryResetTimeThrottle(int providerSize) {
		if (this.providerSize != providerSize) {
			this.providerSize = providerSize;
			timeThrottle = new TimeThrottleCounter(1, maxOnlineNew * providerSize, maxOnlineNew * providerSize);
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
	}

	private void allocateTimer() throws Exception {
		// 每个server分配OnlineNew，随机一半以上的分配量。
		var max = server.providerSize() * maxOnlineNew;
		var half = max / 2;
		if (half > 0)
			max = half + Zeze.Util.Random.getInstance().nextInt(half);
		var allocate = 0;
		for (var e : queue) {
			if (++allocate > max)
				break;
			if (!tryAllocateServer(e))
				break; // 分配失败
			queue.poll();
		}

		// 比分配更长的间隔。每N次timer触发广播一次。
		if (++broadcastCount >= 3) {
			broadcastCount = 0;
			// 给前10000个客户端广播队列长度。
			var i = 0;
			for (var e : queue) {
				if (++i > 10000) // 最多广播10000个，客户端如果没有收到PutQueueSize，就显示>10000。
					break;
				var p = new PutQueuePosition();
				p.Argument.setQueuePosition(i);
				p.Send(e);
			}
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
		if (queue.isEmpty() && timeThrottle.checkNow(1)) {
			if (tryAllocateServer(so))
				return false; // 新连接，直接分配成功，done
		}
		queue.add(so);
		return true;
	}

	void onClose(AsyncSocket so) {
		// queue.remove(so); // 遍历时处理isClosed的。
	}

	public static void main(String [] args) throws Exception {
		int maxOnlineNew = 100;
		boolean choiceLinkOnly = false;
		for (var i = 0; i < args.length; ++i) {
			switch (args[i])
			{
			case "-maxOnlineNew":
				maxOnlineNew = Integer.parseInt(args[++i]);
				break;
			case "-choiceLinkOnly":
				choiceLinkOnly = Boolean.parseBoolean(args[++i]);
				break;
			}
		}
		Task.tryInitThreadPool();
		var lq = new LoginQueue(maxOnlineNew, choiceLinkOnly);
		lq.start();
		synchronized (Thread.currentThread()) {
			Thread.currentThread().wait();
		}
	}
}
