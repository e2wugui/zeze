package Zeze.Dbh2;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import Zeze.Config;
import Zeze.Dbh2.Dbh2AgentManager;
import Zeze.Util.KV;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND19 GA-D03回归：远程提交模式配置直连（拍板方案B）。
 * Dbh2LocalCommit=false时必须显式配置Dbh2Config的CommitServerAddress（单实例），
 * 未配置在Dbh2AgentManager构造期fail-fast（bug时构造成功，第一次commit走
 * choiceCommitServer抛UnsupportedOperationException，应用事务全部失败）；
 * choiceCommitServer改为返回配置直连地址。钉住三件事：
 * 未配置构造期报配置错误；配置后choiceCommitServer返回配置的host:port；
 * 地址格式非法（缺端口）解析期报错。
 */
@Fast
public class TestCommitServerFailFast {

	private static Path writeConfig(Path tempDir, String address) throws Exception {
		var xml = tempDir.resolve("commitServerFailFast.xml");
		Files.writeString(xml, """
				<?xml version="1.0" encoding="utf-8"?>
				<zeze Dbh2LocalCommit="false">
					<CustomizeConf Name="Dbh2Config" CommitServerAddress="%s"/>
				</zeze>
				""".formatted(address));
		return xml;
	}

	@SuppressWarnings("unchecked")
	private static KV<String, Integer> choiceCommitServer(Dbh2AgentManager manager) throws Exception {
		Method method = Dbh2AgentManager.class.getDeclaredMethod("choiceCommitServer");
		method.setAccessible(true);
		return (KV<String, Integer>)method.invoke(manager);
	}

	@Test
	public void testUnconfiguredFailsFastAtConstruction() {
		// 远程提交模式但未配置CommitServerAddress：构造期必须报配置错误。
		var config = new Config();
		config.setDbh2LocalCommit(false);
		var ex = Assertions.assertThrows(RuntimeException.class,
				() -> new Dbh2AgentManager(new Dbh2AgentStubSupport.NullServiceAgent(), config));
		Assertions.assertTrue(ex.getMessage().contains("CommitServerAddress"),
				"报错必须指明缺少CommitServerAddress配置: " + ex.getMessage());
	}

	@Test
	public void testConfiguredDirectConnect(@TempDir Path tempDir) throws Exception {
		// 配置直连：choiceCommitServer返回配置的host:port（bug时抛UnsupportedOperationException）。
		var config = Config.load(writeConfig(tempDir, "127.0.0.1:12345").toString());
		var manager = new Dbh2AgentManager(new Dbh2AgentStubSupport.NullServiceAgent(), config);
		try {
			Assertions.assertEquals("127.0.0.1", manager.getDbh2Config().getCommitServerHost());
			Assertions.assertEquals(12345, manager.getDbh2Config().getCommitServerPort());
			var choice = choiceCommitServer(manager);
			Assertions.assertEquals("127.0.0.1", choice.getKey());
			Assertions.assertEquals(12345, choice.getValue());
		} finally {
			manager.stop();
		}
	}

	@Test
	public void testBadAddressRejectedAtParse(@TempDir Path tempDir) throws Exception {
		// 地址缺端口：Dbh2Config解析期报格式错误。
		var config = Config.load(writeConfig(tempDir, "noPortHere").toString());
		var ex = Assertions.assertThrows(RuntimeException.class,
				() -> new Dbh2AgentManager(new Dbh2AgentStubSupport.NullServiceAgent(), config));
		Assertions.assertTrue(ex.getMessage().contains("host:port"),
				"报错必须指明host:port格式: " + ex.getMessage());
	}
}
