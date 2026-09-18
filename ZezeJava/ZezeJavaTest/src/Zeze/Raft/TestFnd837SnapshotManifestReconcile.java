package Zeze.Raft;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND8-37回归：_commitSnapshot的Files.move与saveFirstIndex两步持久化非原子，
 * 间隙崩溃后磁盘留下"snapshot.dat=新代次S、rafts表firstIndex=旧值F、日志完整"，
 * 重启无任何检测（lastIndex>=F且边界日志在），lastApplied=F起重放(F..S]即双重
 * 应用（增量不幂等），状态机静默分叉。
 * 修复：快照代次写进快照zip自身（manifest entry，随InstallSnapshot字节流自动
 * 传播），启动以文件内嵌代次对账——S>F时saveFirstIndex(S)并令lastApplied=
 * commitIndex=S跳过已应用段；S<F防御性丢弃快照；旧格式无manifest跳过（不劣于
 * 现状）。构造Raft不起server（无网络/端口占用），直接伪造崩溃后磁盘形态后重启。
 */
@Fast
public class TestFnd837SnapshotManifestReconcile {
	private static final String raftName = "127.0.0.1:26370";
	private static final String dbHome = "a3_TestFnd837SnapshotManifest.raft";

	private Raft raft;

	// 显式DbHome；3节点仅是Raft构造的配置要求，本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:26370" DbHome="a3_TestFnd837SnapshotManifest.raft">
					<node Host="127.0.0.1" Port="26370"/>
					<node Host="127.0.0.1" Port="26371"/>
					<node Host="127.0.0.1" Port="26372"/>
				</raft>
				""");
	}

	private static Zeze.Raft.StateMachine newSm() {
		return new Zeze.Raft.StateMachine() {
			@Override
			public SnapshotResult snapshot(String path) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void loadSnapshot(String path) {
			}
		};
	}

	private Raft newRaft() throws Exception {
		raft = new Raft(newSm(), raftName, newRaftConfig());
		raft.getLogSequence().setWriteOptions(RocksDatabase.getDefaultWriteOptions());
		return raft;
	}

	private void closeRaft() {
		if (raft != null) {
			try {
				raft.getLogSequence().close();
				raft.shutdown();
			} catch (Exception ignore) {
			}
			raft = null;
		}
	}

	@BeforeEach
	public void setUp() {
		Task.tryInitThreadPool();
		LogSequence.deletedDirectoryAndCheck(new File(dbHome), 100);
	}

	@AfterEach
	public void tearDown() {
		closeRaft();
		LogSequence.deleteDirectory(new File(dbHome)); // best-effort
	}

	// 伪造崩溃后磁盘形态：logs=[0,5..12]、rafts.firstIndex=F、snapshot.dat带manifest S。
	private void forgeCrashState(long firstIndex, Long manifest) throws Exception {
		var r = newRaft();
		var ls = r.getLogSequence();
		for (long i = 5; i <= 12; i++)
			ls.saveLog(new RaftLog(1, i, new HeartbeatLog()));
		saveFirstIndex(ls, firstIndex);
		var snap = newZipWithManifest(manifest);
		Files.move(snap, Paths.get(ls.getSnapshotFullName()), StandardCopyOption.REPLACE_EXISTING);
		closeRaft();
	}

	// manifest读写助手往返：无entry→null；写入/覆盖后可读回；文件不存在→null。
	@Test
	public void testManifestWriteRead() throws Exception {
		var zip = newZipWithManifest(null);
		try {
			assertNull(LogSequence.readSnapshotManifest(zip), "无manifest entry返回null");
			LogSequence.writeSnapshotManifest(zip, 42L);
			assertEquals(42L, LogSequence.readSnapshotManifest(zip));
			LogSequence.writeSnapshotManifest(zip, 100L); // 覆盖已有entry
			assertEquals(100L, LogSequence.readSnapshotManifest(zip));
			assertNull(LogSequence.readSnapshotManifest(Paths.get(dbHome, "a3_not_exist.zip")));
		} finally {
			Files.deleteIfExists(zip);
		}
	}

	// 崩溃窗口（S>F）：重启对账推进firstIndex到S并跳过(F..S]重放。
	// 修复前：lastApplied=commitIndex=5，(5..10]已被快照包含却会重放（双重应用）。
	@Test
	public void testCrashWindowReconcileAdvances() throws Exception {
		forgeCrashState(5, 10L);
		var r = newRaft();
		var ls = r.getLogSequence();
		assertEquals(10, ls.getFirstIndex(), "对账必须推进firstIndex到manifest代次");
		assertEquals(10, ls.getLastApplied(), "lastApplied跳过已应用段");
		assertEquals(10, ls.getCommitIndex(), "commitIndex对齐");
		assertEquals(12, ls.getLastIndex(), "lastIndex不受影响");
	}

	// 旧格式快照（无manifest）：跳过对账走现状逻辑。
	@Test
	public void testOldFormatSnapshotSkipsReconcile() throws Exception {
		forgeCrashState(5, null);
		var r = newRaft();
		var ls = r.getLogSequence();
		assertEquals(5, ls.getFirstIndex(), "无代次信息不得对账");
		assertEquals(5, ls.getLastApplied());
	}

	// S<F（正常流程不可达的防御方向）：丢弃快照，firstIndex保持。
	@Test
	public void testManifestBehindDiscardsSnapshot() throws Exception {
		forgeCrashState(5, 3L);
		var r = newRaft();
		var ls = r.getLogSequence();
		assertEquals(5, ls.getFirstIndex(), "S<F不推进firstIndex");
		assertFalse(Files.exists(Paths.get(ls.getSnapshotFullName())), "过期快照必须丢弃");
		assertTrue(Files.exists(Paths.get(dbHome)), "dbHome仍在");
	}

	// saveFirstIndex 是 private，按 TestFnd757 的先例反射调用。
	private static void saveFirstIndex(LogSequence ls, long value) throws Exception {
		Method method = LogSequence.class.getDeclaredMethod("saveFirstIndex", long.class);
		method.setAccessible(true);
		try {
			method.invoke(ls, value);
		} catch (InvocationTargetException e) {
			throw Task.forceThrow(e.getCause());
		}
	}

	// 构造测试用快照zip（manifest为null时不含manifest entry）。
	private static Path newZipWithManifest(Long manifest) throws Exception {
		var zipPath = Files.createTempFile("a3_fnd837_snap", ".zip");
		Files.delete(zipPath); // 空文件不是合法zip，删除后由ZipFileSystem新建
		try (var zipFs = FileSystems.newFileSystem(zipPath, Map.of("create", "true"))) {
			Files.writeString(zipFs.getPath("placeholder"), "fnd837");
			if (manifest != null)
				Files.writeString(zipFs.getPath(LogSequence.snapshotManifestEntryName), Long.toString(manifest));
		}
		return zipPath;
	}
}
