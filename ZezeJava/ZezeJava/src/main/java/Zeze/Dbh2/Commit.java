package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.Commit.DummyImportBean;
import Zeze.Config;
import Zeze.Transaction.DispatchMode;
import Zeze.Util.DispatchModeAnnotation;
import org.rocksdb.RocksDBException;
import org.jspecify.annotations.Nullable;

/**
 * Dbh2 事务提交服务，组合本地 CommitRocks 存储与网络协议处理。
 */
public class Commit extends AbstractCommit {
	private @Nullable CommitRocks rocks;
	private final CommitService service;

	public Commit(Dbh2AgentManager manager, Config config) throws RocksDBException {
		rocks = new CommitRocks(manager, config.getServerId());
		service = new CommitService(config);
		RegisterProtocols(service);
	}

	public void start() throws Exception {
		//noinspection DataFlowIssue
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

	public @Nullable CommitRocks getRocks() {
		return rocks;
	}

	public CommitService getService() {
		return service;
	}

	@Override
	protected long ProcessCommitRequest(Zeze.Builtin.Dbh2.Commit.Commit r) throws Exception {
		//noinspection DataFlowIssue
		var query = rocks.getManager().commitServiceAcceptor();
		// 同步投递（2026-11-12 裁定，无条件取代065ea39f7的应答解绑）：decide（prepare+
		// saveCommitPoint(eCommitting)）与deliver（commitBatch循环，桶侧应答点=状态机apply后）
		// 串行完成后才应答——应答0不仅意味着eCommitting已持久化（终局为提交，065ea39f7的
		// 假阴性不变量保持），通常还意味着数据已可读（commit/checkpointRun返回即find可见）。
		// deliver失败/超时由commitDeliver自身吞掉、仍走应答0——不可因投递问题报确定失败
		// （假阴性正是065ea39f7要消灭的形态），redoTimer兜底重发，仅该次应答不保证立即可见。
		// 客户端超时相应扩容（见Dbh2Config.getCommitRpcTimeout）。
		var decision = rocks.commitDecide(query.getKey(), query.getValue(), r.Argument);
		rocks.commitDeliver(decision);
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessDummyImportBean(DummyImportBean p) {
		return 0;
	}

	@DispatchModeAnnotation(mode = DispatchMode.Critical)
	@Override
	protected long ProcessQueryRequest(Zeze.Builtin.Dbh2.Commit.Query r) throws Exception {
		//noinspection DataFlowIssue
		var state = rocks.query(r.Argument.getTid());
		if (null == state)
			r.Result.setState(Commit.eCommitNotExist);
		else
			r.Result = state;
		r.SendResult();
		return 0;
	}
}
