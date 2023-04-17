package Zeze.Dbh2;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.Commit.Commit;
import Zeze.Builtin.Dbh2.Commit.Query;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Connector;
import Zeze.Util.OutObject;

public class CommitAgent extends AbstractCommitAgent {
	public static final String eServiceName = "Zeze.Dbh2.CommitAgent";
	private final ConcurrentHashMap<String, Connector> agents = new ConcurrentHashMap<>();

	public static class Service extends Zeze.Net.Service {
		public Service(Config config) {
			super(eServiceName, config);
		}
	}

	private final Service service;

	private AsyncSocket connect(String host, int port) {
		var name = host + ":" + port;
		var connector = agents.computeIfAbsent(name, __ -> {
			var out = new OutObject<Connector>();
			if (service.getConfig().tryGetOrAddConnector(host, port, true, out)) {
				try {
					out.value.start();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			return out.value;
		});
		return connector.GetReadySocket();
	}

	public CommitAgent() {
		service = new Service(new Config());
		RegisterProtocols(service);
	}

	public void startAndWaitConnectionReady() throws Exception {
		service.start();
		service.getConfig().forEachConnector(Connector::WaitReady);
	}

	public void stop() throws Exception {
		service.stop();
	}

	public long query(String host, int port, Binary sortedNames, long tid) {
		var r = new Query();
		r.Argument.setBucketRaftSortedNames(sortedNames);
		r.Argument.setBucketTid(tid);
		r.SendForWait(connect(host, port)).await();
		return r.getResultCode();
	}

	public void commit(String host, int port, HashMap<Dbh2Agent, Database.BatchWithTid> trans) {
		var r = new Commit();
		r.Argument.setTransactionData(CommitRocks.encodeTransaction(trans));
		r.SendForWait(connect(host, port)).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("commit error=" + IModule.getErrorCode(r.getResultCode()));
	}
}
