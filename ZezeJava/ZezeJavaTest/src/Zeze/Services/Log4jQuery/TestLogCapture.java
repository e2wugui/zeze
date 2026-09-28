package Zeze.Services.Log4jQuery;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.Property;

/**
 * FND22：Log4jFileManager告警捕获（GD-C03"存活链接跳过清理"、GD-C05"宽限可观测"断言用）。
 * Appender直接挂在具体Logger上（与root配置无关）。level必须经Configurator.setLevel设置：
 * core Logger.setLevel在同JVM首次调用时不生效（实测首个capture实例连WARN都收不到，
 * 后续实例才正常——"预热"竞态），Configurator走LoggerConfig显式落位，首个实例即可靠。
 * close恢复原level并摘除appender，不污染其他测试。
 */
final class TestLogCapture implements AutoCloseable {
	private final Logger logger;
	private final CaptureAppender appender = new CaptureAppender();
	private final Level savedLevel;

	TestLogCapture(Class<?> loggingClass, Level level) {
		logger = (Logger)LogManager.getLogger(loggingClass);
		savedLevel = logger.getLevel();
		Configurator.setLevel(logger.getName(), level);
		appender.start();
		logger.addAppender(appender);
	}

	List<String> messages() {
		return appender.messages;
	}

	boolean anyMessageContains(String fragment) {
		for (var message : appender.messages)
			if (message.contains(fragment))
				return true;
		return false;
	}

	@Override
	public void close() {
		logger.removeAppender(appender);
		Configurator.setLevel(logger.getName(), savedLevel == null ? Level.ERROR : savedLevel);
		appender.stop();
	}

	private static final class CaptureAppender extends AbstractAppender {
		private final List<String> messages = new CopyOnWriteArrayList<>();

		CaptureAppender() {
			super("fnd22-capture", null, null, true, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(LogEvent event) {
			messages.add(event.getMessage().getFormattedMessage());
		}
	}
}
