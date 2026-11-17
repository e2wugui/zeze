package Zeze.Dbh2;

import java.nio.file.Files;
import java.nio.file.Path;
import Zeze.Config;
import Zeze.Util.Task;
import harness.Extra;
import harness.Fast;
import harness.TestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * "7788集成形态不监听"立案配套：本地提交的探测端点=Zeze.Dbh2.Commit Acceptor，
 * commitServiceAcceptor发布的事实端点（随prepare写进桶侧并持久化）。Port=0自动分配
 * （多App同JVM同配置各得唯一端点，如Simulate 5-App）必须钉住三件事：
 * 未启动不发布死0端口（沿用"Acceptor Not Set"拒绝）；启动后发布实际绑定端口且可探测；
 * 同JVM两个manager各得互异端口。
 */
@Fast
@Extra
public class TestCommitAcceptorPort0AutoAssign {

	private static final String COMMIT_ROCKS_HOME_PROPERTY = "Dbh2CommitRocksHome"; // 与CommitRocks同源

	private static Path writeConfig(Path tempDir, String name) throws Exception {
		var xml = tempDir.resolve(name);
		Files.writeString(xml, """
				<?xml version="1.0" encoding="utf-8"?>
				<zeze>
					<ServiceConf Name="Zeze.Dbh2.Commit">
						<Acceptor Ip="127.0.0.1" Port="0"/>
					</ServiceConf>
				</zeze>
				""");
		return xml;
	}

	/** CommitRocks的home只在本构造窗口读取（Dbh2CommitRocksHome+serverId），构造完成即还原。 */
	private static Dbh2AgentManager newManager(Path home, Path configXml, int serverId) throws Exception {
		var prior = System.getProperty(COMMIT_ROCKS_HOME_PROPERTY);
		System.setProperty(COMMIT_ROCKS_HOME_PROPERTY, home.toString());
		try {
			return new Dbh2AgentManager(new Dbh2AgentStubSupport.NullServiceAgent(),
					Config.load(configXml.toString()), serverId);
		} finally {
			if (null != prior)
				System.setProperty(COMMIT_ROCKS_HOME_PROPERTY, prior);
			else
				System.clearProperty(COMMIT_ROCKS_HOME_PROPERTY);
		}
	}

	@Test
	public void testPort0PublishedAsActualBoundPort(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var manager1 = newManager(tempDir.resolve("rocks1"), writeConfig(tempDir, "commit0a.xml"), 845);
		var manager2 = newManager(tempDir.resolve("rocks2"), writeConfig(tempDir, "commit0b.xml"), 846);
		try {
			// 未启动：Port=0不得发布为探测端点（死端点，桶侧探测永远失败）。
			Assertions.assertThrows(RuntimeException.class, manager1::commitServiceAcceptor,
					"Port=0未启动必须拒绝发布（Commit Query Acceptor Not Set）");

			manager1.start();
			manager2.start();

			var ep1 = manager1.commitServiceAcceptor();
			var ep2 = manager2.commitServiceAcceptor();

			Assertions.assertEquals("127.0.0.1", ep1.getKey());
			Assertions.assertNotEquals(0, ep1.getValue(), "Port=0启动后必须发布实际绑定端口");
			Assertions.assertNotEquals(0, ep2.getValue());
			Assertions.assertNotEquals(ep1.getValue(), ep2.getValue(), "同JVM两个manager必须各得唯一端口");
			Assertions.assertTrue(TestEnv.portReachable(ep1.getKey(), ep1.getValue()), "端点1必须真实监听");
			Assertions.assertTrue(TestEnv.portReachable(ep2.getKey(), ep2.getValue()), "端点2必须真实监听");
		} finally {
			manager1.stop();
			manager2.stop();
		}
	}
}
