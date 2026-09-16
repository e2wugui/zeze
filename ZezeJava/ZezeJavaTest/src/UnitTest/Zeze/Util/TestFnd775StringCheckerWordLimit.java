package UnitTest.Zeze.Util;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import Zeze.Util.StringChecker;
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
 * FND7-75：addLine 词条长度上限（MAX_WORD_LENGTH=1024）。
 * calFail 按词条长度递归（每字符约3帧，栈深=词长），超长词条（词库文件被污染/误粘整段
 * 文本）会以 StackOverflowError 击穿 reload——实测4096字符在默认栈（512KB~1MB）已处
 * SOE 边缘（本测试首轮即复现），故上限取1024留足余量。
 * 断言：超长词条被拒（不计数、不进 trie）+ warn 日志；上限内词条与正常词不受影响。
 */
@Fast
public class TestFnd775StringCheckerWordLimit {

	/** 捕获型appender：挂到指定logger名的独立LoggerConfig（不干扰并行测试），断言warn内容。 */
	private static final class CapturingAppender extends AbstractAppender {
		private final List<String> messages = new CopyOnWriteArrayList<>();

		CapturingAppender() {
			super("TestFnd775Capture", null, null, true, Property.EMPTY_ARRAY);
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

	/** 超长词条（5000字符）被拒：reload计数不含它、不进trie、记warn。 */
	@Test
	public void testOverlongWordRejectedWithWarn() throws IOException {
		var appender = attachCapture(StringChecker.class.getName());
		try {
			var c = new StringChecker();
			c.addNewLine("x".repeat(5000)); // 超长词条：拒绝
			c.addNewLine("正常词"); // 正常词条对照
			var n = c.reload(null);
			Assertions.assertEquals(1, n, "超长词条被拒，仅正常词条计数");
			Assertions.assertFalse(c.contains("x".repeat(100)), "超长词条未进trie");
			Assertions.assertTrue(c.contains("正常词"), "正常词条不受影响");
			Assertions.assertTrue(appender.messages.stream().anyMatch(
							m -> m.contains("word too long") && m.contains("5000")),
					"超长词条拒绝须warn: " + appender.messages);
		} finally {
			detachCapture(StringChecker.class.getName(), appender);
		}
	}

	/** 边界：恰好1024字符的词条被接受；正常词的contains/replace行为不变。 */
	@Test
	public void testBoundaryAndNormalUnaffected() throws IOException {
		var c = new StringChecker();
		c.addNewLine("y".repeat(1024)); // 恰好上限：接受
		c.addNewLine("敏感词");
		Assertions.assertEquals(2, c.reload(null), "1024恰好上限不被拒");
		Assertions.assertTrue(c.contains("y".repeat(1024)));
		Assertions.assertEquals("a***b", c.replace("a敏感词b", '*'));
	}
}
