package UnitTest.Zeze.Services.ServiceManager;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BServiceInfosVersion;
import Zeze.Services.ServiceManager.ExporterConfig;
import Zeze.Services.ServiceManager.ExporterNginxHttp;
import harness.Fast;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-61：ExporterNginxHttp 的 -version 选桶可观测性。
 * -version 是选桶语义（SM 按 BServiceInfo 注册 version 分桶，导出只取指定桶），桶不匹配时
 * 原实现静默 return——dyups 配置停在旧值且无任何留痕。
 * 修复后：选定桶为空且其他桶非空时 warn 一次（列出非空桶号，提示检查 -version）；
 * 全空（服务全部下线的过渡态）保持静默。
 * 用捕获型 appender（挂独立 LoggerConfig，不干扰并行测试）断言 warn；构造时 -url 指向
 * 不可达回环地址也无妨——选桶为空路径在发 HTTP 前即返回。
 */
@Fast
public class TestFnd761ExporterNginxHttpVersionBucket {

	/** 捕获型appender：挂到指定logger名的独立LoggerConfig（不干扰并行测试），断言warn内容。 */
	private static final class CapturingAppender extends AbstractAppender {
		private final List<String> messages = new CopyOnWriteArrayList<>();

		CapturingAppender() {
			super("TestFnd761Capture", null, null, true, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(LogEvent event) {
			messages.add(event.getMessage().getFormattedMessage());
		}
	}

	private static CapturingAppender attachCapture(String loggerName) {
		var appender = new CapturingAppender();
		appender.start();
		var ctx = (LoggerContext)LogManager.getContext(false);
		var config = ctx.getConfiguration();
		var loggerConfig = new LoggerConfig(loggerName, Level.WARN, false);
		loggerConfig.addAppender(appender, Level.WARN, null);
		config.addLogger(loggerName, loggerConfig);
		ctx.updateLoggers(config);
		return appender;
	}

	private static void detachCapture(String loggerName, CapturingAppender appender) {
		var ctx = (LoggerContext)LogManager.getContext(false);
		var config = ctx.getConfiguration();
		config.removeLogger(loggerName);
		ctx.updateLoggers(config);
		appender.stop();
	}

	private static ExporterNginxHttp newExporterWithVersion0() {
		var props = new Properties();
		props.put("-url", "http://127.0.0.1:1/upstream/");
		// -version 未配置 → 默认桶0；下方订阅结果只有桶7 → 选桶失配
		return new ExporterNginxHttp(new ExporterConfig(props, null));
	}

	/** 选定桶(0)为空且桶7非空：静默返回（不发HTTP），warn恰好一次且含桶号提示。 */
	@Test
	public void testWarnExactlyOnceWhenSelectedBucketEmpty() {
		var exporter = newExporterWithVersion0();
		var appender = attachCapture(ExporterNginxHttp.class.getName());
		try {
			var all = new BServiceInfosVersion();
			var bucket7 = all.getOrAddInfos(7L); // 服务注册在 version=7 桶
			bucket7.getSortedIdentities().add(new BServiceInfo("Game.Linkd", "1", 7L, "127.0.0.1", 8089, null));

			Assertions.assertDoesNotThrow(() -> exporter.exportAll("Game.Linkd", all));
			Assertions.assertEquals(1, appender.messages.size(), "warn恰好一次: " + appender.messages);
			var msg = appender.messages.get(0);
			Assertions.assertTrue(msg.contains("-version=0") && msg.contains("[7]"),
					"warn须含选定桶与非空桶号: " + msg);

			// 第二轮事件：不重复warn（事件风暴下不刷屏）
			Assertions.assertDoesNotThrow(() -> exporter.exportAll("Game.Linkd", all));
			Assertions.assertEquals(1, appender.messages.size(), "第二轮不重复warn");
		} finally {
			detachCapture(ExporterNginxHttp.class.getName(), appender);
			exporter.close(); // 释放内部executor线程
		}
	}

	/** 全部桶为空（服务全部下线的过渡态）：保持静默不warn。 */
	@Test
	public void testSilentWhenAllBucketsEmpty() {
		var exporter = newExporterWithVersion0();
		var appender = attachCapture(ExporterNginxHttp.class.getName());
		try {
			var all = new BServiceInfosVersion();
			all.getOrAddInfos(7L); // 空桶
			Assertions.assertDoesNotThrow(() -> exporter.exportAll("Game.Linkd", all));
			Assertions.assertEquals(0, appender.messages.size(), "全空保持静默");
		} finally {
			detachCapture(ExporterNginxHttp.class.getName(), appender);
			exporter.close();
		}
	}
}
