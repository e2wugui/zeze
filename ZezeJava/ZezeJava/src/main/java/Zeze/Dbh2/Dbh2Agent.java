package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BeginTransaction;
import Zeze.Builtin.Dbh2.CommitTransaction;
import Zeze.Builtin.Dbh2.Delete;
import Zeze.Builtin.Dbh2.Get;
import Zeze.Builtin.Dbh2.KeepAlive;
import Zeze.Builtin.Dbh2.Put;
import Zeze.Builtin.Dbh2.RollbackTransaction;
import Zeze.Builtin.Dbh2.SetBucketMeta;
import Zeze.Net.Binary;
import Zeze.Raft.Agent;
import Zeze.Raft.RaftConfig;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Procedure;
import Zeze.Util.KV;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Dbh2Agent extends AbstractDbh2Agent {
//	private static final Logger logger = LogManager.getLogger(Dbh2Agent.class);
	private final Agent raftClient;
	private final TaskCompletionSource<Boolean> loginFuture = new TaskCompletionSource<>();
	private volatile long lastErrorTime;
	private final Dbh2Config config = new Dbh2Config();
	private volatile long activeTime = System.currentTimeMillis();

	public void setBucketMeta(BBucketMeta.Data meta) {
		var r = new SetBucketMeta();
		r.Argument = meta;
		raftClient.sendForWait(r).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("fail! code=" + r.getResultCode());
	}

	public KV<Boolean, ByteBuffer> get(String databaseName, String tableName, Binary key) {
		var r = new Get();
		r.Argument.setDatabase(databaseName);
		r.Argument.setTable(tableName);
		r.Argument.setKey(key);
		raftClient.sendForWait(r).await();

		if (r.getResultCode() == errorCode(eBucketMissmatch))
			return KV.create(false, null);

		if (r.getResultCode() != 0)
			throw new RuntimeException("fail! code=" + r.getResultCode());

		var bb = r.Result.isNull()
				? null
				: ByteBuffer.Wrap(
						r.Result.getValue().bytesUnsafe(),
						r.Result.getValue().getOffset(),
						r.Result.getValue().size());
		return KV.create(true, bb);
	}

	public Long beginTransaction(String databaseName, String tableName) {
		var r = new BeginTransaction();
		r.Argument.setDatabase(databaseName);
		r.Argument.setTable(tableName);
		raftClient.sendForWait(r).await();

		if (r.getResultCode() == errorCode(eBucketMissmatch))
			return null;

		if (r.getResultCode() != 0)
			throw new RuntimeException("fail! code=" + r.getResultCode());

		return r.Result.getTransactionId();
	}

	public void commitTransaction(long tid) {
		var r = new CommitTransaction();
		r.Argument.setTransactionId(tid);
		raftClient.sendForWait(r).await();
	}

	public void rollbackTransaction(long tid) {
		var r = new RollbackTransaction();
		r.Argument.setTransactionId(tid);
		raftClient.sendForWait(r).await();
	}

	public String put(String databaseName, String tableName, long tid, Binary key, Binary value) {
		var r = new Put();
		r.Argument.setTransactionId(tid);
		r.Argument.setDatabase(databaseName);
		r.Argument.setTable(tableName);
		r.Argument.setKey(key);
		r.Argument.setValue(value);
		raftClient.sendForWait(r).await();

		if (r.getResultCode() == errorCode(eBucketMissmatch))
			return null;

		if (r.getResultCode() != 0)
			throw new RuntimeException("fail! code=" + r.getResultCode());

		return r.Result.getRaftConfig();
	}

	public String delete(String databaseName, String tableName, long tid, Binary key) {
		var r = new Delete();
		r.Argument.setTransactionId(tid);
		r.Argument.setDatabase(databaseName);
		r.Argument.setTable(tableName);
		r.Argument.setKey(key);
		raftClient.sendForWait(r).await();

		if (r.getResultCode() == errorCode(eBucketMissmatch))
			return null;

		if (r.getResultCode() != 0)
			throw new RuntimeException("fail! code=" + r.getResultCode());

		return r.Result.getRaftConfig();
	}

	private void verifyFastFail() {
		if (System.currentTimeMillis() - lastErrorTime < config.serverFastErrorPeriod)
			throw new RuntimeException("FastErrorPeriod");
	}

	private void setFastFail() {
		var now = System.currentTimeMillis();
		if (now - lastErrorTime > config.serverFastErrorPeriod)
			lastErrorTime = now;
	}

	public void keepAlive() {
		if (loginFuture.isDone() && !loginFuture.isCompletedExceptionally() && !loginFuture.isCancelled())
			return; // not login

		var rpc = new KeepAlive();
		raftClient.send(rpc, p -> {
			if (!rpc.isTimeout() && (rpc.getResultCode() == 0 || rpc.getResultCode() == Procedure.RaftApplied))
				activeTime = System.currentTimeMillis(); // KeepAlive.Response
			return 0;
		});
	}

	public Dbh2Agent(RaftConfig raftConf) throws Exception {
		raftClient = new Agent("dbh2.raft", raftConf);
		raftClient.setOnSetLeader(this::raftOnSetLeader);
		RegisterProtocols(raftClient.getClient());
		raftClient.getClient().start();
	}

	private void raftOnSetLeader(Agent agent) {
	}

	public final void close() throws Exception {
		raftClient.stop();
	}
}
