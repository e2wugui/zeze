package Zeze.Services.Log4jQuery;

import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Services.LogService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Server extends Service {
	private final LogService logService;
	// private Log4jFileManager manager;

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
		if (agent != null)
			agent.close();
		super.OnSocketClose(so, e);
	}
}
