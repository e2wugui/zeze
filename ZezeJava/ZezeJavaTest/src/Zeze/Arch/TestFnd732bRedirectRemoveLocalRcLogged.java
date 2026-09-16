package Zeze.Arch;

import java.util.ArrayList;
import java.util.List;
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
 * FND7-32同型新缺陷回归：Arch.Online.redirectRemoveLocal丢弃
 * newProcedure(...).call()返回码——tryRemoveLocal失败（批内单账号error尚有）或
 * 提交阶段失败（raft/cache-sync异常等，完全无日志）时，目标服的local/eLinkBroken
 * 残留静默，只能等其verifyLocal周期（默认10分钟）自愈。FND7-32已修VerifyBatch.perform
 * （afc4d40fa），此处对齐同一判例：接收rc，非0记error（含rc与上下文），成功路径不变。
 * 参照Game.Online.redirectRemoveLocal已有的FND5-43同口径形态。
 * <p>
 * 复现：伪造_tlocal行（无_tonline行→login==null→走removeLocalAndTrigger），
 * localRemoveEvents订阅者返回非0使tryRemoveLocal失败；attach内存appender捕获
 * Online类日志，直接调redirectRemoveLocal（protected+同包可及），断言error出现。
 * 修复前该失败路径完全无error（removeLocalAndTrigger不记日志，rc在
 * redirectRemoveLocal处被丢弃），过程回滚local行残留。
 */
@Fast
public class TestFnd732bRedirectRemoveLocalRcLogged extends AppBase {
	private static final AtomicInteger NextId = new AtomicInteger();
	private static final String ACCOUNT = "fnd732b_acc";
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
			super("TestFnd732bCapture", null, null, true, Property.EMPTY_ARRAY);
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
		dbConf.setDatabaseUrl("fnd7_32b_test_" + conf.getServerId()); // Memory库，独立url=独立存储
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		zeze = new Application("TestFnd732bRedirectRemoveLocalRcLogged" + conf.getServerId(), conf);
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
	public void testFailedRedirectRemoveLocalLogged() throws Exception {
		// 订阅者返回非0：tryRemoveLocal→removeLocalAndTrigger→localRemoveEvents失败码回传
		onlineModule.getLocalRemoveEvents().add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> FAIL_RC);

		// 伪造local行（无online行→login==null→removeLocalAndTrigger）
		assertEquals(Procedure.Success, zeze.newProcedure(() -> {
			var locals = onlineModule._tlocal.getOrAdd(ACCOUNT);
			locals.getLogins().getOrAdd("cid1").setLoginVersion(1L);
			return Procedure.Success;
		}, "seedLocal").call());

		onlineModule.redirectRemoveLocal(1, ACCOUNT); // 目标服执行体：失败必须记error（FND7-32同型）

		// 正控：rc=7导致过程回滚，local行必须仍在（证明失败确实发生，error缺失才有意义）
		assertEquals(Procedure.Success, zeze.newProcedure(() ->
				onlineModule._tlocal.get(ACCOUNT) != null ? Procedure.Success : 1L,
				"checkLocalRemains").call());
		assertTrue(appender.hasErrorContaining("redirectRemoveLocal failed"),
				"redirectRemoveLocal失败必须记error（返回码原先被丢弃，失败完全静默，仅能等verifyLocal周期自愈）");
		assertTrue(appender.hasErrorContaining("rc=7"),
				"失败error必须携带返回码");
	}

	/** 成功路径无失败error（兼容红线：成功行为不变）。 */
	@Test
	public void testSuccessfulRedirectRemoveLocalNotLoggedAsFailed() throws Exception {
		onlineModule.redirectRemoveLocal(1, ACCOUNT + "_clean"); // 无local行→tryRemoveLocal返回0
		assertTrue(!appender.hasErrorContaining("redirectRemoveLocal failed"),
				"成功路径不得记失败error");
	}
}
