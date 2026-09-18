package Zeze.Onz;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import demo.Module1.BKuafu;
import demo.Module1.BKuafuResult;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Builtin.Onz.FuncSaga;
import Zeze.Builtin.Onz.FuncSagaEnd;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.LongConcurrentHashMap;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * R2-M① 回归：Onz.ProcessFuncSagaEndRequest的补偿bean decode发生在sagas.remove之后，
 * decode抛异常（载荷损坏/截断或cancelClass构造失败）时上下文已删除而putIfAbsent回补
 * 被跳过——补偿永久丢失，重发FuncSagaEnd只得eSagaNotFound。
 * 修复：decode先于remove。失败时条目仍在（可重试），由cleanupTimeoutSagas（默认1小时）兜底。
 * <p>
 * 复现：手工构造OnzSaga上下文放入sagas表（内存库Application，不走网络），发送带截断
 * 补偿载荷的FuncSagaEnd(cancel=true)：修复前decode异常且上下文被删；修复后同样异常但
 * 上下文保留，随后合法的FuncSagaEnd（空载荷=无参数补偿）能真正执行补偿并清理条目。
 */
@Fast
public class TestSagaCancelDecodeKeepsContext extends AppBase {
	private static final AtomicInteger NextId = new AtomicInteger(7460);
	private static final long TID = 0x00F7_0002L; // 与其他测试不冲突的参与方tid
	private static final String ProcName = "r2m1DecodeFailSaga";

	private Application zeze;
	private Onz onz;

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
		dbConf.setDatabaseUrl("r2_m1_test_" + conf.getServerId()); // Memory库，独立url=独立存储
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		zeze = new Application("TestSagaCancelDecodeKeepsContext" + conf.getServerId(), conf);
		zeze.initialize(this);
		zeze.start();
		onz = zeze.getOnz();
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
	public void testDecodeFailKeepsContextAndRetryWorks() throws Exception {
		var cancelRuns = new AtomicInteger();
		// 直接构造stub（不经registerSaga，测试专用过程名）：
		var stub = new OnzSagaStub<>(onz, ProcName,
				(saga, argument, result) -> 0, BKuafu.class, BKuafuResult.class,
				(saga, cancelArgument) -> {
					cancelRuns.incrementAndGet();
					return 0;
				}, BKuafu.class);

		// 手工构造saga上下文（不走网络）：编码BKuafu作步骤参数，newProcedure解码入上下文。
		var argumentBean = new BKuafu();
		argumentBean.setAccount(7);
		argumentBean.setMoney(9);
		var bb = ByteBuffer.Allocate();
		argumentBean.encode(bb);
		var rpc = new FuncSaga();
		rpc.Argument.setOnzTid(TID);
		rpc.Argument.setFuncName(ProcName);
		var saga = (OnzSaga)stub.newProcedure(rpc, rpc.Argument, ByteBuffer.Wrap(bb.Bytes, 0, bb.WriteIndex));
		sagasMap().put(TID, saga);
		Assertions.assertSame(saga, sagasMap().get(TID), "测试前提：上下文已就位");

		// 截断的补偿载荷：去掉最后两个字节，Bean.decode必抛（ensureRead失败）。
		var full = new BKuafu();
		full.setAccount(1);
		full.setMoney(2);
		var cb = ByteBuffer.Allocate();
		full.encode(cb);
		var truncated = new Binary(java.util.Arrays.copyOf(cb.Bytes, Math.max(0, cb.WriteIndex - 2)));
		var bad = new FuncSagaEnd();
		bad.Argument.setOnzTid(TID);
		bad.Argument.setCancel(true);
		bad.Argument.setFuncArgument(truncated);

		// 修复前：decode异常且上下文已被remove（永久丢失，重发只得eSagaNotFound）；
		// 修复后：decode同样异常，但上下文必须保留。
		Assertions.assertThrows(Exception.class, () -> onz.ProcessFuncSagaEndRequest(bad),
				"截断载荷decode必须失败（触发条件）");
		Assertions.assertSame(saga, sagasMap().get(TID),
				"decode失败不得删除saga上下文（R2-M①：补偿丢失且重发只得eSagaNotFound）");

		// 重发合法的FuncSagaEnd（空载荷=无参数补偿，FND7-34语义）：必须真正执行补偿并清理条目。
		var good = new FuncSagaEnd();
		good.Argument.setOnzTid(TID);
		good.Argument.setCancel(true);
		var rc = onz.ProcessFuncSagaEndRequest(good);
		Assertions.assertEquals(0L, rc, "合法补偿必须成功（重试可达性正控）");
		Assertions.assertEquals(1, cancelRuns.get(), "补偿函数必须真正执行过");
		Assertions.assertNull(sagasMap().get(TID), "补偿成功后上下文必须清理");
	}

	@SuppressWarnings("unchecked")
	private LongConcurrentHashMap<OnzSaga> sagasMap() throws Exception {
		var field = Onz.class.getDeclaredField("sagas");
		field.setAccessible(true);
		return (LongConcurrentHashMap<OnzSaga>)field.get(onz);
	}
}
