package Zeze.Onz;

import Zeze.Application;
import Zeze.Builtin.Onz.BFuncProcedure;
import Zeze.Builtin.Onz.Commit;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Transaction.EmptyBean;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * FND25 onz-01（裁定：信号补强）回归：参与方超时回滚（哨兵在位）后迟到的 Commit——
 * "无故障假成功"分歧的唯一协调者侧可见信号。修复前应答 0（协调者按成功收场删记录，
 * 调用方完全无感）；修复后应答 eDivergence（协调者 commit 特判 error 后按成功收场，
 * 不置 commitFail 不重发——哨兵随 remove 一次性消耗，重发命中 null 走幂等应答 0，
 * 不成环）。
 */
@Fast
public class TestOnzCommitDivergenceSignal {
	// 独立 serverId+url：@Fast 类并行时避免本地缓存目录与内存库互撞。
	private static final int SERVER_ID = FastServerIds.TEST_ONZ_COMMIT_DIVERGENCE_SIGNAL;

	private Application app;
	private Onz onz;

	@BeforeEach
	public void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(SERVER_ID);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("fnd25_onz01_" + SERVER_ID);
		conf.getDatabaseConfMap().put("", dbConf);
		app = new Application("TestOnzCommitDivergenceSignal", conf);
		app.start();
		onz = new Onz(app); // 无 Onz 服务配置：纯内存结构，无网络
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (app.getStartState() != Application.StartState.eStopped)
			app.stop();
	}

	/** 哨兵命中的迟到 Commit 必须应答 eDivergence（协调者侧分歧信号），重发幂等应答 0（不成环）。 */
	@Test
	public void testSentinelCommitAnswersDivergenceThenIdempotentZero() throws Exception {
		var tid = 7001L;
		var stub = new OnzProcedureStub<EmptyBean, EmptyBean>(
				onz, "Fnd25Onz01." + tid, (p, a, r) -> 0L, EmptyBean.class, EmptyBean.class);
		var funcArgument = new BFuncProcedure.Data();
		funcArgument.setOnzTid(tid);
		funcArgument.setFlushMode(AbstractOnz.eFlushAsync);
		var p = new OnzProcedure(null, funcArgument, stub, new EmptyBean(), new EmptyBean());

		onz.markReadyProcedure(p);
		Assertions.assertTrue(onz.markTimeoutRolledBack(p), "哨兵置位前置（超时自愈回滚）");

		var r = new Commit();
		r.Argument.setOnzTid(tid);
		var rc = onz.ProcessCommitRequest(r);
		Assertions.assertEquals(AbstractOnz.eDivergence, IModule.getErrorCode(rc),
				"哨兵命中的迟到Commit必须应答eDivergence（修复前应答0，协调者对分歧零信号）");

		// 哨兵随 remove 一次性消耗：重发命中 null 走幂等应答 0——eDivergence 不会导致
		// 协调者 redo 无限重发（FND5-44 顾虑在本形态不成立）。
		var r2 = new Commit();
		r2.Argument.setOnzTid(tid);
		Assertions.assertEquals(0, onz.ProcessCommitRequest(r2), "重发必须幂等应答0（不成环论证）");
	}
}
