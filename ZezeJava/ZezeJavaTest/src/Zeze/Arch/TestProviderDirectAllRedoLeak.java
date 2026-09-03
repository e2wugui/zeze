package Zeze.Arch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest;
import Zeze.Builtin.ProviderDirect.ModuleRedirectAllResult;
import Zeze.Config;
import Zeze.Game.ProviderDirectWithTransmit;
import Zeze.Net.Binary;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import demo.Module1.Table1;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND-A1-1回归：ProviderDirect.ProcessModuleRedirectAllRequest事务路径的out.value跨redo泄漏。
 * 过程第一轮执行成功（out.value=F1）后乐观锁冲突redo，最终轮用户handle抛异常时，
 * Procedure.call()返回Procedure.Exception，但out.value仍保留第一轮（已回滚）赋的F1。
 * 修复前不检查返回码直接使用out.value：async future上挂onResult后由第一轮的
 * 非事务副作用完成，sendResultForAsync把回滚数据以Procedure.Success上报
 * （错误数据当成功）。修复后仅提交路径上报：rc != Success时future置null，
 * 按returnCode错误上报，调用端立即收到该hash的Procedure.Exception结果。
 */
@Fast
public class TestProviderDirectAllRedoLeak extends AppBase {
	private static final AtomicInteger NextId = new AtomicInteger();

	private Application zeze;
	private ProviderApp providerApp;
	private ProviderDirectWithTransmit direct;
	private ProviderDirectService directService;
	private Table1 table1;

	@Override
	public Application getZeze() {
		return zeze;
	}

	@BeforeEach
	public void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(NextId.incrementAndGet());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("test_pdr_all_redo_" + conf.getServerId()); // Memory库，独立url=独立存储
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		zeze = new Application("TestPDRAllRedoLeak" + conf.getServerId(), conf);
		zeze.setSchemas(new demo.Schemas());
		// 表注册必须在zeze.start()（打开数据库）之前完成。
		zeze.addTable(conf.getTableConf("demo_Module1_Table1").getDatabaseName(), new Table1());
		// 哑构造 ProviderApp（zeze/redirect 就绪，成员 null——7 参构造需要 ServiceManager，
		// 本测试 SM=disable）：direct 与 directService 本地构造后回填 providerApp（同包可赋值）。
		providerApp = new ProviderApp(zeze);
		direct = new ProviderDirectWithTransmit();
		direct.providerApp = providerApp;
		directService = new ProviderDirectService("ServerDirect", zeze);
		directService.providerApp = providerApp;
		// sendResult(sender==null) 走 providerApp.providerDirectService.dispatchProtocol（进程内闭环），
		// 哑构造的该字段为 null，反射注入（final 实例字段，setAccessible 可写）。
		var serviceField = ProviderApp.class.getDeclaredField("providerDirectService");
		serviceField.setAccessible(true);
		serviceField.set(providerApp, directService);
		// dispatchProtocol 需要协议工厂与结果处理器（ProcessModuleRedirectAllResult 消费
		// manual context），真实装配由 ProviderApp 完成，这里直接注册到本地 directService。
		direct.RegisterProtocols(directService);
		zeze.initialize(this);
		zeze.start();
		table1 = (Table1)zeze.getTable("demo_Module1_Table1");
		assertNotNull(table1);
	}

	@AfterEach
	public void tearDown() throws Exception {
		providerApp = null;
		if (zeze != null) {
			zeze.stop();
			zeze = null;
		}
	}

	@Test
	public void testRedoExceptionNotReportedAsSuccess() throws Exception {
		final int hash = 7;
		final long key = 9_527L;
		// 初始值0：第一轮（将被回滚的轮次）读到0；外部事务提交改为1后，redo最终轮读到1抛异常。
		assertEquals(Procedure.Success, zeze.newProcedure(() -> {
			table1.getOrAdd(key).setInt_1(0);
			return Procedure.Success;
		}, "init").call());

		var readByProcedure = new CountDownLatch(1);
		var modified = new CountDownLatch(1);
		var modifyError = new AtomicReference<Throwable>();

		// 用户handle：第一轮读0，等外部事务提交冲突修改后，返回async future并立即由
		// "非事务副作用"完成；redo最终轮读到1抛异常，过程返回Procedure.Exception。
		var methodFullName = "TestPDRAllRedoLeak#Test";
		zeze.redirect.handles.put(methodFullName, new RedirectHandle(
				TransactionLevel.Serializable,
				(h, params) -> {
					var v = table1.getOrAdd(key).getInt_1();
					if (v != 0) // redo最终轮：数据已被外部事务修改
						throw new RuntimeException("redo with changed value: " + v);
					readByProcedure.countDown();
					if (!modified.await(10, TimeUnit.SECONDS))
						throw new AssertionError("wait modify timeout");
					var future = RedirectAllFuture.<RedirectResult>async();
					future.asyncResult(new RedirectResult()); // 模拟第一轮的非事务副作用完成future
					return future;
				},
				r -> Binary.Empty));

		// 手动注册RedirectAllContext消费结果（进程内闭环：sender==null时sendResult走dispatchProtocol）。
		var ctx = new RedirectAllContext<RedirectResult>(1, binary -> new RedirectResult());
		var sessionId = directService.addManualContextWithTimeout(ctx, 10_000);

		// 外部线程：等过程第一轮读表之后提交冲突修改。
		var modifyThread = new Thread(() -> {
			try {
				assertTrue(readByProcedure.await(10, TimeUnit.SECONDS));
				assertEquals(Procedure.Success, zeze.newProcedure(() -> {
					table1.getOrAdd(key).setInt_1(1);
					return Procedure.Success;
				}, "modify").call());
			} catch (Throwable e) {
				modifyError.set(e);
			} finally {
				modified.countDown();
			}
		});
		modifyThread.start();

		var request = new ModuleRedirectAllRequest();
		var arg = request.Argument;
		arg.setModuleId(0);
		arg.setHashCodeConcurrentLevel(1);
		arg.setSourceProvider(0);
		arg.setSessionId(sessionId);
		arg.setMethodFullName(methodFullName);
		arg.setParams(Binary.Empty);
		arg.setVersion(0);
		arg.getHashCodes().add(hash);

		assertEquals(Procedure.Success, direct.ProcessModuleRedirectAllRequest(request));

		modifyThread.join(10_000);
		if (modifyError.get() != null)
			throw new AssertionError("modify thread failed", modifyError.get());

		ctx.getFuture().await();
		// 该hash必须以过程错误码收尾，而不是把回滚轮次算出的结果当成功上报。
		var result = ctx.getAllResults().get(hash);
		assertNotNull(result, "hash result must be reported");
		assertEquals(Procedure.Exception, result.getResultCode());
	}
}
