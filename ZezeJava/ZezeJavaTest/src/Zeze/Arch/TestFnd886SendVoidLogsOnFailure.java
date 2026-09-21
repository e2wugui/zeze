package Zeze.Arch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.Gen.GenModule;
import Zeze.Arch.RedirectToServer;
import Zeze.Builtin.ProviderDirect.ModuleRedirect;
import Zeze.Config;
import harness.Fast;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * FND8-86回归：void redirect生成代码直接丢弃Rpc.Send的布尔返回——Send在socket失效
 * （null/已关闭/发送缓冲溢出背压）时不抛异常只返回false，该次调用无声消失；socket层
 * error日志无redirect归因（丢了哪个方法无从知晓），与本地回环runVoid失败有日志不对称。
 * 修复：RedirectBase.sendVoid封装，失败记带方法名的error日志（at-most-once语义不变，
 * 内建方法的既有自愈补偿不变）；生成模板改为调用sendVoid。
 */
@Fast
@Isolated // 全局日志系统捕获appender挂载期间独占运行
public class TestFnd886SendVoidLogsOnFailure {

	public static class VoidModule {
		public static final int ModuleId = 8861;
		public static final String ModuleFullName = "TestFnd886.VoidModule";

		@RedirectToServer
		public void cancel(int hash, String id) {
		}
	}

	@TempDir
	Path tempDir;

	private final AppBase dummyApp = new AppBase() {
		@Override
		public @Nullable Application getZeze() {
			return null; // 离线生成只读app.getClass()匹配构造器，不触碰zeze
		}
	};

	private static final class CaptureAppender extends AbstractAppender {
		final List<String> messages = new CopyOnWriteArrayList<>();

		CaptureAppender() {
			super("a7fnd886Capture", null, null, true, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(LogEvent event) {
			messages.add(event.getMessage().getFormattedMessage());
		}
	}

	/** Send失败（socket为null必失败）必须记带方法归因的error日志，不抛异常。 */
	@Test
	public void testSendVoidFailureLogged() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setNoDatabase(true);
		var app = new Application("a7fnd886", config);
		new ProviderApp(app); // 哑构造：设置app.redirect（RedirectBase）
		var redirect = app.redirect;

		var logger = (Logger)LogManager.getLogger(RedirectBase.class);
		var appender = new CaptureAppender();
		appender.start();
		logger.addAppender(appender);
		try {
			redirect.sendVoid(null, new ModuleRedirect(), "a7fnd886:cancel");
		} finally {
			logger.removeAppender(appender);
			appender.stop();
		}
		Assertions.assertTrue(appender.messages.stream().anyMatch(m -> m.contains("a7fnd886:cancel")),
				"失败必须记带方法归因的日志，实际: " + appender.messages);
	}

	/** 生成模板：void方法必须经sendVoid发送（不再直接丢弃Send返回值）。 */
	@Test
	public void testGeneratedCodeUsesSendVoid() throws Exception {
		GenModule.instance.generateRedirectSources(tempDir.toString(), dummyApp, new Class<?>[]{VoidModule.class}, false);
		var file = tempDir.resolve(GenModule.REDIRECT_PREFIX + VoidModule.class.getName().replace('.', '_') + ".java");
		Assertions.assertTrue(Files.exists(file), "生成文件必须存在");
		var content = Files.readString(file);
		Assertions.assertTrue(content.contains("_redirect_.sendVoid(_t_, _p_, \"" + VoidModule.ModuleFullName
						+ ":cancel\");"),
				"void redirect必须经sendVoid发送（FND8-86：原先直接丢弃Send返回值）");
		Assertions.assertFalse(content.contains("_p_.Send(_t_, null);"),
				"不得再生成裸Send调用");
	}
}
