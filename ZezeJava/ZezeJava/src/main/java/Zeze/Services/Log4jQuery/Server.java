package Zeze.Services.Log4jQuery;

import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Services.LogService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Log4jQuery 服务端网络服务：接受查询客户端连接，为每个 socket 挂载 ServerUserState 并在连接关闭时清理。
 */
public class Server extends Service {
	private final LogService logService;

	public Server(LogService logService, Config config) {
		super("Zeze.LogService.Server", config);
		this.logService = logService;
	}

	@Override
	public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
		so.setUserState(new ServerUserState(logService));
		super.OnHandshakeDone(so);
	}

	@Override
	public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
		var agent = (ServerUserState)so.getUserState();
		try {
			if (agent != null)
				agent.close(); // 任一会话close抛IOException即上抛，但不得因此跳过super
		} finally {
			// 必达：super负责socketMap摘除与收发统计归集，上游TcpSocket.doClose对异常只记日志不补调，
			// 跳过即该连接连同缓冲滞留socketMap永久泄漏。
			super.OnSocketClose(so, e);
		}
	}
}
