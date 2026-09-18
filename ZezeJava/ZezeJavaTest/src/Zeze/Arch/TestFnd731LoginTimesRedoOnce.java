package Zeze.Arch;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.Procedure;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND7-31回归：Arch.Online.loginTimes在可重做事务体内裸递增——锁冲突redo整体重跑
 * 过程lambda，一次登录多计一次，虚增LoadBase的onlineNew负载上报（linkd按
 * onlineNew>maxOnlineNew跳过分配、误触发fast-report）。修复：计数挂
 * Transaction.whileCommit，仅最终成功提交执行一次。
 * <p>
 * 受控复现（同TestProviderDirectAllRedoLeak手法）：过程体读写_tonline行并调
 * loginTrigger（私有，反射直调；空事件订阅下即纯计数），第一轮读行后外部事务提交
 * 同行冲突修改迫使redo。修复前redo一轮计2次；修复后最终提交计1次。
 * online模块用哑ProviderApp装配（SM=disable、Memory库），不启动周期任务。
 */
@Fast
public class TestFnd731LoginTimesRedoOnce extends AppBase {
	private static final AtomicInteger NextId = new AtomicInteger(7440);
	private static final String ACCOUNT = "fnd731_acc";

	private Application zeze;
	private Online onlineModule;

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
		dbConf.setDatabaseUrl("fnd7_31_test_" + conf.getServerId()); // Memory库，独立url=独立存储
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		zeze = new Application("TestFnd731LoginTimesRedoOnce" + conf.getServerId(), conf);
		// 哑构造ProviderApp（设置zeze.redirect/providerApp/fake providerService），供Online装配
		new ProviderApp(zeze);
		onlineModule = new Online(this); // RegisterProtocols+RegisterZezeTables，须在start前
		zeze.initialize(this);
		zeze.start();
	}

	@AfterEach
	public void tearDown() throws Exception {
		onlineModule = null;
		if (zeze != null) {
			zeze.stop();
			zeze = null;
		}
	}

	@Test
	public void testRedoCountsLoginOnce() throws Exception {
		// 种子行：victim与modifier的冲突锚点（生产中同账号并发登录冲突_tonline行）
		assertEquals(Procedure.Success, zeze.newProcedure(() -> {
			onlineModule._tonline.getOrAdd(ACCOUNT).setLastLoginVersion(1L);
			return Procedure.Success;
		}, "seed").call());

		var readByRun1 = new CountDownLatch(1);
		var modified = new CountDownLatch(1);
		var modifyError = new AtomicReference<Throwable>();
		var runCount = new AtomicInteger();
		var loginTrigger = Online.class.getDeclaredMethod("loginTrigger", String.class, String.class);
		loginTrigger.setAccessible(true);

		// victim：仿ProcessLoginRequest的事务形态——同行读写+loginTrigger（私有，反射直调）
		var victim = TaskSpec.ofProcedure(zeze.newProcedure(() -> {
			var runs = runCount.incrementAndGet();
			var row = onlineModule._tonline.getOrAdd(ACCOUNT);
			row.setLastLoginVersion(row.getLastLoginVersion() + 1L);
			if (runs == 1) { // 仅第一轮等待冲突修改，redo轮直接重跑
				readByRun1.countDown();
				if (!modified.await(10, TimeUnit.SECONDS))
					throw new AssertionError("wait modify timeout");
			}
			assertEquals(0L, (Long)loginTrigger.invoke(onlineModule, ACCOUNT, "cid1"));
			return Procedure.Success;
		}, "victimLogin")).submitNow();

		// 外部线程（本线程）：等victim第一轮读行后提交冲突修改
		assertTrue(readByRun1.await(10, TimeUnit.SECONDS), "victim第一轮必须先读行");
		try {
			assertEquals(Procedure.Success, zeze.newProcedure(() -> {
				onlineModule._tonline.getOrAdd(ACCOUNT).setLastLoginVersion(100L);
				return Procedure.Success;
			}, "modify").call());
		} catch (Throwable t) {
			modifyError.set(t);
		} finally {
			modified.countDown();
		}
		if (modifyError.get() != null)
			throw new AssertionError("modify failed", modifyError.get());

		victim.get(15, TimeUnit.SECONDS);
		assertTrue(runCount.get() >= 2, "必须发生redo（否则场景未成立），实际轮次=" + runCount.get());
		assertEquals(1L, onlineModule.getLoginTimes(),
				"redo重跑下裸递增会多计（FND7-31：一次登录只允许计一次）");
	}
}
