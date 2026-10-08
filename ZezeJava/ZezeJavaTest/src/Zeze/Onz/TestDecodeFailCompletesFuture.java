package Zeze.Onz;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import Zeze.Net.Binary;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Onz.OnzTransaction;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static Zeze.Onz.GcOnzFastStubSupport.*;

/**
 * FND21 GC-C02 回归：OnzAgent.callProcedureAsync/callSagaAsync在应答回调内裸decode——
 * 回调式发送没有框架future，回调是局部TCS的唯一完成者；真实应答消费rpc上下文后超时兜底
 * 的双参remove必失败直接return（Rpc.schedule），不会重放回调。code==0但decode抛出（载荷
 * 结构性损坏/空载荷decode不足1字节）时异常在setResult之前冲出回调被派发框架吞掉，future
 * 永pending：业务的future.get()（无超时惯例）永久挂起，perform卡死且零可观测性——同文件
 * sendFlushReady对Send-false路径已自证"必须完成future"，唯code==0分支的decode无保护。
 * 修复：decode失败同样以异常完成future（对齐Rpc.handle"resultCode先于future"的先立结果
 * 形态），业务走正常失败/补偿链而不是无声挂死。
 * 形态：@Fast自包含（进程内SM+桩参与方注册"Onz"、两参独立SM构造器——顺带回归非shared
 * 模式零变化），serverId 892段。修复前红：future.get(5s)超时（永pending）；perform路径
 * 挂死由@Timeout拦截。
 */
@Fast
public class TestDecodeFailCompletesFuture {
	// 892段：serverId=892，SM端口51893；桩参与方端口按测试方法错开（31894/31895/51896，
	// 注册缺省"Onz"——两参构造器路径）：Service.stop()的解绑是异步的，同端口跨方法立即
	// 重绑会撞TIME_WAIT窗口（FND20一脚手架每类一方法故未暴露）。
	private static final int ServerId = 892;
	private static final int SmPort = 31893;
	private static final String Cluster = "zeze892";
	private static final String ProcedureName = "decodeFailProc";
	private static final String SagaName = "decodeFailSaga";

	private GcOnzFastStubSupport.OnzFixture fixture;

	private void startFixture(int stubPort) throws Exception {
		fixture = startNonSharedOnzServer(ServerId, SmPort, java.nio.file.Files.createTempDirectory("decodeFail"),
				Cluster, 891,
				new StubSpec("Onz", "894", stubPort, TestDecodeFailCompletesFuture::installStubHandles));
	}

	@AfterEach
	public void after() {
		if (fixture != null) // startFixture半途失败时fixture尚未创建（close幂等口径）
			fixture.close();
	}

	/**
	 * 核心红测（procedure）：code==0但result.decode抛出——future必须以异常完成（消息锚定
	 * "call result decode fail"+原异常）。修复前：get(5s)超时红（永pending，超时兜底已被
	 * 真实应答消费不重放回调）。
	 */
	@Test
	@Timeout(90)
	public void testProcedureDecodeFailCompletesFutureExceptionally() throws Exception {
		startFixture(31894);
		var txn = new DecodeFailTxn();
		txn.setOnzServer(fixture.onzServer);
		var future = txn.callProcedureAsync(Cluster, ProcedureName, EmptyBean.Data.instance, new EvilDecodeResult());
		assertDecodeFailCompletes(future, ProcedureName);
	}

	/** 同型（saga）：callSagaAsync的回调同修，cancelSaga的无超时saga.get()同样依赖完成性。 */
	@Test
	@Timeout(90)
	public void testSagaDecodeFailCompletesFutureExceptionally() throws Exception {
		startFixture(31895);
		var txn = new DecodeFailTxn();
		txn.setOnzServer(fixture.onzServer);
		var future = txn.callSagaAsync(Cluster, SagaName, EmptyBean.Data.instance, new EvilDecodeResult());
		assertDecodeFailCompletes(future, SagaName);
	}

	/**
	 * 端到端（perform路径）：业务perform内无超时future.get()（KuafuTransaction惯例）——
	 * decode失败必须让OnzServer.perform以Procedure.Exception快速返回（rollback链可走），
	 * 而不是永久挂起（案卷机制：commitIndex无记录、redoTimer看不见、hangWarnedTids不warn）。
	 * 修复前红：perform永不返回，@Timeout(SEPARATE_THREAD)拦截（挂死即案卷机制本身）。
	 */
	@Test
	@Timeout(value = 90, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
	public void testPerformPathFailsFastInsteadOfHanging() throws Exception {
		startFixture(31896);
		var txn = new DecodeFailTxn();
		txn.setOnzServer(fixture.onzServer); // perform契约：业务自设（createTransaction同型）
		var rc = fixture.onzServer.perform(txn);
		Assertions.assertEquals(Procedure.Exception, rc,
				"decode失败必须走perform的catch→rollback失败链快速返回，不得无声挂死");
	}

	private static void assertDecodeFailCompletes(Zeze.Util.TaskCompletionSource<?> future, String name) {
		var ex = Assertions.assertThrows(CompletionException.class,
				() -> future.get(5, TimeUnit.SECONDS),
				"decode失败必须以异常完成future（修复前：回调是唯一完成者且超时兜底被真实应答短路，永pending→此处超时红）");
		var cause = ex.getCause();
		Assertions.assertTrue(cause instanceof RuntimeException
						&& cause.getMessage().contains("call result decode fail: " + name),
				"异常消息锚定过程名与decode失败事实: " + cause);
		var root = cause.getCause();
		Assertions.assertTrue(root != null && root.getMessage() != null
						&& root.getMessage().contains(EvilDecodeResult.Marker),
				"原始decode异常作为cause保留（排障需要真实原因）: " + root);
	}

	// 桩参与方协议装配：FuncProcedure/FuncSaga应答0+合法EmptyBean载荷（协调者侧
	// EvilDecodeResult.decode必抛——模拟双端result定义不兼容升级的结构性损坏）；Rollback
	// 补齐应答（perform路径的rollback链会发来，未注册工厂会变未知协议噪音）。
	private static void installStubHandles(Service stub) {
		stub.AddFactoryHandle(Zeze.Builtin.Onz.FuncProcedure.TypeId_,
				new Service.ProtocolFactoryHandle<>(Zeze.Builtin.Onz.FuncProcedure::new, r -> {
					r.Result.setFuncResult(emptyResultPayload());
					r.SendResult(); // 框架仅非0时回发错误码（TaskSpec契约），0需显式应答
					return 0;
				}, Zeze.Transaction.TransactionLevel.None, Zeze.Transaction.DispatchMode.Direct));
		stub.AddFactoryHandle(Zeze.Builtin.Onz.FuncSaga.TypeId_,
				new Service.ProtocolFactoryHandle<>(Zeze.Builtin.Onz.FuncSaga::new, r -> {
					r.Result.setFuncResult(emptyResultPayload());
					r.SendResult();
					return 0;
				}, Zeze.Transaction.TransactionLevel.None, Zeze.Transaction.DispatchMode.Direct));
		stub.AddFactoryHandle(Zeze.Builtin.Onz.Rollback.TypeId_,
				new Service.ProtocolFactoryHandle<>(Zeze.Builtin.Onz.Rollback::new, r -> {
					r.SendResult();
					return 0;
				}, Zeze.Transaction.TransactionLevel.None, Zeze.Transaction.DispatchMode.Direct));
	}

	private static Binary emptyResultPayload() {
		var bb = ByteBuffer.Allocate();
		EmptyBean.Data.instance.encode(bb);
		return new Binary(bb);
	}

	/** 应答载荷decode必抛的result桩（模拟字段类型跨版本改义——decode对未知字段宽容、类型改义即抛）。 */
	private static final class EvilDecodeResult extends Zeze.Transaction.Data {
		static final String Marker = "decode-fail evil decode";

		@Override
		public void decode(Zeze.Serialize.IByteBuffer bb) {
			throw new IllegalStateException(Marker);
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteByte(0);
		}

		@Override
		public void assign(Zeze.Transaction.Bean b) {
		}

		@Override
		public Zeze.Transaction.Bean toBean() {
			return EmptyBean.instance;
		}

		@Override
		public void reset() {
		}

		@Override
		public Zeze.Transaction.Data copy() {
			return new EvilDecodeResult();
		}
	}

	/** 直驱测试用事务：perform内无超时get（业务惯例形态）；直测路径不调用perform本身。 */
	private static final class DecodeFailTxn extends OnzTransaction<EmptyBean.Data, EmptyBean.Data> {
		@Override
		protected long perform() throws Exception {
			var future = callProcedureAsync(Cluster, ProcedureName, EmptyBean.Data.instance, new EvilDecodeResult());
			future.get(); // 无超时（KuafuTransaction惯例）——修复前在此永久挂起
			return 0;
		}
	}
}
