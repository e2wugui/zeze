package Zeze.Services.Log4jQuery;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Util.OutObject;

public class Client extends Service {
	private final LogServiceConf logConf;
	private final ConcurrentHashMap<String, Connector> logServers = new ConcurrentHashMap<>();

	public Client(LogServiceConf logConf, Config config) {
		super("Zeze.LogService.Client", config);
		this.logConf = logConf;
	}

	public LogServiceConf getLogConf() {
		return logConf;
	}

	public ConcurrentHashMap<String, Connector> getLogServers() {
		return logServers;
	}

	public void onSmUpdated(BServiceInfo si) {
		var out = new OutObject<Connector>();
		if (getConfig().tryGetOrAddConnector(si.getPassiveIp(), si.getPassivePort(), true, out)) {
			// 新建的Connector。开始连接。
			out.value.start();
			logServers.put(si.getServiceIdentity(), out.value);
		}
	}

	public void onSmRemoved(BServiceInfo si) {
		var conn = logServers.get(si.getServiceIdentity());
		if (conn != null) {
			conn.stop();
			getConfig().removeConnector(conn);
		}
	}
}
