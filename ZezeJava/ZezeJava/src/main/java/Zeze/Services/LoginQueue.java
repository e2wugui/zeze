package Zeze.Services;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import Zeze.Arch.LoadConfig;
import Zeze.Builtin.LoginQueue.PutLoginToken;
import Zeze.Builtin.LoginQueue.PutQueueSize;
import Zeze.Builtin.LoginQueueServer.BServerLoad;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Service;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LoginQueue extends AbstractLoginQueue {
	/**
	 * 网络服务类 Acceptor
	 * 接受客户端连接。
	 */
	public class LoginQueueService extends Service {
		public LoginQueueService() {
			super("LoginQueue");
		}

		@Override
		public void OnSocketAccept(@NotNull AsyncSocket so) throws Exception {
			super.OnSocketAccept(so);
			LoginQueue.this.onAccept(so);
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

	public LoginQueue() {
		var loadConfig = new LoadConfig();
		this.server = new LoginQueueServer(loadConfig);
		this.allocateTimer = Task.scheduleUnsafe(loadConfig.getDigestionDelayExSeconds() * 1000L,
				loadConfig.getDigestionDelayExSeconds() * 1000L, this::allocateTimer);
	}

	public void stop() {
		allocateTimer.cancel(true);
	}

	private void allocateTimer() {
		for (var e : queue) {
			if (tryAllocateServer(e)) {
				queue.poll();
				continue;
			}
			break;
		}

		// 比分配更长的间隔。每3次timer触发广播一次。
		if (++broadcastCount >= 3) {
			broadcastCount = 0;
			// 一旦碰到分配失败，并且达到广播时间，就给剩余队列中的客户端广播队列长度。
			var size = queue.size();
			var p = new PutQueueSize();
			p.Argument.setQueueSize(size);
			var i = 0;
			for (var e : queue) {
				if (++i > 10000) // 最多广播10000个，客户端如果没有收到PutQueueSize，就显示>10000。
					break;
				p.Send(e);
			}
		}
	}

	private boolean tryAllocateServer(AsyncSocket so) {
		if (so.isClosed())
			return true; // 对于关闭的目标连接，总是认为分配成功。

		var link = server.choiceLink();
		if (null != link) {
			var provider = server.choiceProvider();
			if (null != provider) {
				var p = new PutLoginToken();
				p.Argument.setLinkIp(link.getServiceIp());
				p.Argument.setLinkPort(link.getServicePort());
				p.Argument.setToken(encodeToken(provider));
				p.Send(so);
				so.closeGracefully();
				return true;
			}
		}
		return false;
	}

	void onAccept(AsyncSocket so) {
		if (queue.isEmpty()) {
			if (tryAllocateServer(so))
				return; // 新连接，直接分配成功，done
		}
		queue.add(so);
	}

	private Binary encodeToken(BServerLoad.Data provider) {
		return new Binary(new byte[1024]);
	}

	void onClose(AsyncSocket so) {
		queue.remove(so);
	}
}
