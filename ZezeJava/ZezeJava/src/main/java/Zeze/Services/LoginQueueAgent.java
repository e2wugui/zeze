package Zeze.Services;

import Zeze.Builtin.LoginQueueServer.AnnounceSecret;
import Zeze.Builtin.LoginQueueServer.ReportLinkLoad;
import Zeze.Builtin.LoginQueueServer.ReportProviderLoad;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Net.Service;

public class LoginQueueAgent extends AbstractLoginQueueAgent {

	/**
	 * Connector service. 连接LoginQueueServer.
	 */
	public static class LoginQueueAgentService extends Service {
		public LoginQueueAgentService(Config config) {
			super("LoginQueueAgentService", config);
		}
	}

	private final LoginQueueAgentService service;
	private Binary secretKey;
	private final int serverId;
	private final String serviceIp;
	private final int servicePort;

	public LoginQueueAgent(Config config, int serverId, String serviceIp, int servicePort) {
		this.serverId = serverId;
		this.serviceIp = serviceIp;
		this.servicePort = servicePort;

		this.service = new LoginQueueAgentService(config);
		RegisterProtocols(this.service);
	}

	public LoginQueueAgentService getService() {
		return service;
	}

	public Binary getSecretKey() {
		return secretKey;
	}

	@Override
	protected long ProcessAnnounceSecret(AnnounceSecret r) {
		secretKey = r.Argument.getSecretKey();
		return 0;
	}

	// 下面两个报告对于一个服务只能二选一。要嘛是link，要嘛是provider。

	public void reportLinkLoad(BLoad.Data load) {
		var p = new ReportLinkLoad();
		p.Argument.setServerId(serverId);
		p.Argument.setServiceIp(serviceIp);
		p.Argument.setServicePort(servicePort);
		p.Argument.setLoad(load);
		p.Send(service.GetSocket());
	}

	public void reportProviderLoad(BLoad.Data load) {
		var p = new ReportProviderLoad();
		p.Argument.setServerId(serverId);
		p.Argument.setServiceIp(serviceIp);
		p.Argument.setServicePort(servicePort);
		p.Argument.setLoad(load);
		p.Send(service.GetSocket());
	}
}
