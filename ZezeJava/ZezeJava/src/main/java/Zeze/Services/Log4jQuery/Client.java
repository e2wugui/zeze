package Zeze.Services.Log4jQuery;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Util.OutObject;

/**
 * Log4jQuery 客户端网络服务：按 ServiceManager 通告动态维护到各日志服务端的 Connector。
 */
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
		}
		// 同identity地址变更（自定义ServiceIdentity不含地址时可达；默认identity内嵌地址，
		// 地址变更即identity变更走onSmRemoved配对）：put改指新Connector并回收旧值——
		// 旧Connector不stop则autoReconnect永久重连旧地址且条目残留config.connectors；
		// tryGetOrAddConnector命中既有键（地址回切）返回false时同样必须put改指现有Connector，
		// 否则logServers仍指向死地址。stop+removeConnector与onSmRemoved同构；
		// Connector按name（host_port）键控，old!=out.value即地址不同（同地址必命中同一实例）。
		var old = logServers.put(si.getServiceIdentity(), out.value);
		if (old != null && old != out.value) {
			old.stop(); // stop作废在途重连排程，阻断对旧地址的永久重连
			getConfig().removeConnector(old);
		}
	}

	public void onSmRemoved(BServiceInfo si) {
		// remove必须与stop同时发生：残留死条目会让newSessionAll对其GetReadySocket超时，构造必失败。
		var conn = logServers.remove(si.getServiceIdentity());
		if (conn != null) {
			conn.stop();
			getConfig().removeConnector(conn);
		}
	}
}
