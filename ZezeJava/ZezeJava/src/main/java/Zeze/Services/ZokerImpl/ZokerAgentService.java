package Zeze.Services.ZokerImpl;

import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import Zeze.Services.ZokerAgent;

public class ZokerAgentService extends Service {
	private final ZokerAgent agent;

	public ZokerAgentService(ZokerAgent agent, Config config) {
		super("Zeze.ZokerAgentService", config);
		this.agent = agent;
	}

	@Override
	public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
		var zokerName = (String)so.getUserState();
		if (zokerName != null)
			agent.zokers().remove(zokerName);
		super.OnSocketClose(so, e);
	}
}
