package Zeze.Dbh2;

import harness.Extra;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 快照失败路径的checkpoint目录清理回归：Dbh2StateMachine.snapshot先建checkpoint_&lt;ts&gt;目录
 * 再backup/zip/commitSnapshot，任一步失败时该目录必须被删除（try-finally）。目录名含时间戳
 * 不复用，失败遗留无任何回收路径，持续故障（备份盘满/IO错误）下随失败次数无界累积直至打满数据盘。
 * 故障注入：backup路径被同名普通文件占据 → RocksDatabase.backup(BackupEngine.open)必然失败。
 * 拓扑：单桶raft三节点端口19260-19262（对齐TestGAC02PrefixWalkPositioning），空桶即可
 * （checkpoint与backup不依赖数据量）。
 */
@Fast
@Extra
public class TestSnapshotFailureCleansCheckpointDir {
	private static final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();

	private static final String RAFT = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19260"/>
				<node Host="127.0.0.1" Port="19261"/>
				<node Host="127.0.0.1" Port="19262"/>
			</raft>
			""";

	private static List<Zeze.Dbh2.Dbh2> startBucket(RocksDatabase database, Path tempDir) {
		var nodes = new ArrayList<Zeze.Dbh2.Dbh2>();
		for (var config : RaftConfig.loadFromString(RAFT).getNodes().values()) {
			var nodeConfig = RAFT.replaceFirst("<raft ",
					"<raft DbHome=\"" + java.util.regex.Matcher.quoteReplacement(String.valueOf(tempDir.resolve(config.getName().replace(':', '_')))) + "\" ");
			nodes.add(new Zeze.Dbh2.Dbh2(null, config.getName(), database,
					RaftConfig.loadFromString(nodeConfig), null, false, taskOneByOne));
		}
		return nodes;
	}

	private static int countCheckpointDirs(Path dbHome) {
		var dirs = dbHome.toFile().listFiles((d, name) -> name.startsWith("checkpoint_"));
		return null == dirs ? 0 : dirs.length;
	}

	@Test
	public void testSnapshotFailureStillRemovesCheckpointDir(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var rocks = new RocksDatabase(tempDir.resolve("dbh2SnapshotFailClean").toString());
		var nodes = startBucket(rocks, tempDir);
		try {
			var node = nodes.get(0);
			var dbHome = Path.of(node.getStateMachine().getDbHome());

			// 故障注入：backup目标路径被普通文件占据，backup必失败（模拟备份盘满/IO错误）。
			var backupPath = dbHome.resolve("backup");
			Files.writeString(backupPath, "occupied");
			Assertions.assertTrue(Files.isRegularFile(backupPath));

			Assertions.assertThrows(Exception.class,
					() -> node.getStateMachine().snapshot(tempDir.resolve("snapshot.zip").toString()),
					"backup目录被文件占据时snapshot必须失败");

			// 失败离开路径也必须清理本次checkpoint目录（bug时遗留checkpoint_<ts>，无界累积）。
			Assertions.assertEquals(0, countCheckpointDirs(dbHome),
					"snapshot失败后不得遗留checkpoint目录");
		} finally {
			for (var dbh2 : nodes) {
				dbh2.close();
				LogSequence.deleteDirectory(new File(dbh2.getRaft().getRaftConfig().getDbHome()));
			}
			rocks.close();
		}
	}
}
