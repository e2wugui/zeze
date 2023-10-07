package Zeze.Services.Log4jQuery;

import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Server extends Service {
	private final LogServiceConf conf;

	public Server(LogServiceConf conf, Config config) {
		super("Zeze.LogService.Server", config);
		this.conf = conf;
	}

	@Override
	public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
		so.setUserState(new ServerUserState(conf));
		super.OnHandshakeDone(so);
	}

	@Override
	public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
		var agent = (ServerUserState)so.getUserState();
		if (null != agent)
			agent.close();
		super.OnSocketClose(so, e);
	}
}
