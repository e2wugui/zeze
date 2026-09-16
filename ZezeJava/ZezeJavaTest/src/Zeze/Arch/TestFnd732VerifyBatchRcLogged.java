package Zeze.Arch;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.Procedure;
import Zeze.Util.EventDispatcher;
import harness.Fast;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND7-32回归：Arch.Online.VerifyBatch.perform丢弃newProcedure(...).call()返回码，
 * 批内tryRemoveLocal失败或提交阶段失败时整批回滚但无日志、批次照样清空——失败不可观测
 * （提交失败形态完全静默）。修复：接收rc，非0记error（对齐Game.VerifyBatch的
 * FND5-43+FND6-23形态），探测照发。
 * <p>
 * 复现：伪造_tlocal行（无_tonline行→login==null→走removeLocalAndTrigger），
 * localRemoveEvents订阅者返回非0使tryRemoveLocal失败；attach内存appender捕获
 * Online类日志，断言批级error出现。修复前只有批内单账号error、无批级error。
 */
@Fast
public class TestFnd732VerifyBatchRcLogged extends AppBase {
	private static final AtomicInteger NextId = new AtomicInteger();
	private static final String ACCOUNT = "fnd732_acc";
	private static final long FAIL_RC = 7L;

	private Application zeze;
	private Online onlineModule;
	private Logger onlineLogger;
	private CapturingAppender appender;

	@Override
	public Application getZeze() {
		return zeze;
	}

	static final class CapturingAppender extends AbstractAppender {
		final List<LogEvent> events = new ArrayList<>();

		CapturingAppender() {
			super("TestFnd732Capture", null, null, true, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(@NotNull LogEvent event) {
			synchronized (events) {
				events.add(event.toImmutable());
			}
		}

		boolean hasErrorContaining(@NotNull String fragment) {
			synchronized (events) {
				return events.stream().anyMatch(e ->
						e.getLevel() == Level.ERROR && e.getMessage().getFormattedMessage().contains(fragment));
			}
		}
	}

	@BeforeEach
	public void setUp() throws Exception {
		Zeze.Util.Task.tryInitThreadPool();
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(NextId.incrementAndGet());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("fnd7_32_test_" + conf.getServerId()); // Memory库，独立url=独立存储
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		zeze = new Application("TestFnd732VerifyBatchRcLogged" + conf.getServerId(), conf);
		new ProviderApp(zeze); // 哑构造，供Online装配
		onlineModule = new Online(this);
		zeze.initialize(this);
		zeze.start();

		onlineLogger = (Logger)LogManager.getLogger(Online.class);
		appender = new CapturingAppender();
		appender.start();
		onlineLogger.addAppender(appender);
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (onlineLogger != null && appender != null)
			onlineLogger.removeAppender(appender);
		if (appender != null)
			appender.stop();
		onlineModule = null;
		if (zeze != null) {
			zeze.stop();
			zeze = null;
		}
	}

	@Test
	public void testFailedBatchLogged() throws Exception {
		// 订阅者返回非0：tryRemoveLocal→removeLocalAndTrigger→localRemoveEvents失败码回传
		onlineModule.getLocalRemoveEvents().add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> FAIL_RC);

		// 伪造local行（无online行→login==null→removeLocalAndTrigger）与过期活跃时间
		assertEquals(Procedure.Success, zeze.newProcedure(() -> {
			var locals = onlineModule._tlocal.getOrAdd(ACCOUNT);
			locals.getLogins().getOrAdd("cid1").setLoginVersion(1L);
			return Procedure.Success;
		}, "seedLocal").call());
		putLocalActiveTime(ACCOUNT, System.currentTimeMillis() - 700_000); // 超过默认600s超时

		var batch = onlineModule.new VerifyBatch();
		batch.add(ACCOUNT); // 过期账号入选
		batch.perform(); // 失败批次：rc=7回传，须记批级error（perform同步完成）

		assertTrue(appender.hasErrorContaining("tryRemoveLocal fail. account=" + ACCOUNT),
				"批内单账号error必须存在（正控：日志捕获通路）");
		assertTrue(appender.hasErrorContaining("verifyLocal batch failed"),
				"批级失败必须记error（FND7-32：返回码被丢弃，失败批次静默清空不可观测）");
		assertTrue(appender.hasErrorContaining("rc=7"),
				"批级error必须携带返回码（含批大小与rc）");
	}

	/** 成功批次无批级error（兼容红线：成功路径行为不变）。 */
	@Test
	public void testSuccessfulBatchNotLoggedAsFailed() throws Exception {
		// 无local行→tryRemoveLocal返回0→批成功；不发批级error
		putLocalActiveTime(ACCOUNT + "_clean", System.currentTimeMillis() - 700_000);
		var batch = onlineModule.new VerifyBatch();
		batch.add(ACCOUNT + "_clean");
		batch.perform();
		assertTrue(!appender.hasErrorContaining("verifyLocal batch failed"),
				"成功批次不得记批级失败error");
	}

	@SuppressWarnings("unchecked")
	private void putLocalActiveTime(@NotNull String account, long timeMillis) throws Exception {
		Field field = Online.class.getDeclaredField("localActiveTimes");
		field.setAccessible(true);
		var map = (ConcurrentHashMap<String, Long>)field.get(onlineModule);
		map.put(account, timeMillis);
	}
}
