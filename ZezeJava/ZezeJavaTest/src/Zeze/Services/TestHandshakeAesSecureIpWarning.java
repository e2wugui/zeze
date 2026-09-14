package Zeze.Services;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import Zeze.Config;
import Zeze.Net.ServiceConf;
import Zeze.Services.Handshake.Constant;
import Zeze.Util.Task;
import harness.Fast;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 方案A告警档回归：EncryptType=Aes 且未显式配置 SecureIp 时，握手服务启动必须
 * 显著告警——Aes 会话密钥由连接地址派生（服务器取本端地址、客户端取对端地址），NAT/端口映射
 * 下两侧地址不同，握手必然失败并反复重连。已配置 SecureIp 或使用其他加密类型时不告警。
 * 自包含（无监听无连接，仅捕获启动日志），标 @Fast。
 */
@Fast
public class TestHandshakeAesSecureIpWarning {

	private static final class CaptureAppender extends AbstractAppender {
		private final List<String> messages = new CopyOnWriteArrayList<>();

		CaptureAppender() {
			super("TestAesSecureIpWarnCapture", null, null, true, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(LogEvent event) {
			messages.add(event.getMessage().getFormattedMessage());
		}
	}

	private static List<String> startHandshakeServerAndCaptureWarn(String serviceName, ServiceConf sconf)
			throws Exception {
		var conf = new Config();
		conf.getServiceConfMap().put(serviceName, sconf);
		var server = new HandshakeServer(serviceName, conf);

		var logger = (Logger)LogManager.getLogger(HandshakeBase.class);
		var capture = new CaptureAppender();
		capture.start();
		logger.addAppender(capture);
		try {
			server.start();
		} finally {
			server.stop();
			logger.removeAppender(capture);
			capture.stop();
		}
		return capture.messages;
	}

	@Test
	public void testAesWithoutSecureIpWarnsOnStart() throws Exception {
		Task.tryInitThreadPool();
		var sconf = new ServiceConf();
		sconf.getHandshakeOptions().setEncryptType(Constant.eEncryptTypeAes);

		var messages = startHandshakeServerAndCaptureWarn("TestAesSecureIpWarnNoConf", sconf);

		var warned = messages.stream().filter(m -> m.contains("EncryptType=Aes without SecureIp") && m.contains("TestAesSecureIpWarnNoConf")).count();
		Assertions.assertEquals(1L, warned, () -> "expect one Aes-without-SecureIp warning, got: " + messages);
	}

	@Test
	public void testAesWithSecureIpDoesNotWarn() throws Exception {
		Task.tryInitThreadPool();
		var sconf = new ServiceConf();
		sconf.getHandshakeOptions().setEncryptType(Constant.eEncryptTypeAes);
		sconf.getHandshakeOptions().setSecureIp(InetAddress.getByName("203.0.113.1").getAddress());

		var messages = startHandshakeServerAndCaptureWarn("TestAesSecureIpWarnConfigured", sconf);

		Assertions.assertTrue(messages.stream().noneMatch(m -> m.contains("EncryptType=Aes without SecureIp")),
				() -> "configured SecureIp must not warn, got: " + messages);
	}

	@Test
	public void testAesNoSecureIpModeDoesNotWarn() throws Exception {
		Task.tryInitThreadPool();
		var sconf = new ServiceConf();
		sconf.getHandshakeOptions().setEncryptType(Constant.eEncryptTypeAesNoSecureIp);

		var messages = startHandshakeServerAndCaptureWarn("TestAesNoSecureIpModeNoWarn", sconf);

		Assertions.assertTrue(messages.stream().noneMatch(m -> m.contains("EncryptType=Aes without SecureIp")),
				() -> "AesNoSecureIp mode must not warn, got: " + messages);
	}

	@Test
	public void testRsaAesDoesNotWarn() throws Exception {
		Task.tryInitThreadPool();
		var sconf = new ServiceConf();
		sconf.getHandshakeOptions().setEncryptType(Constant.eEncryptTypeRsaAes);

		var messages = startHandshakeServerAndCaptureWarn("TestRsaAesNoWarn", sconf);

		Assertions.assertTrue(messages.stream().noneMatch(m -> m.contains("EncryptType=Aes without SecureIp")),
				() -> "RsaAes mode must not warn, got: " + messages);
	}
}
