package Zeze.Dbh2;

import java.util.function.ToLongFunction;
import Zeze.Builtin.Dbh2.BBatchTid;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Builtin.Dbh2.BRefused;
import Zeze.Builtin.Dbh2.CommitBatch;
import Zeze.Builtin.Dbh2.Get;
import Zeze.Builtin.Dbh2.KeepAlive;
import Zeze.Builtin.Dbh2.PrepareBatch;
import Zeze.Builtin.Dbh2.SetBucketMeta;
import Zeze.Builtin.Dbh2.UndoBatch;
import Zeze.Builtin.Dbh2.Walk;
import Zeze.Builtin.Dbh2.WalkKey;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Raft.Agent;
import Zeze.Raft.ProxyAgent;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RaftRpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Util.Func3;
import Zeze.Util.KV;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskCompletionSourceX;

public class Dbh2Agent extends AbstractDbh2Agent {
	// private static final Logger logger = LogManager.getLogger(Dbh2Agent.class);
	private final Agent raftClient;
	private final TaskCompletionSource<Boolean> loginFuture = new TaskCompletionSource<>();
	private volatile long lastErrorTime;
	private final Dbh2Config config = new Dbh2Config();
	private final String raftConfigString;

	private volatile long activeTime = System.currentTimeMillis();

	public RaftConfig getRaftConfig() {
		return raftClient.getRaftConfig();
	}

	public String getRaftConfigString() {
		return raftConfigString;
	}

	public void setBucketMeta(BBucketMeta.Data meta) {
		var r = new SetBucketMeta();
		r.Argument = meta;
		r.setTimeout(config.getRpcTimeout());
		raftClient.sendForWait(r).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("fail! code=" + r.getResultCode());
	}

	public void setBucketMetaAsync(BBucketMeta.Data meta, ToLongFunction<Protocol<?>> handle) {
		var r = new SetBucketMeta();
		r.Argument = meta;
		raftClient.send(r, handle);
	}

	public KV<Boolean, ByteBuffer> get(String databaseName, String tableName, Binary key) {
		var r = new Get();
		r.Argument.setDatabase(databaseName);
		r.Argument.setTable(tableName);
		r.Argument.setKey(key);
		r.setTimeout(config.getRpcTimeout());
		raftClient.sendForWait(r).await();

		if (r.getResultCode() == errorCode(eBucketMismatch))
			return KV.create(false, null);

		if (r.getResultCode() != 0 && r.getResultCode() != Procedure.RaftApplied)
			throw new RuntimeException("fail! code=" + r.getResultCode());

		var bb = r.Result.isNull() ? null : ByteBuffer.Wrap(r.Result.getValue());
		return KV.create(true, bb);
	}

	public TaskCompletionSourceX<RaftRpc<BPrepareBatch.Data, BRefused.Data>> prepareBatch(BPrepareBatch.Data batch) {
		var r = new PrepareBatch();
		r.Argument = batch;
		r.setTimeout(config.getRpcTimeout());
		return raftClient.sendForWait(r);
	}

	public TaskCompletionSource<RaftRpc<BBatchTid.Data, EmptyBean.Data>> commitBatch(long tid) {
		var r = new CommitBatch();
		r.Argument.setTid(tid);
		r.setTimeout(config.getRpcTimeout());
		return raftClient.sendForWait(r);
	}

	public TaskCompletionSource<RaftRpc<BBatchTid.Data, EmptyBean.Data>> undoBatch(long tid) {
		var r = new UndoBatch();
		r.Argument.setTid(tid);
		r.setTimeout(config.getRpcTimeout());
		return raftClient.sendForWait(r);
	}

	private void verifyFastFail() {
		if (System.currentTimeMillis() - lastErrorTime < config.getServerFastErrorPeriod())
			throw new RuntimeException("FastErrorPeriod");
	}

	private void setFastFail() {
		var now = System.currentTimeMillis();
		if (now - lastErrorTime > config.getServerFastErrorPeriod())
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

	public Dbh2Agent(String raftConfigString) throws Exception {
		this(raftConfigString, (ProxyAgent)null);
	}

	public Dbh2Agent(String raftConfigString, ProxyAgent proxyAgent) throws Exception {
		this.raftConfigString = raftConfigString;
		var raftConf = RaftConfig.loadFromString(raftConfigString);
		raftClient = new Agent("dbh2.raft", raftConf, null, proxyAgent);
		raftClient.setOnSetLeader(this::raftOnSetLeader);
		RegisterProtocols(raftClient.getClient());
		raftClient.getClient().start();
	}

	public Dbh2Agent(String raftConfigString, Func3<Agent, String, Config, Agent.NetClient> netClientFactory) throws Exception {
		this(raftConfigString, netClientFactory, null);
	}

	public Dbh2Agent(String raftConfigString,
					 Func3<Agent, String, Config, Agent.NetClient> netClientFactory,
					 ProxyAgent proxyAgent) throws Exception {
		this.raftConfigString = raftConfigString;
		var raftConf = RaftConfig.loadFromString(raftConfigString);
		raftClient = new Agent("dbh2.raft", raftConf, null, netClientFactory, proxyAgent);
		raftClient.setOnSetLeader(this::raftOnSetLeader);
		RegisterProtocols(raftClient.getClient());
		raftClient.getClient().start();
	}

	private void raftOnSetLeader(Agent agent) {
	}

	public final void close() throws Exception {
		raftClient.stop();
	}

	public Agent getRaftAgent() {
		return raftClient;
	}

	public Walk walk(Binary exclusiveStartKey, int proposeLimit, boolean desc) {
		var r = new Walk();
		r.Argument.setExclusiveStartKey(exclusiveStartKey);
		r.Argument.setProposeLimit(proposeLimit);
		r.Argument.setDesc(desc);
		r.setTimeout(config.getRpcTimeout());
		raftClient.sendForWait(r).await();
		// 错误在外面处理。
		return r;
	}

	public WalkKey walkKey(Binary exclusiveStartKey, int proposeLimit, boolean desc) {
		var r = new WalkKey();
		r.Argument.setExclusiveStartKey(exclusiveStartKey);
		r.Argument.setProposeLimit(proposeLimit);
		r.Argument.setDesc(desc);
		r.setTimeout(config.getRpcTimeout());
		raftClient.sendForWait(r).await();
		// 错误在外面处理。
		return r;
	}
}
