package Zeze.Services;

import java.util.Set;
import Zeze.Application;
import Zeze.Builtin.LogService.Query;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Net.Connector;
import Zeze.Services.Log4jQuery.Client;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Services.Log4jQuery.Session;
import Zeze.Services.Log4jQuery.SessionAll;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import org.jetbrains.annotations.NotNull;

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
		RegisterProtocols(client);
	}

	public LogServiceConf getLogConf() {
		return logConf;
	}

	void applyOnChanged(@NotNull BEditService edit) {
		for (var r : edit.getRemove()) {
			client.onSmRemoved(r);
		}
		for (var p : edit.getAdd()) {
			client.onSmUpdated(p);
		}
	}

	public void start() throws Exception {
		client.start();
		var serviceManagerConf = conf.getServiceConf(Agent.defaultServiceName);
		if (serviceManagerConf != null && serviceManager != null) {
			serviceManager.setOnChanged(this::applyOnChanged);
			serviceManager.start();
			try {
				serviceManager.waitReady();
			} catch (Exception ignored) {
				// raft 版第一次等待由于选择leader原因肯定会失败一次。
				serviceManager.waitReady();
			}
			serviceManager.subscribeService(new BSubscribeInfo("Zeze.LogService"));
		}
	}

	public void stop() throws Exception {
		this.client.stop();
		if (serviceManager != null)
			serviceManager.close();
	}

	public Session newSession(String serverName, String logName) {
		return new Session(this, serverName, logName);
	}

	public Set<String> getLogServers() {
		return client.getLogServers().keySet();
	}

	public Connector __getLogServer(String serverName) {
		return client.getLogServers().get(serverName);
	}

	public SessionAll newSessionAll(String logName) {
		return new SessionAll(this, logName);
	}

	public String query(String serverName, String jsonArgument) {
		// S3-F4：动态增删的日志服务器表（ConcurrentHashMap）未命中返回null，原裸NPE无任何信息。
		var logServer = __getLogServer(serverName);
		if (logServer == null)
			throw new IllegalArgumentException("unknown log server: " + serverName);
		var r = new Query();
		r.Argument.setJson(jsonArgument);
		r.SendForWait(logServer.GetReadySocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("query error=" + IModule.getErrorCode(r.getResultCode()));
		return r.Result.getJson();
	}
}
