package Zeze.Dbh2;

import java.util.HashMap;
import Zeze.Builtin.Dbh2.Commit.Commit;
import Zeze.Builtin.Dbh2.Commit.Query;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Net.Binary;
import Zeze.Net.Connector;

public class CommitAgent extends AbstractCommitAgent {
	public static final String eServiceName = "Zeze.Dbh2.CommitAgent";

	public static class Service extends Zeze.Net.Service {
		public Service(Config config) {
			super(eServiceName, config);
		}
	}

	private final Service service;

	public CommitAgent(Config config) {
		service = new Service(config);
		RegisterProtocols(service);
	}

	public void startAndWaitConnectionReady() {
		try {
			service.start();
			service.getConfig().forEachConnector(Connector::WaitReady);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void stop() {
		try {
			service.stop();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public long query(Binary transactionKey) {
		var r = new Query();
		r.Argument.setTransactionKey(transactionKey);
		r.SendForWait(service.GetSocket()).await();
		return r.getResultCode();
	}

	public void commit(HashMap<Dbh2Agent, Database.BatchWithTid> trans) {
		var r = new Commit();
		r.Argument.setTransactionData(CommitRocks.encodeTransaction(trans));
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("commit error=" + IModule.getErrorCode(r.getResultCode()));
	}
}
