package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.CommitBatch;
import Zeze.Builtin.Dbh2.Get;
import Zeze.Builtin.Dbh2.KeepAlive;
import Zeze.Builtin.Dbh2.PrepareBatch;
import Zeze.Builtin.Dbh2.SetBucketMeta;
import Zeze.Builtin.Dbh2.UndoBatch;
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
	private static final Logger logger = LogManager.getLogger(Dbh2Agent.class);
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

	public void prepareBatch(Database.BatchWithTid batch) {
		var r = new PrepareBatch();
		r.Argument = batch.data;
		raftClient.sendForWait(r).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("fail! code=" + r.getResultCode());
		batch.tid = r.Result.getTid();
	}

	public void commitBatch(Database.BatchWithTid batch) {
		var r = new CommitBatch();
		r.Argument.setTid(batch.tid);
		raftClient.sendForWait(r).await();
		if (r.getResultCode() != 0)
			logger.warn("commit with result code={}", r.getResultCode());
	}

	public void undoBatch(Database.BatchWithTid batch) {
		var r = new UndoBatch();
		r.Argument.setTid(batch.tid);
		raftClient.sendForWait(r).await();
		if (r.getResultCode() != 0)
			logger.warn("undo with result code={}", r.getResultCode());
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
