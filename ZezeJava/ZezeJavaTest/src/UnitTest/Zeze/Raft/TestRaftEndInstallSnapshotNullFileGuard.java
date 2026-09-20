package UnitTest.Zeze.Raft;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Raft.Server;
import Zeze.Util.Task;

/**
 * R2-F1 回归（可达半侧）：startInstallSnapshot打开快照文件失败时留下file==null的
 * 半初始化installSnapshotting条目，endInstallSnapshot对state.getFile()直接close会NPE；
 * NPE还会沿cancelAllInstallSnapshot打断Raft.shutdown后续的logSequence.close。
 * 修复①：endInstallSnapshot对file==null防御（本测试直接验证）；
 * 修复②：startInstallSnapshot把open+readLog包进try/catch、失败调endInstallSnapshot回收
 * （需注入快照文件IO异常，无法确定性构造，见台账）。
 * InstallSnapshotState与setInstallSnapshotState为包私有，反射构造（仓内测试惯例）。
 */
@Fast
public class TestRaftEndInstallSnapshotNullFileGuard {
	private static final String raftName = "127.0.0.1:17761";
	private static final String dbHome = "TestRaftEndInstallSnapshotNullFile.raft";

	private Rocks rocks;

	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17761" DbHome="TestRaftEndInstallSnapshotNullFile.raft">
					<node Host="127.0.0.1" Port="17761"/>
					<node Host="127.0.0.1" Port="17762"/>
					<node Host="127.0.0.1" Port="17763"/>
				</raft>
				""");
	}

	@BeforeEach
	public void setUp() throws Exception {
		Task.tryInitThreadPool();
		LogSequence.deletedDirectoryAndCheck(new File(dbHome), 100);
		rocks = new Rocks(raftName, RocksMode.Pessimism, newRaftConfig(), new Config(), false);
	}

	@AfterEach
	public void tearDown() throws Exception {
		rocks.close();
		LogSequence.deleteDirectory(new File(dbHome)); // best-effort
	}

	/**
	 * file==null的半初始化条目：endInstallSnapshot必须安全回收（修复前state.getFile()
	 * .close()抛NPE），且条目从installSnapshotting移除、connector状态复位。
	 */
	@Test
	public void testEndInstallSnapshotWithNullFileSafe() throws Exception {
		var ls = rocks.getRaft().getLogSequence();

		// 构造半初始化条目：InstallSnapshotState（file==null，即open失败留下的状态）
		var stateClass = Class.forName("Zeze.Raft.InstallSnapshotState");
		Constructor<?> stateCtor = stateClass.getDeclaredConstructor();
		stateCtor.setAccessible(true);
		var state = stateCtor.newInstance();

		var cex = new Server.ConnectorEx("127.0.0.1", 17762);
		Method setState = Server.ConnectorEx.class.getDeclaredMethod("setInstallSnapshotState", stateClass);
		setState.setAccessible(true);
		setState.invoke(cex, state);
		ls.getInstallSnapshotting().put(cex.getName(), cex);

		// 修复前此处NPE（state.getFile().close()）
		Assertions.assertDoesNotThrow(() -> ls.endInstallSnapshot(cex),
				"file==null的半初始化条目必须被安全回收，不得NPE");
		Assertions.assertFalse(ls.getInstallSnapshotting().containsKey(cex.getName()),
				"回收后条目必须从installSnapshotting移除");
	}
}
