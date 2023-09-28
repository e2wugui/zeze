package Zeze.Services;

import java.util.Set;
import Zeze.Application;
import Zeze.Builtin.LogService.NewSession;
import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Raft.RocksRaft.Collection;
import Zeze.Services.Log4jQuery.Client;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Services.Log4jQuery.Session;
import Zeze.Services.Log4jQuery.SessionAll;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.BSubscribeInfo;

public class LogAgent extends AbstractLogAgent {
	private final Config conf;
	private final LogServiceConf logConf;
	private final AbstractAgent serviceManager;
	private final Client client;

	public LogAgent(Config config) throws Exception {
		this.conf = config;
		logConf = new LogServiceConf();
		config.parseCustomize(logConf);
		serviceManager = Application.createServiceManager(conf, "LogServiceServer");
		client = new Client(logConf, conf);

		if (null != serviceManager) {
			serviceManager.setOnUpdate(client::onSmUpdated);
			serviceManager.setOnRemoved(client::onSmRemoved);
		}
	}

	public void start() throws Exception {
		var serviceManagerConf = conf.getServiceConf(Agent.defaultServiceName);
		if (serviceManagerConf != null && serviceManager != null) {
			serviceManager.start();
			try {
				serviceManager.waitReady();
			} catch (Exception ignored) {
				// raft 版第一次等待由于选择leader原因肯定会失败一次。
				serviceManager.waitReady();
			}
			serviceManager.subscribeService("Zeze.LogService", BSubscribeInfo.SubscribeTypeSimple);
		}
		client.start();
	}

	public void stop() throws Exception {
		this.client.stop();
		if (serviceManager != null)
			serviceManager.close();
	}

	public Session newSession(String serverName) {
		return new Session(this, serverName);
	}

	public Set<String> getLogServers() {
		return client.getLogServers().keySet();
	}

	public Connector __getLogServer(String serverName) {
		return client.getLogServers().get(serverName);
	}

	public SessionAll newSessionAll() {
		return new SessionAll(this);
	}
}
