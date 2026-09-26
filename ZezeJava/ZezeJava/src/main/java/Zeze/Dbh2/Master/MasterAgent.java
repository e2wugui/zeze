package Zeze.Dbh2.Master;

import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Master.BRegisterResult;
import Zeze.Builtin.Dbh2.Master.CheckFreeManager;
import Zeze.Builtin.Dbh2.Master.CreateBucket;
import Zeze.Builtin.Dbh2.Master.CreateDatabase;
import Zeze.Builtin.Dbh2.Master.CreateSplitBucket;
import Zeze.Builtin.Dbh2.Master.CreateTable;
import Zeze.Builtin.Dbh2.Master.EndMove;
import Zeze.Builtin.Dbh2.Master.EndSplit;
import Zeze.Builtin.Dbh2.Master.GetBuckets;
import Zeze.Builtin.Dbh2.Master.Register;
import Zeze.Builtin.Dbh2.Master.ReportBucketCount;
import Zeze.Builtin.Dbh2.Master.ReportLoad;
import Zeze.Builtin.Dbh2.Master.SetDbh2Ready;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Net.Connector;
import Zeze.Net.ProtocolHandle;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action0;
import Zeze.Util.Action3;
import Zeze.Util.OutObject;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MasterAgent extends AbstractMasterAgent {
	private static final Logger logger = LogManager.getLogger(MasterAgent.class);
	public static final String eServiceName = "Zeze.Dbh2.Master.Agent";
	private final Service service;
	private ProtocolHandle<CreateBucket> createBucketHandle;

	public MasterAgent(Config config) {
		service = new Service(config);
		RegisterProtocols(service);
	}

	public MasterAgent(Config config, ProtocolHandle<CreateBucket> handle, Service service) {
		this.service = service;
		this.createBucketHandle = handle;
		RegisterProtocols(this.service);
	}

	public Service getService() {
		return service;
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

	public void createDatabase(String database) {
		var r = new CreateDatabase();
		r.Argument.setDatabase(database);
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("createDatabase error=" + IModule.getErrorCode(r.getResultCode()));
	}

	public boolean createTable(String database, String table, OutObject<MasterTable.Data> out) {
		var r = new CreateTable();
		r.Argument.setDatabase(database);
		r.Argument.setTable(table);
		r.SendForWait(service.GetSocket(), 30_000).await();
		out.value = r.Result;
		var rc = r.getResultCode();
		if (rc != 0 && rc != errorCode(eTableIsNew))
			throw new RuntimeException("fail module=" + IModule.getModuleId(rc) + " code=" + IModule.getErrorCode(rc));
		return IModule.getErrorCode(rc) == eTableIsNew;
	}

	public void createTableAsync(String database, String table, Action3<Integer, Boolean, MasterTable.Data> callback) {
		var r = new CreateTable();
		r.Argument.setDatabase(database);
		r.Argument.setTable(table);
		// Send失败（连接空窗GetSocket()==null）不建rpc上下文、不派发回调：必须显式回调暂时性失败码，
		// 否则Dbh2Table.ready永不完成，waitReady永久挂起（旁路createTableWithRetry的预算）。
		// eTooFewManager=资源暂时不足类码：走白名单重试，autoReconnect恢复后照常建表。
		if (!r.Send(service.GetSocket(), (p) -> {
			var rc = r.getResultCode();
			if (rc == 0) {
				callback.run(0, false, r.Result);
			} else {
				var error = IModule.getErrorCode(rc);
				if (error == eTableIsNew)
					callback.run(0, true, r.Result);
				else
					callback.run(error, false, null);
			}
			return 0;
		}, 60_000)) {
			logger.warn("createTableAsync send fail (no master connection). db={} table={}", database, table);
			try {
				callback.run(eTooFewManager, false, null);
			} catch (Exception e) {
				logger.error("createTableAsync callback error", e);
			}
		}
	}

	public MasterTable.Data getBuckets(String database, String table) {
		var r = new GetBuckets();
		r.Argument.setDatabase(database);
		r.Argument.setTable(table);
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("getBuckets error=" + IModule.getErrorCode(r.getResultCode()));
		return r.Result;
	}

	public BRegisterResult.Data register(String dbh2RaftAcceptorName, int port, int bucketCount) {
		var r = new Register();
		r.Argument.setDbh2RaftAcceptorName(dbh2RaftAcceptorName);
		r.Argument.setPort(port);
		r.Argument.setBucketCount(bucketCount);
		r.SendForWait(service.GetSocket()).await(); // 这里不能等待，现在直接在网络线程中运行。
		if (r.getResultCode() != 0)
			throw new RuntimeException("register error=" + IModule.getErrorCode(r.getResultCode()));
		return r.Result;
	}

	public void setDbh2Ready() {
		var r = new SetDbh2Ready();
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("setDbh2Ready error=" + IModule.getErrorCode(r.getResultCode()));
	}

	@Override
	protected long ProcessCreateBucketRequest(CreateBucket r) throws Exception {
		if (null == createBucketHandle)
			return Procedure.NotImplement;
		return createBucketHandle.handle(r);
	}

	public static class Service extends Zeze.Net.Service {
		public Service(Config config) {
			super(eServiceName, config);
			setNoProcedure(true);
		}
	}

	public void reportLoad(double load) {
		var r = new ReportLoad();
		r.Argument.setLoad(load);
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("reportLoad error=" + IModule.getErrorCode(r.getResultCode()));
	}

	public BBucketMeta.Data createSplitBucket(BBucketMeta.Data bucket) {
		var r = new CreateSplitBucket();
		r.Argument = bucket;
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("error=" + IModule.getErrorCode(r.getResultCode()));
		return r.Result;
	}

	public void reportBucketCount(int count) {
		var r = new ReportBucketCount();
		r.Argument.setCount(count);
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("error=" + IModule.getErrorCode(r.getResultCode()));
	}

	// end重试间隔（对齐GA-D04 createTableRetryBudgetMs形态）：非final便于测试收缩。
	static volatile long endRetryDelayMs = 30_000L;

	public void endMoveWithRetryAsync(BBucketMeta.Data to) {
		endMoveWithRetryAsync(to, null);
	}

	/**
	 * @param onSettled settle终局回调（rc==0成功，或eSplittingBucketNotFound=已结算证据），
	 *                  至多执行一次；null=无回调（原语义）。GA-D01 A1：源桶Dbh2借此追加
	 *                  pending-settle清除日志（LogClearPendingSettle），进程在终局前死亡则
	 *                  标志留存，下轮leader-ready补发终局后同样清除。
	 */
	public void endMoveWithRetryAsync(BBucketMeta.Data to, Action0 onSettled) {
		var r = new EndMove();
		r.Argument.setTo(to);
		if (!r.Send(service.GetSocket(), (p) -> {
			if (p.getResultCode() != 0) {
				// eSplittingBucketNotFound=首次settle已成功、仅响应丢失（重发时splitting表中已无该桶）：
				// 幂等完成的证据，视为成功停止重试。
				if (IModule.getErrorCode(p.getResultCode()) != eSplittingBucketNotFound) {
					logger.warn("endMove fail, retry later. error={} to={}",
							IModule.getErrorCode(p.getResultCode()), to);
					TaskSpec.ofAction(() -> endMoveWithRetryAsync(to, onSettled)).schedule(endRetryDelayMs);
				} else {
					logger.info("endMove already settled. to={}", to);
					runSettled(onSettled);
				}
			} else
				runSettled(onSettled);
			return 0;
		})) {
			TaskSpec.ofAction(() -> endMoveWithRetryAsync(to, onSettled)).schedule(endRetryDelayMs);
		}
	}

	public void endSplitWithRetryAsync(BBucketMeta.Data from, BBucketMeta.Data to) {
		endSplitWithRetryAsync(from, to, null);
	}

	/** 同{@link #endMoveWithRetryAsync(BBucketMeta.Data, Action0)}的onSettled形态。 */
	public void endSplitWithRetryAsync(BBucketMeta.Data from, BBucketMeta.Data to, Action0 onSettled) {
		var r = new EndSplit();
		r.Argument.setFrom(from);
		r.Argument.setTo(to);
		if (!r.Send(service.GetSocket(), (p) -> {
			if (p.getResultCode() != 0) {
				// 同endMove：eSplittingBucketNotFound是幂等完成的证据，停止重试。
				if (IModule.getErrorCode(p.getResultCode()) != eSplittingBucketNotFound) {
					logger.warn("endSplit fail, retry later. error={} from={} to={}",
							IModule.getErrorCode(p.getResultCode()), from, to);
					TaskSpec.ofAction(() -> endSplitWithRetryAsync(from, to, onSettled)).schedule(endRetryDelayMs);
				} else {
					logger.info("endSplit already settled. from={} to={}", from, to);
					runSettled(onSettled);
				}
			} else
				runSettled(onSettled);
			return 0;
		})) {
			TaskSpec.ofAction(() -> endSplitWithRetryAsync(from, to, onSettled)).schedule(endRetryDelayMs);
		}
	}

	private static void runSettled(Action0 onSettled) {
		if (null == onSettled)
			return;
		try {
			onSettled.run();
		} catch (Exception e) {
			// 回调失败不影响settle终局语义（清除日志的收敛由leader-ready补发兜底）。
			logger.error("endSettled callback fail", e);
		}
	}

	public int checkFreeManager() {
		var r = new CheckFreeManager();
		r.SendForWait(service.GetSocket()).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("error=" + IModule.getErrorCode(r.getResultCode()));
		return r.Result.getCount();
	}
}
