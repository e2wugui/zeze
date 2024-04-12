package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.Commit.DummyImportBean;
import Zeze.Config;
import Zeze.Transaction.DispatchMode;
import Zeze.Util.DispatchModeAnnotation;
import org.rocksdb.RocksDBException;

public class Commit extends AbstractCommit {
	private CommitRocks rocks;
	private final CommitService service;

	public Commit(Dbh2AgentManager manager, Config config) throws RocksDBException {
		rocks = new CommitRocks(manager, config.getServerId());
		service = new CommitService(config);
		RegisterProtocols(service);
	}

	public void start() throws Exception {
		rocks.start();
		service.start(); // 网络后启动。
	}

	public void stop() throws Exception {
		lock();
		try {
			service.stop(); // 网络先关闭。否则请求过来访问rocks会导致jvm-crash。
			if (null != rocks) {
				rocks.close();
				rocks = null;
			}
		} finally {
			unlock();
		}
	}

	public CommitRocks getRocks() {
		return rocks;
	}

	public CommitService getService() {
		return service;
	}

	@Override
	protected long ProcessCommitRequest(Zeze.Builtin.Dbh2.Commit.Commit r) throws Exception {
		var query = rocks.getManager().commitServiceAcceptor();
		rocks.commit(query.getKey(), query.getValue(), r.Argument);
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessDummyImportBean(DummyImportBean p) throws Exception {
		return 0;
	}

	@DispatchModeAnnotation(mode = DispatchMode.Critical)
	@Override
	protected long ProcessQueryRequest(Zeze.Builtin.Dbh2.Commit.Query r) throws Exception {
		var state = rocks.query(r.Argument.getTid());
		if (null == state)
			r.Result.setState(Commit.eCommitNotExist);
		else
			r.Result = state;
		r.SendResult();
		return 0;
	}
}
