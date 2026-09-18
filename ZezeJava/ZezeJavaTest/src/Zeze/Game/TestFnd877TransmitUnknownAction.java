package Zeze.Game;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Builtin.ProviderDirect.BLoginKey;
import Zeze.Config;
import Zeze.Net.Binary;
import harness.Fast;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-77回归：processTransmit对未注册actionName曾静默丢弃（无日志无错误码）——
 * 远程目标服因滚动升级版本偏斜/各OnlineSet注册不对称未注册该action时，本源以为已路由、
 * 目标端无痕迹，与本源transmit入口的fail-fast契约及同handler未知onlineSetName记error
 * 的先例不一致。修复（对抗收窄后）：仅记error日志（Transmit为单向Protocol无错误码
 * 通道，抛异常在Task兜底下无增益），roles按size计数+截断采样防日志洪水；
 * Arch版processTransmit同型补丁（孪生）。
 */
@Fast
public class TestFnd877TransmitUnknownAction {

	// a6专属serverId段（上限16383内，避开既有测试段）。
	private static final java.util.concurrent.atomic.AtomicInteger NextServerId =
			new java.util.concurrent.atomic.AtomicInteger(16201);

	private static Application newApp(String name) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(NextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("a6_fnd877_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application(name, conf);
	}

	private static final class TestArchOnline extends Zeze.Arch.Online {
		TestArchOnline(@NotNull AppBase app) {
			super(app);
		}
	}

	private static final class CapturingAppender extends AbstractAppender {
		final List<String> messages = new CopyOnWriteArrayList<>();

		CapturingAppender(String name) {
			super(name, null, null, true, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(@NotNull LogEvent event) {
			if (event.getLevel() == Level.ERROR)
				messages.add(event.getMessage().getFormattedMessage());
		}
	}

	// 挂临时appender收集目标logger的error消息，动作完成后立即摘除（并行下按内容过滤断言）。
	private static List<String> captureErrors(Class<?> loggerClass, Runnable action) {
		var appender = new CapturingAppender("a6_fnd877_capture");
		var log = (Logger)LogManager.getLogger(loggerClass);
		log.addAppender(appender);
		try {
			action.run();
		} finally {
			log.removeAppender(appender);
		}
		return appender.messages;
	}

	/** Game版：未注册actionName记error（含actionName/roles.size），不再静默。 */
	@Test
	public void testGameUnknownActionLogged() throws Exception {
		var zeze = newApp("TestFnd877Game");
		var app = new AppBase() {
			@Override
			public Application getZeze() {
				return zeze;
			}
		};
		new ProviderApp(zeze);
		zeze.initialize(app);
		var online = new Online(app);
		zeze.start();
		try {
			var errs = captureErrors(Zeze.Game.Online.class, () ->
					online.processTransmit(101L, "a6.fnd877.unknown", List.of(161901L, 161902L), null));
			var hit = errs.stream().filter(m -> m.contains("transmit unknown action")).findFirst().orElse(null);
			Assertions.assertNotNull(hit, "未注册action必须记error（修复前静默丢弃）");
			Assertions.assertTrue(hit.contains("a6.fnd877.unknown"), "日志必须含actionName");
			Assertions.assertTrue(hit.contains("roles.size=2"), "日志必须含roles数量");
		} finally {
			try {
				zeze.stop();
			} catch (Exception ignored) {
			}
		}
	}

	/** Game版护栏：已注册action正常执行，不产生unknown日志。 */
	@Test
	public void testGameKnownActionStillWorks() throws Exception {
		var zeze = newApp("TestFnd877GameKnown");
		var app = new AppBase() {
			@Override
			public Application getZeze() {
				return zeze;
			}
		};
		new ProviderApp(zeze);
		zeze.initialize(app);
		var online = new Online(app);
		zeze.start();
		try {
			online.getTransmitActions().put("a6.fnd877.known", (sender, target, parameter) -> 0);
			var errs = captureErrors(Zeze.Game.Online.class, () ->
					online.processTransmit(101L, "a6.fnd877.known", List.of(161903L), null));
			Assertions.assertTrue(errs.stream().noneMatch(m -> m.contains("transmit unknown action")),
					"已注册action不得产生unknown日志");
		} finally {
			try {
				zeze.stop();
			} catch (Exception ignored) {
			}
		}
	}

	/** Arch版孪生：未注册actionName记error（含action/targets.size）。 */
	@Test
	public void testArchUnknownActionLogged() throws Exception {
		var zeze = newApp("TestFnd877Arch");
		var app = new AppBase() {
			@Override
			public Application getZeze() {
				return zeze;
			}
		};
		new ProviderApp(zeze);
		zeze.initialize(app);
		var archOnline = new TestArchOnline(app); // 表注册纯内存，须在start前构造
		zeze.start();
		try {
			var method = Zeze.Arch.Online.class.getDeclaredMethod("processTransmit",
					String.class, String.class, String.class, Collection.class, Binary.class);
			method.setAccessible(true);
			var targets = List.of(new BLoginKey("a6t1", "a6c1"), new BLoginKey("a6t2", "a6c2"));
			var errs = captureErrors(Zeze.Arch.Online.class, () -> {
				try {
					method.invoke(archOnline, "a6acc", "a6cid", "a6.fnd877.unknown", targets, null);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			var hit = errs.stream().filter(m -> m.contains("processTransmit unknown action")).findFirst().orElse(null);
			Assertions.assertNotNull(hit, "Arch版未注册action必须记error（孪生，修复前静默丢弃）");
			Assertions.assertTrue(hit.contains("a6.fnd877.unknown"), "日志必须含action名");
			Assertions.assertTrue(hit.contains("targets.size=2"), "日志必须含targets数量");
		} finally {
			try {
				zeze.stop();
			} catch (Exception ignored) {
			}
		}
	}
}
