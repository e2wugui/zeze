package Zeze.Onz;

import java.util.concurrent.atomic.AtomicInteger;

import demo.Module1.BKuafu;
import demo.Module1.BKuafuResult;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Builtin.Onz.FuncSaga;
import Zeze.Builtin.Onz.FuncSagaEnd;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.EmptyBean;
import Zeze.Util.LongConcurrentHashMap;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * R3-C D①（M②）回归：FuncSagaEnd 可先于 FuncSaga 被参与方处理（两者同为 Normal 派发，
 * 共享线程池不保证同连接处理顺序）——此时 sagas 查无上下文，应答 eSagaNotFound，
 * 迟到的 FuncSaga 随后注册并执行业务，该次补偿被静默吞掉且协调者不再重发。
 * 修复（协调者侧）：cancelSaga 对 rpc 层失败（超时）步骤的 eSagaNotFound 单次延迟重试
 * （见 TestR3cSagaCancelNotFoundRetry）；参与方不新增"尚未注册"错误码——无法与"已清理"
 * 区分，且错误码常量在生成代码。本用例受控乱序注入驱动参与方两个处理器的错误顺序，
 * 验证窗口的存在性与重试可达性：先到的 FuncSagaEnd 得 eSagaNotFound（窗口），FuncSaga
 * 注册执行业务后，再发的 FuncSagaEnd 能真正补偿。
 */
@Fast
public class TestR3cSagaEndBeforeSagaRegistration extends AppBase {
	private static final AtomicInteger NextId = new AtomicInteger(7450);
	private static final String ProcName = "r3cD1SagaEndBeforeSaga";

	private Application zeze;
	private Onz onz;
	final AtomicInteger bizRuns = new AtomicInteger();
	final AtomicInteger cancelRuns = new AtomicInteger();

	@Override
	public Application getZeze() {
		return zeze;
	}

	@BeforeEach
	public void setUp() throws Exception {
		Zeze.Util.Task.tryInitThreadPool();
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(NextId.incrementAndGet());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("r3c_d1_test_" + conf.getServerId()); // Memory库，独立url=独立存储
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		zeze = new Application("TestR3cSagaEndBeforeSaga" + conf.getServerId(), conf);
		zeze.initialize(this);
		zeze.start();
		onz = zeze.getOnz();
		onz.registerSaga(ProcName,
				(saga, argument, result) -> {
					bizRuns.incrementAndGet();
					return 0;
				},
				(saga, cancelArgument) -> {
					cancelRuns.incrementAndGet();
					return 0;
				}, BKuafu.class, BKuafuResult.class, EmptyBean.class);
	}

	@AfterEach
	public void tearDown() throws Exception {
		onz = null;
		if (zeze != null) {
			zeze.stop();
			zeze = null;
		}
	}

	@Test
	public void testEndBeforeSagaRegistration() throws Exception {
		var tid = ((long)NextId.get() << 32) | 0xD1L; // 与其他测试不冲突的参与方tid

		// 受控乱序：FuncSagaEnd 先到（FuncSaga 尚未注册上下文）。
		var endFirst = new FuncSagaEnd();
		endFirst.Argument.setOnzTid(tid);
		endFirst.Argument.setCancel(true);
		Assertions.assertEquals(AbstractOnz.eSagaNotFound,
				Zeze.IModule.getErrorCode(onz.ProcessFuncSagaEndRequest(endFirst)),
				"乱序窗口：注册前的FuncSagaEnd必须应答eSagaNotFound（线上为moduleId组合值，解码后断言；协调者据此单次延迟重试）");

		// 迟到的 FuncSaga：注册上下文并执行业务。
		var argumentBean = new BKuafu();
		argumentBean.setAccount(1);
		argumentBean.setMoney(2);
		var bb = ByteBuffer.Allocate();
		argumentBean.encode(bb);
		var sagaRpc = new FuncSaga();
		sagaRpc.Argument.setOnzTid(tid);
		sagaRpc.Argument.setFuncName(ProcName);
		sagaRpc.Argument.setFuncArgument(new Zeze.Net.Binary(java.util.Arrays.copyOf(bb.Bytes, bb.WriteIndex)));
		var rc = onz.ProcessFuncSagaRequest(sagaRpc);
		Assertions.assertEquals(0L, rc, "迟到FuncSaga的业务必须成功执行");
		Assertions.assertEquals(1, bizRuns.get(), "业务必须真正执行过");
		Assertions.assertNotNull(sagasMap().get(tid), "业务成功后上下文留存（等待补偿/结束）");

		// 重试的 FuncSagaEnd（协调者延迟重发到达）：必须真正补偿并清理。
		var endRetry = new FuncSagaEnd();
		endRetry.Argument.setOnzTid(tid);
		endRetry.Argument.setCancel(true);
		Assertions.assertEquals(0L, onz.ProcessFuncSagaEndRequest(endRetry), "重试的补偿必须成功");
		Assertions.assertEquals(1, cancelRuns.get(), "补偿函数必须真正执行过");
		Assertions.assertNull(sagasMap().get(tid), "补偿成功后上下文必须清理");
	}

	@SuppressWarnings("unchecked")
	private LongConcurrentHashMap<OnzSaga> sagasMap() throws Exception {
		var field = Onz.class.getDeclaredField("sagas");
		field.setAccessible(true);
		return (LongConcurrentHashMap<OnzSaga>)field.get(onz);
	}
}
